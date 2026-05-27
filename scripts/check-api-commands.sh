#!/bin/bash
# ============================================================
# CI Check: Validate api_backend.xml and api_portal.xml
# Detects: duplicate command IDs, missing <path> elements
#
# Comment-aware: XML comments (<!-- ... -->) are stripped before
# duplicate detection so commented-out legacy entries no longer
# trigger false positives.
#
# Run:  bash scripts/check-api-commands.sh
# Exit: 0 = OK, 1 = errors found
# ============================================================
set -e

BACKEND_XML="backend-master/api/VinPlayBackend/config/api_backend.xml"
PORTAL_XML="backend-master/api/VinPlayPortal/config/api_portal.xml"

# Support running from repo root or backend-master/
if [ ! -f "$BACKEND_XML" ] && [ -f "api/VinPlayBackend/config/api_backend.xml" ]; then
    BACKEND_XML="api/VinPlayBackend/config/api_backend.xml"
    PORTAL_XML="api/VinPlayPortal/config/api_portal.xml"
fi

python3 - "$BACKEND_XML" "$PORTAL_XML" <<'PY'
import re
import sys
import collections

files = [
    (sys.argv[1], "api_backend.xml"),
    (sys.argv[2], "api_portal.xml"),
]

total_errors = 0

for path, name in files:
    print(f"=== Checking {name} ===")
    try:
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        print(f"  SKIP: file not found ({path})")
        print()
        continue

    # --- Step 1: compute line ranges that fall inside XML comments ---
    comment_ranges = []
    for m in re.finditer(r"<!--.*?-->", content, flags=re.DOTALL):
        start_line = content[: m.start()].count("\n") + 1
        end_line = content[: m.end()].count("\n") + 1
        comment_ranges.append((start_line, end_line))

    def is_commented(line_no):
        return any(lo <= line_no <= hi for lo, hi in comment_ranges)

    # --- Step 2: collect <id> declarations (with line numbers) ---
    by_id = collections.defaultdict(list)
    for m in re.finditer(r"<id>(\d+)</id>", content):
        line_no = content[: m.start()].count("\n") + 1
        if is_commented(line_no):
            continue
        by_id[m.group(1)].append(line_no)

    dups = {k: v for k, v in by_id.items() if len(v) > 1}
    total = sum(len(v) for v in by_id.values())
    if dups:
        print("  DUPLICATE IDs found:")
        for k in sorted(dups, key=int):
            lines = ",".join(str(n) for n in dups[k])
            print(f"     ID={k} appears {len(dups[k])} times (lines: {lines})")
        total_errors += 1
    else:
        print(f"  OK: no duplicate IDs ({total} commands)")

    # --- Step 3: detect commands with missing <path>, comment-aware ---
    content_stripped = re.sub(r"<!--.*?-->", "", content, flags=re.DOTALL)
    empty_path = []
    for m in re.finditer(r"<command>(.*?)</command>", content_stripped, flags=re.DOTALL):
        block = m.group(1)
        id_match = re.search(r"<id>(\d+)</id>", block)
        path_match = re.search(r"<path>(.*?)</path>", block)
        name_match = re.search(r"<name>(.*?)</name>", block)
        if id_match and not path_match:
            label = name_match.group(1) if name_match else "?"
            empty_path.append((id_match.group(1), label))

    if empty_path:
        print("  Commands with NO <path> element:")
        for cid, label in empty_path:
            print(f'     ID={cid} name="{label}"')
        total_errors += 1
    else:
        print("  OK: all commands have <path> elements")

    print()

print("============================================")
if total_errors:
    print(f"  FAIL: {total_errors} issue(s) found — fix before merging")
    sys.exit(1)
else:
    print("  PASS: all API command configs are valid")
    sys.exit(0)
PY
