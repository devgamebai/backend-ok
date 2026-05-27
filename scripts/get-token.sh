#!/bin/bash
# Usage: ./scripts/get-token.sh [username] [password]
# Default: superadmin / admin123

DOMAIN="staging-play.sunkr.bet"
USER="${1:-superadmin}"
PASS="${2:-admin123}"
PW_MD5=$(echo -n "$PASS" | md5sum | cut -d' ' -f1)

RES=$(curl -sk "https://$DOMAIN/api?c=3&un=$USER&pw=$PW_MD5&platform=web" --max-time 10 2>&1)
TOKEN=$(echo "$RES" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)

if [ -n "$TOKEN" ] && [ "$TOKEN" != "None" ]; then
    echo "$TOKEN"
else
    echo "Login failed: $RES" >&2
    exit 1
fi
