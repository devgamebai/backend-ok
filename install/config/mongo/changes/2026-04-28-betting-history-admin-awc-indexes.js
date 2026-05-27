// Indexes for admin betting-history endpoints.
//
// c=9930 can query without a player filter, so the per-user compound indexes
// cannot satisfy the latest-first sort. These time indexes let Mongo walk the
// newest rows and stop at the endpoint's per-source limit.
//
// c=9895 AWC history counts and pages over log_awc_bets with optional filters.
// The compound indexes below cover the common filter + created_at DESC shapes.

var gameDb = db.getSiblingDB("win123club");
[
  "log_KhoBau",
  "log_VuongQuocVin",
  "log_SieuAnhHung",
  "log_NuDiepVien",
  "log_ChiemTinh",
  "log_mini_poker",
  "log_cao_thap",
  "log_gsc_bets",
  "bau_cua_transaction",
  "log_taixiu",
  "log_sicbo"
].forEach(function(col) {
  if (gameDb.getCollectionNames().indexOf(col) < 0) return;
  gameDb[col].createIndex(
    { create_time: -1 },
    { name: "idx_create_time_desc", background: true }
  );
});

var awcDb = db.getSiblingDB("vinplay");
if (awcDb.getCollectionNames().indexOf("log_awc_bets") >= 0) {
  awcDb.log_awc_bets.createIndex(
    { created_at: -1 },
    { name: "idx_created_at_desc", background: true }
  );
  awcDb.log_awc_bets.createIndex(
    { user_name: 1, created_at: -1 },
    { name: "idx_user_created_at_desc", background: true }
  );
  awcDb.log_awc_bets.createIndex(
    { platform: 1, game_type: 1, created_at: -1 },
    { name: "idx_platform_game_type_created_at_desc", background: true }
  );
  awcDb.log_awc_bets.createIndex(
    { user_name: 1, platform: 1, game_type: 1, created_at: -1 },
    { name: "idx_user_platform_game_type_created_at_desc", background: true }
  );
}
