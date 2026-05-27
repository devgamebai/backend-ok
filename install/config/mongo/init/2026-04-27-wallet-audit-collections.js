// SUN-1141 — wallet integrity audit bot: collections + indexes
//
// Idempotent — safe to re-run; createIndex is a no-op if same key+name exists.
// log_money_user_vin already had idx_time_nick added in a separate migration
// (see docs/architecture/SUN-1141-wallet-integrity-audit-bot.md).

const win = db.getSiblingDB("win123club");

// 1. Snapshot: one row per audited user (bot's working memory)
win.createCollection("wallet_audit_snapshot", { capped: false });
win.wallet_audit_snapshot.createIndex({ last_audit_at: 1 }, { name: "idx_last_audit_at", background: true });
win.wallet_audit_snapshot.createIndex(
    { consecutive_mismatch: 1 },
    {
        name: "idx_mismatch_partial",
        partialFilterExpression: { consecutive_mismatch: { $gte: 1 } },
        background: true,
    }
);

// 2. Alert dedup: TTL'd; same user+drift bucket alerts at most 1× per 24h
win.createCollection("wallet_audit_alert_dedup", { capped: false });
win.wallet_audit_alert_dedup.createIndex(
    { expires_at: 1 },
    { name: "idx_ttl_expires_at", expireAfterSeconds: 0, background: true }
);
win.wallet_audit_alert_dedup.createIndex({ user: 1 }, { name: "idx_user", background: true });

print("✓ wallet_audit_snapshot + wallet_audit_alert_dedup collections + indexes ready");
print("  snapshot indexes:");
win.wallet_audit_snapshot.getIndexes().forEach((i) => print("   - " + i.name + " " + JSON.stringify(i.key)));
print("  dedup indexes:");
win.wallet_audit_alert_dedup.getIndexes().forEach((i) => print("   - " + i.name + " " + JSON.stringify(i.key)));
