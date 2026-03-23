import os
import json
from datetime import datetime

def generate_summary(f):
    path = f["path"]
    name = os.path.basename(path)
    lang = f["language"]
    fc = f["metrics"]["functionCount"]
    cc = f["metrics"]["classCount"]
    
    parts = path.split('/')
    module = parts[0]
    
    role = "file"
    if "controller" in path.lower(): role = "API Controller"
    elif "service" in path.lower(): role = "Business Service"
    elif "repository" in path.lower(): role = "Data Access Repository"
    elif "dto" in path.lower(): role = "Data Transfer Object"
    elif "config" in path.lower(): role = "Configuration module"
    elif "filter" in path.lower(): role = "Request/Response Filter"
    elif "component" in path.lower(): role = "UI Component"
    elif "page" in path.lower(): role = "UI Page"
    
    summary = f"A {lang} {role} in the {module} service."
    if cc > 0:
        summary += f" Defines {cc} class(es)."
    if fc > 0:
        summary += f" Contains {fc} function(s)."
        
    return summary

def generate_tags(f):
    path = f["path"].lower()
    tags = ["code"]
    
    parts = f["path"].split('/')
    tags.append(parts[0]) # module tag
    
    if "controller" in path: tags.append("api-handler")
    if "service" in path: tags.append("service")
    if "repository" in path: tags.append("data-access")
    if "dto" in path: tags.append("data-transfer")
    if "config" in path: tags.append("config")
    if "filter" in path: tags.append("middleware")
    if "app" in path and "frontend" in path: tags.append("ui-page")
    if "component" in path: tags.append("ui-component")
    if "test" in path: tags.append("test")
    if "dto" in path: tags.append("data-model")
    
    # Ensure at least 3 tags
    if len(tags) < 3:
        tags.extend(["logic", "implementation", "module"][:3-len(tags)])
        
    return list(set(tags))[:5]

def get_complexity(f):
    lines = f["totalLines"]
    if lines < 50: return "simple"
    if lines < 200: return "moderate"
    return "complex"

def main():
    extract_path = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\intermediate\full-extract-results.json'
    scan_result_path = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\intermediate\scan-result.json'
    
    with open(extract_path, 'r', encoding='utf-8') as f:
        extract_data = json.load(f)
    with open(scan_result_path, 'r', encoding='utf-8') as f:
        scan_data = json.load(f)
        
    nodes = []
    edges = []
    
    all_file_paths = [r["path"] for r in extract_data["results"]]
    
    for r in extract_data["results"]:
        path = r["path"]
        file_id = f"file:{path}"
        
        # File Node
        nodes.append({
            "id": file_id,
            "type": "file",
            "name": os.path.basename(path),
            "filePath": path,
            "summary": generate_summary(r),
            "tags": generate_tags(r),
            "complexity": get_complexity(r)
        })
        
        # Classes
        for cls in r["classes"]:
            cls_id = f"class:{path}:{cls['name']}"
            nodes.append({
                "id": cls_id,
                "type": "class",
                "name": cls["name"],
                "filePath": path,
                "lineRange": [cls["startLine"], cls["endLine"]],
                "summary": f"Class {cls['name']} defined in {path}.",
                "tags": ["class", "structure"],
                "complexity": "simple"
            })
            edges.append({
                "source": file_id, "target": cls_id, "type": "contains", "direction": "forward", "weight": 1.0
            })
            
        # Functions
        for fn in r["functions"]:
            # Only significant or exported
            is_exported = any(ex["name"] == fn["name"] for ex in r["exports"])
            if fn["endLine"] - fn["startLine"] > 5 or is_exported:
                fn_id = f"func:{path}:{fn['name']}"
                nodes.append({
                    "id": fn_id, "type": "function", "name": fn["name"], "filePath": path,
                    "lineRange": [fn["startLine"], fn["endLine"]],
                    "summary": f"Function {fn['name']} implemented in {path}.",
                    "tags": ["function", "logic"],
                    "complexity": "simple"
                })
                edges.append({
                    "source": file_id, "target": fn_id, "type": "contains", "direction": "forward", "weight": 1.0
                })
                if is_exported:
                    edges.append({
                        "source": file_id, "target": fn_id, "type": "exports", "direction": "forward", "weight": 0.8
                    })

        # Imports -> Edges
        for imp in r["imports"]:
            src = imp["source"]
            # Heuristic resolution for Java package to file path
            resolved_target = None
            if r["language"] == "java":
                # package com.ctuconnect.filter.JwtAuthenticationFilter -> api-gateway/src/main/java/com/ctuconnect/filter/JwtAuthenticationFilter.java
                # This is tricky but we can try matching against all_file_paths
                target_suffix = src.replace('.', '/') + ".java"
                for p in all_file_paths:
                    if p.endswith(target_suffix):
                        resolved_target = p
                        break
            elif r["language"] in ('typescript', 'javascript'):
                if src.startswith('.'):
                    # Resolve relative
                    base_dir = os.path.dirname(path)
                    target_path = os.path.normpath(os.path.join(base_dir, src)).replace('\\', '/')
                    # Check with extensions
                    for ext in ['.ts', '.tsx', '.js', '.jsx', '/index.ts', '/index.tsx']:
                        if target_path + ext in all_file_paths:
                            resolved_target = target_path + ext
                            break
                        if target_path.endswith(ext) and target_path in all_file_paths:
                            resolved_target = target_path
                            break
            
            if resolved_target:
                edges.append({
                    "source": file_id, "target": f"file:{resolved_target}", "type": "imports", "direction": "forward", "weight": 0.7
                })

    # Deduplicate Edges
    unique_edges = {}
    for e in edges:
        key = f"{e['source']}|{e['target']}|{e['type']}"
        if key not in unique_edges:
            unique_edges[key] = e
    edges = list(unique_edges.values())

    # Layers
    layers = [
        {"id": "layer:frontend", "name": "Frontend Applications", "description": "Admin and Client React/Next.js applications.", "nodeIds": [n["id"] for n in nodes if "frontend" in n["id"]]},
        {"id": "layer:gateway", "name": "API Gateway", "description": "Spring Cloud Gateway routing and security filters.", "nodeIds": [n["id"] for n in nodes if "api-gateway" in n["id"]]},
        {"id": "layer:auth", "name": "Authentication Service", "description": "User authentication, JWT, and security logic.", "nodeIds": [n["id"] for n in nodes if "auth-service" in n["id"]]},
        {"id": "layer:services", "name": "Microservices", "description": "Core business logic services (User, Chat, Post, etc.)", "nodeIds": [n["id"] for n in nodes if any(x in n["id"] for x in ["user-service", "chat-service", "post-service", "media-service"])]},
        {"id": "layer:ai", "name": "AI & Recommendation", "description": "PhoBERT-powered recommendation and specialized services.", "nodeIds": [n["id"] for n in nodes if "recommend-service" in n["id"]]}
    ]

    # Tour
    tour = [
        {"order": 1, "title": "Entry Point: API Gateway", "description": "The gateway handles all incoming requests and applies JWT authentication filters.", "nodeIds": [n["id"] for n in nodes if "ApiGatewayApplication.java" in n["id"] or "JwtAuthenticationFilter.java" in n["id"]][:3]},
        {"order": 2, "title": "Identity: Auth Service", "description": "Manages user registration, login, and token generation.", "nodeIds": [n["id"] for n in nodes if "AuthServiceApplication.java" in n["id"] or "AuthController.java" in n["id"]][:3]},
        {"order": 3, "title": "Core Domain: User Service", "description": "Handles user profiles and relationships.", "nodeIds": [n["id"] for n in nodes if "UserService.java" in n["id"] or "UserController.java" in n["id"]][:3]},
        {"order": 4, "title": "Client Frontend", "description": "The Next.js application for students and faculty.", "nodeIds": [n["id"] for n in nodes if "client-frontend/src/app/page.tsx" in n["id"]][:1]}
    ]

    graph = {
        "version": "1.0.0",
        "project": {
            "name": scan_data["name"],
            "languages": scan_data["languages"],
            "frameworks": scan_data["frameworks"],
            "description": scan_data["description"],
            "analyzedAt": datetime.now().isoformat(),
            "gitCommitHash": "ad7d6670829220aa6385a89b7d6eb8001c459d9d"
        },
        "nodes": nodes,
        "edges": edges,
        "layers": layers,
        "tour": tour
    }

    final_path = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\knowledge-graph.json'
    with open(final_path, 'w', encoding='utf-8') as f:
        json.dump(graph, f, indent=2, ensure_ascii=False)
        
    meta = {
        "lastAnalyzedAt": datetime.now().isoformat(),
        "gitCommitHash": "ad7d6670829220aa6385a89b7d6eb8001c459d9d",
        "version": "1.0.0",
        "analyzedFiles": len(extract_data["results"])
    }
    with open(r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\meta.json', 'w', encoding='utf-8') as f:
        json.dump(meta, f, indent=2)

    print(f"Graph assembled: {len(nodes)} nodes, {len(edges)} edges saved to {final_path}")

if __name__ == '__main__':
    main()
