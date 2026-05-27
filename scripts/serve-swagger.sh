#!/bin/bash
# scripts/serve-swagger.sh — Serve the Swagger UI locally over HTTP.
#
# The `docs/openapi/index.html` page fetches `./openapi.json`, so it must be
# served over HTTP, not opened via file:// (browsers block local-file fetches
# under CORS). This script runs Python's stdlib http.server in the doc dir,
# optionally regenerating the spec first.
#
# Usage:
#   bash scripts/serve-swagger.sh                # serve existing spec at http://localhost:8000/
#   bash scripts/serve-swagger.sh --regen        # regenerate openapi.json first
#   bash scripts/serve-swagger.sh --port 9000    # custom port
#   bash scripts/serve-swagger.sh --regen --port 9000
#
# Stop with Ctrl+C.

set -euo pipefail

PORT=8000
REGEN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --regen)      REGEN=true; shift ;;
        --port)       PORT="$2"; shift 2 ;;
        --port=*)     PORT="${1#*=}"; shift ;;
        -h|--help)    sed -n '2,15p' "$0"; exit 0 ;;
        *)            echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OPENAPI_DIR="$ROOT_DIR/docs/openapi"
SPEC_FILE="$OPENAPI_DIR/openapi.json"
INDEX_FILE="$OPENAPI_DIR/index.html"

if [[ "$REGEN" == "true" || ! -f "$SPEC_FILE" ]]; then
    echo "==> Regenerating $SPEC_FILE"
    python3 "$ROOT_DIR/scripts/gen_openapi.py"
fi

if [[ ! -f "$SPEC_FILE" || ! -f "$INDEX_FILE" ]]; then
    echo "missing $SPEC_FILE or $INDEX_FILE — try: bash $0 --regen" >&2
    exit 1
fi

echo
echo "==> Swagger UI: http://localhost:$PORT/"
echo "    spec:       http://localhost:$PORT/openapi.json"
echo "    Ctrl+C to stop"
echo

cd "$OPENAPI_DIR"
exec python3 -m http.server "$PORT"
