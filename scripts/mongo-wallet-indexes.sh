#!/usr/bin/env bash
# Phase E — wallet audit Mongo index bootstrap.
#
# Idempotent: drops any index whose key is on the legacy non-existent
# 'time' field, then re-creates on the real `trans_time` field. Safe to
# re-run after every deploy or volume reset.
#
# Background: vbee-consumer (LogMoneyUserProcessor) writes wallet audit
# to Mongo db `win123club`, collections log_money_user_{vin,xu,nap_vin,
# tieu_vin}. log_money_user_vin has 23.3M+ rows; queries by user_id were
# full scans before idx_user_time landed.

set -euo pipefail

CONTAINER="${MONGO_CONTAINER:-sunwinkr-mongodb}"

ENV_FILE="$(dirname "$0")/../.env"
if [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^MONGO_(USER|PASSWORD)=' "${ENV_FILE}")
fi
USER="${MONGO_USER:-sunwinkr_admin}"
PASS="${MONGO_PASSWORD:-}"

if [[ -z "${PASS}" ]]; then
    echo "ERROR: MONGO_PASSWORD not set in env or .env"
    exit 1
fi

echo "==> Bootstrapping wallet audit indexes..."

docker exec "${CONTAINER}" mongosh --quiet \
    -u "${USER}" -p "${PASS}" --authenticationDatabase admin \
    --eval "
db = db.getSiblingDB('win123club');
const COLS = ['log_money_user_vin','log_money_user_xu','log_money_user_nap_vin','log_money_user_tieu_vin'];
COLS.forEach(c => {
    const col = db.getCollection(c);
    // Purge any index keyed on the bogus 'time' field (legacy mistake)
    col.getIndexes()
       .filter(i => JSON.stringify(i.key).includes('\"time\"'))
       .forEach(i => col.dropIndex(i.name));
    // Real time field is trans_time
    col.createIndex({user_id:1, trans_time:-1}, {name:'idx_user_time', background:true});
    col.createIndex({nick_name:1, trans_time:-1}, {name:'idx_nick_time', background:true});
    col.createIndex({trans_id:1}, {name:'idx_trans', background:true});
    print(c + ' indexes: ' + col.getIndexes().map(i => i.name).join(', '));
});
" 2>&1 | grep -v -E '(deprecation|MongoSrv)'

echo
echo "Done. Query patterns supported:"
echo "  - by user_id + trans_time range  (idx_user_time)"
echo "  - by nick_name + trans_time range (idx_nick_time)"
echo "  - by trans_id (dedup)             (idx_trans)"
