// MongoDB Indexes & Views for Game History
// This script is idempotent - safe to run multiple times
// Run with: mongo -u admin -p password --authenticationDatabase admin < mongo-init-indexes.js

db = db.getSiblingDB('win123club');

// === INDEXES on slot log collections ===
// Speeds up history queries from 9s → 50ms
var logCollections = ['log_KhoBau', 'log_SieuAnhHung', 'log_NuDiepVien', 'log_VuongQuocVin'];
logCollections.forEach(function(c) {
    if (db.getCollectionNames().indexOf(c) >= 0) {
        db[c].createIndex({user_name: 1, _id: -1}, {background: true, name: 'idx_user_history'});
        db[c].createIndex({result: 1, _id: -1}, {background: true, name: 'idx_result_leaderboard'});
    }
});

// === VIEWS mapping client game names to server collection names ===
// Client sends gn=Spartan but server logs to log_KhoBau
var viewMappings = {
    'log_Spartan': 'log_KhoBau',           // Slot3 (Than Tai)
    'log_Audition': 'log_KhoBau',          // Slot1 (Kho Bau variant)
    'log_ChiemTinh': 'log_SieuAnhHung',   // Slot6 (Pirate King)
    'log_CHIEMTINH': 'log_SieuAnhHung',   // Slot6 uppercase
    'log_Benley': 'log_SieuAnhHung',      // Slot7 (Ngu Long)
    'log_BENLEY': 'log_SieuAnhHung',      // Slot7 uppercase
    'log_MAYBACH': 'log_NuDiepVien',      // Slot10 (Candy)
    'log_BIKINI': 'log_SieuAnhHung',      // Slot11 (Bikini)
    'log_Galaxy': 'log_VuongQuocVin',     // Slot8 (Galaxy)
};

Object.keys(viewMappings).forEach(function(viewName) {
    var source = viewMappings[viewName];
    try { db[viewName].drop(); } catch(e) {}
    try {
        db.createView(viewName, source, []);
    } catch(e) {
        // View might already exist as a collection, skip
    }
});

// === SUN-XXX (agency betting-history perf fix, 2026-04-13) ===
// /api/betting (c=9843) was doing full COLLSCAN + in-memory SORT across
// log_taixiu (49k), log_sicbo (79k), and log_money_user_vin (5.1M) on every
// request. For master agents with >5000-player subtrees this produced 40–60s
// response times. Compound indexes bring this to <1s.
//
// Two-index strategy per collection:
//   (user/nick, create_time DESC) → best for small-scope queries (few players)
//   (create_time DESC)            → best for huge-scope queries (~all players),
//                                    planner walks latest-first and filters in RAM.
//
// Idempotent: createIndex is a no-op if the same spec already exists.
// timeDir = sort direction that matches the actual query:
//   most game-log queries sort create_time DESC ("latest bets first")
//   supplementMissingBalances on log_money_user_vin sorts trans_time ASC (first deduction)
var perfIndexTargets = [
    // Collection,              userField,   timeField,     timeDir, userIdxName,     timeIdxName
    ['log_taixiu',               'user_name', 'create_time', -1, 'idx_user_time', 'idx_time'],
    ['log_sicbo',                'user_name', 'create_time', -1, 'idx_user_time', 'idx_time'],
    ['log_game_detail',          'user_name', 'create_time', -1, 'idx_user_time', 'idx_time'],
    ['log_game',                 'nick_name', 'create_time', -1, 'idx_nick_time', 'idx_time'],
    ['log_money_user_vin',       'nick_name', 'trans_time',   1, 'idx_nick_time', null],
    ['bau_cua_transaction_detail','user_name','create_time', -1, 'idx_user_time', 'idx_time'],
    ['log_no_hu_slot',           'nick_name', 'create_time', -1, 'idx_nick_time', 'idx_time']
];
perfIndexTargets.forEach(function(t) {
    var col = t[0], userField = t[1], timeField = t[2], timeDir = t[3], userIdxName = t[4], timeIdxName = t[5];
    if (db.getCollectionNames().indexOf(col) < 0) return;
    try {
        var userIdx = {}; userIdx[userField] = 1; userIdx[timeField] = timeDir;
        db[col].createIndex(userIdx, { background: true, name: userIdxName });
    } catch(e) { print('skip ' + col + '.' + userIdxName + ' — ' + e.message); }
    if (timeIdxName) {
        try {
            var timeIdx = {}; timeIdx[timeField] = timeDir;
            db[col].createIndex(timeIdx, { background: true, name: timeIdxName });
        } catch(e) { print('skip ' + col + '.' + timeIdxName + ' — ' + e.message); }
    }
});

// === SUN-1108 (agency betting-history GSC scaling, 2026-04-25) ===
// /api/betting (c=9843) → GameHistoryService.fetchAll iterates 8 Mongo
// collections under MONGO_SOURCES, each filtering by user_name $in [nicks]
// and sorting create_time DESC. log_gsc_bets in particular is projected
// to grow to 100M+ rows once GSC live-casino traffic ramps; without a
// compound (user_name, create_time DESC) index every query is a COLLSCAN
// over the full collection.
//
// Compound order: equality field (user_name / nick_name) FIRST so the $in
// filter binds tightly, then create_time DESC for the sort. The 8th
// collection in MONGO_SOURCES (log_ChiemTinh) is a pass-through view on
// log_SieuAnhHung — view inherits underlying collection's index, no
// separate index needed. log_cao_thap uses nick_name (not user_name).
//
// Adding here so any fresh deploy / staging rebuild gets the same indexes
// the prod hotfix put down on 2026-04-25.
var gscMongoSources = [
    {col: 'log_gsc_bets',          userField: 'user_name'},
    {col: 'log_KhoBau',             userField: 'user_name'},
    {col: 'log_VuongQuocVin',       userField: 'user_name'},
    {col: 'log_SieuAnhHung',        userField: 'user_name'},
    {col: 'log_NuDiepVien',         userField: 'user_name'},
    {col: 'log_mini_poker',         userField: 'user_name'},
    {col: 'log_cao_thap',           userField: 'nick_name'},
    {col: 'bau_cua_transaction',    userField: 'user_name'}
];
gscMongoSources.forEach(function(s) {
    if (db.getCollectionNames().indexOf(s.col) < 0) return;
    try {
        var idx = {};
        idx[s.userField] = 1;
        idx['create_time'] = -1;
        var name = s.userField + '_1_create_time_-1';
        db[s.col].createIndex(idx, {background: true, name: name});
    } catch (e) {
        print('skip ' + s.col + '.' + s.userField + ',create_time — ' + e.message);
    }
});

// === INDEX on user_login_info.time_log for c=110 USER_LOGIN_LOG_DUPLICATE ===
// The duplicate-IP aggregation opens with
//   { $match: { time_log: { $gte: tsFrom, $lte: tsTo } } }
// so a single-key ascending index on time_log lets the planner do a bounded
// IXSCAN instead of a full COLLSCAN. Mirrors the standalone migration at
// install/config/mongo/changes/2026-05-11-user-login-info-time-log-index.js
// — included here so a fresh deploy via deploy.sh / docker-init also picks
// it up. createIndex is idempotent.
try {
    db.user_login_info.createIndex(
        { time_log: 1 },
        { name: 'idx_user_login_info_time_log', background: true }
    );
    print('user_login_info.time_log index created');
} catch (e) {
    print('skip user_login_info.time_log — ' + e.message);
}

print('MongoDB indexes and views initialized.');
