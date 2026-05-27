// Add the per-user compound index on log_taixiu so it matches the other
// game-history collections (log_KhoBau, log_VuongQuocVin, log_SieuAnhHung,
// log_NuDiepVien, log_mini_poker, bau_cua_transaction, log_sicbo, log_gsc_bets).
//
// Before this index, c=9843 betting-history queries on log_taixiu were
// scanning 370k+ docs filtered only by the TTL date index, then post-
// filtering by user_name. With the compound index the planner does an
// IXSCAN on (user_name, create_time) for the typical
// `{user_name: {$in: [...]}, create_time: {$gte, $lt}}` query.
//
// Idempotent — createIndex is a no-op when the same key+name exists.
db.getSiblingDB("win123club").log_taixiu.createIndex(
    { user_name: 1, create_time: -1 },
    { name: "idx_user_time", background: true }
);
