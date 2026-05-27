package com.vinplay.api.backend.processors.agent;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.dal.dao.AgencyWalletDao;
import com.vinplay.dal.dao.impl.AgencyWalletDaoImpl;
import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.usercore.dao.impl.AgentDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class GetAgencyWalletProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        
        try {
            // Accept either 'rc' (agent code — preferred by sunkr-agency) or 'nn' (nickname)
            String agentCode = request.getParameter("rc");
            String nickName = request.getParameter("nn");

            if ((agentCode == null || agentCode.isEmpty()) && (nickName == null || nickName.isEmpty())) {
                response.setErrorCode("1002");
                return response.toJson();
            }

            int agentId = -1;
            try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                java.sql.PreparedStatement stm;
                if (agentCode != null && !agentCode.isEmpty()) {
                    stm = conn.prepareStatement("SELECT id FROM vinplay_admin.useragent WHERE code = ?");
                    stm.setString(1, agentCode);
                } else {
                    stm = conn.prepareStatement("SELECT id FROM vinplay_admin.useragent WHERE nickname = ?");
                    stm.setString(1, nickName);
                }
                java.sql.ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    agentId = rs.getInt("id");
                }
                rs.close();
                stm.close();
            }
            if (agentId == -1) {
                response.setErrorCode("1002"); // Not an agent
                return response.toJson();
            }

            AgencyWalletDao walletDao = new AgencyWalletDaoImpl();
            long balance = walletDao.getBalance(agentId);

            // SUN-863: fetch credit_wallet balance for this agent
            long creditWalletBalance = 0L;
            try {
                CreditWalletDao creditWalletDao = new CreditWalletDaoImpl();
                creditWalletBalance = creditWalletDao.getBalance(agentId);
            } catch (Exception cwErr) {
                // Non-fatal — log and keep default 0
                logger.warn("GetAgencyWalletProcessor: credit_wallet lookup failed agentId=" + agentId, cwErr);
            }

            // SUN-778: join vinplay.users by the agent's nickname to return the
            // main game wallet (vin + xu) alongside the agency commission wallet.
            // FE /partner/detail page needs both balances side-by-side on one round-trip.
            // MUST read `vin` column (current balance), NOT `vin_total` (cumulative P&L).
            // Same class of bug as SUN-748 / SUN-791 — see docs/SUN-791_loi-nhuan-formula-fix.md.
            String resolvedNickName = null;
            long mainWalletVin = 0L;
            long mainWalletXu = 0L;
            try (java.sql.Connection userConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 java.sql.PreparedStatement userPs = userConn.prepareStatement(
                     "SELECT u.nick_name, u.vin, 0 AS xu " + // SUN-13xx: xu dropped, kept alias for response parity
                     "FROM vinplay.users u " +
                     "JOIN vinplay_admin.useragent ua " +
                     "  ON ua.nickname COLLATE utf8mb4_general_ci = u.nick_name " +
                     "WHERE ua.id = ? LIMIT 1")) {
                userPs.setInt(1, agentId);
                try (java.sql.ResultSet userRs = userPs.executeQuery()) {
                    if (userRs.next()) {
                        resolvedNickName = userRs.getString("nick_name");
                        mainWalletVin   = userRs.getLong("vin");
                        mainWalletXu    = userRs.getLong("xu");
                    }
                }
            } catch (Exception mainWalletErr) {
                // Non-fatal — keep agency wallet data and return defaults for main wallet.
                logger.warn("GetAgencyWalletProcessor: main wallet lookup failed agentId=" + agentId, mainWalletErr);
            }

            // SUN-842 pattern: overlay live vin/xu from Hazelcast users cache.
            // UserServiceImpl.updateMoney writes cache first, DB second (via RMQ
            // cmd=16). DB can lag the game HUD by up to the RMQ consumer latency.
            // Same pattern as DetailMemberOfAgencyProcessor (c=9543).
            if (resolvedNickName != null && !resolvedNickName.isEmpty()) {
                try {
                    com.hazelcast.core.IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> usersMap =
                            com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
                    com.vinplay.vbee.common.models.cache.UserCacheModel cached = usersMap.get(resolvedNickName);
                    if (cached != null) {
                        mainWalletVin = cached.getVin();
                        mainWalletXu  = cached.getXu();
                    }
                } catch (Exception cacheErr) {
                    logger.warn("GetAgencyWalletProcessor: users cache overlay failed for "
                            + resolvedNickName + " — falling back to DB values", cacheErr);
                }
            }

            JSONObject resJson = new JSONObject();
            resJson.put("success", true);
            resJson.put("errorCode", "0");
            resJson.put("agentId", agentId);
            // Backward-compat: `balance` is the legacy field name for the agency wallet.
            resJson.put("balance", balance);
            // New fields (SUN-778):
            resJson.put("nick_name", resolvedNickName != null ? resolvedNickName : "");
            resJson.put("main_wallet", mainWalletVin);
            resJson.put("main_wallet_xu", mainWalletXu);
            resJson.put("agency_wallet", balance);
            // SUN-863: credit_wallet balance (User can transfer money here via c=3056)
            resJson.put("credit_wallet", creditWalletBalance);

            return resJson.toString();

        } catch (Exception e) {
            logger.error("Error in GetAgencyWalletProcessor", e);
            response.setErrorCode("1001");
            return response.toJson();
        }
    }
}
