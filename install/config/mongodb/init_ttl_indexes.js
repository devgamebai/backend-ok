// =============================================================================
// TTL indexes on game log collections — auto-expire after 90 days
// =============================================================================
// Prevents orphan data accumulation. MongoDB automatically deletes
// documents when the indexed timestamp field is older than expireAfterSeconds.
//
// Run: mongosh "mongodb://user:pass@localhost:27017/?authSource=admin" < this_file.js
// =============================================================================

var TTL_90_DAYS = 7776000;

// vinplay database
var vp = db.getSiblingDB("vinplay");
var vpCollections = [
    {col: "log_awc_bets", field: "created_at"},
    {col: "log_gsc_bets", field: "created_at"},
    {col: "log_taixiu", field: "create_time"},
    {col: "log_sicbo", field: "create_time"},
    {col: "user_bet_tai_xiu", field: "create_time"},
    {col: "user_bet_tai_xiu_sicbo", field: "create_time"},
    {col: "user_bet_tai_xiu_md5", field: "create_time"}
];

vpCollections.forEach(function(item) {
    try {
        vp.getCollection(item.col).createIndex(
            {[item.field]: 1},
            {expireAfterSeconds: TTL_90_DAYS, name: "ttl_90days_" + item.field}
        );
        print("vinplay." + item.col + " → TTL on " + item.field);
    } catch(e) { print("Skip vinplay." + item.col + ": " + e.message); }
});

// win123club database
var w = db.getSiblingDB("win123club");
var wCollections = [
    {col: "log_KhoBau", field: "create_time"},
    {col: "log_VuongQuocVin", field: "create_time"},
    {col: "log_SieuAnhHung", field: "create_time"},
    {col: "log_NuDiepVien", field: "create_time"},
    {col: "log_ChiemTinh", field: "create_time"},
    {col: "log_mini_poker", field: "create_time"},
    {col: "log_cao_thap", field: "create_time"},
    {col: "log_cao_thap_win", field: "create_time"},
    {col: "bau_cua_transaction", field: "create_time"},
    {col: "bau_cua_results", field: "create_time"},
    {col: "log_no_hu_slot", field: "create_time"},
    {col: "log_no_hu_game_bai", field: "create_time"},
    {col: "log_hu_game_bai", field: "create_time"},
    {col: "log_game_detail", field: "create_time"},
    {col: "log_taixiu", field: "create_time"},
    {col: "log_sicbo", field: "create_time"}
];

wCollections.forEach(function(item) {
    try {
        w.getCollection(item.col).createIndex(
            {[item.field]: 1},
            {expireAfterSeconds: TTL_90_DAYS, name: "ttl_90days_" + item.field}
        );
        print("win123club." + item.col + " → TTL on " + item.field);
    } catch(e) { print("Skip win123club." + item.col + ": " + e.message); }
});

print("TTL indexes done — documents auto-expire after 90 days.");
