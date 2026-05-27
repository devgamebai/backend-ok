// Add an index on user_login_info.time_log to speed up the c=110
// (USER_LOGIN_LOG_DUPLICATE) aggregation's initial $match stage.
//
// c=110 groups user_login_info records by IP to find accounts sharing
// the same client IP within a caller-supplied time window. The aggregation
// pipeline opens with:
//
//   { $match: { time_log: { $gte: tsFrom, $lte: tsTo } } }
//
// Without an index on time_log every invocation is a full COLLSCAN over
// the entire collection (append-only, grows unboundedly). A single-key
// ascending index lets the planner do a bounded IXSCAN and stop as soon
// as it exits the requested range.
//
// Apply manually on staging, then on production after validation:
//
//   mongo -u <user> -p <pass> --authenticationDatabase admin \
//     install/config/mongo/changes/2026-05-11-user-login-info-time-log-index.js
//
// Or, if the shell is already connected to the right DB:
//
//   load("install/config/mongo/changes/2026-05-11-user-login-info-time-log-index.js")
//
// Idempotent: createIndex with the same name and key spec is a no-op
// after the first successful run.
//
// background: true keeps the build non-blocking on a live, busy collection.

db = db.getSiblingDB("win123club");

db.user_login_info.createIndex(
    { time_log: 1 },
    { name: "idx_user_login_info_time_log", background: true }
);

print("[2026-05-11] idx_user_login_info_time_log applied to user_login_info");

print("=== current indexes on user_login_info ===");
db.user_login_info.getIndexes().forEach(function(i) {
    print("  " + i.name + " -> " + JSON.stringify(i.key));
});
