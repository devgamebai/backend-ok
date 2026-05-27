// SUN-1246: indexes required by the commutative-upsert pattern in
// GscBetSideEffectPublisher.runBetInsert / runSettleUpdate.
//
// The fix made BET_INSERT and SETTLE_UPDATE race-free by switching from
// insertOne+updateOne to upsert+upsert with disjoint $set field sets.
// The upsert filter is "wager_code" (default path) or "event_key"
// (multi-tx batched-bet path, e.g. CQ9 fish + free-spin chains).
//
// Without indexes on those fields the upsert does a collection scan on
// every Mongo write, which is fine at <10K docs but degrades fast as
// the collection grows (~250 docs/day at current load → 50K in ~6
// months → ~10ms scan per upsert → backlog risk on the
// queue_log_gsc_bets_async consumer at peak).
//
// Both indexes are non-unique:
//   - wager_code: not unique because the free-spin chain (SUN-1196)
//     associates multiple sub-bet rows with the same wager_code in the
//     vendor's eyes; we keep them as separate rows in Mongo.
//   - event_key: not unique either (rare CQ9 multi-amount cases) but
//     made sparse since many docs have no event_key field.
//
// background: true so the build doesn't block writes during creation
// on the live collection.

db = db.getSiblingDB("win123club");

print("[2026-05-03] log_gsc_bets indexes for SUN-1246 commutative upsert");

db.log_gsc_bets.createIndex(
    {"wager_code": 1},
    {background: true, name: "wager_code_1"}
);
print("  wager_code_1 ensured");

db.log_gsc_bets.createIndex(
    {"event_key": 1},
    {background: true, sparse: true, name: "event_key_1"}
);
print("  event_key_1 (sparse) ensured");

print("=== final indexes ===");
db.log_gsc_bets.getIndexes().forEach(function (i) {
    print("  " + i.name + " → " + JSON.stringify(i.key)
        + (i.sparse ? " (sparse)" : "")
        + (i.unique ? " (unique)" : ""));
});
