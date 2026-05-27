// SUN-1112 (CQRS Phase 0) — initialize the vinplay.commission_history
// collection that will become the sole store of commission records.
//
// Phase 0 creates the collection empty + all indexes. No code path writes
// to it yet. Phase 1 enables the drainer and dual-write begins.
//
// Indexes are deliberately compound and cover the four read shapes:
//   * agent rolling history    → c=9541 (sorted by bet_at desc)
//   * player betting history   → c=9843 (sorted by bet_at desc)
//   * game-filtered queries    → narrows by agent + game
//   * SELF/DOWNLINE split      → reports filter on rebate_type
// Plus dedup (unique on bet_event_id+agent_id), retention (bet_at), and
// correction tracing (corrects_event_id sparse).
//
// Idempotent: createCollection / createIndex are no-ops when the target
// already exists with matching options.

(function () {
    var DB = "vinplay";
    var COLL = "commission_history";

    db = db.getSiblingDB(DB);

    if (!db.getCollectionNames().includes(COLL)) {
        db.createCollection(COLL);
        print("Created collection " + DB + "." + COLL);
    } else {
        print("Collection " + DB + "." + COLL + " already exists — skipping create");
    }

    var coll = db.getCollection(COLL);

    function ensureIndex(spec, options) {
        try {
            coll.createIndex(spec, options || {});
            print("  ✓ index " + JSON.stringify(spec) + (options ? " " + JSON.stringify(options) : ""));
        } catch (e) {
            print("  ! index " + JSON.stringify(spec) + " failed: " + e.message);
        }
    }

    // Dedup — drainer upserts on this composite key
    ensureIndex({bet_event_id: 1, agent_id: 1}, {unique: true, name: "uk_bet_event_agent"});

    // Read patterns
    ensureIndex({agent_id: 1, bet_at: -1},                              {name: "ix_agent_betat"});
    ensureIndex({player_nickname: 1, bet_at: -1},                       {name: "ix_player_betat"});
    ensureIndex({agent_id: 1, game_action: 1, bet_at: -1},              {name: "ix_agent_game_betat"});
    ensureIndex({agent_id: 1, rebate_type: 1, bet_at: -1},              {name: "ix_agent_type_betat"});

    // Time-range scans (purges, daily aggregation builds, ops queries)
    ensureIndex({bet_at: 1}, {name: "ix_betat"});

    // Correction-tracing (sparse: only correction rows have this field)
    ensureIndex({corrects_event_id: 1}, {sparse: true, name: "ix_corrects"});

    print("commission_history collection setup complete on " + DB);
})();
