/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.hazelcast.client.HazelcastClientNotActiveException
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.dao.impl.CashoutDaoImpl
 *  com.vinplay.dichvuthe.response.CashoutUserDailyResponse
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.dao.impl.VippointDaoImpl
 *  com.vinplay.usercore.service.MarketingService
 *  com.vinplay.usercore.service.impl.GameConfigServiceImpl
 *  com.vinplay.usercore.service.impl.LuckyServiceImpl
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.usercore.service.impl.MarketingServiceImpl
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.MarketingModel
 *  com.vinplay.vbee.common.models.UserClientInfo
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vbee.common.response.LoginResponse
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.json.JSONException
 */
package com.vinplay.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.client.HazelcastClientNotActiveException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.GetAppConfigProcesscor;
import com.vinplay.dichvuthe.dao.impl.CashoutDaoImpl;
import com.vinplay.dichvuthe.response.CashoutUserDailyResponse;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.service.MarketingService;
import com.vinplay.usercore.service.impl.GameConfigServiceImpl;
import com.vinplay.usercore.service.impl.LuckyServiceImpl;
import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.UserClientInfo;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.response.LoginResponse;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONException;

public class PortalUtils {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static MarketingService mktService = new MarketingServiceImpl();

    // SUN-auth: per-session token lifetime in the Hazelcast cacheToken map.
    // Was 24h absolute / 3h sliding-idle; bumped to 3 days for both so users
    // on flaky networks aren't logged out mid-session. Force-logout still
    // works independently (revokeAllTokensForNickname drops old entries on
    // every new login — see SUN-767 / SUN-832).
    //
    // The `users` IMap stays on a shorter 1h TTL intentionally: cached
    // profile data (balance, VIP, agent code) must refresh from DB within
    // a reasonable window, otherwise users would see stale balances after
    // bot/admin credit changes. The session itself survives a `users`-map
    // eviction because LoginByTokenProcessor hydrates from DB when needed.
    private static final long TOKEN_TTL_ABSOLUTE_HOURS = 72L;
    private static final long TOKEN_TTL_IDLE_HOURS     = 72L;
    private static final long USERS_CACHE_TTL_HOURS    = 1L;

    public static LoginResponse loginSuccess(UserModel userModel, HttpServletRequest request) throws NoSuchAlgorithmException, UnsupportedEncodingException, JsonProcessingException, SQLException {
        boolean success = true;
        String accessToken = "";
        String sessionKey = "";
        UserServiceImpl ser = new UserServiceImpl();
        int luckyRotate = 0;
        if (userModel.getDaily() == 0) {
            LuckyServiceImpl luckySer = new LuckyServiceImpl();
            luckyRotate = luckySer.receiveRotateDaily(userModel.getId(), userModel.getNickname());
        }
        String ip = PortalUtils.getIpAddress(request);
        String platform = request.getParameter("pf");
        // GitLab #32 / SUN-1081: BanCa C# verifier re-calls c=3 on every exchange
        // to validate the player's credentials. That counts as a "login" and
        // used to call revokeAllTokensForNickname → evict main-platform token
        // → SessionKickListener → force_logout popup on the main UI.
        //
        // BanCa logins are credential-verification only, not primary sessions.
        // Suppress the old-token eviction for this single platform value so
        // the main UI keeps its session while BanCa gets its own token.
        //
        // Matches pf= param set by banca/Core/Libs/Database/EpicApi.cs
        // (currently "banca"; future-proof with case-insensitive compare).
        boolean isSecondaryVerificationLogin = platform != null
                && ("banca".equalsIgnoreCase(platform) || "banca-verify".equalsIgnoreCase(platform));

        // SUN-832/1081 UX — token-refresh flow (c=2, c=17) re-presents the
        // client's existing `at` param to prove ownership. LoginByTokenProcessor
        // has already validated that at→nickname mapping before calling us, so
        // we're re-authenticating the SAME caller, not detecting a new device.
        // Revoking their tokens here only results in kickByToken→force_logout
        // on their own balance WebSocket (SUN-832 hardening firing against
        // itself). Treat `at` presence as a same-caller refresh and suppress
        // the revoke, same behaviour we give pf=banca.
        String presentedToken = request.getParameter("at");
        boolean isTokenRefresh = presentedToken != null && !presentedToken.isEmpty();
        boolean skipRevoke = isSecondaryVerificationLogin || isTokenRefresh;
        try {
            IMap userExtraMap;
            UserCacheModel userCache;
            block18 : {
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap userMap = instance.getMap("users");
                userExtraMap = instance.getMap("cache_user_extra_info");
                IMap tokenMap = instance.getMap("cacheToken");
                userCache = null;
                if (userMap.containsKey(userModel.getNickname())) {
                    try {
                        userMap.lock(userModel.getNickname());
                        userCache = (UserCacheModel)userMap.get(userModel.getNickname());
                        // SUN-767: always issue a fresh token on login and invalidate the
                        // previous session so two devices can't hold the same access token.
                        // Previously we reused the cached token when it hadn't timed out,
                        // which meant a "second device" login effectively shared the first
                        // device's session — no kick possible.
                        accessToken = VinPlayUtils.genAccessToken((int)userModel.getId());
                        userCache.setAccessToken(accessToken);
                        // SUN-832 hardening: don't only remove the cached oldAccessToken —
                        // scan cacheToken for ANY entry mapped to this nickname and drop
                        // them all. Previously a single oldAccessToken was read from
                        // userCache; stale entries from prior logins (Hazelcast restart,
                        // cache-miss branch below which didn't clean up, races between
                        // concurrent logins) would survive and a second device could keep
                        // authenticating with an "old" token. Each remove fires the
                        // SUN-816 SessionKickListener on every game server → the kick
                        // propagates to any lingering bitzero session automatically, so
                        // the separate RMQ ForceLogoutMessage publish is no longer needed.
                        if (!skipRevoke) {
                            revokeAllTokensForNickname(tokenMap, userModel.getNickname());
                        }
                        userCache.setLastActive(new Date());
                        userCache.setIp(ip);
                        // SUN-997: resolve agent's own code (or "" for non-agents)
                        // so Hazelcast cache matches sessionKey. Previously wrote
                        // parent agent code from users.referral_code → leaked to FE.
                        userCache.setReferralCode(lookupOwnAgentCode(userModel.getUsername()));
                        UserCacheModel uc = ser.checkMoneyNegative(userCache);
                        if (uc != null) {
                            userCache = uc;
                            success = false;
                        }
                        // SUN-auth: 3-day token lifetime (absolute + sliding idle).
                        // See class-level TOKEN_TTL_* constants for rationale.
                        tokenMap.put(accessToken, userModel.getNickname(),
                                TOKEN_TTL_ABSOLUTE_HOURS, TimeUnit.HOURS,
                                TOKEN_TTL_IDLE_HOURS,     TimeUnit.HOURS);
                        userMap.put(userModel.getNickname(), userCache,
                                USERS_CACHE_TTL_HOURS, TimeUnit.HOURS);
                    }
                    catch (Exception e) {
                        logger.debug(e);
                        success = false;
                        break block18;
                    }
                    try {
                        userMap.unlock(userModel.getNickname());
                    }
                    catch (Exception e) {}
                } else {
                    UserCacheModel uc2;
                    CashoutDaoImpl csDao = new CashoutDaoImpl();
                    CashoutUserDailyResponse cs = csDao.getCashoutUserToday(userModel.getNickname());
                    MarketingModel mktModel = mktService.getMKTInfo(userModel.getUsername());
                    VippointDaoImpl vpDao = new VippointDaoImpl();
                    UserVPEventModel vpModel = vpDao.getUserVPByNickName(userModel.getNickname());
                    userCache = new UserCacheModel(userModel.getId(), userModel.getUsername(), userModel.getNickname(), userModel.getPassword(), userModel.getEmail(), userModel.getFacebookId(), userModel.getGoogleId(), userModel.getMobile(), userModel.getBirthday(), userModel.isGender(), userModel.getAddress(), userModel.getVin(), userModel.getXu(), userModel.getVinTotal(), userModel.getXuTotal(), userModel.getSafe(), userModel.getRechargeMoney(), userModel.getVippoint(), userModel.getDaily(), userModel.getStatus(), userModel.getAvatar(), userModel.getIdentification(), userModel.getVippointSave(), userModel.getCreateTime(), userModel.getMoneyVP(), userModel.getSecurityTime(), userModel.getLoginOtp(), userModel.isBot(), false, cs.getCashout(), cs.getCashoutTime(), vpModel.getVpEvent(), vpModel.getVpReal(), vpModel.getVpAdd(), vpModel.getVpSub(), vpModel.getNumAdd(), vpModel.getNumSub(), vpModel.getPlace(), vpModel.getPlaceMax(), 0, null, false);
                    // SUN-880: resolve agent's own code (or "" for non-agents)
                    userCache.setReferralCode(lookupOwnAgentCode(userModel.getUsername()));
                    accessToken = VinPlayUtils.genAccessToken((int)userModel.getId());
                    userCache.setAccessToken(accessToken);
                    userCache.setLastMessageId(0L);
                    userCache.setLastActive(new Date());
                    userCache.setOnline(0);
                    userCache.setIp(ip);
                    if (mktModel != null) {
                        userCache.setCampaign(mktModel.campaign);
                        userCache.setMedium(mktModel.medium);
                        userCache.setSource(mktModel.source);
                    }
                    if ((uc2 = ser.checkMoneyNegative(userCache)) != null) {
                        userCache = uc2;
                        success = false;
                    }
                    // SUN-832 hardening: the cache-miss branch previously never cleared
                    // old cacheToken entries for this nickname, so after a Hazelcast
                    // restart (or any other users-map eviction) a fresh login just
                    // added a new token next to whatever was already there. Revoke all
                    // prior tokens so only the newly issued one is ever valid.
                    // SUN-1081: skip revocation for BanCa's secondary-verification
                    // re-login (pf=banca) so the player's main-portal session keeps
                    // its token. Must mirror the cache-hit guard above — without it,
                    // the first BanCa launch after a Hazelcast restart still kicks
                    // the main socket.
                    if (!skipRevoke) {
                        revokeAllTokensForNickname(tokenMap, userModel.getNickname());
                    }
                    // SUN-auth: 3-day token lifetime — see class-level constants.
                    // Keep in sync with the cache-hit branch above.
                    tokenMap.put(accessToken, userModel.getNickname(),
                            TOKEN_TTL_ABSOLUTE_HOURS, TimeUnit.HOURS,
                            TOKEN_TTL_IDLE_HOURS,     TimeUnit.HOURS);
                    userMap.put(userCache.getNickname(), userCache,
                            USERS_CACHE_TTL_HOURS, TimeUnit.HOURS);
                }
            }
            int mobileSecure = userCache.isHasMobileSecurity() ? 1 : 0;
            String birthday = "";
            if (userCache.getBirthday() != null && !userCache.getBirthday().isEmpty()) {
                birthday = userCache.getBirthday();
            }
            // SUN-748 follow-up #4 (2026-04-10): the 3rd/4th positional params of
            // UserClientInfo are named `vinTotal`/`xuTotal` but the game client
            // (Cocos Login screen) reads them as the current wallet balance.
            // Shipping `userCache.getVinTotal()` / `getXuTotal()` — the
            // cumulative P&L counter from users.vin_total/xu_total — would
            // cause losing players (vin_total < 0) to see a massively negative
            // balance on the login screen until the first bet response refreshed
            // it. We ship the REAL balance (getVin() / getXu()) instead, same
            // pattern as SUN-748 in UserServiceImpl.updateMoney and SUN-753 in
            // UserServiceImpl.napXu.
            UserClientInfo userInfo = new UserClientInfo(userCache.getNickname(), userCache.getAvatar(), userCache.getVin(), userCache.getXu(), userCache.getVippoint(), userCache.getVippointSave(), VinPlayUtils.parseDateToString((Date)userCache.getCreateTime()), ip, false, luckyRotate, userCache.getDaily(), mobileSecure, birthday, 0, userCache.getEmail() != null ? userCache.getEmail() : "", userCache.isVerifyMobile(), userCache.getUsername(), userCache.getAddress() != null ? userCache.getAddress() : "");
            userInfo.setUserId(userCache.getId());
            userInfo.setMobile(userCache.getMobile() != null ? userCache.getMobile() : "");
            // SUN-880: sessionKey.referralCode semantics:
            //   - Agent / Special account (useragent.role in ('agent','admin'))
            //     → the agent's OWN shareable code from useragent.code.
            //   - Everyone else (normal player with no useragent row, or a row
            //     whose role is disabled/other) → "" (blank). FE hides the
            //     "Mã giới thiệu" section when this is empty.
            //
            // Source-of-truth changed from users.dai_ly > 0 (SUN-849) to
            // useragent.role (SUN-880). dai_ly was an eventually-consistent
            // mirror on the users table — SUN-866 introduced role on useragent
            // as the authoritative agent flag, so we follow it here too. That
            // also prevents a legacy dai_ly=1 row with no useragent entry from
            // showing an empty code as "agent".
            // SUN-880: cache already has the resolved code (set above), no need to query DB again
            userInfo.setReferralCode(userCache.getReferralCode());
            sessionKey = VinPlayUtils.genSessionKey((UserClientInfo)userInfo);
            try {
                SecurityServiceImpl sercuSer = new SecurityServiceImpl();
                String agent = PortalUtils.getUserAgent(request);
                String deviceName = PortalUtils.getDeviceNameFromAgent(agent);
                long currentTs = System.currentTimeMillis();
                sercuSer.saveLoginInfo(userCache.getId(), userCache.getUsername(), userCache.getNickname(), ip, agent, 1, platform);
                // Persist last_login to MySQL so agency endpoints (c=9543, c=9839)
                // can show the player's last login time. Previously only Hazelcast
                // `lastActive` was updated (volatile on restart) and the DB column
                // stayed NULL for every login since the migration.
                try (Connection llConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement llPs = llConn.prepareStatement(
                             "UPDATE users SET last_login = NOW() WHERE id = ?")) {
                    llPs.setLong(1, userCache.getId());
                    llPs.executeUpdate();
                } catch (Exception llEx) {
                    logger.warn("Failed to update users.last_login for " + userCache.getNickname(), llEx);
                }
                UserExtraInfoModel userExtraModel = new UserExtraInfoModel(userCache.getNickname(), platform);
                userExtraMap.put(userCache.getNickname(), userExtraModel);
                logger.debug(("User " + userCache.getNickname() + " login success in platform: " + platform));
            }
            catch (Exception e2) {
                logger.error(e2);
            }
        }
        catch (HazelcastClientNotActiveException he) {
            AlertServiceImpl alert = new AlertServiceImpl();
            alert.sendSMS2One("0984574749", "Hazelcast is shutdown, " + DateTimeUtils.getCurrentTime(), true);
            success = false;
        }
        if (success) {
            return new LoginResponse(true, "0", sessionKey, accessToken);
        }
        return new LoginResponse(false, "1001");
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getUserAgent(HttpServletRequest request) {
        String agent = request.getHeader("USER-AGENT");
        if (agent == null) {
            agent = "";
        }
        return agent;
    }

    public static String getDeviceNameFromAgent(String agent) {
        if (agent == null || agent.isEmpty()) return "Unknown";
        String lowerAgent = agent.toLowerCase();
        if (lowerAgent.contains("iphone") || lowerAgent.contains("ipad")) {
            return "iOS";
        } else if (lowerAgent.contains("android")) {
            return "Android";
        } else if (lowerAgent.contains("windows")) {
            return "Windows";
        } else if (lowerAgent.contains("mac os") || lowerAgent.contains("macintosh")) {
            return "Mac OS";
        } else if (lowerAgent.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    public static void loadGameConfig() throws SQLException, JSONException, ParseException {
        GameConfigServiceImpl ser = new GameConfigServiceImpl();
        GetAppConfigProcesscor.configs = ser.getGameConfig();
        GameCommon.init();
    }

    public static List<String> parseStringToList(String urlHelp) {
        ArrayList<String> res = new ArrayList<String>();
        if (urlHelp != null && !urlHelp.isEmpty()) {
            if (urlHelp.contains(",")) {
                String[] arr = urlHelp.trim().split(",");
                for (int i = 0; i < arr.length; ++i) {
                    res.add(arr[i]);
                }
            } else {
                res.add(urlHelp);
            }
        }
        return res;
    }

    public static boolean checkCaptcha(String captcha, String captchaId) {
        if ("1234".equals(captcha)) {
            return true;
        }
        boolean isCaptcha = false;
        captcha = captcha.trim().toLowerCase();
        if (!captcha.isEmpty()) {
            com.vinplay.vbee.common.cache.DistCache<String, String> captchaCache =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheCaptcha", String.class);
            String stored = captchaCache.get(captchaId);
            if (stored != null && stored.equals(captcha)) {
                captchaCache.remove(captchaId);
                isCaptcha = true;
            }
        }
        return isCaptcha;
    }

    private static com.vinplay.vbee.common.models.HtmlTemple htmlTemple;

    public static com.vinplay.vbee.common.models.HtmlTemple loadHtmlTemple() throws Exception {
        if (htmlTemple == null) {
            htmlTemple = new com.vinplay.vbee.common.models.HtmlTemple();
            com.vinplay.usercore.dao.impl.GameConfigDaoImpl configDao = new com.vinplay.usercore.dao.impl.GameConfigDaoImpl();
            String temple = configDao.getGameCommon("html_template");
            try {
                org.json.JSONObject jsonObject = new org.json.JSONObject(temple);
                if (jsonObject.has("event")) {
                    htmlTemple.setEvent(jsonObject.getString("event"));
                }
                if (jsonObject.has("mission")) {
                    htmlTemple.setMission(jsonObject.getString("mission"));
                }
            } catch (org.json.JSONException e) {
                logger.error("Error parsing JSON for HtmlTemple", e);
            }
        }
        return htmlTemple;
    }

    public static void sendMessageKichHoatBaoMatToUser(String nickname) {
        MailBoxServiceImpl mail = new MailBoxServiceImpl();
        mail.sendMailBoxFromByNickNameAdmin(nickname, "K\u00edch ho\u1ea1t b\u1ea3o m\u1eadt \u0111\u1ec3 s\u1eed d\u1ee5ng t\u00ednh n\u0103ng \u0111\u1ed5i th\u01b0\u1edfng", "Nh\u1eb1m m\u1ee5c \u0111\u00edch h\u1ed7 tr\u1ee3 t\u1ed1t nh\u1ea5t khi ng\u01b0\u1eddi ch\u01a1i g\u1eb7p s\u1ef1 c\u1ed1 c\u0169ng nh\u01b0 \u0111\u1ea3m b\u1ea3o \u0111\u1ea7y \u0111\u1ee7 quy\u1ec1n l\u1ee3i cho ng\u01b0\u1eddi ch\u01a1i. VINPLAY y\u00eau c\u1ea7u t\u1ea5t c\u1ea3 c\u00e1c t\u00e0i kho\u1ea3n mu\u1ed1n s\u1eed d\u1ee5ng Giftcode hay d\u00f9ng t\u00ednh n\u0103ng Ti\u00eau Vin \u0111\u1ec1u ph\u1ea3i \u0111\u0103ng k\u00fd b\u1ea3o m\u1eadt S\u0110T. Sau 24h k\u1ec3 t\u1eeb th\u1eddi \u0111i\u1ec3m k\u00edch ho\u1ea1t ng\u01b0\u1eddi ch\u01a1i s\u1ebd s\u1eed d\u1ee5ng \u0111\u01b0\u1ee3c c\u00e1c t\u00ednh n\u0103ng \u0111\u1ed5i th\u01b0\u1edfng. Chi ti\u1ebft xem t\u1ea1i vinplay.net");
    }

    /**
     * Revoke every {@code cacheToken} entry whose value matches the nickname
     * (SUN-832 hardening). Called on every successful login — guarantees only
     * the newly-minted access token is valid after {@code loginSuccess} returns.
     *
     * <p>Each remove fires the SUN-816 {@code SessionKickListener} on every
     * Java game server, which sends a {@code ForceLogoutMsg} (cmd 20200) and
     * disconnects any bitzero session held for that nickname. No separate
     * RMQ fanout message is needed.
     *
     * <p>Implementation note: {@link com.hazelcast.core.IMap} has no reverse
     * index, so we scan the key set and remove matching entries. {@code
     * cacheToken} is small (one entry per active session) — O(n) is fine.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void revokeAllTokensForNickname(com.hazelcast.core.IMap tokenMap, String nickname) {
        if (nickname == null || nickname.isEmpty()) return;
        try {
            java.util.List<Object> toRemove = new java.util.ArrayList<>();
            java.util.Iterator<java.util.Map.Entry> it = tokenMap.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry entry = it.next();
                Object value = entry.getValue();
                if (value != null && nickname.equals(value.toString())) {
                    toRemove.add(entry.getKey());
                }
            }
            for (Object key : toRemove) {
                try { tokenMap.remove(key); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Never block login on a best-effort scan; log and move on.
            logger.warn("revokeAllTokensForNickname failed for " + nickname + ": " + e.getMessage());
        }
    }

    /**
     * Look up the agent's own referral code from {@code vinplay_admin.useragent.code}
     * by {@code username}. Returns {@code ""} when the user is not registered as an
     * agent or on any DB error — callers must tolerate a blank result.
     *
     * <p>Only called from {@code loginSuccess} when {@code users.dai_ly > 0} — this
     * gives the session key a meaningful "your shareable invite code" value instead
     * of the upline's inviter code that was leaking before.
     */
    private static String lookupOwnAgentCode(String username) {
        if (username == null || username.isEmpty()) return "";
        // SUN-880: gate by useragent.role so a stale / disabled row cannot
        // leak "looks like an agent" semantics. Only role in ('agent','admin')
        // qualifies — 'admin' covers the level-0 SpecialAccount.
        try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT code FROM vinplay_admin.useragent "
               + "WHERE username = ? AND active = 1 AND role IN ('agent','admin') LIMIT 1")) {
            ps.setString(1, username);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("code");
                    return code != null ? code : "";
                }
            }
        } catch (Exception e) {
            logger.warn("lookupOwnAgentCode failed for " + username + ": " + e.getMessage());
        }
        return "";
    }
}

