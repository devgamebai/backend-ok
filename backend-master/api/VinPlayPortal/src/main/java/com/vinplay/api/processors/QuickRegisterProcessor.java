/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.service.impl.MarketingServiceImpl
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.usercore.utils.UserMakertingUtil
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.api.utils.PortalUtils;
import com.vinplay.dal.service.SigningBonusService;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.UserMakertingUtil;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.UserValidaton;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class QuickRegisterProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel res;
        block10 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String username = request.getParameter("un");
            String password = request.getParameter("pw");
            String captcha = request.getParameter("cp");
            String captchaId = request.getParameter("cid");
            String referralCode = request.getParameter("rc");
            String campaign = request.getParameter("utm_campaign");
            String medium = request.getParameter("utm_medium");
            String source = request.getParameter("utm_source");
            logger.debug(("Request quickRegister: username: " + username + ", password: " + password));
            res = new BaseResponseModel(false, "1001");
            try {
                int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                if (statusGame == StatusGames.MAINTAIN.getId() || statusGame == StatusGames.SANDBOX.getId()) {
                    res.setErrorCode("1114");
                    logger.debug(("Response login: " + res.toJson()));
                    return res.toJson();
                }
                // Internal-only smoke-test bypass for load tests / QC.
                // When SMOKE_TEST_BYPASS_KEY is set on the server AND the
                // caller submits matching X-Smoke-Test-Key header AND comes
                // from an RFC 1918 / loopback IP, captcha is skipped. Public
                // callers always go through the captcha path. See
                // SmokeTestBypass class doc for the full threat model.
                final boolean smokeBypass = com.vinplay.vbee.common.security
                        .SmokeTestBypass.isAllowed(request, "quick-register");

                if (username == null || password == null) break block10;
                // 2026-05-14: player register always requires captcha as an
                // anti-bot baseline — the CAPTCHA_REQUIRED env flag only
                // disables admin + agency login captcha, not signup.
                if (!smokeBypass && (captcha == null || captchaId == null)) break block10;
                // SUN-851: validate password against central policy before we
                // ever insert. Errors surface the Vietnamese message so FE
                // can relay it verbatim on the signup form.
                String pwdError = com.vinplay.vbee.common.utils.PasswordPolicy.validate(password);
                if (pwdError != null) {
                    res.setErrorCode("6");
                    res.setMessage(pwdError);
                    return res.toJson();
                }
                if (smokeBypass || PortalUtils.checkCaptcha(captcha, captchaId)) {
                    if (UserValidaton.validateUserName((String)username)) {
                        try {
                            UserServiceImpl userService = new UserServiceImpl();
                            res.setErrorCode(userService.insertUser(username, password));
                            logger.info("QuickRegister insertUser result=" + res.getErrorCode() + " user=" + username + " rc=" + referralCode);
                            if ("1006".equals(res.getErrorCode())) {
                                res.setMessage("Tên đăng nhập đã tồn tại!");
                                break block10;
                            }
                            if (!res.getErrorCode().equals("0")) break block10;
                            res.setSuccess(true);
                            try {
                                if (campaign != null && medium != null && source != null) {
                                    MarketingServiceImpl mktService = new MarketingServiceImpl();
                                    UserMarketingMessage message = new UserMarketingMessage(username, "", 0, VinPlayUtils.getCurrentDateMarketing(), campaign, medium, source);
                                    mktService.saveUserMarketing(message);
                                    UserMakertingUtil.newRegisterUser((String)campaign, (String)medium, (String)source);
                                }
                                UserDaoImpl dao = new UserDaoImpl();
                                int userId = dao.getIdByUsername(username);

                                // === REFERRAL CODE → LINK TO AGENT (với fallback về Company Agent) ===
                                logger.info("QuickRegister referralCode=" + referralCode + " userId=" + userId + " username=" + username);
                                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                                    int agentId = 0;
                                    String resolvedCode = null;

                                    // Bước 1: Thử tìm agent theo referral code (nếu có)
                                    // SUN-928: must check active=1 so deleted agents' codes are rejected
                                    if (referralCode != null && !referralCode.trim().isEmpty()) {
                                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                "SELECT id FROM vinplay_admin.useragent WHERE code = ? AND level > 0 AND active = 1")) {
                                            ps.setString(1, referralCode.trim());
                                            try (java.sql.ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                    agentId = rs.getInt(1);
                                                    resolvedCode = referralCode.trim();
                                                }
                                            }
                                        }
                                        if (agentId == 0) {
                                            logger.warn("Referral code not found or invalid, fallback to Company Agent: rc=" + referralCode);
                                        }
                                    }

                                    // Bước 2: Fallback — gán về Company Agent (code="1") nếu chưa có agent
                                    if (agentId == 0) {
                                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                "SELECT id FROM vinplay_admin.useragent WHERE code = '1' LIMIT 1")) {
                                            try (java.sql.ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                    agentId = rs.getInt(1);
                                                    resolvedCode = "1";
                                                }
                                            }
                                        }
                                    }

                                    // Bước 3: Gán agent vào user
                                    if (agentId > 0) {
                                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                "UPDATE users SET referral_code = ?, parent_agent_id = ? WHERE id = ?")) {
                                            ps.setString(1, resolvedCode);
                                            ps.setInt(2, agentId);
                                            ps.setInt(3, userId);
                                            ps.executeUpdate();
                                        }
                                        logger.info("Agent linked: user=" + username + " resolvedCode=" + resolvedCode + " agentId=" + agentId);
                                    } else {
                                        logger.error("CRITICAL: Company Agent (code=1) not found! User " + username + " has no parent agent.");
                                    }
                                } catch (Exception refEx) {
                                    logger.error("Referral code link error for user=" + username + " rc=" + referralCode, refEx);
                                }
                                // === END REFERRAL CODE ===

                                SecurityServiceImpl sercuSer = new SecurityServiceImpl();
                                String agent = PortalUtils.getUserAgent(request);
                                String deviceName = PortalUtils.getDeviceNameFromAgent(agent);
                                sercuSer.saveLoginInfo(userId, username, "", PortalUtils.getIpAddress(request), agent, 0, "web");

                                // === SIGNING BONUS ===
                                try {
                                    String deviceFp = request.getParameter("device_fp");
                                    if (deviceFp != null && !deviceFp.trim().isEmpty()) {
                                        com.vinplay.vbee.common.cache.DistCache<String, String> fpCache =
                                            com.vinplay.vbee.common.cache.CacheFactory.get("cache_device_fp", String.class);
                                        fpCache.put(username, deviceFp, 1, java.util.concurrent.TimeUnit.HOURS);
                                    }
                                } catch (Exception bonusEx) {
                                    logger.error("SigningBonus cache error for user=" + username, bonusEx);
                                }
                                // === END SIGNING BONUS ===

                                break block10;
                            }
                            catch (Exception e) {
                                logger.debug(e);
                            }
                        }
                        catch (SQLException e2) {
                            logger.debug(e2);
                        }
                        break block10;
                    }
                    res.setErrorCode("101");
                    break block10;
                }
                res.setErrorCode("115");
            }
            catch (Exception e3) {
                e3.printStackTrace();
            }
        }
        logger.debug(("Response quickRegister: " + res.toJson()));
        return res.toJson();
    }
}
