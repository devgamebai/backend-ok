package com.vinplay.dal.rebate;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Real-time commission calculation — called on every bet in LogMoneyUserExtraProcessor.
 *
 * SUN-764 / SUN-750 / SUN-751 redesign (2026-04-10):
 *  - Admin configures a per-game "player cashback rate" in tbl_cashback_game_config.
 *  - When a user bets, the rebate cascade now has the player as the lowest tier at
 *    that configured rate. Differential commission is computed as before, but with
 *    the user rate as the floor instead of 0.
 *  - Player cashback goes into rebate_logs as PENDING (rebate_type=SELF) and credits
 *    users.vin only after the player hits the Claim button (handled by a separate
 *    portal processor). Downline rebates for agents remain instant credit into
 *    agency_wallet exactly as before.
 *  - If the admin has not configured a player rate for the game, the cascade falls
 *    back to the legacy behaviour (floor = 0, no player cashback).
 *  - Orphan users (parent_agent_id IS NULL) never earn cashback — Q7 confirmed by PM.
 */
public class RealTimeCommission {

    private static final Logger logger = Logger.getLogger("vbee");

    // SUN-1248: rebate_amount column is DECIMAL(20,4) after the same-day
    // migration; all per-bet money math is BigDecimal at scale=4 to keep
    // `volume * rate / 100` exact for 2-decimal rates. Wallet credit
    // rounds the scale-4 amount to whole-vin via amountForWallet
    // (HALF_UP scale=0) — same contract as
    // LogMoneyUserExtraProcessor / AutoCommissionPipeline.
    private static final int AMOUNT_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    // SUN-799: period_start / period_end in rebate_logs represent the Seoul
    // calendar day of the bet. The rolling-history filter (GetRebateLogs4Agency)
    // receives Seoul dates from FE, so we MUST anchor "today" to Asia/Seoul
    // regardless of the JVM default TZ (which was UTC before the migration and
    // produced 123 off-by-one-day rows — backfilled in fix_rebate_logs_period_tz.sql).
    private static final TimeZone SEOUL_TZ = TimeZone.getTimeZone("Asia/Seoul");
    private static String todaySeoul() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setTimeZone(SEOUL_TZ);
        return fmt.format(new Date());
    }

    // Kill-switch: set RTC_PLAYER_CASHBACK_DISABLED=1 to fall back to legacy cascade
    // (no player tier, no PENDING insert) in case the new logic misbehaves in prod.
    private static boolean isPlayerCashbackDisabled() {
        String v = System.getenv("RTC_PLAYER_CASHBACK_DISABLED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /**
     * Calculate and insert commission for a single bet.
     * @param nickname user who placed the bet
     * @param betAmount absolute value of money_exchange
     * @param gameAction game action name (e.g. "TaiXiu", "BauCua")
     */
    // Non-game actions that should NEVER generate commission.
    // Admin topups, deposits, withdrawals, and cron-generated actions are not bets.
    private static final java.util.Set<String> EXCLUDED_ACTIONS = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "Admin", "DepositApprove", "DepositReject",
                    "RechargeByCard", "RechargeByVinCard", "RechargeByMegaCard",
                    "RechargeByBank", "RechargeByIAP", "RechargeBySMS",
                    "RechargeManual", "RechargePrincePay", "RechargeClickPay",
                    "RechargePaywell", "TopupVTCPay",
                    "CashOutByCard", "CashOutByTopUp", "CashOutByClickPay",
                    "CashOutByPrincePay", "RefundRecharge", "RefundFee",
                    "RequestCashout", "WithdrawBankRefund",
                    "BankWithdraw", "CashOut", "Withdraw", "WithdrawBank",
                    "deposit", "Deposit", "DepositBank", "DepositCard", "DepositManual",
                    "TransferMoney", "NapXu", "ChargeSMS",
                    "GiftCode", "GiftCodeMKT", "GiftCodeVH",
                    "GcAgent", "GcAgentExport",
                    "VQMM", "VQVIP", "CashoutByVP",
                    "EventVPBonus", "EventVP", "BonusTopDS",
                    "KhoBauVqFree", "NuDiepVienVqFree", "SieuAnhHungVqFree", "VuongQuocVinVqFree",
                    "DepositPromo", "deposit_promotion",
                    // SUN-980 — CQ9 fish game-launch wallet transfer. Amount is
                    // the full balance pulled into CQ9 side, not a real bet.
                    // WithdrawProcess.java tags these with this action when
                    // valid_bet_amount and bet_amount are both 0. Real fish
                    // bets come through as gsc_1009_<game_code> once pushbet
                    // is enabled on the GSC+/CQ9 side (SUN-1057).
                    "Cq9FishTransfer"
            ));

    public static void calculate(String nickname, long betAmount, String gameAction) {
        if (betAmount <= 0 || nickname == null || nickname.isEmpty()) return;
        // Only game bets generate commission — skip admin, deposit, withdraw, promo actions.
        if (gameAction == null || gameAction.isEmpty() || EXCLUDED_ACTIONS.contains(gameAction)) {
            return;
        }

        try {
            // 1. Get user's id + parent_agent_id in a single query.
            long userId = -1;
            int parentAgentId = -1;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, parent_agent_id FROM users WHERE nick_name = ?")) {
                ps.setString(1, nickname);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    userId = rs.getLong("id");
                    parentAgentId = rs.getInt("parent_agent_id");
                    if (rs.wasNull()) parentAgentId = -1;
                }
                rs.close();
            }
            // Q7 rule: no cashback for orphan users (not belonging to any agency)
            if (parentAgentId <= 0 || userId <= 0) return;

            // 2. Look up per-game player cashback rate from tbl_cashback_game_config.
            // Returns 0 if admin has not configured a rate for this game — in that case
            // the cascade falls back to legacy behaviour (floor = 0, no player tier).
            BigDecimal userRate = isPlayerCashbackDisabled() ? BigDecimal.ZERO : getPlayerCashbackRate(gameAction);

            String today = todaySeoul();
            BigDecimal volume = BigDecimal.valueOf(betAmount);

            // 3. PLAYER cashback: insert as PENDING (claim-required).
            //    Stored in rebate_logs with rebate_type='SELF', agent_user_id=userId,
            //    agent_nickname=user nickname (so the claim endpoint can find it).
            if (userRate.signum() > 0) {
                BigDecimal userCashback = volume.multiply(userRate)
                        .divide(ONE_HUNDRED, AMOUNT_SCALE, RoundingMode.HALF_UP);
                if (userCashback.signum() > 0) {
                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO rebate_logs (agent_user_id, agent_nickname, agent_level, " +
                             "period_start, period_end, period_type, total_f1_volume, " +
                             "rebate_percentage, rebate_amount, share_percentage, own_percentage, " +
                             "child_percentage, differential_pct, share_amount, net_rebate, " +
                             "status, note, rebate_type, player_nickname, game_action) " +
                             "VALUES (?,?,0, ?,?, 'REALTIME', ?, ?,?,0,?,?,?,0,?, 'PENDING', ?, 'SELF', ?, ?)")) {
                        int idx = 1;
                        ps.setLong(idx++, userId);          // agent_user_id stores user id here
                        ps.setString(idx++, nickname);      // agent_nickname stores user nick
                        ps.setString(idx++, today + " 00:00:00");
                        ps.setString(idx++, today + " 23:59:59");
                        ps.setLong(idx++, betAmount);
                        ps.setBigDecimal(idx++, userRate);
                        ps.setBigDecimal(idx++, userCashback);
                        ps.setBigDecimal(idx++, userRate);
                        ps.setBigDecimal(idx++, BigDecimal.ZERO);  // prev max rate = 0 for user floor
                        ps.setBigDecimal(idx++, userRate);         // differential = full user rate
                        ps.setBigDecimal(idx++, userCashback);
                        ps.setString(idx++, "Cashback " + gameAction + " bet=" + betAmount);
                        // SUN-1201: schema-first grouping — for SELF rows the
                        // player IS the agent, so player_nickname = nickname.
                        ps.setString(idx++, nickname);
                        ps.setString(idx, gameAction);
                        ps.executeUpdate();
                    }
                    logger.debug("RealTimeCommission: user cashback PENDING user=" + nickname
                            + " game=" + gameAction + " rate=" + userRate.toPlainString()
                            + "% amount=" + userCashback.toPlainString());
                }
            }

            // 4. Build the agent chain walking up useragent.parentid from parent_agent_id.
            //    Unlike the old code we no longer add a "self-rebate" entry for agents
            //    playing their own account — agents get the same flat user_rate floor and
            //    their commission_rate only applies to bets placed BY THEIR DOWNLINE.
            //
            //    SUN-1248: read commission_rate as BigDecimal so the chain rates carry
            //    DECIMAL(5,2) precision into the differential math without a DOUBLE
            //    round-trip. useragent.commission_rate is DECIMAL(5,2) on prod.
            List<int[]> chain = new ArrayList<>();
            List<BigDecimal> rates = new ArrayList<>();

            int currentId = parentAgentId;
            int maxDepth = 5;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                while (currentId > 0 && maxDepth-- > 0) {
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, parentid, commission_rate FROM useragent WHERE id = ?");
                    ps.setInt(1, currentId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { rs.close(); ps.close(); break; }
                    int agentId = rs.getInt("id");
                    BigDecimal rate = rs.getBigDecimal("commission_rate");
                    if (rate == null) rate = BigDecimal.ZERO;
                    int parentId = rs.getInt("parentid");
                    chain.add(new int[]{agentId});
                    rates.add(rate);
                    rs.close();
                    ps.close();
                    currentId = parentId > 0 ? parentId : -1;
                }
            }

            if (chain.isEmpty()) return;

            // 5. Walk the chain in order (closest agent first → master agent last).
            //    The cascade floor is the user rate (not 0). Each level earns the
            //    differential between its own rate and the previous (lower) level's rate.
            //    All downline rebates are credited INSTANTLY to agency_wallet.
            BigDecimal prevMaxRate = userRate;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                for (int i = 0; i < chain.size(); i++) {
                    int agentId = chain.get(i)[0];
                    BigDecimal ownRate = rates.get(i);
                    BigDecimal diffRate = ownRate.subtract(prevMaxRate);

                    // SUN-1015: Even when diffRate <= 0 (commission_rate=0%), still write
                    // a rebate_logs entry with amount=0 so the rolling API shows bet volume.
                    // Only the first ancestor (direct parent) gets the zero-amount log.
                    BigDecimal commissionAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE);
                    if (diffRate.signum() > 0) {
                        commissionAmount = volume.multiply(diffRate)
                                .divide(ONE_HUNDRED, AMOUNT_SCALE, RoundingMode.HALF_UP);
                    } else if (i > 0) {
                        // Non-direct ancestors with no differential — skip entirely
                        if (ownRate.compareTo(prevMaxRate) > 0) prevMaxRate = ownRate;
                        continue;
                    }

                    // Look up the agent nickname for the log entry.
                    String agentNick = "";
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "SELECT nickname FROM vinplay_admin.useragent WHERE id = ?")) {
                        ps2.setInt(1, agentId);
                        ResultSet rs2 = ps2.executeQuery();
                        if (rs2.next()) agentNick = rs2.getString("nickname");
                        rs2.close();
                    }

                    // Wallet ledger holds whole-vin (BIGINT). Round the scale-4
                    // rebate amount HALF_UP to whole vin for the wallet credit;
                    // the scale-4 value still goes into rebate_logs. Same
                    // contract LogMoneyUserExtraProcessor uses (SUN-1150/1209).
                    long commissionWalletAmount = commissionAmount
                            .setScale(0, RoundingMode.HALF_UP)
                            .longValueExact();
                    String logStatus = commissionWalletAmount > 0 ? "PAID" : "TRACKED";
                    try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rebate_logs (agent_user_id, agent_nickname, agent_level, " +
                        "period_start, period_end, period_type, total_f1_volume, " +
                        "rebate_percentage, rebate_amount, share_percentage, own_percentage, " +
                        "child_percentage, differential_pct, share_amount, net_rebate, " +
                        "status, note, rebate_type, player_nickname, game_action) " +
                        "VALUES (?,?,0, ?,?, 'REALTIME', ?, ?,?,0,?,?,?,0,?, ?, ?, 'DOWNLINE', ?, ?)")) {
                        int idx = 1;
                        ps.setInt(idx++, agentId);
                        ps.setString(idx++, agentNick);
                        ps.setString(idx++, today + " 00:00:00");
                        ps.setString(idx++, today + " 23:59:59");
                        ps.setLong(idx++, betAmount);
                        ps.setBigDecimal(idx++, ownRate);
                        ps.setBigDecimal(idx++, commissionAmount);
                        ps.setBigDecimal(idx++, ownRate);
                        ps.setBigDecimal(idx++, prevMaxRate);
                        ps.setBigDecimal(idx++, diffRate.signum() > 0 ? diffRate : BigDecimal.ZERO);
                        ps.setBigDecimal(idx++, commissionAmount);
                        ps.setString(idx++, logStatus);
                        ps.setString(idx++, "Realtime from " + nickname + " game=" + gameAction);
                        // SUN-1201 grouping columns — DOWNLINE row's source
                        // player is the bettor (`nickname`), game is the
                        // bet's game action.
                        ps.setString(idx++, nickname);
                        ps.setString(idx, gameAction);
                        ps.executeUpdate();
                    }

                    // Credit instantly to the agent's agency wallet (skip if zero — SUN-1015).
                    if (commissionWalletAmount > 0) {
                        boolean credited = RebateService.creditAgencyWallet(agentId, commissionWalletAmount,
                                "COMMISSION_DOWNLINE", nickname, gameAction,
                                "Realtime commission from " + nickname + " game=" + gameAction);
                        if (!credited) {
                            logger.error("RealTimeCommission: agency wallet credit FAILED agent=" + agentNick
                                    + " amount=" + commissionWalletAmount
                                    + " — rebate_log is PAID but wallet not updated!");
                        }
                    }

                    logger.debug("RealTimeCommission: agent=" + agentNick + " diff=" + diffRate.toPlainString()
                            + "% amount=" + commissionAmount.toPlainString()
                            + " wallet=" + commissionWalletAmount
                            + " floor=" + prevMaxRate.toPlainString()
                            + " from=" + nickname);

                    if (ownRate.compareTo(prevMaxRate) > 0) prevMaxRate = ownRate;
                }
            }
        } catch (Exception e) {
            logger.error("RealTimeCommission.calculate error user=" + nickname, e);
        }
    }

    /**
     * Look up the admin-configured player cashback rate for the given game.
     * Returns 0 if no active config or no rate for this game (→ legacy cascade).
     *
     * Reads from tbl_cashback_game_config joined with the active tbl_cashback_config
     * program. Matches game_code OR game_name against the bet's gameAction (case-
     * insensitive) so admins can configure either identifier.
     */
    private static BigDecimal getPlayerCashbackRate(String gameAction) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT g.rebate_percent FROM tbl_cashback_game_config g " +
                 "INNER JOIN tbl_cashback_config c ON c.id = g.config_id " +
                 "WHERE c.is_active = 1 AND g.is_active = 1 " +
                 "AND (LOWER(g.game_code) = LOWER(?) OR LOWER(g.game_name) = LOWER(?)) " +
                 "ORDER BY g.updated_at DESC LIMIT 1")) {
            ps.setString(1, gameAction);
            ps.setString(2, gameAction);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal rate = rs.getBigDecimal("rebate_percent");
                if (rate != null && rate.signum() > 0) return rate;
            }
        } catch (Exception e) {
            logger.warn("getPlayerCashbackRate lookup failed gameAction=" + gameAction
                    + " err=" + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
