import os
import sys
import json
import subprocess

def main():
    if len(sys.argv) < 3:
        print("Usage: python ua-project-scan.py <project-root> <output-file>", file=sys.stderr)
        sys.exit(1)
    
    project_root = sys.argv[1]
    output_file = sys.argv[2]
    
    if not os.path.isdir(project_root):
        print(f"Error: {project_root} is not a directory", file=sys.stderr)
        sys.exit(1)
    
    # Step 1: File Discovery via git ls-files
    try:
        result = subprocess.run(
            ["git", "ls-files"],
            cwd=project_root,
            capture_output=True, text=True, timeout=30
        )
        all_files = [f.strip() for f in result.stdout.strip().split("\n") if f.strip()]
    except Exception:
        all_files = []
        for root, dirs, files in os.walk(project_root):
            dirs[:] = [d for d in dirs if d not in {'node_modules', '.git', 'vendor', 'venv', '__pycache__'}]
            for f in files:
                rel = os.path.relpath(os.path.join(root, f), project_root).replace("\\", "/")
                all_files.append(rel)
    
    # Step 2: Exclusion filtering
    exclude_dirs = ['node_modules/', '.git/', 'vendor/', 'venv/', '.venv/', '__pycache__/',
                    'dist/', 'build/', 'out/', 'coverage/', '.next/', '.cache/', '.turbo/', 'target/',
                    '.idea/', '.vscode/', '.understand-anything/']
    exclude_exts = {'.lock', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.ico', '.woff', '.woff2',
                    '.ttf', '.eot', '.mp3', '.mp4', '.pdf', '.zip', '.tar', '.gz',
                    '.map', '.log', '.jar', '.class', '.original'}
    exclude_files = {'package-lock.json', 'yarn.lock', 'pnpm-lock.yaml', 'LICENSE', '.gitignore',
                     '.editorconfig', '.prettierrc', '.dockerignore', 'Makefile', 'Dockerfile',
                     '.gitattributes', 'mvnw', 'mvnw.cmd'}
    
    ext_to_lang = {
        '.ts': 'typescript', '.tsx': 'typescript',
        '.js': 'javascript', '.jsx': 'javascript', '.mjs': 'javascript',
        '.py': 'python', '.go': 'go', '.rs': 'rust',
        '.java': 'java', '.rb': 'ruby',
        '.cpp': 'cpp', '.cc': 'cpp', '.cxx': 'cpp', '.h': 'cpp', '.hpp': 'cpp',
        '.c': 'c', '.cs': 'csharp', '.swift': 'swift', '.kt': 'kotlin',
        '.php': 'php', '.vue': 'vue', '.svelte': 'svelte',
        '.sh': 'bash', '.bash': 'bash',
        '.css': 'css', '.sql': 'sql', '.properties': 'properties',
    }
    
    source_files = []
    for f in all_files:
        f_normalized = f.replace("\\", "/")
        
        if any(excl in f_normalized for excl in exclude_dirs):
            continue
        
        basename = os.path.basename(f_normalized)
        _, ext = os.path.splitext(basename)
        
        if basename in exclude_files:
            continue
        if ext in exclude_exts:
            continue
        if '.min.' in basename or '.d.ts' in basename or '.generated.' in basename:
            continue
        if ext in {'.md', '.txt', '.cfg', '.ini', '.yml', '.yaml', '.toml', '.example',
                   '.xml', '.json', '.iml', '.cmd', '.bat'}:
            continue
        
        if ext in ext_to_lang:
            source_files.append(f_normalized)
    
    # Step 3: Language detection
    languages = set()
    for f in source_files:
        _, ext = os.path.splitext(f)
        if ext in ext_to_lang and ext_to_lang[ext] not in ('css', 'sql', 'properties'):
            languages.add(ext_to_lang[ext])
    
    # Step 4: Line counting
    files_with_lines = []
    for f in source_files:
        full_path = os.path.join(project_root, f)
        try:
            with open(full_path, 'r', encoding='utf-8', errors='ignore') as fh:
                line_count = sum(1 for _ in fh)
        except Exception:
            line_count = 0
        
        _, ext = os.path.splitext(f)
        lang = ext_to_lang.get(ext, ext.lstrip('.'))
        files_with_lines.append({
            "path": f,
            "language": lang,
            "sizeLines": line_count
        })
    
    files_with_lines.sort(key=lambda x: x["path"])
    
    # Step 5: Framework detection
    frameworks = []
    for f in all_files:
        if f.endswith('pom.xml') and 'target/' not in f:
            try:
                full_path = os.path.join(project_root, f)
                with open(full_path, 'r', encoding='utf-8', errors='ignore') as fh:
                    content = fh.read()
                if 'spring-boot' in content and 'Spring Boot' not in frameworks:
                    frameworks.append('Spring Boot')
                if 'spring-cloud' in content and 'Spring Cloud' not in frameworks:
                    frameworks.append('Spring Cloud')
                if 'spring-kafka' in content and 'Spring Kafka' not in frameworks:
                    frameworks.append('Spring Kafka')
                if 'neo4j' in content and 'Neo4j' not in frameworks:
                    frameworks.append('Neo4j')
                if 'spring-websocket' in content.lower() and 'Spring WebSocket' not in frameworks:
                    frameworks.append('Spring WebSocket')
            except Exception:
                pass
    
    for f in all_files:
        if f.endswith('package.json') and 'node_modules' not in f:
            try:
                full_path = os.path.join(project_root, f)
                with open(full_path, 'r', encoding='utf-8', errors='ignore') as fh:
                    pkg = json.load(fh)
                deps = {}
                deps.update(pkg.get('dependencies', {}))
                deps.update(pkg.get('devDependencies', {}))
                fw_map = {
                    'react': 'React', 'next': 'Next.js', 'tailwindcss': 'TailwindCSS',
                    '@tanstack/react-query': 'TanStack Query', '@stomp/stompjs': 'STOMP.js',
                }
                for dep, fw_name in fw_map.items():
                    if dep in deps and fw_name not in frameworks:
                        frameworks.append(fw_name)
            except Exception:
                pass
    
    for f in all_files:
        if f.endswith('requirements.txt'):
            try:
                full_path = os.path.join(project_root, f)
                with open(full_path, 'r', encoding='utf-8', errors='ignore') as fh:
                    content = fh.read().lower()
                if 'fastapi' in content and 'FastAPI' not in frameworks:
                    frameworks.append('FastAPI')
                if 'torch' in content and 'PyTorch' not in frameworks:
                    frameworks.append('PyTorch')
                if 'transformers' in content and 'HuggingFace Transformers' not in frameworks:
                    frameworks.append('HuggingFace Transformers')
            except Exception:
                pass
    
    # Step 6: Complexity
    total = len(files_with_lines)
    if total <= 20:
        complexity = "small"
    elif total <= 100:
        complexity = "moderate"
    elif total <= 500:
        complexity = "large"
    else:
        complexity = "very-large"
    
    # Step 7: Project name
    name = "CTU-Connect"
    
    output = {
        "name": name,
        "description": "CTU-Connect is an intelligent academic social network platform for Can Tho University featuring microservices architecture with Spring Boot backend, Next.js frontend, and PhoBERT-powered AI recommendation engine.",
        "languages": sorted(list(languages)),
        "frameworks": sorted(list(set(frameworks))),
        "files": files_with_lines,
        "totalFiles": total,
        "estimatedComplexity": complexity
    }
    
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w', encoding='utf-8') as fh:
        json.dump(output, fh, indent=2, ensure_ascii=False)
    
    print(f"Scan complete: {name}, {total} source files, languages: {sorted(list(languages))}, complexity: {complexity}")

if __name__ == '__main__':
    main()
