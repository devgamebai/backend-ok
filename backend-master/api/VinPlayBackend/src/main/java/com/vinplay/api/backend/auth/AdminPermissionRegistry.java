package com.vinplay.api.backend.auth;

import com.vinplay.api.backend.processors.role.RbacSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command-level RBAC registry for Admin CMS endpoints.
 *
 * Notes:
 * - Mapping is intentionally "any-of" (OR) to keep backward compatibility with
 *   legacy permission keys while new finance/game keys are rolled out.
 * - Super-admin bypass is handled in {@link #canAccessCommand(String, int)}.
 */
public final class AdminPermissionRegistry {

    private static final Map<Integer, List<String>> COMMAND_PERMISSIONS;

    static {
        Map<Integer, List<String>> m = new HashMap<Integer, List<String>>();

        // Game control (TaiXiu / Sicbo)
        registerAny(m, 8798, "game.taixiu.force", "game.config");
        registerAny(m, 8804, "game.sicbo.force", "game.config");
        registerAny(m, 8805, "game.fund.view", "game.config");
        registerAny(m, 8806, "game.fund.manage", "game.config");
        registerAny(m, 8799, "game.force_slot", "game.config");
        registerAny(m, 8800, "game.force_slot", "game.config");
        registerAny(m, 9620, "game.force_slot", "game.config");
        registerAny(m, 9622, "game.force_slot", "game.config");
        registerAny(m, 9623, "game.force_slot", "game.config");

        // User info / IP-device / login logs
        registerAny(m, 109, "user.ip_device.view", "user.view");
        registerAny(m, 9733, "user.loginlog.view", "user.ip_device.view", "user.view");

        // Bet history / win-loss style drilldowns
        registerAny(m, 2, "bet.history.view", "report.view");
        registerAny(m, 137, "bet.history.view");
        registerAny(m, 162, "bet.history.view");
        registerAny(m, 163, "bet.history.view");
        registerAny(m, 501, "bet.history.view");
        registerAny(m, 502, "bet.history.view");
        registerAny(m, 503, "bet.history.view");
        registerAny(m, 504, "bet.history.view");
        registerAny(m, 505, "bet.history.view", "report.view");
        registerAny(m, 506, "bet.history.view", "report.view");
        registerAny(m, 507, "bet.history.view", "report.view");
        registerAny(m, 511, "bet.history.view", "report.view");
        registerAny(m, 512, "bet.history.view", "report.view");
        registerAny(m, 513, "bet.history.view");
        registerAny(m, 516, "bet.history.view", "report.view");
        registerAny(m, 606, "finance.winloss.view");
        registerAny(m, 9930, "bet.history.view", "report.view");

        // Finance logs (view)
        registerAny(m, 9104, "finance.deposit.view", "deposit.view", "report.view");
        registerAny(m, 721, "finance.withdraw.view", "withdrawal.view", "report.view");
        registerAny(m, 112, "finance.transaction.view", "report.view");
        registerAny(m, 1122, "finance.transaction.view", "report.view");
        registerAny(m, 115, "finance.transaction.view", "report.view");
        registerAny(m, 9610, "finance.deposit.view", "deposit.view");
        registerAny(m, 9616, "finance.deposit.view", "deposit.view");
        registerAny(m, 9630, "finance.withdraw.view", "withdrawal.view");
        registerAny(m, 9652, "finance.withdraw.view", "withdrawal.view");
        registerAny(m, 9920, "finance.credit_wallet", "finance.deposit.view", "deposit.view", "report.view"); // Get credit wallet balance
        registerAny(m, 9925, "finance.credit_wallet", "finance.deposit.view", "deposit.view", "report.view"); // Get credit wallet history
        registerAny(m, 9926, "finance.credit_wallet", "finance.deposit.view", "deposit.view", "report.view"); // Get admin credit wallet history

        // Finance actions (approve/reject)
        registerAny(m, 9611, "finance.deposit.approve", "deposit.approve");
        registerAny(m, 9612, "finance.deposit.approve", "deposit.approve");
        registerAny(m, 9614, "finance.deposit.approve", "deposit.approve");
        registerAny(m, 9615, "finance.deposit.approve", "deposit.approve");
        registerAny(m, 9613, "finance.deposit.reject", "deposit.reject");
        registerAny(m, 9631, "finance.withdraw.approve", "withdrawal.approve");
        registerAny(m, 9653, "finance.withdraw.approve", "withdrawal.approve");
        registerAny(m, 9632, "finance.withdraw.reject", "withdrawal.reject");
        registerAny(m, 9654, "finance.withdraw.reject", "withdrawal.reject");
        registerAny(m, 9921, "finance.credit_wallet"); // Admin credit wallet topup
        registerAny(m, 9922, "finance.credit_wallet", "finance.deposit.approve", "finance.withdraw.approve"); // Agent transfer credit wallet (admin view)
        registerAny(m, 9923, "finance.credit_wallet", "finance.deposit.approve", "finance.withdraw.approve"); // Agent deposit credit wallet (admin view)
        registerAny(m, 9924, "finance.credit_wallet"); // Admin revoke credit wallet

        // RBAC / admin-management surfaces
        registerAny(m, 9820, "system.admin_account");
        registerAny(m, 9821, "system.admin_account");
        registerAny(m, 9822, "system.admin_account");
        registerAny(m, 9713, "system.admin_account");
        registerAny(m, 9700, "system.roles");
        registerAny(m, 9701, "system.roles");
        registerAny(m, 9702, "system.roles");
        registerAny(m, 9703, "system.roles");
        registerAny(m, 9705, "system.roles");
        registerAny(m, 9706, "system.roles");
        registerAny(m, 9716, "system.roles");
        registerAny(m, 9704, "system.permissions");
        registerAny(m, 9709, "system.permissions");
        registerAny(m, 9710, "system.permissions");
        registerAny(m, 9711, "system.permissions");
        registerAny(m, 9712, "system.rbac_audit");
        registerAny(m, 9732, "system.rbac_audit");

        // ── Dashboard reports (SUN-907 RBAC expansion) ───────────────────────
        registerAny(m, 7, "report.view");        // REPORT_DASHBOARD
        registerAny(m, 9, "report.view");        // REPORT_MONEY_TOTAL
        registerAny(m, 12, "report.view");       // REPORT_TOP_GAME
        registerAny(m, 18, "report.view");       // REPORT_TOTAL_REVENUE
        registerAny(m, 19, "report.view");       // REPORT_DAILY_REVENUE
        registerAny(m, 164, "report.view");      // REPORT_NEW_PLAYERS_DAILY
        registerAny(m, 165, "report.view");      // REPORT_PLAYER_STATS

        // ── User management ──────────────────────────────────────────────────
        registerAny(m, 104, "user.view");                       // Search users (autocomplete)
        registerAny(m, 9910, "user.view");                      // List users
        registerAny(m, 9911, "user.view");                      // Get user detail
        registerAny(m, 9912, "user.manage");                    // Create user
        registerAny(m, 9913, "user.manage");                    // Update user
        registerAny(m, 9914, "user.manage");                    // Delete user
        registerAny(m, 9101, "user.manage");                    // Change password
        registerAny(m, 100, "user.balance_adjust");             // Cộng/trừ tiền user

        // ── Admin money log ──────────────────────────────────────────────────
        registerAny(m, 9906, "finance.transaction.view", "report.view"); // List admin money logs

        // ── Bank management (platform + user banks) ──────────────────────────
        registerAny(m, 8801, "user.view");                      // Update user bank
        registerAny(m, 8802, "user.view");                      // Search user bank
        registerAny(m, 8803, "user.manage");                    // Delete user bank
        registerAny(m, 8819, "system.config");                  // Create/update bank name
        registerAny(m, 8820, "system.config");                  // List bank names
        registerAny(m, 8821, "system.config");                  // Delete bank name
        registerAny(m, 9520, "system.config");                  // Search admin bank
        registerAny(m, 9521, "system.config");                  // Insert/update admin bank
        registerAny(m, 9522, "system.config");                  // Delete admin bank

        // ── Game config (operator console extras) ────────────────────────────
        registerAny(m, 8807, "game.fund.view", "game.config");  // Get live bet list TX/SB
        registerAny(m, 9621, "game.force_slot", "game.config"); // Get slot game symbols

        // ── RTP / Win-Rate control (SUN-907) ─────────────────────────────────
        registerAny(m, 9771, "game.rtp.manage");                // Update game RTP config
        registerAny(m, 9773, "game.rtp.manage");                // Set user RTP override
        registerAny(m, 9774, "game.rtp.manage");                // Delete user RTP override
        registerAny(m, 9775, "game.rtp.view", "report.view");   // Get RTP config audit
        registerAny(m, 9780, "game.rtp.view", "report.view");   // Get house overall P&L
        registerAny(m, 9783, "game.rtp.view", "report.view");   // Get user P&L detail
        registerAny(m, 9784, "game.rtp.view", "report.view");   // Get player distribution
        registerAny(m, 9785, "game.rtp.view", "report.view");   // Get player heatmap (RTP targeting)

        // ── Cashback / Hoàn thua ─────────────────────────────────────────────
        registerAny(m, 9801, "cashback.view", "report.view");   // List cashback configs
        registerAny(m, 9802, "cashback.manage");                // Update cashback config
        registerAny(m, 9803, "cashback.manage");                // Create cashback program
        registerAny(m, 9804, "cashback.view", "report.view");   // List cashback logs
        registerAny(m, 9806, "cashback.manage");                // Trigger cashback payout
        registerAny(m, 9807, "cashback.manage");                // Batch cashback payout
        registerAny(m, 9808, "cashback.manage");                // Reject cashback
        registerAny(m, 9809, "cashback.manage");                // Check cashback expiry
        registerAny(m, 9810, "cashback.view", "report.view");   // Get cashback changelog
        registerAny(m, 9814, "cashback.view", "report.view");   // Get cashback log game detail

        // ── Promotion (Khuyến mãi nạp) ───────────────────────────────────────
        registerAny(m, 9640, "system.config");                  // Create deposit promotion
        registerAny(m, 9641, "system.config", "report.view");   // List deposit promotions
        registerAny(m, 9642, "system.config");                  // Update deposit promotion
        registerAny(m, 9643, "report.view");                    // List deposit promotion logs (read-only)

        // ── Agent management (admin-side) ────────────────────────────────────
        registerAny(m, 9847, "agent.manage");                   // Admin make agent (promote to TĐL)

        // ── Game Catalog (unified AWC/GSC + per-user block) ──────────────────
        registerAny(m, 9980, "game.catalog.view");                              // List providers
        registerAny(m, 9981, "game.catalog.view");                              // List games
        registerAny(m, 9982, "game.catalog.manage");                            // Toggle active
        registerAny(m, 9983, "game.catalog.manage");                            // Sync from provider
        registerAny(m, 9984, "game.catalog.view");                              // List user game blocks
        registerAny(m, 9985, "game.catalog.manage");                            // Add user game block
        registerAny(m, 9986, "game.catalog.manage");                            // Remove user game block

        // ── Admin GiftCode with Rollover (2026-05-01) ─────────────────────────
        registerAny(m, 9940, "finance.promotion.manage");                        // Create giftcode batch
        registerAny(m, 9941, "finance.promotion.manage", "finance.promotion.view", "report.view"); // List giftcodes (raw)
        registerAny(m, 9942, "finance.promotion.manage");                        // Deactivate giftcode batch
        registerAny(m, 9943, "finance.promotion.manage", "finance.promotion.view", "report.view"); // List campaigns
        registerAny(m, 9944, "finance.promotion.manage", "finance.promotion.view", "report.view"); // Campaign detail

        COMMAND_PERMISSIONS = Collections.unmodifiableMap(m);
    }

    private AdminPermissionRegistry() {
    }

    public static boolean isPermissionControlled(int commandId) {
        return COMMAND_PERMISSIONS.containsKey(commandId);
    }

    public static List<String> getRequiredPermissions(int commandId) {
        List<String> perms = COMMAND_PERMISSIONS.get(commandId);
        if (perms == null) {
            return Collections.emptyList();
        }
        return perms;
    }

    public static String describeRequiredPermissions(int commandId) {
        List<String> perms = getRequiredPermissions(commandId);
        if (perms.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < perms.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(perms.get(i));
        }
        return sb.toString();
    }

    public static boolean canAccessCommand(String adminNickname, int commandId) {
        if (!isPermissionControlled(commandId)) {
            return true;
        }
        if (adminNickname == null || adminNickname.isEmpty()) {
            return false;
        }
        if (RbacSupport.hasSuperAdminRole(adminNickname)) {
            return true;
        }

        List<String> required = getRequiredPermissions(commandId);
        for (String permissionKey : required) {
            if (RbacSupport.hasPermission(adminNickname, permissionKey)) {
                return true;
            }
        }
        return false;
    }

    private static void registerAny(Map<Integer, List<String>> map, int commandId, String... permissionKeys) {
        Set<String> orderedUnique = new LinkedHashSet<String>();
        if (permissionKeys != null) {
            for (String key : permissionKeys) {
                if (key != null && !key.trim().isEmpty()) {
                    orderedUnique.add(key.trim());
                }
            }
        }
        map.put(commandId, Collections.unmodifiableList(new ArrayList<String>(orderedUnique)));
    }
}
