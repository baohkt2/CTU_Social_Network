import os
import sys
import json
import re

def extract_java(content, file_path):
    functions = []
    classes = []
    imports = []
    exports = []
    
    # Simple regex for Java
    for i, line in enumerate(content.splitlines()):
        line_num = i + 1
        # Imports
        m_imp = re.match(r'^\s*import\s+(?:static\s+)?([\w\.]+);', line)
        if m_imp:
            imports.append({"source": m_imp.group(1), "line": line_num})
        
        # Classes
        m_cls = re.search(r'(?:public|protected|private|static|\s)*class\s+(\w+)', line)
        if m_cls:
            classes.append({"name": m_cls.group(1), "startLine": line_num, "endLine": line_num + 5}) # dummy end
            exports.append({"name": m_cls.group(1), "line": line_num, "isDefault": False})

        # Methods (simplified)
        m_fn = re.search(r'(?:public|protected|private|static|\s)*[\w<>]+\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w,\s]+)?\s*{', line)
        if m_fn and m_fn.group(1) not in ('if', 'for', 'while', 'switch', 'catch', 'synchronized'):
            functions.append({"name": m_fn.group(1), "startLine": line_num, "endLine": line_num + 3})
            
    return {
        "functions": functions,
        "classes": classes,
        "imports": imports,
        "exports": exports,
        "metrics": {
            "functionCount": len(functions),
            "classCount": len(classes),
            "importCount": len(imports),
            "exportCount": len(exports)
        }
    }

def extract_ts_js(content, file_path):
    functions = []
    classes = []
    imports = []
    exports = []
    
    for i, line in enumerate(content.splitlines()):
        line_num = i + 1
        # Imports
        m_imp = re.match(r'^\s*import\s+.*from\s+[\'"](.*?)[\'"]', line)
        if m_imp:
            imports.append({"source": m_imp.group(1), "line": line_num})
            
        # Exports
        if 'export ' in line:
            m_exp = re.search(r'export\s+(?:default\s+)?(?:class|function|const|let|var|type|interface)\s+(\w+)', line)
            if m_exp:
                exports.append({"name": m_exp.group(1), "line": line_num, "isDefault": 'default' in line})

        # Functions
        m_fn = re.search(r'function\s+(\w+)\s*\(', line)
        if not m_fn:
            m_fn = re.search(r'const\s+(\w+)\s*=\s*\(.*?\)\s*=>', line)
        if m_fn:
            functions.append({"name": m_fn.group(1), "startLine": line_num, "endLine": line_num + 3})
            
        # Classes
        m_cls = re.search(r'class\s+(\w+)', line)
        if m_cls:
            classes.append({"name": m_cls.group(1), "startLine": line_num, "endLine": line_num + 5})

    return {
        "functions": functions,
        "classes": classes,
        "imports": imports,
        "exports": exports,
        "metrics": {
            "functionCount": len(functions),
            "classCount": len(classes),
            "importCount": len(imports),
            "exportCount": len(exports)
        }
    }

def extract_python(content, file_path):
    functions = []
    classes = []
    imports = []
    exports = []
    
    for i, line in enumerate(content.splitlines()):
        line_num = i + 1
        # Imports
        if line.startswith('import ') or line.startswith('from '):
            imports.append({"source": line.strip(), "line": line_num})
            
        # Functions
        m_fn = re.match(r'^\s*def\s+(\w+)', line)
        if m_fn:
            functions.append({"name": m_fn.group(1), "startLine": line_num, "endLine": line_num + 5})
            
        # Classes
        m_cls = re.match(r'^\s*class\s+(\w+)', line)
        if m_cls:
            classes.append({"name": m_cls.group(1), "startLine": line_num, "endLine": line_num + 5})

    return {
        "functions": functions,
        "classes": classes,
        "imports": imports,
        "exports": exports,
        "metrics": {
            "functionCount": len(functions),
            "classCount": len(classes),
            "importCount": len(imports),
            "exportCount": len(exports)
        }
    }

def main():
    scan_result_path = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\intermediate\scan-result.json'
    project_root = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo'
    
    with open(scan_result_path, 'r', encoding='utf-8') as f:
        scan_data = json.load(f)
    
    results = []
    for f_info in scan_data["files"]:
        rel_path = f_info["path"]
        lang = f_info["language"]
        full_path = os.path.join(project_root, rel_path)
        
        if not os.path.exists(full_path):
            continue
            
        try:
            with open(full_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception:
            continue
            
        if lang in ('typescript', 'javascript'):
            data = extract_ts_js(content, rel_path)
        elif lang == 'java':
            data = extract_java(content, rel_path)
        elif lang == 'python':
            data = extract_python(content, rel_path)
        else:
            data = {
                "functions": [], "classes": [], "imports": [], "exports": [],
                "metrics": {"functionCount": 0, "classCount": 0, "importCount": 0, "exportCount": 0}
            }
            
        data["path"] = rel_path
        data["language"] = lang
        data["totalLines"] = content.count('\n') + 1
        results.append(data)
        
    output = {
        "scriptCompleted": True,
        "filesAnalyzed": len(results),
        "results": results
    }
    
    output_path = r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\intermediate\full-extract-results.json'
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    
    print(f"Extraction complete: {len(results)} files processed.")

if __name__ == '__main__':
    main()
