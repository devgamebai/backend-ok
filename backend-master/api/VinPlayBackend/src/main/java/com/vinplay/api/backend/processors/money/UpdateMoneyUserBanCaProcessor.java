package com.vinplay.api.backend.processors.money;

import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

/**
 * c=8797 — Transfer money between main platform (vinplay) and BanCa (cgame).
 * Called by BanCa server when player deposits/withdraws from fish game.
 *
 * SUN-1000: After transfer, publishes LogMoneyUserMessage to RMQ so the
 * commission pipeline (LogMoneyUserExtraProcessor) can calculate rolling.
 * Only BET transactions (money < 0 = player depositing into BanCa) trigger
 * commission. Win returns (money > 0) are excluded by the pipeline's
 * moneyExchange < 0 filter.
 *
 * Params: nn (nickname), mn (money change: negative=deduct, positive=add), h (hash)
 *
 * Auth: hash h = md5(nickname + moneyStr + secret) where secret is
 * env BANCA_XXENG_SECRET (default "gamebai#66@88" for legacy compat with
 * the C# side at banca/Core/Libs/Database/EpicApi.cs). GitLab #31 added
 * the validation — previously the param was accepted but never checked.
 */
public class UpdateMoneyUserBanCaProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final String BANCA_GAME_ID = "99";
    private static final String DEFAULT_SECRET = "gamebai#66@88";

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String moneyStr = request.getParameter("mn");
            String hashParam = request.getParameter("h");

            if (nickname == null || nickname.isEmpty() || moneyStr == null || moneyStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            // GitLab #31: validate hash before doing anything. Prevents anyone
            // who guesses the URL from moving money in/out of BanCa. Previous
            // revision accepted the `h` param but never checked it.
            if (hashParam == null || hashParam.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "hash required");
                return response.toString();
            }
            String secret = System.getenv("BANCA_XXENG_SECRET");
            if (secret == null || secret.isEmpty()) secret = DEFAULT_SECRET;
            String expected = md5(nickname + moneyStr + secret);
            if (!expected.equalsIgnoreCase(hashParam)) {
                logger.warn("UpdateMoneyUserBanCaProcessor: bad hash from caller nn=" + nickname);
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "invalid hash");
                return response.toString();
            }

            long money = Long.parseLong(moneyStr);

            UserServiceImpl userService = new UserServiceImpl();
            BaseResponseModel result = userService.updateMoneyFromAdmin(
                    nickname, money, "vin", "BanCaTransfer", "banca",
                    "BanCa transfer " + money);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", new JSONObject());

                // SUN-1000: publish to RMQ so commission pipeline sees BanCa bets.
                // serviceName must be numeric for isCommissionEligibleMessage().
                // actionName="Fish" maps to game_key "fish" in mapActionToGameKey().
                // Only negative money (bet/deposit into BanCa) triggers commission
                // because the pipeline filters moneyExchange < 0.
                try {
                    int userId = 0;
                    try {
                        IMap<String, UserCacheModel> users = HazelcastClientFactory.getInstance().getMap("users");
                        UserCacheModel u = users.get(nickname);
                        if (u != null) userId = u.getId();
                    } catch (Exception ignored) {}

                    LogMoneyUserMessage logMsg = new LogMoneyUserMessage(
                            userId, nickname, "Fish", BANCA_GAME_ID,
                            0L, money, "vin",
                            "BanCa bet " + money, 0, false, false);
                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", logMsg, 601);
                } catch (Exception rmqErr) {
                    logger.warn("SUN-1000: BanCa RMQ publish failed for " + nickname + ": " + rmqErr.getMessage());
                }
            } else {
                response.put("success", false);
                response.put("errorCode", result.getErrorCode());
                logger.warn("UpdateMoneyUserBanCaProcessor failed nn=" + nickname +
                        " money=" + money + " error=" + result.getErrorCode());
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
        } catch (Exception e) {
            logger.error("UpdateMoneyUserBanCaProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5001");
        }
        return response.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
