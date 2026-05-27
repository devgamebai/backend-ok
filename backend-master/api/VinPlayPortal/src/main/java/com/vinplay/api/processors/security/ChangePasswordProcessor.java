package com.vinplay.api.processors.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.DoipassResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * HTTP change-password endpoint for player (SUN-834).
 *
 * <p>Replaces the WebSocket path in {@code Minigame/LobbyModule.doiPass} so
 * clients can change password without holding a bitzero socket. FE-friendly:
 * web, mobile, any HTTP transport works.
 *
 * <h3>Contract</h3>
 * <pre>
 * GET /api?c=134&un={username}&t={accessToken}&op={md5Old}&np={md5New}
 * </pre>
 *
 * <h3>Error codes</h3>
 * <table>
 *   <tr><td>0</td>    <td>Success</td></tr>
 *   <tr><td>1001</td> <td>Missing required param / internal error</td></tr>
 *   <tr><td>1002</td> <td>Invalid access token (not in cacheToken)</td></tr>
 *   <tr><td>1005</td> <td>User not found</td></tr>
 *   <tr><td>2001</td> <td>Nickname not set on account</td></tr>
 *   <tr><td>3</td>    <td>Wrong old password</td></tr>
 *   <tr><td>4</td>    <td>Facebook-linked account (can't change)</td></tr>
 *   <tr><td>5</td>    <td>Google-linked account (can't change)</td></tr>
 *   <tr><td>6</td>    <td>New password too short or same as old</td></tr>
 * </table>
 *
 * <p>On success, every accessToken currently held by this user is removed
 * from Hazelcast {@code cacheToken}. That fires {@code SessionKickListener}
 * (SUN-816) on every game server, disconnecting all active bitzero
 * sessions — a changed password invalidates every device at once.
 */
public class ChangePasswordProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final int MIN_PASSWORD_LENGTH = 6; // md5 is always 32 chars; guard raw submissions

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponseModel res = new BaseResponseModel(false, "1001");

        String username = request.getParameter("un");
        String token    = request.getParameter("t");
        String oldPass  = request.getParameter("op");
        String newPass  = request.getParameter("np");

        if (username == null || username.isEmpty()
                || token == null || token.isEmpty()
                || oldPass == null || oldPass.isEmpty()
                || newPass == null || newPass.isEmpty()) {
            return res.toJson();
        }

        // SUN-851: central policy — accepts MD5 hex passthrough, enforces
        // char-set + 6..32 length on raw submissions.
        String pwdError = com.vinplay.vbee.common.utils.PasswordPolicy.validate(newPass);
        if (pwdError != null) {
            res.setErrorCode("6");
            res.setMessage(pwdError);
            return res.toJson();
        }
        if (newPass.equals(oldPass)) {
            res.setErrorCode("6");
            res.setMessage("Mật khẩu mới không được trùng với mật khẩu cũ");
            return res.toJson();
        }

        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");

            // Validate the caller's token — must be in cacheToken and must
            // belong to the claimed username (nickname, which is what
            // PortalUtils stores as the value).
            String tokenOwnerNickname = tokenMap.get(token);
            if (tokenOwnerNickname == null) {
                res.setErrorCode("1002");
                return res.toJson();
            }

            UserServiceImpl userService = new UserServiceImpl();
            UserModel userModel = userService.getUserByUserName(username);
            if (userModel == null) {
                res.setErrorCode("1005");
                return res.toJson();
            }

            String nickname = userModel.getNickname();
            if (nickname == null || nickname.isEmpty()) {
                res.setErrorCode("2001");
                return res.toJson();
            }

            if (!nickname.equals(tokenOwnerNickname)) {
                // Token is valid but for a different user — block.
                res.setErrorCode("1002");
                return res.toJson();
            }

            IMap<String, UserCacheModel> userMap = hz.getMap("users");
            if (!userMap.containsKey(nickname)) {
                res.setErrorCode("1005");
                return res.toJson();
            }

            // Delegate to the shared service — same logic the WebSocket path uses.
            SecurityServiceImpl securityService = new SecurityServiceImpl();
            DoipassResponse doipassResult =
                    securityService.changePassword(nickname, oldPass, newPass, false);
            byte result = doipassResult.getResult();
            res.setErrorCode(String.valueOf(result));

            if (result == 0) {
                res.setSuccess(true);
                // Post-success: revoke every accessToken bound to this nickname.
                // SessionKickListener (SUN-816) will disconnect the bitzero
                // sessions on every game server.
                revokeAllTokens(tokenMap, nickname);
                try {
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
                    logger.info("ChangePasswordProcessor: password changed user=" + username
                            + " nick=" + nickname + " ip=" + ip);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            logger.warn("ChangePasswordProcessor error: " + e.getMessage(), e);
            res.setSuccess(false);
            res.setErrorCode("1001");
        }
        return res.toJson();
    }

    /**
     * Revoke every {@code cacheToken} entry whose value matches the nickname.
     * {@code IMap} has no reverse index, so we scan the key set locally and
     * remove in a single pass. The map is small (one entry per active
     * session, typically a few thousand tops), so O(n) here is acceptable.
     */
    private static void revokeAllTokens(IMap<String, String> tokenMap, String nickname) {
        try {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
                if (nickname.equals(entry.getValue())) {
                    toRemove.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                try { tokenMap.remove(key); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Don't fail the password change on a best-effort cleanup.
            logger.warn("ChangePasswordProcessor: revokeAllTokens failed for " + nickname
                    + ": " + e.getMessage());
        }
    }
}
