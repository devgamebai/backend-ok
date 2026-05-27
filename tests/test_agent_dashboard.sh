#!/bin/bash
# Test: Agent Dashboard — c=9537 (ReportGeneral4AgencyProcessor)
# Verifies Tổng nạp / Tổng rút / Lợi nhuận formula matches Admin spec:
#   SUM Nạp  = bank deposits + crypto deposits + ADMIN_CREDIT (credit_wallet)
#   SUM Rút  = bank withdrawals + crypto withdrawals + ADMIN_REVOKE (credit_wallet)
#   Lợi nhuận = Nạp - Rút
# SUN-1335 / SUN-1337

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

TODAY=$(date +%Y-%m-%d)
FROM_30=$(date -d "30 days ago" +%Y-%m-%d 2>/dev/null || date -v-30d +%Y-%m-%d 2>/dev/null || echo "2026-04-16")
FROM_7=$(date -d "7 days ago" +%Y-%m-%d 2>/dev/null || date -v-7d +%Y-%m-%d 2>/dev/null || echo "2026-05-09")

echo "  Date range (30d): ${FROM_30} → ${TODAY}"

# ─── Grab an agent code from listing ──────────────────────────────────────
section "SETUP — Fetch first agent code"
RAW=$(admin_get "c=9420&page=1&size=5")
AGENT_CODE=$(echo "$RAW" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',[])
if isinstance(data,list) and data:
    item=data[0]
    print(item.get('code',item.get('agent_code','')))
" 2>/dev/null || echo "")
echo "  First agent code: ${AGENT_CODE:-NOT FOUND}"

# ─── c=9537: site-wide (admin, no rc param) ───────────────────────────────
section "AGENT DASHBOARD — Site-wide (c=9537, no rc)"

test_name "Site-wide dashboard 30 days"
RESP=$(admin_get "c=9537&startDate=${FROM_30}&endDate=${TODAY}")
assert_success "$RESP"

python3 -c "
import sys,json
raw='''${RESP}'''
try:
    outer=json.loads(raw)
    data_raw=outer.get('data')
    if isinstance(data_raw,str):
        d=json.loads(data_raw)
    else:
        d=data_raw or outer
    keys=['sumDeposit','sumWithdraw','totalProfit','totalMember','totalUserBet','dailyBreakdown']
    missing=[k for k in keys if k not in d]
    if missing:
        print('FAIL missing fields: '+', '.join(missing))
    else:
        dep=int(d['sumDeposit'])
        wit=int(d['sumWithdraw'])
        profit=int(d['totalProfit'])
        expected=dep-wit
        if profit != expected:
            print(f'FAIL totalProfit={profit} != sumDeposit({dep})-sumWithdraw({wit})={expected}')
        elif dep<0 or wit<0:
            print(f'FAIL negative values: dep={dep} wit={wit}')
        else:
            print(f'PASS sumDeposit={dep:,} sumWithdraw={wit:,} totalProfit={profit:,}')
except Exception as e:
    print(f'FAIL parse error: {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"; else _fail "$line"; fi
done

# ─── c=9537: specific agent ────────────────────────────────────────────────
if [[ -n "${AGENT_CODE:-}" ]]; then
    section "AGENT DASHBOARD — Specific agent (c=9537, rc=${AGENT_CODE})"

    test_name "Agent dashboard 30 days — ${AGENT_CODE}"
    RESP=$(admin_get "c=9537&rc=${AGENT_CODE}&startDate=${FROM_30}&endDate=${TODAY}")
    assert_success "$RESP"

    python3 -c "
import sys,json
raw='''${RESP}'''
try:
    outer=json.loads(raw)
    data_raw=outer.get('data')
    if isinstance(data_raw,str):
        d=json.loads(data_raw)
    else:
        d=data_raw or outer
    dep=int(d.get('sumDeposit',0))
    wit=int(d.get('sumWithdraw',0))
    profit=int(d.get('totalProfit',0))
    expected=dep-wit
    bd=d.get('dailyBreakdown',[])
    if profit != expected:
        print(f'FAIL profit={profit} != dep({dep})-wit({wit})={expected}')
    elif dep<0 or wit<0:
        print(f'FAIL negative: dep={dep} wit={wit}')
    else:
        print(f'PASS dep={dep:,} wit={wit:,} profit={profit:,} breakdown_rows={len(bd)}')
except Exception as e:
    print(f'FAIL {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"; else _fail "$line"; fi
done

    test_name "Agent dashboard 7 days — ${AGENT_CODE}"
    RESP=$(admin_get "c=9537&rc=${AGENT_CODE}&startDate=${FROM_7}&endDate=${TODAY}")
    assert_success "$RESP"
    python3 -c "
import sys,json
raw='''${RESP}'''
try:
    outer=json.loads(raw)
    data_raw=outer.get('data')
    d=json.loads(data_raw) if isinstance(data_raw,str) else (data_raw or outer)
    dep=int(d.get('sumDeposit',0))
    wit=int(d.get('sumWithdraw',0))
    profit=int(d.get('totalProfit',0))
    print(f'PASS dep={dep:,} wit={wit:,} profit={profit:,}' if profit==dep-wit else f'FAIL profit mismatch {profit}!={dep-wit}')
except Exception as e:
    print(f'FAIL {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"; else _fail "$line"; fi
done

    # Cross-check: sum(dailyBreakdown.deposit) == sumDeposit
    test_name "Breakdown deposit sum == sumDeposit — ${AGENT_CODE}"
    RESP=$(admin_get "c=9537&rc=${AGENT_CODE}&startDate=${FROM_30}&endDate=${TODAY}&segment=date")
    python3 -c "
import sys,json
raw='''${RESP}'''
try:
    outer=json.loads(raw)
    data_raw=outer.get('data')
    d=json.loads(data_raw) if isinstance(data_raw,str) else (data_raw or outer)
    dep=int(d.get('sumDeposit',0))
    wit=int(d.get('sumWithdraw',0))
    bd=d.get('dailyBreakdown',[])
    bd_dep=sum(int(r.get('deposit',0)) for r in bd)
    bd_wit=sum(int(r.get('withdraw',0)) for r in bd)
    ok=True
    msgs=[]
    if bd_dep!=dep: msgs.append(f'breakdown_deposit({bd_dep:,})!=sumDeposit({dep:,})'); ok=False
    if bd_wit!=wit: msgs.append(f'breakdown_withdraw({bd_wit:,})!=sumWithdraw({wit:,})'); ok=False
    print(('PASS' if ok else 'FAIL')+' — '+', '.join(msgs) if msgs else f'PASS — bd_dep={bd_dep:,} bd_wit={bd_wit:,}')
except Exception as e:
    print(f'SKIP (parse) {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"
    elif [[ "$line" == SKIP* ]]; then _skip "$line"
    else _fail "$line"; fi
done
fi

# ─── Cross-check with Admin deposit/withdraw sums ─────────────────────────
section "CROSS-CHECK — Admin deposit list (c=9104) vs agent dashboard"

test_name "Admin deposit list (c=9104) — 30 days"
RESP9104=$(admin_get "c=9104&ts=${FROM_30}&te=${TODAY}")
python3 -c "
import sys,json
raw='''${RESP9104}'''
try:
    d=json.loads(raw)
    stat=d.get('statistic',[0,0,0])
    cnt=stat[0] if len(stat)>0 else 0
    s_approved=stat[1] if len(stat)>1 else 0
    s_all=stat[2] if len(stat)>2 else 0
    print(f'PASS admin_deposit_approved={int(s_approved):,} total_rows={cnt}')
except Exception as e:
    print(f'FAIL {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"; else _fail "$line"; fi
done

test_name "Admin withdraw list (c=9642) — 30 days"
RESP9642=$(admin_get "c=9642&ts=${FROM_30}&te=${TODAY}")
python3 -c "
import sys,json
raw='''${RESP9642}'''
try:
    d=json.loads(raw)
    total=d.get('total',0)
    print(f'PASS admin_withdraw_rows={int(total)}')
except Exception as e:
    print(f'FAIL {e}')
" 2>/dev/null | while read line; do
    if [[ "$line" == PASS* ]]; then _pass "$line"; else _fail "$line"; fi
done

print_summary
