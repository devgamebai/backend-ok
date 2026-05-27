#!/usr/bin/env bash
#
# seed-gsc-catalog.sh — Bulk-load vinplay.gsc_game_catalog from GSC.
#
# Reads operator credentials and DB config from the .env file next to this
# repo root. Designed to be idempotent (ON DUPLICATE KEY UPDATE) so it's
# safe to re-run any time to refresh the catalog.
#
# Typical workflow:
#   1. First deploy on a new env (staging/prod):  bash scripts/seed-gsc-catalog.sh
#   2. Refresh after a provider adds new games:   bash scripts/seed-gsc-catalog.sh
#   3. One-shot refresh of a single product code: bash scripts/seed-gsc-catalog.sh 1185
#
# Dependencies: docker (for mysql exec), curl, python3, openssl, awk.
# Expects the sunwinkr-mysql container to be running on the host.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: .env not found at $ENV_FILE" >&2
  exit 1
fi

# shellcheck disable=SC1091
set -a; source "$ENV_FILE"; set +a

: "${GSC_OPERATOR_URL:?GSC_OPERATOR_URL not set in .env}"
: "${GSC_OPERATOR_CODE:?GSC_OPERATOR_CODE not set in .env}"
: "${GSC_SECRET_KEY:?GSC_SECRET_KEY not set in .env}"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-sunwinkr-mysql}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD not set in .env}"

# If a product_code is passed on the CLI, only refresh that. Otherwise
# discover every provisioned product_code from gsc_product_map.
if [[ $# -ge 1 ]]; then
  PRODUCT_CODES="$1"
  echo "Mode: single product_code=$PRODUCT_CODES"
else
  echo "Mode: full refresh — discovering active product_codes from gsc_product_map"
  PRODUCT_CODES="$(docker exec -i "$MYSQL_CONTAINER" \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s vinplay \
    -e "SELECT product_code FROM gsc_product_map WHERE is_active=1 ORDER BY product_code" 2>/dev/null)"
fi

[[ -z "$PRODUCT_CODES" ]] && { echo "No product_codes to process."; exit 0; }

TMP_SQL="$(mktemp /tmp/gsc_catalog_seed.XXXXXX.sql)"
trap 'rm -f "$TMP_SQL" /tmp/gsc_resp.*.json' EXIT

total_rows=0
total_pcs=0

for pc in $PRODUCT_CODES; do
  request_time="$(date +%s)"
  sign="$(printf '%s%s%s%s' "$request_time" "$GSC_SECRET_KEY" "gamelist" "$GSC_OPERATOR_CODE" | md5sum | awk '{print $1}')"
  url="${GSC_OPERATOR_URL}/api/operators/provider-games?operator_code=${GSC_OPERATOR_CODE}&product_code=${pc}&sign=${sign}&request_time=${request_time}"

  rows_before=$(wc -l < "$TMP_SQL")
  TMP_JSON="$(mktemp /tmp/gsc_resp.XXXXXX.json)"
  curl -sk --max-time 10 "$url" > "$TMP_JSON"
  PARSED_PC="$pc" PARSED_FILE="$TMP_JSON" python3 <<'PYEOF' >> "$TMP_SQL"
import os, sys, json
pc = int(os.environ["PARSED_PC"])
try:
    with open(os.environ["PARSED_FILE"]) as f:
        d = json.load(f)
except Exception as e:
    sys.stderr.write(f"  pc={pc} json parse err: {e}\n")
    sys.exit(0)
if d.get("code", -1) != 0:
    sys.stderr.write(f"  pc={pc} GSC error: {d.get('message','unknown')}\n")
    sys.exit(0)
def infer_category(name, game_type):
    """Heuristic — same rules as api_backend ListGSCGamesProcessor.inferCategory
    and api_portal GSCGameListProcessor.inferCategory. Keep in sync with Java
    code if changed. Raw GSC /api/operators/provider-games does NOT return a
    category field, so we derive one from game_name / game_type so the
    gsc_game_catalog row is useful at rebate resolution time."""
    lc = (name or "").lower()
    if "baccarat" in lc or "baccarrat" in lc: return "Baccarat"
    if "sic bo" in lc or "sicbo" in lc: return "SicBoDice"
    if "dragon tiger" in lc or "dragontiger" in lc or "rồng hổ" in lc: return "DragonTiger"
    if "roulette" in lc: return "Roulette"
    if "blackjack" in lc: return "Blackjack"
    if "poker" in lc or "hold'em" in lc or "holdem" in lc: return "Poker"
    if "crazy time" in lc or "crazytime" in lc: return "GameShow"
    if "mega ball" in lc or "megaball" in lc: return "GameShow"
    if "monopoly" in lc: return "GameShow"
    if "deal or no deal" in lc: return "GameShow"
    if "dream catcher" in lc: return "GameShow"
    if "bac bo" in lc: return "GameShow"
    if "lightning" in lc: return "GameShow"
    if "fantan" in lc or "fan tan" in lc: return "GameShow"
    if "keno" in lc: return "GameShow"
    if "teen patti" in lc: return "GameShow"
    if "andar bahar" in lc: return "GameShow"
    if "hi lo" in lc or "hilo" in lc: return "GameShow"
    if "slot" in lc: return "Slot"
    # Fallback to game_type
    if game_type == "SLOT": return "Slot"
    if game_type == "FISHING": return "Fishing"
    if game_type == "SPORT_BOOK": return "Sports"
    return "Other"

games = d.get("provider_games") or d.get("data") or []
for g in games:
    if g.get("status","ACTIVATED").upper() != "ACTIVATED":
        continue
    gc = (g.get("game_code") or "").replace("'", "''")
    gn = (g.get("game_name") or "").replace("'", "''")
    gt = (g.get("game_type") or "").replace("'", "''")
    cat_raw = g.get("category") or ""
    # GSC upstream doesn't return category → infer from name/type.
    cat = (cat_raw or infer_category(g.get("game_name",""), gt)).replace("'", "''")
    img = (g.get("image_url") or "").replace("'", "''")
    if not gc: continue
    print(f"INSERT INTO gsc_game_catalog (product_code, game_code, game_name, game_type, category, image_url) "
          f"VALUES ({pc}, '{gc}', '{gn}', '{gt}', '{cat}', '{img}') "
          f"ON DUPLICATE KEY UPDATE game_name=VALUES(game_name), game_type=VALUES(game_type), "
          f"category=VALUES(category), image_url=VALUES(image_url);")
PYEOF
  rm -f "$TMP_JSON"
  rows_after=$(wc -l < "$TMP_SQL")
  added=$((rows_after - rows_before))
  total_rows=$((total_rows + added))
  total_pcs=$((total_pcs + 1))
  printf "  pc=%s  +%d games\n" "$pc" "$added"
done

if [[ $total_rows -eq 0 ]]; then
  echo "Nothing to upsert."
  exit 0
fi

echo ""
echo "Upserting $total_rows rows across $total_pcs product_codes into gsc_game_catalog …"
docker cp "$TMP_SQL" "$MYSQL_CONTAINER":/tmp/gsc_catalog_seed.sql >/dev/null
docker exec -i "$MYSQL_CONTAINER" bash -c \
  "mysql -uroot -p'$MYSQL_ROOT_PASSWORD' vinplay < /tmp/gsc_catalog_seed.sql" 2>&1 \
  | grep -v 'Using a password' || true

total_in_db="$(docker exec -i "$MYSQL_CONTAINER" \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s vinplay \
  -e "SELECT COUNT(*) FROM gsc_game_catalog" 2>/dev/null)"
echo "Done. gsc_game_catalog now has $total_in_db rows total."
