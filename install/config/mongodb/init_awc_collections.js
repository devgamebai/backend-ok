// AWC (Asia Win Connect) MongoDB collections — SUN-945
// Run: mongosh vinplay < init_awc_collections.js

db = db.getSiblingDB("vinplay");

// log_awc_bets: mirrors log_gsc_bets — stores every bet/settle from AWC callbacks
db.createCollection("log_awc_bets");

db.log_awc_bets.createIndex({ "platform_tx_id": 1 }, { unique: true, name: "uk_platform_tx_id" });
db.log_awc_bets.createIndex({ "user_name": 1, "created_at": -1 }, { name: "idx_user_created" });
db.log_awc_bets.createIndex({ "user_id": 1, "created_at": -1 }, { name: "idx_userid_created" });
db.log_awc_bets.createIndex({ "round_id": 1 }, { name: "idx_round_id" });
db.log_awc_bets.createIndex({ "platform": 1, "game_code": 1 }, { name: "idx_platform_game" });
db.log_awc_bets.createIndex({ "action": 1 }, { name: "idx_action" });
db.log_awc_bets.createIndex({ "bet_time": 1 }, { name: "idx_bet_time" });
db.log_awc_bets.createIndex({ "created_at": 1 }, { expireAfterSeconds: 7776000, name: "ttl_90days" });

print("AWC collections and indexes created successfully.");
