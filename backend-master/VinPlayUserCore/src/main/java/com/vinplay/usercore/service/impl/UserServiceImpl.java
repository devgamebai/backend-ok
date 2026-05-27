/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.hazelcast.transaction.TransactionContext
 *  com.hazelcast.transaction.TransactionOptions
 *  com.hazelcast.transaction.TransactionOptions$TransactionType
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.VippointMessage
 *  com.vinplay.vbee.common.messages.vippoint.VippointEventMessage
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.response.NapXuResponse
 *  com.vinplay.vbee.common.response.UserInfoModel
 *  com.vinplay.vbee.common.response.UserResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.MapUtils
 *  com.vinplay.vbee.common.utils.NumberUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.vinplay.dichvuthe.entities.TransferMoneyBankModel;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.dichvuthe.service.impl.TransferMoneyBankService;
import com.vinplay.usercore.dao.impl.AgentDaoImpl;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.TransferMoneyResponse;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.common.models.BankSmsModel;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.response.NapXuResponse;
import com.vinplay.vbee.common.response.UserInfoModel;
import com.vinplay.vbee.common.response.UserResponse;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.MapUtils;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.NumberUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;

public class UserServiceImpl
        implements UserService {
    private static final Logger logger = Logger.getLogger((String) "user_core");

    @Override
    public String insertUser(String username, String password) throws SQLException {
        String err = "1001";
        UserDaoImpl userDao = new UserDaoImpl();
        if (userDao.checkUsername(username)) {
            err = "1006";
        } else {
            SecurityDaoImpl securDao = new SecurityDaoImpl();
            if (securDao.updateUserInfo(0, username + "," + password, 9)) {
                err = "0";
            }
        }
        return err;
    }

    @Override
    public boolean insertUserBySocial(String socialId, String social) throws SQLException {
        SecurityDaoImpl securDao = new SecurityDaoImpl();
        if (social.equals("fb")) {
            return securDao.updateUserInfo(0, socialId, 10);
        }
        return securDao.updateUserInfo(0, socialId, 11);
    }

    @Override
    public String updateNickname(int userId, String nickname) throws SQLException {
        SecurityDaoImpl dao;
        String err = "1010";
        UserDaoImpl userDao = new UserDaoImpl();
        if (!userDao.checkNicknameExist(nickname)
                && (dao = new SecurityDaoImpl()).updateUserInfo(userId, nickname, 6)) {
            err = "0";
        }
        return err;
    }

    @Override
    public boolean checkNickname(String nickname) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.checkNickname(nickname);
    }

    @Override
    public UserModel getUserByUserName(String username) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.getUserByUserName(username);
    }

    @Override
    public UserModel getUserByNickName(String nickname) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.getUserByNickName(nickname);
    }

    @Override
    public UserModel getUserBySocialId(String socialId, String social) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        if (social.equals("fb")) {
            return userDao.getUserByFBId(socialId);
        }
        return userDao.getUserByGGId(socialId);
    }

    @Override
    public long getMoneyUserCache(String nickname, String moneyType) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get( nickname);
            return user.getMoney(moneyType);
        }
        return 0L;
    }

    @Override
    public UserCacheModel getMoneyUser(String nickname) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get( nickname);
            return user;
        }
        return null;
    }

    @Override
    public long getMoneyCashout(String nickname) throws ParseException {
        UserCacheModel user;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)
                && (user = (UserCacheModel) userMap.get( nickname)).getCashoutTime() != null
                && VinPlayUtils.compareDate((Date) user.getCashoutTime(), (Date) new Date()) == 0) {
            return user.getCashout();
        }
        return 0L;
    }

    @Override
    public long getCurrentMoneyUserCache(String nickname, String moneyType) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get( nickname);
            return user.getTotalPnl(moneyType);
        }
        return 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public UserResponse checkSessionKey(String nickname, String accessToken, Games game) {
        UserResponse res;
        block19: {
            logger.debug( ("Request checkSessionKey: nickname: " + nickname + ", accessToken: " + accessToken));
            UserModel user = null;
            res = new UserResponse(false, "1001", user);
            try {
                int statusGame = GameCommon.getValueInt("STATUS_GAME");
                if (statusGame == StatusGames.MAINTAIN.getId()) {
                    logger.debug( ("Response checkSessionKey: server maintain" + res.toJson()));
                    res.setErrorCode("1114");
                    return res;
                }
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap userMap = instance.getMap("users");
                if (!userMap.containsKey( nickname))
                    break block19;
                if (statusGame == StatusGames.SANDBOX.getId()
                        && !((UserCacheModel) userMap.get( nickname)).isCanLoginSandbox()) {
                    logger.debug( ("Response checkSessionKey: server maintain" + res.toJson()));
                    res.setErrorCode("1114");
                    return res;
                }
                try {
                    userMap.lock(nickname);
                    UserCacheModel userCache = (UserCacheModel) userMap.get( nickname);
                    if (!userCache.isBanLogin()) {
                        if (game == Games.MINIGAME || userCache.getDaily() == 0
                                && !StatusUser.checkStatus((int) userCache.getStatus(), (int) game.getId())) {
                            if (userCache.getAccessToken().equals(accessToken)) {
                                if (!VinPlayUtils.sessionTimeout((long) userCache.getLastActive().getTime())) {
                                    UserCacheModel uc = this.checkMoneyNegative(userCache);
                                    if (uc != null) {
                                        userCache = uc;
                                    } else {
                                        user = new UserModel(userCache.getId(), userCache.getUsername(),
                                                userCache.getNickname(), userCache.getPassword(), userCache.getEmail(),
                                                userCache.getFacebookId(), userCache.getFacebookId(),
                                                userCache.getMobile(), userCache.getBirthday(), userCache.isGender(),
                                                userCache.getAddress(), userCache.getVin(), userCache.getXu(),
                                                userCache.getVinTotal(), userCache.getXuTotal(), userCache.getSafe(),
                                                userCache.getRechargeMoney(), userCache.getVippoint(),
                                                userCache.getDaily(), userCache.getStatus(), userCache.getAvatar(),
                                                userCache.getIdentification(), userCache.getVippointSave(),
                                                userCache.getCreateTime(), userCache.getMoneyVP(),
                                                userCache.getSecurityTime(), userCache.getLoginOtp(),
                                                userCache.isBot(), false, 0, null, false);
                                        res.setSuccess(true);
                                        res.setErrorCode("0");
                                        res.setUser(user);
                                        userCache.setLastActive(new Date());
                                        userCache.setOnline(userCache.getOnline() + 1);
                                    }
                                    userMap.put(nickname,  userCache);
                                } else {
                                    res.setErrorCode("1015");
                                }
                            } else {
                                res.setErrorCode("1014");
                            }
                        } else {
                            res.setErrorCode("1111");
                        }
                    } else {
                        res.setErrorCode("1109");
                    }
                } catch (Exception e) {
                    logger.debug( e);
                } finally {
                    userMap.unlock(nickname);
                }
            } catch (Exception e2) {
                logger.debug( e2);
            }
        }
        logger.debug( ("Response checkSessionKey: " + res.toJson()));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void logout(String nickname) {
        IMap userMap = HazelcastClientFactory.getInstance().getMap("users");
        if (userMap.containsKey( nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel userCache = (UserCacheModel) userMap.get( nickname);
                if (userCache.getOnline() > 0) {
                    userCache.setOnline(userCache.getOnline() - 1);
                }
                userCache.setLastActive(new Date());
                userMap.put(nickname,  userCache);
            } catch (Exception e) {
                logger.debug( e);
            } finally {
                userMap.unlock(nickname);
            }
        }
    }

    /**
     * SUN-1240 compensating refund. Called only when the atomic SQL gate has
     * already mutated MySQL but the subsequent RMQ side-effect publish failed.
     * Reverses the {@code money} delta on {@code (vin, vin_total)} or
     * {@code (xu, xu_total)} so we don't leave a wallet movement orphaned
     * without an audit log. Best-effort: a refund failure is escalated to
     * ERROR but never re-thrown — the caller has already returned 1031.
     */
    private static void compensateAtomicGate(String nickname, int userId, String moneyType, long money) {
        if (userId <= 0 || money == 0L) return;
        // SUN-1235: route compensating refund through MoneyGateway so the
        // reversal also lands an audit row + ledger entry. Negate the original
        // delta to undo the previous credit/debit.
        String col = moneyType.equalsIgnoreCase("vin") ? "vin" : "xu";
        com.vinplay.dal.service.MoneyGateway.CreditResultWithCumulative cr =
                com.vinplay.dal.service.MoneyGateway.creditUserWithCumulative(
                        userId, nickname, col, -money,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_USERSERVICE_GAME,
                        null,
                        "UserServiceImpl.compensateAtomicGate");
        if (!cr.success) {
            logger.error("CRITICAL updateMoney compensating-refund FAILED user=" + nickname
                    + " userId=" + userId + " moneyType=" + moneyType + " money=" + money
                    + " err=" + cr.error
                    + " — DB now holds an uncompensated wallet write");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse updateMoney(String nickname, long money, String moneyType, String gameName, String serviceName,
            String description, long fee, Long transId, TransType type) {
        // SUN-1205/1206: legacy 9-arg path delegates to the new method with
        // validBetAmount=0 (= "use abs(money)"). Existing 72 callers pass
        // through unchanged.
        return updateMoney(nickname, money, moneyType, gameName, serviceName, description, fee, transId, type, 0L);
    }

    public MoneyResponse updateMoney(String nickname, long money, String moneyType, String gameName, String serviceName,
            String description, long fee, Long transId, TransType type, long validBetAmount) {
        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1000", "Request updateMoney");
        logger.debug( ("Request updateMoney:  nickname: " + nickname + ", money: " + money + ", moneyType: "
                + moneyType + ", gameName: " + gameName + ", serviceName: " + serviceName + ", description: "
                + description + ", fee: " + fee + ", transId: " + transId + ", TransType: " + type.getId()
                + ", validBetAmount: " + validBetAmount));
        MoneyResponse response = new MoneyResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030",
                    "can not connect hazelcast");
            response.setErrorCode("1030");
            return response;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            // SUN-1240: DB-FIRST ATOMIC GATE.
            //
            // The legacy path read balance from the Hazelcast `users` cache and
            // validated `cache + delta >= 0` before writing — a textbook
            // time-of-check/time-of-use bug. The cache is volatile in-memory
            // (no MapStore on this map) and the async cmd=16 stored proc
            // writes ABSOLUTE values from cache → so any window where cache
            // diverged from MySQL let a player bet on phantom funds. quochuy98
            // drained ~65M VIN through SicBo against this exact gap; every
            // offline game (TaiXiu, SicBo, Slot, Poker, TLMN, ...) routed
            // through this method shares the same exposure class.
            //
            // We now route through MoneyGateway.creditUserWithCumulative
            // which does the atomic 2-column update with a floor check on
            // debit (vin/xu, vin_total/xu_total) BEFORE touching the cache.
            // MySQL row lock
            // serializes concurrent writers; on a debit, rowcount==0 means
            // insufficient balance and we bail with 1002. We then read fresh
            // balance back inside the same connection and stamp DB-fresh
            // values into the cache — so the VipPoint / RMQ side-effects run
            // on reality, not stale cache.
            //
            // The async cmd=16 RMQ message still publishes (UpdateMoneyProcessor
            // needs it for users_active timeouts and recharge tracking) but
            // the wallet write inside the stored proc becomes idempotent: it
            // re-asserts the same absolute value we just wrote.
            long preGateBalance = -1L;
            long preGateTotal = -1L;
            int preGateUserId = -1;
            if (money != 0L) {
                UserCacheModel preUser = (UserCacheModel) userMap.get(nickname);
                if (preUser == null) {
                    response.setErrorCode("1001");
                    logger.debug( ("Response updateMoney:" + response.toJson()));
                    return response;
                }
                preGateUserId = preUser.getId();
                // SUN-1235: route through MoneyGateway.creditUserWithCumulative.
                // Atomic balance + total update with floor check on debit + audit
                // row in money_gateway_log + ledger dual-write. Cache stamping
                // stays inline below (we own userMap.lock for VipPoint coherence).
                String col = moneyType.equalsIgnoreCase("vin") ? "vin" : "xu";
                // SUN-1289: discriminate the dedup key by gameName + transType +
                // user_id so round-keyed games (TaiXiu, TaiXiuMD5, BauCua, SicBo,
                // MiniPoker, ...) don't collide BET vs WIN vs HOANTIEN vs REFUND
                // for the same user inside one round. Before this, every
                // settlement after a BET (or vice-versa) hit
                // money_gateway_log.uk_tx_source and returned 1031 "Duplicate
                // transaction" — winners stopped being credited at scale (audit
                // 2026-05-10 found ~47B VIN un-credited in <24h across TaiXiuMD5
                // wins and BauCua wins). The 2026-05-08 patch on
                // MoneyGateway.isDuplicate (added user_id) only fixed
                // cross-user collisions for round-keyed txIds; same-user
                // cross-operation collisions need the gameName+type
                // discriminator at the txKey-build site.
                //
                // Same-user same-side multiple bets in one round still need the
                // CALLER to pass a unique transId per individual bet (handled
                // separately in MGRoomTaiXiu / MGRoomTaiXiuMD5 — see SUN-1290).
                int typeId = type != null ? type.getId() : 0;
                String txKey = transId != null
                        ? "userservice:" + (gameName != null ? gameName : "_") + ":" + typeId + ":" + transId
                        : null;
                com.vinplay.dal.service.MoneyGateway.CreditResultWithCumulative gw =
                        com.vinplay.dal.service.MoneyGateway.creditUserWithCumulative(
                                preGateUserId, nickname, col, money,
                                com.vinplay.dal.service.MoneyGateway.SOURCE_USERSERVICE_GAME,
                                txKey,
                                "UserService " + serviceName + " / " + gameName + ": " + description);
                if (!gw.success) {
                    if (gw.error != null && gw.error.contains("Insufficient")) {
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1002",
                                "khong du tien (gateway gate)");
                        response.setErrorCode("1002");
                    } else if (gw.error != null && gw.error.contains("User not found")) {
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1001",
                                "user not found id=" + preGateUserId);
                        response.setErrorCode("1001");
                    } else {
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1031",
                                "gateway error: " + gw.error);
                        response.setErrorCode("1031");
                    }
                    logger.debug( ("Response updateMoney:" + response.toJson()));
                    return response;
                }
                preGateBalance = gw.newBalance;
                preGateTotal = gw.newTotal;
            }
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get( nickname);
                long moneyUser;
                long currentMoney;
                if (money != 0L) {
                    // Stamp DB-fresh values into cache; the legacy in-cache
                    // delta arithmetic below is now redundant (already done
                    // atomically in MySQL) but the locals are kept so the
                    // VipPoint / RMQ paths read the same authoritative numbers.
                    user.setMoney(moneyType, preGateBalance);
                    user.setTotalPnl(moneyType, preGateTotal);
                    moneyUser = preGateBalance;
                    currentMoney = preGateTotal;
                } else {
                    moneyUser = user.getMoney(moneyType);
                    currentMoney = user.getTotalPnl(moneyType);
                }
                if (money != 0L) {
                    TransactionContext context = client.newTransactionContext(new TransactionOptions()
                            .setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                    context.beginTransaction();
                    try {
                        long moneyVP = VippointUtils.calculateMoneyVP(moneyType, transId, client, nickname,
                                gameName, money, type);
                        int vp = 0;
                        int moneyVPs = 0;
                        int vpAddEvent = 0;
                        if (moneyVP > 0L) {
                            List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(),
                                    (long) user.getMoneyVP() + moneyVP, false);
                            vp = vpLst.get(0);
                            moneyVPs = vpLst.get(1);
                            vpAddEvent = vpLst.get(2);
                            user.setVippoint(user.getVippoint() + vp);
                            user.setVippointSave(user.getVippointSave() + vp);
                            user.setMoneyVP(moneyVPs);
                            if (vpAddEvent > 0) {
                                int vpReal = user.getVpEventReal();
                                int vpEvent = user.getVpEvent();
                                int place = VippointUtils.calculatePlace(vpEvent += vpAddEvent);
                                int placeMax = place > user.getPlace() ? place : user.getPlace();
                                user.setVpEventReal(vpReal += vpAddEvent);
                                user.setVpEvent(vpEvent);
                                user.setPlace(place);
                                user.setPlaceMax(placeMax);
                                VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(),
                                        nickname, vpReal, vpEvent, 0, 0, 0, 0, place, placeMax, 0, 0);
                                MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage,
                                        (int) 801);
                            }
                        }
                        boolean playgame = false;
                        if (transId != null || type == TransType.VIPPOINT) {
                            playgame = true;
                        }
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                serviceName, currentMoney, money, moneyType, description, fee, playgame,
                                user.isBot());
                        // SUN-1205/1206: when caller (e.g. WithdrawProcess
                        // for a Dream hedge bet) provides an explicit
                        // valid_bet_amount, stamp it on the message so
                        // LogMoneyUserExtraProcessor uses it for
                        // commission volume instead of abs(money).
                        if (validBetAmount > 0L) {
                            messageLog.setValidBetAmount(validBetAmount);
                        }
                        // 2026-05-16 Phase B C-fix — drop redundant queue_payment publish.
                        // Wallet write is already committed atomically above by
                        // MoneyGateway.creditUserWithCumulative (SOURCE_USERSERVICE_GAME).
                        // The legacy queue_payment route fed vbee's
                        // UpdateMoneyProcessor → CALL update_money_user → a
                        // SECOND atomic delta on users.vin, causing
                        // Quanlu99-class double-debits (V4 dedup masked them but
                        // had a 0-balance edge-case bug — see migration v5).
                        // Removing the publish eliminates the dual-write entirely;
                        // LogMoneyUserMessage (queue_log_money) stays so Mongo
                        // audit + commission pipeline + downstream consumers
                        // still get notified.
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                        System.out.println("TAIXIUDEBUG updateMoney totalVin " + user.getVinTotal() + " vin " + user.getVin());
                        userMap.put(nickname, user);
                        context.commitTransaction();
                        response.setSuccess(true);  
                        response.setErrorCode("0");
                    } catch (Exception e) {
                        context.rollbackTransaction();
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1031",
                                "error rmq: " + e.getMessage());
                        response.setErrorCode("1031");
                        // SUN-1240 compensating refund: the atomic SQL gate
                        // already wrote MySQL, but the RMQ side-effects (VP /
                        // money log / cmd=16 stored proc) failed to publish.
                        // Reverse the DB delta so we don't leave a phantom
                        // wallet movement with no audit row.
                        compensateAtomicGate(nickname, preGateUserId, moneyType, money);
                    }
                } else {
                    if (moneyType.equals("vin") && transId != null && type.getId() == TransType.END_TRANS.getId()) {
                        IMap vpCache = client.getMap("VPMinigame");
                        String vpCacheId = nickname + gameName + transId;
                        long moneyVP2 = 0L;
                        if (vpCache.containsKey( vpCacheId)) {
                            moneyVP2 = Math.abs((Long) vpCache.get( vpCacheId));
                            vpCache.remove( vpCacheId);
                            if (moneyVP2 > 0L) {
                                List<Integer> vpLst2 = VippointUtils.calculateVP(client, user.getNickname(),
                                        (long) user.getMoneyVP() + moneyVP2, false);
                                int vp2 = vpLst2.get(0);
                                int moneyVPs2 = vpLst2.get(1);
                                int vpAddEvent2 = vpLst2.get(2);
                                user.setVippoint(user.getVippoint() + vp2);
                                user.setVippointSave(user.getVippointSave() + vp2);
                                user.setMoneyVP(moneyVPs2);
                                if (vpAddEvent2 > 0) {
                                    int vpReal2 = user.getVpEventReal();
                                    int vpEvent2 = user.getVpEvent();
                                    int place2 = VippointUtils.calculatePlace(vpEvent2 += vpAddEvent2);
                                    int placeMax2 = place2 > user.getPlace() ? place2 : user.getPlace();
                                    user.setVpEventReal(vpReal2 += vpAddEvent2);
                                    user.setVpEvent(vpEvent2);
                                    user.setPlace(place2);
                                    user.setPlaceMax(placeMax2);
                                    VippointEventMessage vpEventMessage2 = new VippointEventMessage(user.getId(),
                                            nickname, vpReal2, vpEvent2, 0, 0, 0, 0, place2, placeMax2, 0, 0);
                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event",
                                            (BaseMessage) vpEventMessage2, (int) 801);
                                }
                                VippointMessage message2 = new VippointMessage(user.getId(), nickname, moneyVPs2, vp2);
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message2, (int) 18);
                                userMap.put(nickname, user);
                            }
                        }
                    }
                    response.setSuccess(true);
                    response.setErrorCode("0");
                }
                // SUN-748 follow-up: Send the ACTUAL current balance (vin/xu) to the
                // client, NOT the cumulative vin_total/xu_total counter. Historically
                // `currentMoney` in this response carried vinTotal (via UserModel.getCurrentMoney
                // which inexplicably returns vin_total), and all game clients assign
                // res.currentMoney → Configs.Login.Coin as the displayed balance. For any
                // losing player (vin_total < 0) this caused the wallet to DISPLAY as
                // negative immediately after a bet, even though the real balance (vin)
                // was still positive. Sending moneyUser (= vin) fixes the display without
                // changing DB semantics.
                response.setCurrentMoney(moneyUser);
                response.setMoneyUse(moneyUser);
            } catch (Exception e2) {
                logger.debug( e2);
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030",
                        "error hazelcast: " + e2.getMessage());
                response.setErrorCode("1030");
                // SUN-1xxx (2026-05-11): direction-aware compensation. If the
                // atomic SQL gate committed (preGateBalance != -1) and we're
                // here because a Hazelcast op threw afterward, vinplay state
                // is half-applied. Whether to compensate depends on the
                // caller's protocol:
                //
                // money < 0 (DEBIT — player putting money INTO a sub-wallet):
                //   The convention across our callers (BanCa LobbyService.cs
                //   ccash>0 branch, AwcDebitProcessor, GSC withdraw etc.) is:
                //   call us first → only commit their side if we return
                //   success. On a 1030 the caller refuses, so vinplay holds a
                //   one-sided debit with no matching credit anywhere. Reverse
                //   the debit to keep ledger conservation.
                //   Nohu888adm 2026-05-11 20:02:56 KST: 650k debit trapped
                //   exactly this way; same class as Mankr1 -90k, SUNKR_BET
                //   -150k, synohu -110k, koreavip86 -109k, etc.
                //
                // money > 0 (CREDIT — caller already committed a matching debit
                //   on its side BEFORE asking us to credit):
                //   The BanCa cash→vin path (LobbyService.cs ccash<0 branch)
                //   debits cgame.users.cash FIRST then calls us to credit
                //   vinplay.vin — and notably does NOT check our response
                //   before reporting ok=true to the player. If we reverse the
                //   vin credit here, money disappears: cash already gone, vin
                //   not credited. Better to leave the credit standing — the
                //   inconsistency is the failed HZ-side-effect (e.g., the
                //   balance push notification) not the wallet state itself.
                //   The .NET side needs its own fix to check our response and
                //   roll back its cash debit on failure (separate change).
                if (preGateBalance != -1L && preGateUserId > 0 && money < 0L) {
                    logger.error("updateMoney outer-catch fired AFTER atomic gate committed"
                            + " user=" + nickname + " userId=" + preGateUserId
                            + " moneyType=" + moneyType + " money=" + money
                            + " — issuing compensating refund (debit direction)");
                    compensateAtomicGate(nickname, preGateUserId, moneyType, money);
                } else if (preGateBalance != -1L && money > 0L) {
                    logger.error("updateMoney outer-catch fired AFTER atomic gate committed"
                            + " user=" + nickname + " userId=" + preGateUserId
                            + " moneyType=" + moneyType + " money=" + money
                            + " — NOT compensating (credit direction; caller may have"
                            + " already debited its side and won't see our error)."
                            + " Manual reconciliation may be required if caller dropped this credit.");
                }
            } finally {
                userMap.unlock(nickname);
            }
        }
        // SUN-WS-PUSH (2026-05-08): every money mutation must end with a
        // balance-update notification so the FE WebSocket sees changes in
        // real-time (~5-15ms after commit). Previously skipped on this
        // path — minigame bets (BauCua, TaiXiu, TaiXiuMD5, PokeGo,
        // ChatWorld, ToiChonCa, CaoThap, …) wallet-debited correctly but
        // the portal top-bar wallet display only refreshed on round-end.
        // Fired AFTER the Hazelcast cache write commits (above) so the
        // PortalBalanceConsumer sees fresh balance when it reads userMap.
        // Money flow / ledger pattern is unaffected — this is a pure
        // notification, no DB or money_gateway_log writes.
        if (response.isSuccess()) {
            com.vinplay.dal.service.MoneyGateway.publishBalanceUpdate(nickname);
        }
        logger.debug( ("Response updateMoney:" + response.toJson()));
        return response;
    }

    @Override
    public BaseResponseModel updateMoneyFromAdmin(String nickname, long money, String moneyType, String actionName,
            String serviceName, String description) {
        BaseResponseModel response;
        block11: {
            logger.debug( ("Request updateMoneyFromAdmin:  nickname: " + nickname + ", money: " + money
                    + ", moneyType: " + moneyType + ", actionName: " + actionName + ", serviceName: " + serviceName
                    + ", description: " + description));
            response = new BaseResponseModel(false, "1001");
            if (nickname != null && !nickname.isEmpty() && money != 0L) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickname, actionName, money, 0L, moneyType, serviceName, "1030",
                            "can not connect hazelcast");
                    response.setErrorCode("1030");
                    return response;
                }
                try {
                    UserDaoImpl userDao = new UserDaoImpl();
                    UserModel model = userDao.getUserByNickName(nickname);
                    logger.debug( model);
                    if (model != null) {
                        nickname = model.getNickname();
                        IMap<String, UserModel> userMap = client.getMap("users");
                        if (userMap.containsKey( nickname)) {
                            MoneyResponse moneyRes = this.updateMoney(nickname, money, moneyType, actionName,
                                    serviceName, description, 0L, null, TransType.NO_VIPPOINT);
                            response.setSuccess(moneyRes.isSuccess());
                            response.setErrorCode(moneyRes.getErrorCode());

                            logger.debug( ("catch user in hazel"));
                            logger.debug( moneyRes);
                            break block11;
                        }
                        try {
                            // SUN-1235: route the offline-user admin top-up branch
                            // through MoneyGateway.creditUserWithCumulative —
                            // same atomic gate, plus audit row + ledger dual-write.
                            int userId = model.getId();
                            String col = moneyType.equalsIgnoreCase("vin") ? "vin" : "xu";
                            long currentMoney;
                            com.vinplay.dal.service.MoneyGateway.CreditResultWithCumulative gw =
                                    com.vinplay.dal.service.MoneyGateway.creditUserWithCumulative(
                                            userId, nickname, col, money,
                                            com.vinplay.dal.service.MoneyGateway.SOURCE_USERSERVICE_GAME,
                                            null,
                                            "UserService offline-user " + serviceName + ": " + description);
                            if (!gw.success) {
                                if (gw.error != null && gw.error.contains("Insufficient")) {
                                    MoneyLogger.log(nickname, actionName, money, 0L, moneyType, serviceName,
                                            "1002", "khong du tien (gateway gate)");
                                    response.setErrorCode("1002");
                                } else {
                                    response.setErrorCode("1001");
                                }
                                break block11;
                            }
                            currentMoney = gw.newTotal;
                            try {
                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(userId, nickname,
                                        actionName, serviceName, currentMoney, money, moneyType, description, 0L,
                                        false, model.isBot());
                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                            } catch (Exception e) {
                                MoneyLogger.log(nickname, actionName, money, 0L, moneyType, serviceName, "1031",
                                        "error rmq: " + e.getMessage());
                                response.setErrorCode("1031");
                            }
                            response.setSuccess(true);
                            response.setErrorCode("0");
                            break block11;
                        } catch (Exception e2) {
                            logger.debug( e2);
                            MoneyLogger.log(nickname, actionName, money, 0L, moneyType, serviceName, "1032",
                                    "error mysql: " + e2.getMessage());
                            response.setErrorCode("1032");
                            return response;
                        }
                    }
                    response.setErrorCode("2001");
                } catch (Exception e3) {
                    logger.debug( e3);
                    MoneyLogger.log(nickname, actionName, money, 0L, moneyType, serviceName, "1032",
                            "error mysql: " + e3.getMessage());
                    response.setErrorCode("1032");
                }
            }
        }
        logger.debug( ("Response updateMoneyFromAdmin: " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public BaseResponseModel updateMoneyCacheToDB(String nickname) throws SQLException {
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap != null && userMap.containsKey( nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get( nickname);
                long moneyUseVin = user.getVin();
                long moneyTotalVin = user.getVinTotal();
                long moneySafe = user.getSafe();
                long moneyUseXu = user.getXu();
                long moneyTotalXu = user.getXuTotal();
                UserDaoImpl dao = new UserDaoImpl();
                if (dao.restoreMoneyByAdmin(user.getId(), moneyUseVin, moneyTotalVin, moneySafe, "vin")
                        && dao.restoreMoneyByAdmin(user.getId(), moneyUseXu, moneyTotalXu, 0L, "xu")) {
                    response.setErrorCode("0");
                    response.setSuccess(true);
                }
            } catch (Exception e) {
                logger.debug( e);
            } finally {
                userMap.unlock(nickname);
            }
        }
        return response;
    }

    @Override
    public boolean checkMobile(String mobile) throws SQLException {
        boolean res = false;
        UserDaoImpl dao = new UserDaoImpl();
        res = dao.checkMobile(mobile);
        if (!res) {
            SecurityDaoImpl sercuDao = new SecurityDaoImpl();
            res = sercuDao.checkNewMobile(mobile);
        }
        return res;
    }

    @Override
    public boolean checkMobileDaiLy(String mobile) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.checkMobileDaiLy(mobile);
    }

    @Override
    public boolean checkMobileSecurity(String mobile) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.checkMobileSecurity(mobile);
    }

    @Override
    public boolean checkEmailSecurity(String email) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.checkEmailSecurity(email);
    }

    @Override
    public byte checkUser(String nickname) {
        int res = -1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        int agent = -1;
        if (userMap.containsKey( nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get( nickname);
            agent = user.getDaily();
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            try {
                agent = dao.checkAgent(nickname);
            } catch (SQLException sQLException) {
                // empty catch block
            }
        }
        res = agent == -1 ? -1 : (agent == 1 ? 1 : (agent == 2 ? 2 : 0));
        return (byte) res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public NapXuResponse napXu(String nickname, long moneyVinToXu, boolean check) {
        NapXuResponse response;
        block20: {
            response = new NapXuResponse();
            response.setResult((byte) 1);
            response.setIsAuth((byte) 0);
            try {
                IMap userMap;
                if (GameCommon.getValueInt("IS_NAP_XU") == 1) {
                    return response;
                }
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickname, "NapXu", moneyVinToXu, 0L, "vin", "Nap xu", "1030",
                            "can not connect hazelcast");
                }
                if (!(userMap = client.getMap("users")).containsKey( nickname))
                    break block20;
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get( nickname);
                    long moneyUserVin = user.getMoney("vin");
                    long currentMoneyVin = user.getTotalPnl("vin");
                    long moneyUserXu = user.getMoney("xu");
                    long currentMoneyXu = user.getTotalPnl("xu");
                    /* if (user.getMobile() != null && user.isHasMobileSecurity()) { */
                    if (true) {
                        /*
                         * if (user.getSecurityTime() != null &&
                         * VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(),
                         * (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                         */
                        if (moneyVinToXu > 0L) {
                            if (moneyUserVin - moneyVinToXu >= 0L) {
                                if (check && user.getMobile() != null && user.isHasMobileSecurity()) {
                                    response.setResult((byte) 0);
                                    response.setIsAuth((byte) 1);
                                } else {
                                    TransactionContext context = client.newTransactionContext(new TransactionOptions()
                                            .setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                    context.beginTransaction();
                                    try {
                                        user.setMoney("vin", moneyUserVin -= moneyVinToXu);
                                        user.setTotalPnl("vin", currentMoneyVin -= moneyVinToXu);
                                        long moneyXuAdded = Math.round(
                                                (double) moneyVinToXu * GameCommon.getValueDouble("RATIO_NAP_XU"));
                                        user.setMoney("xu", moneyUserXu += moneyXuAdded);
                                        user.setTotalPnl("xu", currentMoneyXu += moneyXuAdded);
                                        MoneyMessageInMinigame message = new MoneyMessageInMinigame(
                                                VinPlayUtils.genMessageId(), user.getId(), nickname, "NapXu",
                                                moneyUserVin, currentMoneyVin, -moneyVinToXu, "vin", 0L, 0, 0);
                                        LogMoneyUserMessage messageLogVin = new LogMoneyUserMessage(user.getId(),
                                                nickname, "NapXu", "N\u1ea1p Xu", currentMoneyVin, -moneyVinToXu, "vin",
                                                "Chuy\u1ec3n Vin sang Xu", 0L, false, user.isBot());
                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 16);
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogVin, 601);
                                        message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(),
                                                nickname, "NapXu", moneyUserXu, currentMoneyXu, moneyXuAdded, "xu", 0L,
                                                0, 0);
                                        LogMoneyUserMessage messageLogXu = new LogMoneyUserMessage(user.getId(),
                                                nickname, "NapXu", "N\u1ea1p Xu", currentMoneyXu, moneyXuAdded, "xu",
                                                "Chuy\u1ec3n Vin sang Xu", 0L, false, user.isBot());
                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 16);
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogXu, 601);
                                        userMap.put(nickname, user);
                                        context.commitTransaction();
                                        response.setResult((byte) 0);
                                    } catch (Exception e) {
                                        logger.debug( e);
                                        MoneyLogger.log(nickname, "NapXu", moneyVinToXu, 0L, "vin", "Nap xu", "1031",
                                                "rmq error: " + e.getMessage());
                                        context.rollbackTransaction();
                                    }
                                }
                            } else {
                                response.setResult((byte) 2);
                            }
                        }
                        // SUN-753 follow-up (laviai 2026-04-10): Send the ACTUAL current
                        // balance (vin/xu), NOT the cumulative vin_total/xu_total counter.
                        // Game client displays currentMoneyVin/currentMoneyXu in
                        // ResultNapXuMsg after a VIN→XU conversion. For losing players
                        // (vin_total < 0) the display flipped to a large negative number
                        // immediately after converting, even though the real vin stayed
                        // positive. This is the same pattern SUN-748 fixed in updateMoney;
                        // the napXu branch was missed in the original fix.
                        response.setCurrentMoneyVin(moneyUserVin);
                        response.setCurrentMoneyXu(moneyUserXu);
                        /*
                         * } else {
                         * response.setResult((byte)10);
                         * }
                         */
                    } else {
                        response.setResult((byte) 3);
                    }
                } catch (Exception e2) {
                    logger.debug( e2);
                    MoneyLogger.log(nickname, "NapXu", moneyVinToXu, 0L, "vin", "Nap xu", "1001", e2.getMessage());
                } finally {
                    userMap.unlock(nickname);
                }
            } catch (Exception e3) {
                logger.debug( e3);
            }
        }
        return response;
    }

    @Override
    public List<TopCaoThu> getTopCaoThu(String date, String moneyType, int num) {
        List<TopCaoThu> res = new ArrayList<TopCaoThu>();
        if (moneyType.equals("vin")) {
            // cacheTopCaoThuVin has no writer in this codebase — always empty.
            // Keeping the read path so it goes through the routing flag.
            @SuppressWarnings({"rawtypes", "unchecked"})
            com.vinplay.vbee.common.cache.DistCache<String, HashMap> topMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheTopCaoThuVin", HashMap.class);
            if (topMap.containsKey(date)) {
                HashMap<String, Long> map = (HashMap<String, Long>) topMap.get(date);
                TreeMap<String, Long> sortedMap = MapUtils.sortMapByValue((HashMap) map);
                int n = 0;
                for (Map.Entry entry : sortedMap.entrySet()) {
                    if ((Long) entry.getValue() > 0L) {
                        TopCaoThu top = new TopCaoThu((String) entry.getKey(), ((Long) entry.getValue()).longValue());
                        res.add(top);
                    }
                    if (++n < num)
                        continue;
                    break;
                }
            } else {
                UserDaoImpl dao = new UserDaoImpl();
                res = dao.getTopCaoThu(date, moneyType, num);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte checkMoney(String nickname, long money, byte type) {
        int res = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get( nickname);
                if (user.isHasMobileSecurity() && user.getSecurityTime() != null) {
                    if (VinPlayUtils.checkSecurityTimeout((Date) user.getSecurityTime())) {
                        if (type == 2 && !user.isBanTransferMoney()) {
                            if (user.getVin() >= money) {
                                res = 0;
                            }
                        } else {
                            res = 2;
                        }
                        if (type == 3 && !user.isBanCashOut()) {
                            if (user.getVin() >= money) {
                                res = 0;
                            }
                        } else {
                            res = 3;
                        }
                        if (type == 4 && user.getVin() >= money) {
                            res = 0;
                        }
                    } else {
                        res = 6;
                    }
                } else {
                    res = 5;
                }
            } catch (Exception e) {
                logger.debug( e);
            } finally {
                userMap.unlock(nickname);
            }
        }
        return (byte) res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public TransferMoneyResponse transferMoney(String nicknameSend, String nicknameReceive, long vin,
            String description, boolean check) {
        long moneyReceive;
        TransferMoneyResponse res;
        boolean updateVP;
        int status;
        block71: {
            logger.debug( ("Request transferMoney: nicknameSend: " + nicknameSend + ", nicknameReceive: "
                    + nicknameReceive + ", money: " + vin + ", description: " + description + ", check: " + check));
            res = new TransferMoneyResponse((byte) 1, 0L, 0L);
            res.setIsAuth((byte) 0);
            updateVP = false;
            status = 0;
            moneyReceive = 0L;
            try {
                if (GameCommon.getValueInt("IS_TRANSFER_MONEY") == 1) {
                    logger.debug( "Khoa chuyen tien");
                    return res;
                }
                if (nicknameSend == null || nicknameReceive == null || description == null
                        || nicknameSend.equals(nicknameReceive)) {
                    logger.debug( "Missing param");
                    return res;
                }
                UserModel userReceive = this.getUserByNickName(nicknameReceive);
                AgentDaoImpl agentDao = new AgentDaoImpl();
                AlertServiceImpl alertSer = new AlertServiceImpl();
                if (userReceive != null) {
                    nicknameReceive = userReceive.getNickname();
                    res.setNicknameReceive(nicknameReceive);
                    int feeSMS = GameCommon.getValueInt("SMS_FEE");
                    boolean sendSMS = false;
                    try {
                        if ((userReceive.getDaily() == 1 || userReceive.getDaily() == 2)
                                && userReceive.getMobile() != null && userReceive.isHasMobileSecurity()
                                && agentDao.checkSMSAgent(nicknameReceive, vin)) {
                            sendSMS = true;
                        }
                    } catch (Exception e) {
                        logger.debug( e);
                    }

                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    if (client == null) {
                        MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin", "chuyen khoan", "1030",
                                "can not connect hazelcast");
                        return res;
                    }
                    IMap<String, UserModel> userMap = client.getMap("users");
                    UserCacheModel userSend = null;
                    UserCacheModel userCacheReceive = null;
                    // get user send
                    if (userMap.containsKey( nicknameSend)) {
                        userSend = (UserCacheModel) userMap.get( nicknameSend);
                    }
                    // get user receive
                    if (userMap.containsKey( nicknameReceive)) {
                        userCacheReceive = (UserCacheModel) userMap.get( nicknameReceive);
                    }
                    if (userSend == null || userCacheReceive == null)
                        return res;
                    if (userSend.getDaily() == 0) {
                        if (userCacheReceive.getDaily() == 0) {
                            res.setCode((byte) 11);
                            return res;
                        } else {
                            try {
                                com.vinplay.usercore.dao.impl.UserDaoImpl uDao = new com.vinplay.usercore.dao.impl.UserDaoImpl();
                                String userRefCode = uDao.getReferralCode(nicknameSend);
                                String agentCode = null;
                                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                                    java.sql.PreparedStatement stm = conn.prepareStatement("SELECT code FROM vinplay_admin.useragent WHERE nick_name = ?");
                                    stm.setString(1, nicknameReceive);
                                    java.sql.ResultSet rs = stm.executeQuery();
                                    if (rs.next()) {
                                        agentCode = rs.getString("code");
                                    }
                                    rs.close();
                                    stm.close();
                                }
                                if (userRefCode == null || userRefCode.isEmpty() || agentCode == null || !userRefCode.equals(agentCode)) {
                                    res.setCode((byte) 10); // Custom error code for wrong agent
                                    return res;
                                }
                            } catch (Exception e) {
                                logger.error("Error validating User to Agent transfer", e);
                                res.setCode((byte) 11);
                                return res;
                            }
                        }
                    }
                    if (vin >= (long) GameCommon.getValueInt("TRANSFER_MONEY_MIN")
                            || (userSend != null && userSend.getDaily() == 1)) {
                        if (userMap.containsKey( nicknameSend)) {
                            try {
                                userMap.lock(nicknameSend);
                                String superAgent = GameCommon.getValueStr("SUPER_AGENT");
                                long dl1Max = GameCommon.getValueLong("DL1_TO_SUPER_MAX");
                                long dl1Min = GameCommon.getValueLong("DL1_TO_SUPER_MIN");
                                long dl1MinX = GameCommon.getValueLong("DL1_TO_SUPER_MIN_X");
                                boolean dl1ToSuperAgent = userSend.getDaily() == 1
                                        && nicknameReceive.equals(superAgent);
                                long moneyUser = userSend.getVin();
                                long currentMoney = userSend.getVinTotal();
                                if (!dl1ToSuperAgent || dl1ToSuperAgent && dl1Min <= vin && dl1Max >= vin) {
                                    if (!dl1ToSuperAgent || dl1ToSuperAgent && currentMoney - vin >= dl1MinX) {
                                        res.setMoneyUse(moneyUser);
                                        res.setCurrentMoney(currentMoney);
                                        if (!userSend.isBanTransferMoney()) {
                                            // if (userSend.getMobile() != null && !userSend.getMobile().isEmpty() &&
                                            // userSend.isHasMobileSecurity()) {
                                            if (true) {
                                                if (moneyUser >= vin) {
                                                    if (check && userSend.getMobile() != null
                                                            && !userSend.getMobile().isEmpty()
                                                            && userSend.isHasMobileSecurity()) {
                                                        res.setCode((byte) 0);
                                                        res.setIsAuth((byte) 1);
                                                        break block71;
                                                    }
                                                    TransactionContext context = client.newTransactionContext(
                                                            new TransactionOptions().setTransactionType(
                                                                    TransactionOptions.TransactionType.ONE_PHASE));
                                                    if (userMap.containsKey( nicknameReceive)) {
                                                        try {
                                                            context.beginTransaction();
                                                            userMap.lock(nicknameReceive);
                                                            userCacheReceive = (UserCacheModel) userMap
                                                                    .get( nicknameReceive);
                                                            status = this.getStatusChuyenTienDaiLy(userSend.getDaily(),
                                                                    userCacheReceive.getDaily());
                                                            long fee = Math
                                                                    .round((double) vin * this.getFeeTransfer(status));
                                                            moneyReceive = vin - fee;
                                                            res.setMoneyReceive(moneyReceive);
                                                            long moneyUserReceive = userCacheReceive.getVin();
                                                            long currentMoneyReceive = userCacheReceive.getVinTotal();
                                                            userSend.setVin(moneyUser -= vin);
                                                            userSend.setVinTotal(currentMoney -= vin);
                                                            userCacheReceive.setVin(moneyUserReceive += moneyReceive);
                                                            userCacheReceive
                                                                    .setVinTotal(currentMoneyReceive += moneyReceive);
                                                            MoneyMessageInMinigame messageSend = new MoneyMessageInMinigame(
                                                                    VinPlayUtils.genMessageId(), userSend.getId(),
                                                                    nicknameSend, "TransferMoney", moneyUser,
                                                                    currentMoney, -vin, "vin", fee, 0, 0);
                                                            String desSend = "Chuy\u1ec3n t\u1edbi " + nicknameReceive
                                                                    + ": " + description;
                                                            LogMoneyUserMessage messageLogSend = new LogMoneyUserMessage(
                                                                    userSend.getId(), nicknameSend, "TransferMoney",
                                                                    "Chuy\u1ec3n kho\u1ea3n", currentMoney, -vin, "vin",
                                                                    desSend, fee, false, userSend.isBot());
                                                            int recharge = 0;
                                                            if (status == 3 || status == 6) {
                                                                recharge = -1;
                                                                userCacheReceive.setRechargeMoney(
                                                                        userCacheReceive.getRechargeMoney()
                                                                                + moneyReceive);
                                                                // ADD REQUIRED VOLUME
                                                                try {
                                                                    com.vinplay.dal.withdraw.VolumeTrackingService.addRequiredVolume(
                                                                            userCacheReceive.getId(), 
                                                                            nicknameReceive, 
                                                                            moneyReceive);
                                                                } catch(Exception e) {
                                                                    logger.error("Failed to add required volume for Agent transfer", e);
                                                                }
                                                            }
                                                            MoneyMessageInMinigame messageReceive = new MoneyMessageInMinigame(
                                                                    VinPlayUtils.genMessageId(),
                                                                    userCacheReceive.getId(), nicknameReceive,
                                                                    "TransferMoney", moneyUserReceive,
                                                                    currentMoneyReceive, moneyReceive, "vin", 0L,
                                                                    recharge, 0);
                                                            String desReceive = "Nh\u1eadn t\u1eeb " + nicknameSend
                                                                    + ": " + description;
                                                            LogMoneyUserMessage messageLogReceive = new LogMoneyUserMessage(
                                                                    userCacheReceive.getId(), nicknameReceive,
                                                                    "TransferMoney", "Chuy\u1ec3n kho\u1ea3n",
                                                                    currentMoneyReceive, moneyReceive, "vin",
                                                                    desReceive, 0L, false, userCacheReceive.isBot());
                                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageSend, (int) 16);
                                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageReceive, (int) 16);
                                                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogSend, 601);
                                                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogReceive, 601);
                                                            if (status != 0) {
                                                                LogChuyenTienDaiLyMessage messageDaily = new LogChuyenTienDaiLyMessage(
                                                                        nicknameSend, nicknameReceive, vin,
                                                                        moneyReceive, fee,
                                                                        VinPlayUtils.getCurrentDateTime(), status,
                                                                        desSend, desReceive,
                                                                        VinPlayUtils.genTransactionId(
                                                                                (int) userSend.getId()),
                                                                        1, this.getAgentLevel1(nicknameSend,
                                                                                nicknameReceive),
                                                                        "");
                                                                MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish(
                                                                        (String) "queue_log_chuyen_tien_dai_ly",
                                                                        (BaseMessage) messageDaily, (int) 603);
                                                            }
                                                            if (sendSMS) {
                                                                if (feeSMS > 0) {
                                                                    userCacheReceive
                                                                            .setVin(moneyUserReceive -= (long) feeSMS);
                                                                    userCacheReceive.setVinTotal(
                                                                            currentMoneyReceive -= (long) feeSMS);
                                                                    MoneyMessageInMinigame messageReceiveSMSFee = new MoneyMessageInMinigame(
                                                                            VinPlayUtils.genMessageId(),
                                                                            userCacheReceive.getId(), nicknameReceive,
                                                                            "ChargeSMS", moneyUserReceive,
                                                                            currentMoneyReceive, (long) (-feeSMS),
                                                                            "vin", 0L, 0, 0);
                                                                    LogMoneyUserMessage messageLogReceiveSMSFee = new LogMoneyUserMessage(
                                                                            userCacheReceive.getId(), nicknameReceive,
                                                                            "ChargeSMS", "Ph\u00ed SMS",
                                                                            currentMoneyReceive, (long) (-feeSMS),
                                                                            "vin",
                                                                            "Tr\u1eeb ph\u00ed d\u1ecbch v\u1ee5 SMS",
                                                                            0L, false, userCacheReceive.isBot());
                                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment",
                                                                            (BaseMessage) messageReceiveSMSFee,
                                                                            (int) 16);
                                                                    MessageBusFactory.get("queue_log_money").publish(
                                                                            "queue_log_money",
                                                                            (LogMoneyUserMessage) messageLogReceiveSMSFee, 601);
                                                                }
                                                                SimpleDateFormat format = new SimpleDateFormat(
                                                                        "HH:mm:ss dd-MM-yyyy");
                                                                String time = format.format(new Date());
                                                                String content = nicknameSend + " da chuyen "
                                                                        + NumberUtils.formatNumber(
                                                                                (String) String.valueOf(vin))
                                                                        + " vin cho ban luc " + time + ". So vin nhan: "
                                                                        + NumberUtils.formatNumber(
                                                                                (String) String.valueOf(moneyReceive))
                                                                        + ". So du vin: "
                                                                        + NumberUtils.formatNumber((String) String
                                                                                .valueOf(currentMoneyReceive));
                                                                alertSer.sendSMS2One(userCacheReceive.getMobile(),
                                                                        content, false);
                                                            }
                                                            userMap.put(nicknameSend, userSend);
                                                            ;
                                                            userMap.put(nicknameReceive, userCacheReceive);
                                                            context.commitTransaction();
                                                            res.setCode((byte) 0);
                                                            res.setMoneyUse(moneyUser);
                                                            res.setCurrentMoney(currentMoney);
                                                            res.setCurrentMoneyReceive(currentMoneyReceive);
                                                            updateVP = true;
                                                            if (dl1ToSuperAgent) {
                                                                Timer timer = new Timer();
                                                                TransferMoneyBankModel model = new TransferMoneyBankModel(
                                                                        nicknameSend, moneyReceive);
                                                                timer.schedule(
                                                                        (TimerTask) new TransferMoneyBankService(model),
                                                                        5000L);
                                                            }
                                                            break block71;
                                                        } catch (Exception e2) {
                                                            logger.debug( e2);
                                                            MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L,
                                                                    "vin", "chuyen khoan", "1001", e2.getMessage());
                                                            context.rollbackTransaction();
                                                            break block71;
                                                        } finally {
                                                            userMap.unlock(nicknameReceive);
                                                        }
                                                    }
                                                    if (userReceive == null)
                                                        break block71;
                                                    try {
                                                        context.beginTransaction();
                                                        long currentMoneyReceive2 = userReceive.getVinTotal();
                                                        status = this.getStatusChuyenTienDaiLy(userSend.getDaily(),
                                                                userReceive.getDaily());
                                                        long fee2 = Math
                                                                .round((double) vin * this.getFeeTransfer(status));
                                                        moneyReceive = vin - fee2;
                                                        res.setMoneyReceive(moneyReceive);
                                                        currentMoneyReceive2 += moneyReceive;
                                                        userSend.setVin(moneyUser -= vin);
                                                        userSend.setVinTotal(currentMoney -= vin);
                                                        MoneyMessageInMinigame messageSend2 = new MoneyMessageInMinigame(
                                                                VinPlayUtils.genMessageId(), userSend.getId(),
                                                                nicknameSend, "TransferMoney", moneyUser, currentMoney,
                                                                -vin, "vin", fee2, 0, 0);
                                                        String desSend2 = "Chuy\u1ec3n t\u1edbi " + nicknameReceive
                                                                + ": " + description;
                                                        LogMoneyUserMessage messageLogSend2 = new LogMoneyUserMessage(
                                                                userSend.getId(), nicknameSend, "TransferMoney",
                                                                "Chuy\u1ec3n kho\u1ea3n", currentMoney, -vin, "vin",
                                                                desSend2, fee2, false, userSend.isBot());
                                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageSend2,
                                                                (int) 16);
                                                        MessageBusFactory.get("queue_log_money").publish(
                                                                "queue_log_money",
                                                                (LogMoneyUserMessage) messageLogSend2, 601);
                                                        userMap.put(nicknameSend, userSend);
                                                        ;
                                                        context.commitTransaction();
                                                        res.setCode((byte) 0);
                                                        res.setMoneyUse(moneyUser);
                                                        res.setCurrentMoney(currentMoney);
                                                        updateVP = true;
                                                        UserDaoImpl dao = new UserDaoImpl();
                                                        if (!dao.updateMoney(userReceive.getId(), moneyReceive, "vin"))
                                                            break block71;
                                                        if (status == 3 || status == 6) {
                                                            try {
                                                                dao.updateRechargeMoney(userReceive.getId(),
                                                                        moneyReceive);
                                                            } catch (Exception e3) {
                                                                logger.debug( e3);
                                                            }
                                                        }
                                                        res.setCurrentMoneyReceive(currentMoneyReceive2);
                                                        String desReceive2 = "Nh\u1eadn t\u1eeb " + nicknameSend + ": "
                                                                + description;
                                                        LogMoneyUserMessage messageLogReceive2 = new LogMoneyUserMessage(
                                                                userReceive.getId(), nicknameReceive, "TransferMoney",
                                                                "Chuy\u1ec3n kho\u1ea3n", currentMoneyReceive2,
                                                                moneyReceive, "vin", desReceive2, 0L, false,
                                                                userReceive.isBot());
                                                        MessageBusFactory.get("queue_log_money").publish(
                                                                "queue_log_money",
                                                                (LogMoneyUserMessage) messageLogReceive2, 601);
                                                        if (status != 0) {
                                                            LogChuyenTienDaiLyMessage messageDaily2 = new LogChuyenTienDaiLyMessage(
                                                                    nicknameSend, nicknameReceive, vin, moneyReceive,
                                                                    fee2, VinPlayUtils.getCurrentDateTime(), status,
                                                                    desSend2, desReceive2,
                                                                    VinPlayUtils.genTransactionId(
                                                                            (int) userSend.getId()),
                                                                    1,
                                                                    this.getAgentLevel1(nicknameSend, nicknameReceive),
                                                                    "");
                                                            MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish(
                                                                    (String) "queue_log_chuyen_tien_dai_ly",
                                                                    (BaseMessage) messageDaily2, (int) 603);
                                                        }
                                                        if (sendSMS) {
                                                            if (feeSMS > 0 && dao.updateMoney(userReceive.getId(),
                                                                    -feeSMS, "vin")) {
                                                                LogMoneyUserMessage messageLogReceiveSMSFee2 = new LogMoneyUserMessage(
                                                                        userReceive.getId(), nicknameReceive,
                                                                        "ChargeSMS", "Ph\u00ed SMS",
                                                                        currentMoneyReceive2 -= (long) feeSMS,
                                                                        (long) (-feeSMS), "vin",
                                                                        "Tr\u1eeb ph\u00ed d\u1ecbch v\u1ee5 SNS", 0L,
                                                                        false, userReceive.isBot());
                                                                MessageBusFactory.get("queue_log_money").publish(
                                                                        "queue_log_money",
                                                                        (LogMoneyUserMessage) messageLogReceiveSMSFee2, 601);
                                                            }
                                                            SimpleDateFormat format2 = new SimpleDateFormat(
                                                                    "HH:mm:ss dd-MM-yyyy");
                                                            String time2 = format2.format(new Date());
                                                            String content2 = nicknameSend + " da chuyen "
                                                                    + NumberUtils
                                                                            .formatNumber((String) String.valueOf(vin))
                                                                    + " vin cho ban luc " + time2 + ". So vin nhan: "
                                                                    + NumberUtils.formatNumber(
                                                                            (String) String.valueOf(moneyReceive))
                                                                    + ".So du vin: "
                                                                    + NumberUtils.formatNumber((String) String
                                                                            .valueOf(currentMoneyReceive2));
                                                            alertSer.sendSMS2One(userReceive.getMobile(), content2,
                                                                    false);
                                                        }
                                                        if (dl1ToSuperAgent) {
                                                            Timer timer2 = new Timer();
                                                            TransferMoneyBankModel model2 = new TransferMoneyBankModel(
                                                                    nicknameSend, moneyReceive);
                                                            timer2.schedule(
                                                                    (TimerTask) new TransferMoneyBankService(model2),
                                                                    5000L);
                                                        }
                                                        break block71;
                                                    } catch (Exception e2) {
                                                        logger.debug( e2);
                                                        MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin",
                                                                "chuyen khoan", "1001", e2.getMessage());
                                                        context.rollbackTransaction();
                                                    }
                                                    break block71;
                                                }
                                                res.setCode((byte) 4);
                                                break block71;
                                            }
                                            res.setCode((byte) 3);
                                            break block71;
                                        }
                                        res.setCode((byte) 5);
                                        break block71;
                                    }
                                    res.setCode((byte) 12);
                                    break block71;
                                }
                                res.setCode((byte) 11);
                                break block71;
                            } catch (Exception e4) {
                                logger.debug( e4);
                                MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin", "chuyen khoan", "1001",
                                        e4.getMessage());
                                break block71;
                            } finally {
                                userMap.unlock(nicknameSend);
                            }
                        }
                        try {
                            UserDaoImpl userDao = new UserDaoImpl();
                            UserCacheModel userSend2 = userDao.getUserByNickNameCache(nicknameSend);
                            if (userSend2 == null)
                                break block71;
                            String superAgent2 = GameCommon.getValueStr("SUPER_AGENT");
                            long dl1Max2 = GameCommon.getValueLong("DL1_TO_SUPER_MAX");
                            long dl1Min2 = GameCommon.getValueLong("DL1_TO_SUPER_MIN");
                            long dl1MinX2 = GameCommon.getValueLong("DL1_TO_SUPER_MIN_X");
                            boolean dl1ToSuperAgent2 = userSend2.getDaily() == 1 && nicknameReceive.equals(superAgent2);
                            long moneyUser2 = userSend2.getVin();
                            long currentMoney2 = userSend2.getVinTotal();
                            if (!dl1ToSuperAgent2 || dl1ToSuperAgent2 && dl1Min2 <= vin && dl1Max2 >= vin) {
                                if (!dl1ToSuperAgent2 || dl1ToSuperAgent2 && currentMoney2 - vin >= dl1MinX2) {
                                    res.setMoneyUse(moneyUser2);
                                    res.setCurrentMoney(currentMoney2);
                                    if (!userSend2.isBanTransferMoney()) {
                                        if (userSend2.getMobile() != null && !userSend2.getMobile().isEmpty()
                                                && userSend2.isHasMobileSecurity()) {
                                            if (moneyUser2 >= vin) {
                                                if (check) {
                                                    res.setCode((byte) 0);
                                                    break block71;
                                                }
                                                TransactionContext context2 = client.newTransactionContext(
                                                        new TransactionOptions().setTransactionType(
                                                                TransactionOptions.TransactionType.ONE_PHASE));
                                                if (userMap.containsKey( nicknameReceive)) {
                                                    try {
                                                        context2.beginTransaction();
                                                        userMap.lock(nicknameReceive);
                                                        UserCacheModel userCacheReceive2 = (UserCacheModel) userMap
                                                                .get( nicknameReceive);
                                                        status = this.getStatusChuyenTienDaiLy(userSend2.getDaily(),
                                                                userCacheReceive2.getDaily());
                                                        long fee2 = Math
                                                                .round((double) vin * this.getFeeTransfer(status));
                                                        moneyReceive = vin - fee2;
                                                        res.setMoneyReceive(moneyReceive);
                                                        long moneyUserReceive2 = userCacheReceive2.getVin();
                                                        long currentMoneyReceive3 = userCacheReceive2.getVinTotal();
                                                        userSend2.setVin(moneyUser2 -= vin);
                                                        userSend2.setVinTotal(currentMoney2 -= vin);
                                                        userCacheReceive2.setVin(moneyUserReceive2 += moneyReceive);
                                                        userCacheReceive2
                                                                .setVinTotal(currentMoneyReceive3 += moneyReceive);
                                                        MoneyMessageInMinigame messageSend3 = new MoneyMessageInMinigame(
                                                                VinPlayUtils.genMessageId(), userSend2.getId(),
                                                                nicknameSend, "TransferMoney", moneyUser2,
                                                                currentMoney2, -vin, "vin", fee2, 0, 0);
                                                        String desSend3 = "Chuy\u1ec3n t\u1edbi " + nicknameReceive
                                                                + ": " + description;
                                                        LogMoneyUserMessage messageLogSend3 = new LogMoneyUserMessage(
                                                                userSend2.getId(), nicknameSend, "TransferMoney",
                                                                "Chuy\u1ec3n kho\u1ea3n", currentMoney2, -vin, "vin",
                                                                desSend3, fee2, false, userSend2.isBot());
                                                        int recharge2 = 0;
                                                        if (status == 3 || status == 6) {
                                                            recharge2 = -1;
                                                            userCacheReceive2.setRechargeMoney(
                                                                    userCacheReceive2.getRechargeMoney()
                                                                            + moneyReceive);
                                                            // ADD REQUIRED VOLUME
                                                            try {
                                                                com.vinplay.dal.withdraw.VolumeTrackingService.addRequiredVolume(
                                                                        userCacheReceive2.getId(), 
                                                                        nicknameReceive, 
                                                                        moneyReceive);
                                                            } catch(Exception e) {
                                                                logger.error("Failed to add required volume for Agent transfer", e);
                                                            }
                                                        }
                                                        MoneyMessageInMinigame messageReceive2 = new MoneyMessageInMinigame(
                                                                VinPlayUtils.genMessageId(), userCacheReceive2.getId(),
                                                                nicknameReceive, "TransferMoney", moneyUserReceive2,
                                                                currentMoneyReceive3, moneyReceive, "vin", 0L,
                                                                recharge2, 0);
                                                        String desReceive3 = "Nh\u1eadn t\u1eeb " + nicknameSend + ": "
                                                                + description;
                                                        LogMoneyUserMessage messageLogReceive3 = new LogMoneyUserMessage(
                                                                userCacheReceive2.getId(), nicknameReceive,
                                                                "TransferMoney", "Chuy\u1ec3n kho\u1ea3n",
                                                                currentMoneyReceive3, moneyReceive, "vin", desReceive3,
                                                                0L, false, userCacheReceive2.isBot());
                                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageSend3, (int) 16);
                                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageReceive2, (int) 16);
                                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogSend3, 601);
                                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogReceive3, 601);
                                                        if (status != 0) {
                                                            LogChuyenTienDaiLyMessage messageDaily3 = new LogChuyenTienDaiLyMessage(
                                                                    nicknameSend, nicknameReceive, vin, moneyReceive,
                                                                    fee2, VinPlayUtils.getCurrentDateTime(), status,
                                                                    desSend3, desReceive3,
                                                                    VinPlayUtils.genTransactionId(
                                                                            (int) userSend2.getId()),
                                                                    1,
                                                                    this.getAgentLevel1(nicknameSend, nicknameReceive),
                                                                    "");
                                                            MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish(
                                                                    (String) "queue_log_chuyen_tien_dai_ly",
                                                                    (BaseMessage) messageDaily3, (int) 603);
                                                        }
                                                        if (sendSMS) {
                                                            if (feeSMS > 0) {
                                                                userCacheReceive2
                                                                        .setVin(moneyUserReceive2 -= (long) feeSMS);
                                                                userCacheReceive2.setVinTotal(
                                                                        currentMoneyReceive3 -= (long) feeSMS);
                                                                MoneyMessageInMinigame messageReceiveSMSFee2 = new MoneyMessageInMinigame(
                                                                        VinPlayUtils.genMessageId(),
                                                                        userCacheReceive2.getId(), nicknameReceive,
                                                                        "ChargeSMS", moneyUserReceive2,
                                                                        currentMoneyReceive3, (long) (-feeSMS), "vin",
                                                                        0L, 0, 0);
                                                                LogMoneyUserMessage messageLogReceiveSMSFee3 = new LogMoneyUserMessage(
                                                                        userCacheReceive2.getId(), nicknameReceive,
                                                                        "ChargeSMS", "Ph\u00ed SMS",
                                                                        currentMoneyReceive3, (long) (-feeSMS), "vin",
                                                                        "Tr\u1eeb ph\u00ed d\u1ecbch v\u1ee5 SMS", 0L,
                                                                        false, userCacheReceive2.isBot());
                                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment",
                                                                        (BaseMessage) messageReceiveSMSFee2, (int) 16);
                                                                MessageBusFactory.get("queue_log_money").publish(
                                                                        "queue_log_money",
                                                                        (LogMoneyUserMessage) messageLogReceiveSMSFee3, 601);
                                                            }
                                                            SimpleDateFormat format3 = new SimpleDateFormat(
                                                                    "HH:mm:ss dd-MM-yyyy");
                                                            String time3 = format3.format(new Date());
                                                            String content3 = nicknameSend + " da chuyen "
                                                                    + NumberUtils
                                                                            .formatNumber((String) String.valueOf(vin))
                                                                    + " vin cho ban luc " + time3 + ". So vin nhan: "
                                                                    + NumberUtils.formatNumber(
                                                                            (String) String.valueOf(moneyReceive))
                                                                    + ". So du vin: "
                                                                    + NumberUtils.formatNumber((String) String
                                                                            .valueOf(currentMoneyReceive3));
                                                            alertSer.sendSMS2One(userCacheReceive2.getMobile(),
                                                                    content3, false);
                                                        }
                                                        userMap.put(nicknameSend, userSend2);
                                                        userMap.put(nicknameReceive, userCacheReceive2);
                                                        context2.commitTransaction();
                                                        res.setCode((byte) 0);
                                                        res.setMoneyUse(moneyUser2);
                                                        res.setCurrentMoney(currentMoney2);
                                                        res.setCurrentMoneyReceive(currentMoneyReceive3);
                                                        updateVP = true;
                                                        if (dl1ToSuperAgent2) {
                                                            Timer timer3 = new Timer();
                                                            TransferMoneyBankModel model3 = new TransferMoneyBankModel(
                                                                    nicknameSend, moneyReceive);
                                                            timer3.schedule(
                                                                    (TimerTask) new TransferMoneyBankService(model3),
                                                                    5000L);
                                                        }
                                                        break block71;
                                                    } catch (Exception e5) {
                                                        logger.debug( e5);
                                                        MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin",
                                                                "chuyen khoan", "1001", e5.getMessage());
                                                        context2.rollbackTransaction();
                                                        break block71;
                                                    } finally {
                                                        userMap.unlock(nicknameReceive);
                                                    }
                                                }
                                                if (userReceive == null)
                                                    break block71;
                                                try {
                                                    context2.beginTransaction();
                                                    long currentMoneyReceive4 = userReceive.getVinTotal();
                                                    status = this.getStatusChuyenTienDaiLy(userSend2.getDaily(),
                                                            userReceive.getDaily());
                                                    long fee3 = Math.round((double) vin * this.getFeeTransfer(status));
                                                    moneyReceive = vin - fee3;
                                                    res.setMoneyReceive(moneyReceive);
                                                    currentMoneyReceive4 += moneyReceive;
                                                    userSend2.setVin(moneyUser2 -= vin);
                                                    userSend2.setVinTotal(currentMoney2 -= vin);
                                                    MoneyMessageInMinigame messageSend4 = new MoneyMessageInMinigame(
                                                            VinPlayUtils.genMessageId(), userSend2.getId(),
                                                            nicknameSend, "TransferMoney", moneyUser2, currentMoney2,
                                                            -vin, "vin", fee3, 0, 0);
                                                    String desSend4 = "Chuy\u1ec3n t\u1edbi " + nicknameReceive + ": "
                                                            + description;
                                                    LogMoneyUserMessage messageLogSend4 = new LogMoneyUserMessage(
                                                            userSend2.getId(), nicknameSend, "TransferMoney",
                                                            "Chuy\u1ec3n kho\u1ea3n", currentMoney2, -vin, "vin",
                                                            desSend4, fee3, false, userSend2.isBot());
                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) messageSend4, (int) 16);
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLogSend4, 601);
                                                    userMap.put(nicknameSend, userSend2);
                                                    context2.commitTransaction();
                                                    res.setCode((byte) 0);
                                                    res.setMoneyUse(moneyUser2);
                                                    res.setCurrentMoney(currentMoney2);
                                                    updateVP = true;
                                                    UserDaoImpl dao2 = new UserDaoImpl();
                                                    if (!dao2.updateMoney(userReceive.getId(), moneyReceive, "vin"))
                                                        break block71;
                                                    if (status == 3 || status == 6) {
                                                        try {
                                                            dao2.updateRechargeMoney(userReceive.getId(), moneyReceive);
                                                            // ADD REQUIRED VOLUME
                                                            com.vinplay.dal.withdraw.VolumeTrackingService.addRequiredVolume(
                                                                    userReceive.getId(), 
                                                                    nicknameReceive, 
                                                                    moneyReceive);
                                                        } catch (Exception e6) {
                                                            logger.debug( e6);
                                                        }
                                                    }
                                                    res.setCurrentMoneyReceive(currentMoneyReceive4);
                                                    String desReceive4 = "Nh\u1eadn t\u1eeb " + nicknameSend + ": "
                                                            + description;
                                                    LogMoneyUserMessage messageLogReceive4 = new LogMoneyUserMessage(
                                                            userReceive.getId(), nicknameReceive, "TransferMoney",
                                                            "Chuy\u1ec3n kho\u1ea3n", currentMoneyReceive4,
                                                            moneyReceive, "vin", desReceive4, 0L, false,
                                                            userReceive.isBot());
                                                    MessageBusFactory.get("queue_log_money").publish(
                                                            "queue_log_money",
                                                            (LogMoneyUserMessage) messageLogReceive4, 601);
                                                    if (status != 0) {
                                                        LogChuyenTienDaiLyMessage messageDaily4 = new LogChuyenTienDaiLyMessage(
                                                                nicknameSend, nicknameReceive, vin, moneyReceive, fee3,
                                                                VinPlayUtils.getCurrentDateTime(), status, desSend4,
                                                                desReceive4,
                                                                VinPlayUtils.genTransactionId((int) userSend2.getId()),
                                                                1, this.getAgentLevel1(nicknameSend, nicknameReceive),
                                                                "");
                                                        MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish((String) "queue_log_chuyen_tien_dai_ly",
                                                                (BaseMessage) messageDaily4, (int) 603);
                                                    }
                                                    if (sendSMS) {
                                                        if (feeSMS > 0 && dao2.updateMoney(userReceive.getId(), -feeSMS,
                                                                "vin")) {
                                                            LogMoneyUserMessage messageLogReceiveSMSFee4 = new LogMoneyUserMessage(
                                                                    userReceive.getId(), nicknameReceive, "ChargeSMS",
                                                                    "Ph\u00ed SMS",
                                                                    currentMoneyReceive4 -= (long) feeSMS,
                                                                    (long) (-feeSMS), "vin",
                                                                    "Tr\u1eeb ph\u00ed d\u1ecbch v\u1ee5 SNS", 0L,
                                                                    false, userReceive.isBot());
                                                            MessageBusFactory.get("queue_log_money").publish(
                                                                    "queue_log_money",
                                                                    (LogMoneyUserMessage) messageLogReceiveSMSFee4, 601);
                                                        }
                                                        SimpleDateFormat format4 = new SimpleDateFormat(
                                                                "HH:mm:ss dd-MM-yyyy");
                                                        String time4 = format4.format(new Date());
                                                        String content4 = nicknameSend + " da chuyen "
                                                                + NumberUtils.formatNumber((String) String.valueOf(vin))
                                                                + " vin cho ban luc " + time4 + ". So vin nhan: "
                                                                + NumberUtils.formatNumber(
                                                                        (String) String.valueOf(moneyReceive))
                                                                + ".So du vin: " + NumberUtils.formatNumber(
                                                                        (String) String.valueOf(currentMoneyReceive4));
                                                        alertSer.sendSMS2One(userReceive.getMobile(), content4, false);
                                                    }
                                                    if (dl1ToSuperAgent2) {
                                                        Timer timer4 = new Timer();
                                                        TransferMoneyBankModel model4 = new TransferMoneyBankModel(
                                                                nicknameSend, moneyReceive);
                                                        timer4.schedule(
                                                                (TimerTask) new TransferMoneyBankService(model4),
                                                                5000L);
                                                    }
                                                    break block71;
                                                } catch (Exception e5) {
                                                    logger.debug( e5);
                                                    MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin",
                                                            "chuyen khoan", "1001", e5.getMessage());
                                                    context2.rollbackTransaction();
                                                }
                                                break block71;
                                            }
                                            res.setCode((byte) 4);
                                            break block71;
                                        }
                                        res.setCode((byte) 3);
                                        break block71;
                                    }
                                    res.setCode((byte) 5);
                                    break block71;
                                }
                                res.setCode((byte) 12);
                                break block71;
                            }
                            res.setCode((byte) 11);
                        } catch (Exception e4) {
                            logger.debug( e4);
                            MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin", "chuyen khoan", "1001",
                                    e4.getMessage());
                        }
                        break block71;
                    }
                    res.setCode((byte) 2);
                    break block71;
                }
                res.setCode((byte) 6);
            } catch (Exception e7) {
                logger.debug( e7);
                MoneyLogger.log(nicknameSend, "TransferMoney", vin, 0L, "vin", "chuyen khoan", "1001", e7.getMessage());
            }
        }
        if (updateVP) {
            VippointServiceImpl vpSer = new VippointServiceImpl();
            vpSer.updateVippointAgent(nicknameSend, nicknameReceive, vin, moneyReceive, status);
        }
        logger.debug( ("Response transferMoney: " + res.getCode()));
        return res;
    }

    private String getAgentLevel1(String nicknameSend, String nicknameReceive) {
        AgentDaoImpl agentDao = new AgentDaoImpl();
        String agentLevel1 = "";
        try {
            agentLevel1 = agentDao.getAgentLevel1ByNickName(nicknameSend);
            if (agentLevel1.equals("")) {
                agentLevel1 = agentDao.getAgentLevel1ByNickName(nicknameReceive);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return agentLevel1;
    }

    private int getStatusChuyenTienDaiLy(int user1, int user2) {
        int status = 0;
        if ((user1 == 0 || user1 == 100) && user2 == 1) {
            status = 1;
        } else if ((user1 == 0 || user1 == 100) && user2 == 2) {
            status = 2;
        } else if (user1 == 1 && (user2 == 0 || user2 == 100)) {
            status = 3;
        } else if (user1 == 1 && user2 == 1) {
            status = 4;
        } else if (user1 == 1 && user2 == 2) {
            status = 5;
        } else if (user1 == 2 && (user2 == 0 || user2 == 100)) {
            status = 6;
        } else if (user1 == 2 && user2 == 1) {
            status = 7;
        } else if (user1 == 2 && user2 == 2) {
            status = 8;
        }
        return status;
    }

    @Override
    public double getFeeTransfer(int status) {
        double fee = 0.98;
        try {
            switch (status) {
                case 0: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER");
                    break;
                }
                case 1: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_01");
                    break;
                }
                case 2: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_02");
                    break;
                }
                case 3: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_DL_1");
                    break;
                }
                case 4: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_11");
                    break;
                }
                case 5: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_12");
                    break;
                }
                case 6: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_20");
                    break;
                }
                case 7: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_21");
                    break;
                }
                case 8: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER_22");
                    break;
                }
                default: {
                    fee = 1.0 - GameCommon.getValueDouble("RATIO_TRANSFER");
                    break;
                }
            }
        } catch (Exception e) {
            logger.debug( e);
        }
        return fee;
    }

    @Override
    public byte calFeeTransfer(int user1, int user2) {
        byte res = 0;
        int status = this.getStatusChuyenTienDaiLy(user1, user2);
        double fee = this.getFeeTransfer(status);
        res = (byte) (fee * 100.0);
        return res;
    }

    @Override
    public UserCacheModel getUser(String nickname) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            return (UserCacheModel) userMap.get( nickname);
        }
        return null;
    }

    @Override
    public boolean insertBot(String un, String nn, String pw, long vin, long xu, int status) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.insertBot(un, nn, pw, vin, xu, status);
    }

    @Override
    public long getTotalRechargeMoney(String username) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( username)) {
            UserCacheModel model = (UserCacheModel) userMap.get( username);
            return model.getRechargeMoney();
        }
        return 0L;
    }

    @Override
    public int getVipPointSave(String username) {
        UserCacheModel model = this.getUser(username);
        if (model != null) {
            return model.getVippointSave();
        }
        return 0;
    }

    @Override
    public boolean checkAccesstoken(String nickname, String accessToken) {
        boolean res = false;
        if (nickname != null && accessToken != null && !nickname.isEmpty() && !accessToken.isEmpty()) {
            try {
                UserCacheModel userCache;
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap userMap = instance.getMap("users");
                if (userMap.containsKey( nickname)
                        && (userCache = (UserCacheModel) userMap.get( nickname)).getAccessToken()
                                .equals(accessToken)) {
                    res = true;
                }
            } catch (Exception e) {
                logger.debug( e);
            }
        }
        return res;
    }

    @Override
    public boolean isActiveToken(String nickname, String accessToken) {
        return checkAccesstoken(nickname, accessToken);
    }

    @Override
    public int checkBot(String nickname) {
        int res = 0;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey( nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get( nickname);
            if (user.isBot()) {
                res = 1;
            }
        } else {
            try {
                UserDaoImpl dao = new UserDaoImpl();
                res = dao.checkBotByNickname(nickname);
            } catch (Exception e) {
                logger.debug( e);
            }
        }
        return res;
    }

    @Override
    public UserExtraInfoModel getUserExtraInfo(String nickname) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userExtraMap = client.getMap("cache_user_extra_info");
        if (userExtraMap.containsKey( nickname)) {
            return (UserExtraInfoModel) userExtraMap.get( nickname);
        }
        return null;
    }

    @Override
    public UserModel getNicknameExactly(String nickname, IMap<String, UserCacheModel> userMap) throws SQLException {
        UserModel model = null;
        if (userMap.containsKey( nickname)) {
            model = new UserModel();
            model.setNickname(nickname);
            model.setBot(((UserCacheModel) userMap.get( nickname)).isBot());
        } else {
            model = this.getUserByNickName(nickname);
        }
        return model;
    }

    @Override
    public List<UserInfoModel> checkPhoneByUser(String phone) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.checkPhoneByUser(phone);
    }

    @Override
    public UserCacheModel checkMoneyNegative(UserCacheModel user) throws SQLException {
        // Only trigger anti-abuse ban on ACTUAL current balance going negative.
        // vinTotal / xuTotal are cumulative game P&L counters — it is perfectly
        // normal for losing players to have negative totals (e.g. deposited 1M,
        // lost 100k → vinTotal = -100k). The previous check incorrectly banned
        // normal losing users on their next login (SUN-748).
        //
        // Self-heal: if balance is OK but ban flags are still set from the old
        // buggy check, clear them so losing players can resume playing without
        // needing a manual CMS unlock.
        if (user.getVin() >= 0L && user.getXu() >= 0L
                && (user.isBanLogin() || user.isBanCashOut() || user.isBanTransferMoney())
                && (user.getStatus() & 11) > 0) {
            logger.warn("checkMoneyNegative: self-healing stale ban for user=" + user.getNickname()
                    + " vin=" + user.getVin() + " xu=" + user.getXu() + " oldStatus=" + user.getStatus());
            user.setBanLogin(false);
            user.setBanCashOut(false);
            user.setBanTransferMoney(false);
            int statusNew = user.getStatus();
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 0, (String) "0");
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 1, (String) "0");
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 3, (String) "0");
            user.setStatus(statusNew);
            try {
                SecurityDaoImpl dao = new SecurityDaoImpl();
                dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7);
            } catch (Exception healErr) {
                logger.warn("checkMoneyNegative self-heal: DB update failed for " + user.getNickname() + ": " + healErr.getMessage());
            }
            return null; // treat as healthy
        }
        if (user.getVin() < 0L || user.getXu() < 0L) {
            user.setBanLogin(true);
            user.setBanCashOut(true);
            user.setBanTransferMoney(true);
            int statusNew = user.getStatus();
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 0, (String) "1");
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 1, (String) "1");
            statusNew = StatusUser.changeStatus((int) statusNew, (int) 3, (String) "1");
            user.setStatus(statusNew);
            SecurityDaoImpl dao = new SecurityDaoImpl();
            dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7);
            AlertServiceImpl alertSer = new AlertServiceImpl();
            alertSer.sendSMS2One("0986354389", "User am tien: " + user.getNickname(), false);
            return user;
        }
        return null;
    }

    @Override
    public long insertBankSms(String content, String sms, long amount, int status) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.insertBankSms(content, sms, amount, status);
    }

    @Override
    public List<BankSmsModel> getBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd,
            int status, int page, int pageSize, String from, String to) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.getBankSmsLst(id, content, sms, timeStart, timeEnd, status, page, pageSize, from, to);
    }

    @Override
    public boolean updateBankSmsStatus(int status, long id) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.updateBankSmsStatus(status, id);
    }

    public boolean updateBankSmsStatus(String content, int status, long id) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.updateBankSmsStatus(content, status, id);
    }

    @Override
    public int countBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd, int status,
            String from, String to) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.countBankSmsLst(id, content, sms, timeStart, timeEnd, status, from, to);
    }

    @Override
    public long getUserValue(String username) {
        // TODO: Stub - returns total recharge money as user value
        try {
            return this.getTotalRechargeMoney(username);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public boolean isUserBigWin(String username) {
        // TODO: Stub - always returns false (no forced big win)
        return false;
    }

    @Override
    public boolean isUserJackpot(String username) {
        // TODO: Stub - always returns false (no forced jackpot)
        return false;
    }

    @Override
    public MoneyResponse updateMoneyFromAdmin(String nickname, long money, String moneyType, String actionName, String serviceName, String description, long fee, boolean playGame) {
        BaseResponseModel base = updateMoneyFromAdmin(nickname, money, moneyType, actionName, serviceName, description);
        MoneyResponse response = new MoneyResponse(base.isSuccess(), base.getErrorCode());
        return response;
    }

    @Override
    public boolean isPhoneUsed(String phone) throws java.sql.SQLException {
        int count = 0;
        String sql = "SELECT count(id) as cnt FROM users WHERE mobile=?";
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, phone);
            java.sql.ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count > 0;
    }

    @Override
    public boolean isPhoneUsed(String phone, String nickname) throws java.sql.SQLException {
        int count = 0;
        String sql = "SELECT count(id) as cnt FROM users WHERE mobile=? and nick_name <> ?";
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, phone);
            stm.setString(2, nickname);
            java.sql.ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count > 0;
    }

    @Override
    public boolean updateVerifyMobile(String nickname, String phoneNumber, boolean hasVerify) throws java.sql.SQLException {
        com.vinplay.usercore.dao.impl.UserDaoImpl userDao = new com.vinplay.usercore.dao.impl.UserDaoImpl();
        return userDao.verifyMobile(nickname, phoneNumber, hasVerify);
    }

    @Override
    public boolean isXacThucSDT(String userName) throws Exception {
        long count = 0L;
        String sql = "SELECT count(*) as cnt from users WHERE nick_name=? and is_verify_mobile =1 and mobile is not null";
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, userName);
            java.sql.ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return count > 0L;
    }

    public boolean updateGifCodeAgent(String nickName, long gift_code) {
        // SUN-13xx Phase 7: users.gift_total column dropped. No-op for API compat.
        return true;
    }
}
