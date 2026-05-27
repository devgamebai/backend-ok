// Backfill log_gsc_bets.current_money from log_money_user_vin (one-shot).
//
// Why: Phase 2 (writer-stamping) was deployed 2026-05-06 ~16:00 UTC. Bets
// placed before that don't have current_money on the log_gsc_bets doc, so
// the c=303 reader's walk-back from users.vin (current) drifts as the
// player keeps betting and shows negative balances on older rows.
//
// log_money_user_vin (the audit table) DOES have correct current_money
// for every wallet operation. This script cross-references the two
// collections by (user_name, money_exchange, time-window) and copies the
// correct snapshot into log_gsc_bets.
//
// Match logic:
//   - bet's BET tx (debit, negative money_exchange in audit)
//   - same user_name
//   - audit's trans_time (UTC+7 string) within ±300s of bet's create_time
//     when both converted to epoch
//   - audit's money_exchange magnitude == bet's bet_value
//
// Run from inside the mongodb container:
//   docker exec -i sunwinkr-mongodb mongosh ... < backfill-gsc-current-money.js
//
// Idempotent — only writes when current_money is missing on the bet doc.

const dryRun = false;     // set true for stats-only
const verbose = false;

const stats = {
  scanned: 0,
  matched: 0,
  no_match: 0,
  already_set: 0,
  zero_bet: 0,
};

const cursor = db.log_gsc_bets.find({
  current_money: { $exists: false },
  user_name: { $exists: true, $ne: "" },
  bet_value: { $gt: 0 }
}).noCursorTimeout();

while (cursor.hasNext()) {
  const bet = cursor.next();
  stats.scanned++;

  if (bet.current_money) { stats.already_set++; continue; }

  // log_money_user_vin uses UTC+7 strings. Bet's create_time is a Date
  // object — convert to UTC+7 string for window-matching, ±300s span
  // captures slight timestamp jitter.
  const t = bet.create_time;
  if (!t) { stats.no_match++; continue; }
  const tMs = t.getTime();
  const lowerMs = tMs - 300_000;
  const upperMs = tMs + 300_000;

  // Find audit rows for this user with negative exchange (BET) of the right magnitude.
  // bet.bet_value may be a Mongo Long; convert to native Number for the
  // comparison query so the driver matches cleanly.
  const betVal = Number(bet.bet_value);
  const candidates = db.log_money_user_vin.find({
    nick_name: bet.user_name,
    action_name: { $regex: /^gsc_/ },
    money_exchange: -betVal
  }).limit(20).toArray();

  let best = null;
  let bestGap = Infinity;
  for (const c of candidates) {
    if (!c.trans_time) continue;
    // trans_time is UTC+7 string e.g. "2026-05-06 21:47:47"
    // Treat as UTC+7 → convert to UTC ms
    const cMs = Date.parse(c.trans_time + "+07:00");
    if (isNaN(cMs)) continue;
    if (cMs < lowerMs || cMs > upperMs) continue;
    const gap = Math.abs(cMs - tMs);
    if (gap < bestGap) { bestGap = gap; best = c; }
  }

  if (!best) {
    stats.no_match++;
    if (verbose) print("NO_MATCH wager=" + bet.wager_code + " user=" + bet.user_name + " bet=" + bet.bet_value);
    continue;
  }

  // Audit's current_money is balance AFTER the debit, so balance BEFORE = current + |exchange|.
  // CRITICAL: cast both operands to Number — Mongo Long + Long can stringify (saw the
  // 4722900 + 15000000 → "472290015000000" trillion-dollar bug on the first backfill pass).
  const cMoney = Number(best.current_money);
  const cExchange = Number(best.money_exchange);
  if (!Number.isFinite(cMoney) || !Number.isFinite(cExchange)) {
    stats.no_match++;
    continue;
  }
  const moneyBefore = cMoney + Math.abs(cExchange);
  if (!Number.isFinite(moneyBefore) || moneyBefore < 0 || moneyBefore > 1e12) {
    // Sanity guard — anything beyond 1 trillion KRW is wrong. Skip.
    stats.no_match++;
    if (verbose) print("INSANE wager=" + bet.wager_code + " moneyBefore=" + moneyBefore);
    continue;
  }
  stats.matched++;

  if (!dryRun) {
    db.log_gsc_bets.updateOne(
      { _id: bet._id },
      { $set: { current_money: NumberLong(moneyBefore), current_money_backfilled: true } }
    );
  }
}
cursor.close();

print("=== backfill complete ===");
print("scanned:     " + stats.scanned);
print("matched:     " + stats.matched);
print("no_match:    " + stats.no_match);
print("already_set: " + stats.already_set);
