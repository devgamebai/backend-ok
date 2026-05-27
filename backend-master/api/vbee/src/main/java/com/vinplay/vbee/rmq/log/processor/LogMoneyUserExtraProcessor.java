/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.models.cache.TransactionList
 *  com.vinplay.vbee.common.response.LogMoneyUserResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.log.processor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.TransactionList;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.main.VBeeMain;
import java.text.ParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class LogMoneyUserExtraProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        LogMoneyUserMessage message = (LogMoneyUserMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        // Volume tracking for withdrawal eligibility: SUN-673
        // Track ANY non-bot vin transaction that's part of gameplay so wins also count.
        // Previously only `moneyExchange < 0` (bet/loss) qualified, which silently dropped
        // volume for winning spins where the publishLogMoney message reports net (positive) value.
        try {
            if (!message.isBot() && "vin".equalsIgnoreCase(message.getMoneyType())) {
                long me = message.getMoneyExchange();
                String volServiceName = message.getServiceName();
                String actionName = message.getActionName();
                // SUN-673 INSTRUMENTATION: log every candidate so we can see why volume isn't tracking
                logger.info("SUN-673 vol check user=" + message.getNickname()
                        + " action=" + actionName + " service=" + volServiceName
                        + " me=" + me + " isBot=" + message.isBot());
                if (this.isCommissionEligibleMessage(message)) {
                    // SUN-1205/1206: prefer message.validBetAmount if the
                    // upstream caller (e.g. WithdrawProcess for Dream
                    // Gaming) populated it via the seamless wager push.
                    // Falls back to abs(moneyExchange) for the legacy
                    // path so existing callers behave identically.
                    long override = message.getValidBetAmount();
                    long betAmount = override > 0L ? override : Math.abs(me);
                    int volUserId = message.getUserId();
                    String volNickname = message.getNickname();
                    com.vinplay.dal.withdraw.VolumeTrackingService.addBetVolume(volUserId, volNickname, volServiceName, betAmount);
                    com.vinplay.dal.withdraw.VolumeTrackingService.updateWithdrawalStatus(volUserId);

                    // SUN-1205/1206 — Dream Gaming (product_code 1052) ships
                    // valid_bet_amount = bet_amount in BOTH the seamless
                    // BET push AND the deposit/settle push (placeholders).
                    // The post-hedge truth is only published via GSC's
                    // wager-detail PULL API, with a ~4s lag after settle.
                    // Posting commission at BET time would over-pay on
                    // hedge bets (Banker+Player full hedge → real
                    // valid_bet=0 but our message carries 600k). Defer
                    // commission for these actions; GscWagerReconciler
                    // posts it once with the GSC-truth volume after
                    // settlement.
                    if (actionName != null && actionName.startsWith("gsc_1052_")) {
                        logger.info("[SUN-1205] deferring rebate for Dream wager — action="
                                + actionName + " user=" + message.getNickname()
                                + " (reconciler will post with GSC-truth volume)");
                    } else {
                        // Commission: only triggerAutoCommission (new system) runs here.
                        // RealTimeCommission.calculate() was removed to prevent double-write
                        // into rebate_logs — both systems write to the same table via the same
                        // differential chain, and hasExistingAutoCommission() only dedup-checks
                        // the AUTO_COMMISSION note format, not the old "Realtime from …" rows.
                        // Keeping both would produce 2 rebate_logs rows per bet → 2× real money.
                        this.triggerAutoCommission(message, betAmount);
                    }
                }
            }
        } catch (Exception volEx) {
            logger.error((Object)"volume tracking error in LogMoneyUserExtraProcessor", (Throwable)volEx);
        }
        if (!message.isBot()) {
            block14 : {
                long transId = 0L;
                int moneyType = -1;
                int queryType = -1;
                if (message.getMoneyType().equalsIgnoreCase("vin")) {
                    transId = VBeeMain.moneyVinReferenceId;
                    moneyType = 1;
                    if (message.getMoneyExchange() > 0L) {
                        if (Consts.NAP_VIN.contains(message.getActionName())) {
                            queryType = 3;
                        }
                    } else if (Consts.TIEU_VIN.contains(message.getActionName())) {
                        queryType = 5;
                    }
                } else {
                    transId = VBeeMain.moneyXuReferenceId;
                    moneyType = 2;
                    if (message.getMoneyExchange() > 0L && Consts.NAP_XU.contains(message.getActionName())) {
                        queryType = 4;
                    }
                }
                LogMoneyUserResponse model = new LogMoneyUserResponse();
                model.transId = transId;
                model.serviceName = message.getServiceName();
                model.description = message.getDescription();
                model.currentMoney = message.getCurrentMoney();
                model.moneyExchange = message.getMoneyExchange();
                model.transactionTime = message.getCreateTime();
                com.vinplay.vbee.common.cache.DistCache<String, TransactionList> transMap =
                        com.vinplay.vbee.common.cache.CacheFactory.get("cacheTransaction", TransactionList.class);
                this.pushNewTransaction(transMap, message.getNickname(), model, moneyType);
                if (queryType > 2) {
                    this.pushNewTransaction(transMap, message.getNickname(), model, queryType);
                }
                try {
                    MessageBusFactory.get("queue_report").publish("queue_report", (BaseMessage)message, (int)701);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    if (!message.getMoneyType().equals("vin")) break block14;
                    if (message.getActionName().equals("TaiXiu") && (message.getServiceName().equals("T\u00e0i x\u1ec9u - T\u00e1n l\u1ed9c") || message.getServiceName().equals("T\u00e0i x\u1ec9u - R\u00fat l\u1ed9c"))) {
                        return true;
                    }
                    IMap reportMap = client.getMap("cacheReports");
                    String date = "";
                    try {
                        date = VinPlayUtils.getDateFromDateTime((String)message.getCreateTime());
                    }
                    catch (ParseException e2) {
                        date = VinPlayUtils.getCurrentDate();
                    }
                    this.pushReportMap((IMap<String, ReportModel>)reportMap, message.getNickname(), message.getActionName(), date, message.getMoneyExchange(), message.getFee(), message.isVp(), message.isBot());
                }
            }
            return true;
        }
        return true;
    }

    private void pushNewTransaction(com.vinplay.vbee.common.cache.DistCache<String, TransactionList> transMap, String nickname, LogMoneyUserResponse model, int queryType) {
        String key = nickname + "-" + queryType;
        if (transMap.containsKey(key)) {
            TransactionList tranList = transMap.get(key);
            if (tranList != null) {
                tranList.add(model);
                transMap.put(key, tranList, 72L, TimeUnit.HOURS);
            }
        }
    }

    private void pushReportMap(IMap<String, ReportModel> reportMap, String nickname, String actionname, String date, long money, long fee, boolean playGame, boolean isBot) {
        String key = nickname + "," + actionname + "," + date;
        try {
            if (reportMap.containsKey((Object)key)) {
                try {
                    ReportModel reportModel = (ReportModel)reportMap.get((Object)key);
                    if (playGame) {
                        if (money > 0L) {
                            ReportModel reportModel2 = reportModel;
                            reportModel2.moneyWin += money;
                        } else {
                            ReportModel reportModel3 = reportModel;
                            reportModel3.moneyLost += money;
                        }
                    } else {
                        ReportModel reportModel4 = reportModel;
                        reportModel4.moneyOther += money;
                    }
                    ReportModel reportModel5 = reportModel;
                    reportModel5.fee += fee;
                    reportMap.put(key, reportModel);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                }
            } else {
                ReportModel reportModel = new ReportModel();
                reportModel.isBot = isBot;
                if (playGame) {
                    if (money > 0L) {
                        reportModel.moneyWin = money;
                    } else {
                        reportModel.moneyLost = money;
                    }
                } else {
                    reportModel.moneyOther = money;
                }
                reportModel.fee = fee;
                reportMap.put(key, reportModel);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
    }

    private void triggerAutoCommission(LogMoneyUserMessage message, long volume) {
        if (volume <= 0L) {
            return;
        }
        try {
            // SUN-818 (QC direction 2026-04-12): only games explicitly set in
            // vinplay.game_commission_rate pay commission. Games without a row
            // are treated as rate=0 (no commission). Resolve game_key once up
            // front from the bet's actionName so each agent's effective rate
            // can be looked up per-game. Real schema keys on string game_key
            // (e.g. "taixiu"), NOT on a numeric game_id.
            String gameKey = mapActionToGameKey(message.getActionName());

            String sourceKey = this.buildSourceKey(message);
            String[] period = this.resolveDailyPeriod(message.getCreateTime());
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int selfAgentId = -1;
                double selfAgentRate = 0.0;
                String selfAgentNickname = null;
                String refCode = null;

                try (PreparedStatement ps = conn.prepareStatement("SELECT referral_code FROM vinplay.users WHERE nick_name = ?")) {
                    ps.setString(1, message.getNickname());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            refCode = rs.getString("referral_code");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE nickname = ? OR username = ? LIMIT 1")) {
                    ps.setString(1, message.getNickname());
                    ps.setString(2, message.getNickname());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            selfAgentId = rs.getInt("id");
                            selfAgentNickname = rs.getString("nickname");
                            selfAgentRate = rs.getDouble("commission_rate");
                        }
                    }
                }

                List<AgentRate> chain = new ArrayList<AgentRate>();
                if (refCode != null && !refCode.trim().isEmpty()) {
                    chain.addAll(this.buildAgentChain(conn, refCode.trim(), gameKey));
                }
                // SUN-DRIFT: users.referral_code is a STRING joined against
                // useragent.code, while users.parent_agent_id is the INTEGER
                // useragent.id. Admin-side write paths (UpdateUserProcessor,
                // AdminMakeAgentProcessor, …) don't enforce that the two stay
                // in sync, so a typo or stale code can produce a row whose
                // referral_code does not resolve to any useragent. When that
                // happens the chain comes back empty and the player's upline
                // earns nothing. parent_agent_id, on the other hand, is the
                // FK the rest of the agency system trusts, so fall back to it
                // here. Warn so ops sees how often the safety net fires.
                if (chain.isEmpty()) {
                    int parentAgentId = -1;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT parent_agent_id FROM vinplay.users WHERE nick_name = ?")) {
                        ps.setString(1, message.getNickname());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                parentAgentId = rs.getInt("parent_agent_id");
                                if (rs.wasNull()) parentAgentId = -1;
                            }
                        }
                    }
                    if (parentAgentId > 0) {
                        chain.addAll(this.buildAgentChainById(conn, parentAgentId, gameKey));
                        if (!chain.isEmpty()) {
                            logger.warn((Object)("SUN-DRIFT: auto commission used parent_agent_id fallback "
                                    + "(referral_code=" + refCode + " did not resolve in useragent) user="
                                    + message.getNickname() + " parent_agent_id=" + parentAgentId));
                        }
                    }
                }
                if (selfAgentId > 0 && !this.containsAgent(chain, selfAgentId)) {
                    double selfEffectiveRate = this.effectiveRateFor(conn, selfAgentNickname, selfAgentRate, gameKey);
                    chain.add(0, new AgentRate(selfAgentId, selfEffectiveRate));
                }
                if (chain.isEmpty()) {
                    return;
                }

                // calculateDifferential emits one row per chain agent that
                // earns a non-zero amount; cascade-zero tiers are skipped
                // (see comment on calculateDifferential for the previousRate
                // advance that keeps upper-tier math correct).
                List<Distribution> distributions = this.calculateDifferential(chain, volume);
                for (Distribution distribution : distributions) {
                    String rebateType = selfAgentId > 0 && distribution.agentId == selfAgentId ? "SELF" : "DOWNLINE";
                    this.insertPendingLogIfAbsent(conn, distribution, message, volume, period[0], period[1], rebateType, sourceKey);
                }
            }
        } catch (Exception e) {
            logger.error((Object)("auto commission error nick=" + message.getNickname()), (Throwable)e);
        }
    }

    /**
     * SUN-818: per-game commission rate lookup with "unconfigured = 0" rule.
     *
     * <p>QC's direction (2026-04-12): a game only pays commission if the agent
     * has an explicit row in {@code vinplay.game_commission_rate} for that
     * (agent_nickname, game_key) pair. Any other case — row missing or game
     * action not mapped to a known game_key — means zero commission for that
     * agent on this game, regardless of the agent's global
     * {@code useragent.commission_rate}.
     *
     * <p>Real schema (matches {@code SetGameCommissionRateProcessor} and
     * {@code CalcDifferentialCommissionProcessor}):
     * {@code agent_nickname VARCHAR, game_key VARCHAR, rate DECIMAL(5,2)}.
     * There is no {@code status} column; there is no integer {@code game_id}.
     *
     * @param conn               live MySQL connection
     * @param agentNickname      agent's nickname (FK to game_commission_rate.agent_nickname)
     * @param globalRate         fallback when the bet's action name can't be mapped
     *                           to any known game_key (safety net for bet types that
     *                           slip past isCommissionEligibleMessage)
     * @param gameKey            lower-snake-case game key ("taixiu", "caothap", …) or
     *                           {@code null}/empty to signal "action unmappable"
     * @return effective rate (%) to plug into the differential chain
     */
    private double effectiveRateFor(Connection conn, String agentNickname, double globalRate, String gameKey) {
        if (agentNickname == null || agentNickname.isEmpty()) {
            return 0.0;
        }
        if (gameKey == null || gameKey.isEmpty()) {
            // Couldn't resolve the action to a known game_key — fall back to
            // the agent's global commission_rate so non-mapped bet types aren't
            // silently zeroed. Matches CalcDifferentialCommissionProcessor.
            return globalRate;
        }
        // SUN-1201 Phase 2: delegate to the shared resolver so the
        // EXACT → CATEGORY → PROVIDER chain runs uniformly across every
        // caller (here, RealTimeCommission, CommissionCronJob). The
        // local lookupRate / categoryKeyFor methods stay for now as
        // legacy entry points until we drop their last in-tree callers
        // (mostly tests).
        com.vinplay.dal.service.CommissionRateResolver.Result res =
                com.vinplay.dal.service.CommissionRateResolver.resolve(conn, agentNickname, gameKey);
        if (res.layer != com.vinplay.dal.service.CommissionRateResolver.Layer.NONE) {
            return res.rate;
        }
        // QC rule: no row at any layer => this game doesn't pay commission
        // for this agent. Differential math will naturally produce zero.
        return 0.0;
    }

    private Double lookupRate(Connection conn, String agentNickname, String gameKey) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT rate FROM vinplay.game_commission_rate " +
                "WHERE agent_nickname = ? AND game_key = ? LIMIT 1")) {
            ps.setString(1, agentNickname);
            ps.setString(2, gameKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("rate");
            }
        } catch (Exception e) {
            logger.warn("lookupRate failed nick=" + agentNickname + " gameKey=" + gameKey
                    + " err=" + e.getMessage());
        }
        return null;
    }

    /**
     * Map a per-game key to its category lookup key.
     * <ul>
     *   <li>{@code gsc_<pc>_<gc>}  → {@code live_cat_<Category>}   via
     *       {@code gsc_game_catalog.category} (SUN-category)</li>
     *   <li>Offline game_key in OfflineGameCategoryMap →
     *       {@code offline_cat_<Category>}</li>
     * </ul>
     * Returns {@code null} when no category can be inferred.
     */
    private String categoryKeyFor(String gameKey) {
        if (gameKey == null || gameKey.isEmpty()) return null;
        if (gameKey.startsWith("gsc_")) {
            // gsc_<pc>_<gc> → split and look up in gsc_game_catalog
            int p1 = gameKey.indexOf('_', 4);
            if (p1 < 0) return null;
            String pcStr = gameKey.substring(4, p1);
            String gc = gameKey.substring(p1 + 1);
            int pc;
            try { pc = Integer.parseInt(pcStr); } catch (NumberFormatException e) { return null; }
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                    .getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT category FROM vinplay.gsc_game_catalog "
                   + "WHERE product_code = ? AND game_code = ? LIMIT 1")) {
                ps.setInt(1, pc);
                ps.setString(2, gc);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String cat = rs.getString("category");
                        if (cat != null && !cat.isEmpty()) return "live_cat_" + cat;
                    }
                }
            } catch (Exception ignore) { /* catalog table may be absent — silent fallback */ }
            return null;
        }
        if (gameKey.startsWith("awc_")) {
            // SUN-AWC-COMM: delegate to the canonical CommissionRateResolver
            // so AWC's coarse catalog buckets (CASINO/SLOT/FISH/SPORT/EGAME)
            // get normalized to the GSC-aligned live_cat_<X> keys ops
            // actually configure (Baccarat/Slot/Fishing/Sports/GameShow/Other).
            // Earlier this file duplicated the lookup with the wrong tail
            // mapping (live_cat_Casino / live_cat_Fish / live_cat_Sport)
            // — keys that don't exist in game_commission_rate, so every
            // AWC bet resolved at rate=0 even when the TĐL's
            // live_cat_Baccarat row had 1.25.
            return com.vinplay.dal.service.CommissionRateResolver.categoryKeyFor(gameKey);
        }
        // Offline mapping is static
        String cat = com.vinplay.dal.service.OfflineGameCategoryMap.categoryOf(gameKey);
        if (cat != null) return "offline_cat_" + cat;
        return null;
    }

    /**
     * Map the RMQ bet's {@code action_name} (e.g. "TaiXiu", "BauCua", "MiniPoker")
     * to the game_key string stored in {@code game_commission_rate}
     * (e.g. "taixiu", "baucua", "minipoker").
     *
     * <p>Returns {@code null} for actions that don't correspond to any game_key
     * on file — the caller treats this as "fall back to agent's globalRate"
     * rather than silently zero the commission.
     */
    private static String mapActionToGameKey(String actionName) {
        if (actionName == null || actionName.isEmpty()) return null;
        // SUN-865: live-game action names already carry the canonical
        // game_key in `gsc_<product_code>_<game_code>` form (set by
        // thirdParty WithdrawProcess / DepositProcess). Return as-is so
        // rebate resolves via game_commission_rate directly. Admin configures
        // rate per live game using the same key via c=9849.
        if (actionName.startsWith("gsc_")) return actionName;
        if (actionName.startsWith("awc_")) return actionName;
        switch (actionName) {
            case "TaiXiu":        return "taixiu";
            case "TaiXiuMD5":     return "taixiu";      // same rate bucket as TaiXiu
            case "TaiXiuLive":    return "taixiu";
            case "TaiXiuSicbo":   return "taixiu";      // Sicbo-style minigame, paid as taixiu bucket
            case "SicBo":         return "taixiu";      // alias used by some clients
            case "TaiXiuST":      return "taixiu_st";
            case "CaoThap":       return "caothap";
            case "MiniPoker":     return "minipoker";
            case "BauCua":        return "baucua";
            case "XocDia":        return "xocdia";
            case "TLMN":          return "tlmn";
            case "BaCay":         return "bacay";
            case "Fish":
            case "FISH":          return "fish";
            // slot games — action names align with game_key rows
            case "Pokemon":       return "slot_pokemon";
            case "ChiemTinh":     return "slot_chiemtinh";
            case "Bikini":        return "slot_bikini";
            case "Galaxy":        return "slot_galaxy";
            case "ThanBai":       return "slot_thanbai";
            case "Bitcoin":       return "slot_bitcoin";
            case "TayDu":         return "slot_taydu";
            case "AngryBird":     return "slot_angrybird";
            case "ThanTai":       return "slot_thantai";
            case "TheThao":       return "slot_thethao";
            // 3rd-party providers
            case "WM": case "wm": return "wm";
            case "IBC": case "ibc": return "ibc";
            case "AG":  case "ag":  return "ag";
            case "CMD": case "cmd": return "cmd";
            case "EBET": case "ebet": return "ebet";
            case "SBO": case "sbo": return "sbo";
            default: return null;
        }
    }

    private List<AgentRate> buildAgentChain(Connection conn, String refCode, String gameKey) throws Exception {
        List<AgentRate> chain = new ArrayList<AgentRate>();
        int directId = -1;
        String directNickname = null;
        String ancestorsStr = null;
        double directRate = 0.0;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, nickname, ancestors, commission_rate FROM vinplay_admin.useragent WHERE code = ? LIMIT 1")) {
            ps.setString(1, refCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    directId = rs.getInt("id");
                    directNickname = rs.getString("nickname");
                    ancestorsStr = rs.getString("ancestors");
                    directRate = rs.getDouble("commission_rate");
                }
            }
        }
        if (directId <= 0) {
            return chain;
        }

        chain.add(new AgentRate(directId, this.effectiveRateFor(conn, directNickname, directRate, gameKey)));
        if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
            String normalizedAncestors = ancestorsStr.trim();
            if (!normalizedAncestors.matches("^[0-9,]+$")) {
                logger.warn("auto commission invalid ancestors format=" + normalizedAncestors + " refCode=" + refCode);
                return chain;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE id IN (" + normalizedAncestors + ") ORDER BY level DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ancId = rs.getInt("id");
                        String ancNickname = rs.getString("nickname");
                        double ancGlobalRate = rs.getDouble("commission_rate");
                        double ancRate = this.effectiveRateFor(conn, ancNickname, ancGlobalRate, gameKey);
                        chain.add(new AgentRate(ancId, ancRate));
                    }
                }
            }
        }
        return chain;
    }

    /**
     * SUN-DRIFT fallback: build the agent chain starting from a known
     * useragent.id rather than from users.referral_code. Used when
     * referral_code → useragent.code lookup returns nothing (drift between
     * the two parallel upline fields). Mirrors {@link #buildAgentChain}
     * row-for-row except for the entry query — direct agent is selected
     * by id, ancestors are still walked via the {@code ancestors} column.
     */
    private List<AgentRate> buildAgentChainById(Connection conn, int parentAgentId, String gameKey) throws Exception {
        List<AgentRate> chain = new ArrayList<AgentRate>();
        int directId = -1;
        String directNickname = null;
        String ancestorsStr = null;
        double directRate = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, nickname, ancestors, commission_rate FROM vinplay_admin.useragent WHERE id = ? LIMIT 1")) {
            ps.setInt(1, parentAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    directId = rs.getInt("id");
                    directNickname = rs.getString("nickname");
                    ancestorsStr = rs.getString("ancestors");
                    directRate = rs.getDouble("commission_rate");
                }
            }
        }
        if (directId <= 0) {
            return chain;
        }
        chain.add(new AgentRate(directId, this.effectiveRateFor(conn, directNickname, directRate, gameKey)));
        if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
            String normalizedAncestors = ancestorsStr.trim();
            if (!normalizedAncestors.matches("^[0-9,]+$")) {
                logger.warn((Object)("auto commission invalid ancestors format=" + normalizedAncestors + " parent_agent_id=" + parentAgentId));
                return chain;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE id IN (" + normalizedAncestors + ") ORDER BY level DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ancId = rs.getInt("id");
                        String ancNickname = rs.getString("nickname");
                        double ancGlobalRate = rs.getDouble("commission_rate");
                        double ancRate = this.effectiveRateFor(conn, ancNickname, ancGlobalRate, gameKey);
                        chain.add(new AgentRate(ancId, ancRate));
                    }
                }
            }
        }
        return chain;
    }

    private List<Distribution> calculateDifferential(List<AgentRate> chain, long volume) {
        // SUN-1086 (reopened 2026-04-25): the initial fix wrapped the
        // final multiplication in BigDecimal but STILL subtracted two
        // doubles first — so the IEEE-754 drift was baked in before the
        // wrap. `BigDecimal.valueOf(1.15 - 1.05)` is
        // `BigDecimal.valueOf(0.09999999999999987)` which preserves the
        // drift exactly, and
        // `3_000_000 × 0.09999999999999987 / 100` = 2999.99999… → FLOOR
        // = 2999 instead of 3000. Fix: do the subtraction in BigDecimal
        // space from the start, never round-trip through double.
        // SUN-1102: previousRate must advance whenever a tier participates
        // (differential > 0), even when the resulting amount rounds to 0;
        // otherwise the next tier up computes its differential against a
        // stale running max (visible as a TDL row with diff=0.20 instead
        // of 0.10 when DL1 was skipped). 2026-04-27: zero-amount rows are
        // no longer emitted at all — `rebate_logs.status` enum doesn't
        // include a "TRACKED" value, and the previous SUN-1102 visibility
        // pass was silently losing those writes to STRICT_TRANS_TABLES
        // ("Data truncated for column 'status'"). Cascade math still
        // correct because previousRate advance is preserved here.
        // SUN-1150: keep 2-decimal precision via HALF_UP scale=2.
        // SUN-1209 (2026-05-01): widened to scale=4 because per-bet
        // rounding to 2 decimals discards fractional cents that
        // accumulate over many bets — QC reported sum drifts of +0.03
        // across 15 rows. With rate having at most 2 decimals (1.25%,
        // 1.15%, 1.05%), volume × rate / 100 has at most 4 decimals,
        // so scale=4 is lossless. rebate_amount column was widened to
        // DECIMAL(20,4) by the matching migration. Display layer rounds
        // SUM(rebate_amount) back to 2 decimals at render time so the
        // FE shows clean cents.
        //
        // Emit a Distribution for EVERY chain agent — including those whose
        // computed amount rounds to 0.0000 (because their rate is 0% on this
        // game category, or differential resolves to 0). Required by admin
        // agency-rolling page so admins can see "this game was played, this
        // agent earned 0 because their rate is 0%".
        //
        // Status of the resulting row is set in insertPendingLogIfAbsent
        // based on rebate_type (SELF → PENDING, DOWNLINE → PAID), regardless
        // of amount. Wallet credit and claim sum gates are amount > 0 so
        // amount=0 rows are visibility-only.
        //
        // previousRate ALWAYS advances when this tier participates (differential
        // > 0) — keeps cascade math correct for upper tiers even when the
        // current tier rounds to amount=0.
        List<Distribution> out = new ArrayList<Distribution>();
        java.math.BigDecimal previousRate = java.math.BigDecimal.ZERO;
        for (AgentRate node : chain) {
            java.math.BigDecimal ownRate = java.math.BigDecimal.valueOf(node.commissionRate);
            java.math.BigDecimal differential = ownRate.subtract(previousRate);
            // SUN-1209: scale=4 keeps per-bet math lossless. For rates
            // with 2 decimals, volume × rate / 100 always fits exactly
            // in 4 decimals — HALF_UP only fires for the rare case of
            // a rate beyond 2 decimals.
            java.math.BigDecimal amount = java.math.BigDecimal.valueOf(volume)
                    .multiply(differential.signum() > 0 ? differential : java.math.BigDecimal.ZERO)
                    .divide(java.math.BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            out.add(new Distribution(node.agentId, node.commissionRate,
                    previousRate.doubleValue(),
                    differential.signum() > 0 ? differential.doubleValue() : 0.0,
                    amount));
            if (differential.signum() > 0) {
                previousRate = ownRate;
            }
        }
        return out;
    }

    private void insertPendingLogIfAbsent(Connection conn, Distribution distribution, LogMoneyUserMessage message,
                                          long volume, String periodStart, String periodEnd,
                                          String rebateType, String sourceKey) throws Exception {
        String note = this.buildAutoCommissionNote(message, sourceKey, rebateType);
        if (this.incrementExistingAutoCommission(conn, distribution, message, volume, note)) {
            return;
        }

        String agentNickname = "";
        try (PreparedStatement ps = conn.prepareStatement("SELECT nickname FROM vinplay_admin.useragent WHERE id = ? LIMIT 1")) {
            ps.setInt(1, distribution.agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    agentNickname = rs.getString("nickname");
                }
            }
        }

        // Status is driven purely by rebate_type:
        //   SELF      → PENDING (claim via c=3083 → vin)
        //   DOWNLINE  → PAID    (auto-credit agency_wallet)
        // Amount=0 rows still land with PENDING/PAID. Wallet credit and claim
        // amount are both gated on amount > 0 below, so a zero-amount row is
        // visibility-only — admin /rolling sees "this game was played, this
        // agent earned 0 because their rate is 0%".
        String status = "SELF".equals(rebateType) ? "PENDING" : "PAID";

        // SUN-1248: write source_key (per-txn dedup id) + wager_code
        // (round-level grouping key) so the row is a true ledger entry —
        // immutable, idempotent, and aggregatable at read time without
        // mutating existing rows.
        // SUN-GAME-FK Phase 2: dual-write game_id + category_id alongside
        // the legacy game_action string. Resolves via the unified games
        // catalog. Both columns NULLABLE so an unknown gameKey doesn't
        // block the INSERT — Phase 1 backfill covered all known shapes.
        com.vinplay.dal.service.GameLookup.Result fk =
                com.vinplay.dal.service.GameLookup.resolve(message.getActionName());

        // SUN-1252 follow-up 2026-05-18 — also populate round_id so the LS
        // Rolling renderer (GetRebateLogs4AgencyProcessor) can pass it to
        // AwcGameNameResolver.displayName(platform, gameCode, roundId) and
        // resolve per-table Sexy Baccarat tables (C07, M71, etc.) instead
        // of the generic "Sexy Baccarat" stub. For AWC the round_id equals
        // the wager_code on the vendor wire; for GSC they differ
        // (round_id is per-round, wager_code is per-txn UUID) — but the
        // resolver-via-roundId path is only used for AWC SEXYBCRT today,
        // where the two values are identical. LogMoneyUserMessage doesn't
        // carry round_id as a distinct field yet — we reuse wager_code,
        // which gives correct per-table resolution for AWC and harmless
        // (unused) values for GSC. If we ever need true round_id on GSC
        // we'll add LogMoneyUserMessage.roundId in a follow-up.
        String roundId = message.getWagerCode();

        String sql = "INSERT INTO vinplay.rebate_logs " +
                "(agent_user_id, agent_nickname, agent_level, period_start, period_end, period_type, " +
                "total_f1_volume, rebate_percentage, share_percentage, own_percentage, child_percentage, differential_pct, " +
                "rebate_amount, share_amount, net_rebate, status, note, rebate_type, " +
                "player_nickname, game_action, source_key, wager_code, round_id, game_id, category_id, created_at) " +
                "VALUES (?, ?, 0, ?, ?, 'DAILY', ?, ?, 0, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, distribution.agentId);
            ps.setString(2, agentNickname);
            ps.setString(3, periodStart + " 00:00:00");
            ps.setString(4, periodEnd + " 23:59:59");
            ps.setLong(5, volume);
            ps.setDouble(6, distribution.ownPct);
            ps.setDouble(7, distribution.ownPct);
            ps.setDouble(8, distribution.childPct);
            ps.setDouble(9, distribution.differentialPct);
            ps.setBigDecimal(10, distribution.amount);
            ps.setBigDecimal(11, distribution.amount);
            ps.setString(12, status);
            ps.setString(13, note);
            ps.setString(14, rebateType);
            // SUN-1201: schema-first grouping. The high-volume insert path
            // populates player_nickname + game_action so the agency LSR
            // (c=9541) can GROUP BY at SQL level instead of regex-parsing
            // the note column.
            ps.setString(15, message.getNickname());
            ps.setString(16, message.getActionName());
            // SUN-1248: source_key is per-txn dedup id (e.g. "gsc:<txn_id>").
            // wager_code groups sub-bets of one round (Evolution multi-bet).
            ps.setString(17, sourceKey);
            ps.setString(18, message.getWagerCode());
            // SUN-1252 follow-up: round_id for per-table catalog resolution.
            // Falls back to wager_code if the message didn't carry round_id
            // (older publishers, GSC paths) — for AWC SEXY they're equal.
            ps.setString(19, roundId);
            // SUN-GAME-FK Phase 2: NULL when resolver couldn't find a
            // matching games row — Phase 4 will tighten to NOT NULL once
            // dual-write coverage hits 100%.
            if (fk.hasGameId()) ps.setLong(20, fk.gameId);
            else ps.setNull(20, java.sql.Types.BIGINT);
            if (fk.hasCategoryId()) ps.setInt(21, fk.categoryId);
            else ps.setNull(21, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }

        // Instant credit to agency_wallet — DOWNLINE only. SELF commission
        // is PENDING; the agent claims it via c=3083 which credits their
        // in-game `vin` balance (handled in ClaimCashbackProcessor).
        // SUN-1150: creditAgencyWallet takes long — derive integer KRW from the
        // 2-decimal BigDecimal via amountForWallet (HALF_UP setScale(0)).
        if (distribution.amount.signum() > 0 && "DOWNLINE".equals(rebateType)) {
            try {
                boolean credited = com.vinplay.dal.rebate.RebateService.creditAgencyWallet(
                        distribution.agentId,
                        distribution.amountForWallet,
                        "COMMISSION_DOWNLINE",
                        String.valueOf(message.getNickname()),
                        String.valueOf(message.getActionName()),
                        "AUTO_COMMISSION from " + message.getNickname() + " game=" + message.getActionName());
                if (!credited) {
                    logger.error("insertPendingLogIfAbsent: agency_wallet credit FAILED agent="
                            + agentNickname + " amount=" + distribution.amount
                            + " — rebate_log is PAID but wallet not updated!");
                }
            } catch (Exception e) {
                logger.error("insertPendingLogIfAbsent: creditAgencyWallet threw agent="
                        + agentNickname + " amount=" + distribution.amount, e);
            }
        }
    }

    private boolean incrementExistingAutoCommission(Connection conn, Distribution distribution,
                                                    LogMoneyUserMessage message, long volume,
                                                    String note) throws Exception {
        // SUN-1248: this method now does NO-OP DEDUP, not aggregation.
        // The previous implementation incremented total_f1_volume / rebate_amount
        // when a row with matching note already existed — that mutated a
        // ledger row, violating the immutability principle and silently
        // double-counting on RMQ redelivery (since the same note arrives
        // multiple times for at-least-once delivery).
        //
        // New rule: every distinct event INSERTs its own immutable row.
        // The reader (RebateService.queryLogs / c=9541) GROUPs BY wager_code
        // and SUMs volume/amount to produce the operator's per-round Rolling
        // view — no mutation needed.
        //
        // This method now just CHECKS whether a row with this exact note
        // (= same source_key, same agent, same period) already exists.
        // If yes, skip the new INSERT (RMQ retry of the same event).
        // If no, return false so the caller proceeds with INSERT.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM vinplay.rebate_logs "
                        + "WHERE agent_user_id = ? AND note = ? LIMIT 1")) {
            ps.setInt(1, distribution.agentId);
            ps.setString(2, note);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Already inserted (RMQ retry). No-op — caller skips INSERT.
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAgent(List<AgentRate> chain, int agentId) {
        for (AgentRate rate : chain) {
            if (rate.agentId == agentId) {
                return true;
            }
        }
        return false;
    }

    private String[] resolveDailyPeriod(String createTime) {
        String datePart = null;
        if (createTime != null && createTime.length() >= 10) {
            datePart = createTime.substring(0, 10);
        }
        if (datePart == null || !datePart.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            // SUN-799: force Asia/Seoul so the fallback matches MySQL server TZ
            // and the rolling-history filter from FE (which sends Seoul dates).
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            datePart = fmt.format(new Date());
        }
        return new String[]{datePart, datePart};
    }

    private String buildSourceKey(LogMoneyUserMessage message) {
        if (message.getId() != null && !message.getId().trim().isEmpty()) {
            return message.getId().trim();
        }
        String actionName = message.getActionName();
        if (actionName != null && actionName.startsWith("gsc_")) {
            String explicitSource = extractSourceKeyFromDescription(message.getDescription());
            if (explicitSource != null && !explicitSource.isEmpty()) {
                return explicitSource;
            }
        }
        return String.valueOf(message.getNickname()) + "|" + String.valueOf(message.getActionName()) + "|" +
                String.valueOf(message.getServiceName()) + "|" + message.getMoneyExchange() + "|" +
                String.valueOf(message.getCreateTime());
    }

    static String extractSourceKeyFromDescription(String description) {
        if (description == null) {
            return null;
        }
        int idx = description.indexOf("source=");
        if (idx < 0) {
            return null;
        }
        int start = idx + "source=".length();
        int end = start;
        while (end < description.length()) {
            char ch = description.charAt(end);
            if (Character.isWhitespace(ch) || ch == '|') {
                break;
            }
            end++;
        }
        return end > start ? description.substring(start, end).trim() : null;
    }

    private String extractRebateType(String note) {
        if (note != null && note.contains(" type=DOWNLINE ")) {
            return "DOWNLINE";
        }
        return "SELF";
    }

    private String buildAutoCommissionNote(LogMoneyUserMessage message, String sourceKey, String rebateType) {
        return "AUTO_COMMISSION source=" + sourceKey
                + " type=" + String.valueOf(rebateType)
                + " user=" + String.valueOf(message.getNickname())
                + " action=" + String.valueOf(message.getActionName())
                + " service=" + String.valueOf(message.getServiceName());
    }

    /**
     * SUN-850 kill-switch. Default ON (GSC bets pay commission). Set env
     * GSC_COMMISSION_ENABLED=0 to revert to pre-SUN-850 behaviour where the
     * GAME3RD action names (ag/ibc2/wm/cmd) were excluded from the pipeline.
     */
    private static boolean isGscCommissionEnabled() {
        String v = System.getenv("GSC_COMMISSION_ENABLED");
        if (v == null || v.isEmpty()) return true;
        return !(v.equals("0") || v.equalsIgnoreCase("false"));
    }

    private boolean isCommissionEligibleMessage(LogMoneyUserMessage message) {
        if (message == null || message.isBot() || !"vin".equalsIgnoreCase(message.getMoneyType())) {
            return false;
        }
        // SUN-850: GSC+ bets now flow through this queue with a resolved
        // game_key actionName ("sbo", "ag", "wm", …). We intentionally no
        // longer exclude GAME3RD.LIST_GAME so those bets sink into the
        // rebate pipeline. Kill-switch env GSC_COMMISSION_ENABLED=0 restores
        // the pre-SUN-850 legacy exclusion if the new path misbehaves.
        if (!isGscCommissionEnabled()
                && Consts.GAME3RD.LIST_GAME.contains(message.getActionName())) {
            return false;
        }
        if (Consts.ADMIN.equals(message.getServiceName())) {
            return false;
        }
        if (Consts.REAL_DEPOSIT_VIN.contains(message.getActionName())
                || Consts.REAL_WITHDRAW_VIN.contains(message.getActionName())
                || "Cq9FishTransfer".equalsIgnoreCase(message.getActionName())) {
            return false;
        }
        String serviceName = message.getServiceName();
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(serviceName.trim());
            // SUN-979: only BET transactions (negative moneyExchange) are commission-eligible.
            // Win/settle transactions (positive) must NOT generate commission — they're not bets.
            return message.getMoneyExchange() < 0L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static class AgentRate {
        private final int agentId;
        private final double commissionRate;

        private AgentRate(int agentId, double commissionRate) {
            this.agentId = agentId;
            this.commissionRate = commissionRate;
        }
    }

    private static class Distribution {
        private final int agentId;
        private final double ownPct;
        private final double childPct;
        private final double differentialPct;
        // SUN-1150 / SUN-1209: rebate_amount column is DECIMAL(20,4).
        // amount stays at full 4-decimal precision (lossless for any
        // 2-decimal commission rate); amountForWallet is the integer-vin
        // form fed to creditAgencyWallet (which still takes long).
        // The wallet HALF_UP rounding bound is per-bet at most 0.5 vin —
        // accumulated drift between rebate_logs SUM and wallet sum is
        // capped at 0.5 × bet_count, intrinsic to whole-vin payouts.
        private final java.math.BigDecimal amount;
        private final long amountForWallet;

        private Distribution(int agentId, double ownPct, double childPct, double differentialPct,
                             java.math.BigDecimal amount) {
            this.agentId = agentId;
            this.ownPct = ownPct;
            this.childPct = childPct;
            this.differentialPct = differentialPct;
            this.amount = amount != null ? amount : java.math.BigDecimal.ZERO;
            this.amountForWallet = this.amount.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        }
    }
}
