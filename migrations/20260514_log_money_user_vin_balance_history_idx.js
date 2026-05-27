// Migration: add compound index on log_money_user_vin to support c=9972 (User Balance History).
// Run: mongo "$MONGO_URI" migrations/20260514_log_money_user_vin_balance_history_idx.js
// Idempotent — createIndex is a no-op when the index already exists.

(function () {
    var coll = db.getCollection("log_money_user_vin");
    var indexName = "idx_balance_history";

    var existing = coll.getIndexes().filter(function (i) { return i.name === indexName; });
    if (existing.length > 0) {
        print("[skip] Index '" + indexName + "' already exists on log_money_user_vin");
        return;
    }

    print("[create] Building compound index '" + indexName + "' on log_money_user_vin ...");
    var result = coll.createIndex(
        { nick_name: 1, trans_time: -1 },
        { name: indexName, background: true }
    );
    print("[ok] createIndex returned: " + tojson(result));
})();
