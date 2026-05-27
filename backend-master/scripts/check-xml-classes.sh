#!/usr/bin/env bash
# XML-class parity lint.
# Fails the build when any <path> in api_backend.xml or api_portal.xml
# routes to a class that does not exist in the corresponding compiled JAR.
#
# Run from repo root after gradle build:
#   bash backend-master/scripts/check-xml-classes.sh
#
# Catches the !216 class — every <command><path>FQN</path></command>
# missing from the JAR is treated as a CI failure, not a runtime warning.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

check_one() {
    local xml="$1"
    local jar_glob="$2"
    local label="$3"

    local jar
    jar="$(ls -1 $jar_glob 2>/dev/null | head -1 || true)"
    if [ -z "${jar:-}" ]; then
        echo "[$label] no JAR found at $jar_glob — skipping (build first)"
        return 0
    fi

    local classes
    classes="$(unzip -l "$jar" | awk '/\.class$/{ gsub("/",".",$NF); sub("\\.class$","",$NF); print $NF }')"

    local missing=0
    while IFS= read -r path; do
        if ! grep -Fxq "$path" <<< "$classes"; then
            echo "[$label] MISSING: $path"
            missing=$((missing + 1))
        fi
    done < <(grep -oP '<path>\K[^<]+' "$xml" | sort -u)

    echo "[$label] $missing missing class(es) referenced by $(basename "$xml")"
    return $missing
}

fail=0
check_one \
    backend-master/api/VinPlayBackend/config/api_backend.xml \
    "backend-master/api/VinPlayBackend/build/libs/VinPlayBackend-*.jar" \
    "backend" || fail=$((fail + $?))

check_one \
    backend-master/api/VinPlayPortal/config/api_portal.xml \
    "backend-master/api/VinPlayPortal/build/libs/VinPlayPortal-*.jar" \
    "portal" || fail=$((fail + $?))

if [ $fail -gt 0 ]; then
    echo
    echo "FAIL: $fail XML <path> entries do not resolve to a class in the built JAR."
    echo "Either restore the deleted class or remove the <command> entry from XML."
    exit 1
fi

echo "OK: all XML <path> entries resolve to classes in the built JARs."
