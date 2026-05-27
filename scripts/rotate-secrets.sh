#!/usr/bin/env bash
# =============================================================================
# rotate-secrets.sh — GitLab #41 rotation helper (P0 SECURITY)
# =============================================================================
# Credentials were leaked via .env.bak-* files committed to git history
# (see commit 8a4f6547 where we removed tracking, and #42 for full scrub).
# Anyone with repo access could read the old .bak file commits — assume
# every secret below is burned until proven rotated.
#
# This script:
#   1. Inventories every secret and flags which ones this script can rotate
#      autonomously (self-signed, high-entropy random) vs which require
#      vendor coordination (API keys issued by third parties).
#   2. Generates proposed new values for the autonomous ones.
#   3. Writes them to a candidate .env.new (never overwrites .env directly).
#   4. Prints a checklist for the vendor-coordinated ones with contact hints.
#
# SAFETY:
#   - Never overwrites .env. Writes to .env.new so operator can diff + promote.
#   - Never pushes changes anywhere.
#   - Default mode is DRY-RUN. Pass --apply to write .env.new.
#
# Usage:
#   ./scripts/rotate-secrets.sh             # dry-run, prints what it would do
#   ./scripts/rotate-secrets.sh --apply     # writes .env.new next to .env
# =============================================================================

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

DRY_RUN=1
[[ "${1:-}" == "--apply" ]] && DRY_RUN=0

if [[ ! -f .env ]]; then
    echo "ERROR: .env not found in $(pwd). Run from repo root or adjust paths." >&2
    exit 1
fi

rand_hex() { openssl rand -hex "${1:-32}"; }
rand_b64() { openssl rand -base64 "${1:-32}" | tr -d '\n'; }
rand_alphanum() { openssl rand -base64 48 | tr -cd '[:alnum:]' | head -c "${1:-32}"; }

# Secrets this script can rotate autonomously (high-entropy, no external trust).
declare -A AUTO_SECRETS=(
    [MYSQL_ROOT_PASSWORD]="rand_alphanum 40"
    [MYSQL_PASSWORD]="rand_alphanum 40"
    [MONGO_PASSWORD]="rand_alphanum 40"
    [REDIS_PASSWORD]="rand_alphanum 40"
    [REDIS_COMMAND_SECRET]="rand_hex 32"
    [RABBITMQ_PASSWORD]="rand_alphanum 40"
    [HAZELCAST_PASSWORD]="rand_alphanum 40"
    [API_AUTH_SECRET]="rand_hex 64"
    [AES_ENCRYPTION_KEY]="rand_hex 32"
    [KEYSTORE_PASSWORD]="rand_alphanum 32"
    [WEBHOOK_SECRET]="rand_hex 32"
)

# Secrets that REQUIRE vendor coordination. This script only prints the
# rotation procedure; the operator executes with the vendor's help.
declare -A VENDOR_SECRETS=(
    [GSC_SECRET_KEY]="GSC+ aggregator — contact Zeus et al. to reissue the seamless-wallet signing key"
    [MOMO_SECRET]="MoMo dashboard → API credentials → rotate. Update the new value before old expires."
    [COINPAYMENTS_PRIVATE_KEY]="CoinPayments merchant dashboard → API keys → regenerate pair. Breaks existing webhooks until the public key is updated on their side too."
    [COINPAYMENTS_PUBLIC_KEY]="Rotated together with COINPAYMENTS_PRIVATE_KEY above."
    [BANCA_CERT_PASSWORD]="BanCa TLS cert passphrase — coordinate with certificate issuer"
    [CLOUDFLARE_TUNNEL_TOKEN]="Cloudflare dashboard → Zero Trust → Networks → Tunnels → Refresh Token"
    [CLOUDFLARE_API_TOKEN]="Cloudflare dashboard → My Profile → API Tokens → Roll"
    [DES_LEGACY_KEY]="Legacy DES key — rotating breaks old-format payment callbacks. Schedule a maintenance window + notify payment partners."
)

echo "========================================================================="
echo "  rotate-secrets.sh  —  GitLab #41 ($(if [[ $DRY_RUN == 1 ]]; then echo DRY-RUN; else echo APPLY; fi))"
echo "========================================================================="
echo

NEW_ENV=/tmp/.env.new.rotation
: > "$NEW_ENV"
rotated_auto=0
echo "# Candidate .env values generated $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$NEW_ENV"
echo "# Diff against .env, verify with ops, then mv to .env + restart stack." >> "$NEW_ENV"
echo >> "$NEW_ENV"

echo "## Autonomous rotation (script-generated values)"
echo
for key in "${!AUTO_SECRETS[@]}"; do
    gen_cmd="${AUTO_SECRETS[$key]}"
    new_val=$(eval "$gen_cmd")
    old_present=$(grep -c "^${key}=" .env || true)
    printf '  %-30s  present_in_.env=%s  new_len=%d\n' "$key" "$old_present" "${#new_val}"
    echo "${key}=${new_val}" >> "$NEW_ENV"
    rotated_auto=$((rotated_auto + 1))
done
echo
echo "  → ${rotated_auto} candidate values written to $NEW_ENV"
echo

echo "## Vendor-coordinated (script can NOT rotate these — follow hint)"
echo
for key in "${!VENDOR_SECRETS[@]}"; do
    printf '  %-30s  %s\n' "$key" "${VENDOR_SECRETS[$key]}"
done
echo

echo "## Operator next steps"
cat <<'STEPS'
  1. diff -u .env /tmp/.env.new.rotation    # sanity check
  2. For each vendor secret above, coordinate rotation with the vendor and
     edit /tmp/.env.new.rotation manually to paste the new value.
  3. When ready:
       cp .env .env.bak.$(date +%s)         # private, NEVER commit
       mv /tmp/.env.new.rotation .env
       ./deploy.sh --rebuild                # pick up new values
  4. After a healthy deploy cycle, remove .env.bak.* from the server — DO NOT
     add them to the repo (that's the bug #41 exists to fix).
  5. After all secrets rotated, run scripts/scrub-git-history.sh (GitLab #42)
     to purge the leaked files from history.
STEPS

if [[ $DRY_RUN == 1 ]]; then
    echo
    echo "DRY-RUN: no file was written. Re-run with --apply to persist the candidate .env."
    rm -f "$NEW_ENV"
else
    echo
    echo "APPLY: wrote $NEW_ENV. Diff, rotate vendor secrets, then promote to .env."
fi
