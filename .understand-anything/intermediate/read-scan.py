import json
with open(r'd:\CanThoUniversity\LVTN\CTU-Connect-demo\.understand-anything\intermediate\scan-result.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
print("Name:", data["name"])
print("Total files:", data["totalFiles"])
print("Languages:", data["languages"])
print("Frameworks:", data["frameworks"])
print("Complexity:", data["estimatedComplexity"])
print()
from collections import Counter
services = Counter()
for fi in data["files"]:
    parts = fi["path"].replace("\\", "/").split("/")
    services[parts[0]] += 1
for svc, cnt in sorted(services.items()):
    print("  %s: %d files" % (svc, cnt))

# Also print all file paths
print("\n--- All files ---")
for fi in data["files"]:
    print("%s|%d" % (fi["path"], fi["sizeLines"]))
