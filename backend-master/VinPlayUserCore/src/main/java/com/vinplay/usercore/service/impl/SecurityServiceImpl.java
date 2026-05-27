/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.hazelcast.transaction.TransactionContext
 *  com.hazelcast.transaction.TransactionOptions
 *  com.hazelcast.transaction.TransactionOptions$TransactionType
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.SafeMoneyMessage
 *  com.vinplay.vbee.common.messages.statistic.LoginPortalInfoMsg
 *  com.vinplay.vbee.common.models.ConfigGame
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.EmailActive;
import com.vinplay.usercore.service.SecurityService;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.SafeMoneyMessage;
import com.vinplay.vbee.common.messages.statistic.LoginPortalInfoMsg;
import com.vinplay.vbee.common.models.ConfigGame;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.DoipassResponse;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.UserValidaton;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class SecurityServiceImpl
implements SecurityService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public byte updateEmail(String nickname, String email) {
        int res = 1;
        if (!UserValidaton.validateEmail((String)email)) return (byte)2;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) return (byte)res;
        try {
             userMap.lock(nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            if (!this.checkSecureInfo(user)) return (byte)res;
            if (user.isHasEmailSecurity()) return (byte)res;
            UserServiceImpl userService = new UserServiceImpl();
            if (!userService.checkEmailSecurity(email)) {
                SecurityDaoImpl dao = new SecurityDaoImpl();
                if (!dao.updateUserInfo(user.getId(), email, 5)) return (byte)res;
                user.setEmail(email);
                userMap.put(nickname, user);
                res = 0;
                return (byte)res;
            } else {
                res = 3;
            }
            return (byte)res;
        }
        catch (Exception e) {
            logger.debug(("updateEmail error: " + e.getMessage()));
            return (byte)res;
        }
        finally {
             userMap.unlock(nickname);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public byte updateMobile(String nickname, String mobile) {
        int res = 1;
        if (!UserValidaton.validateMobileVN((String)mobile)) return (byte)2;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) return (byte)res;
        try {
             userMap.lock(nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            if (!this.checkSecureInfo(user)) return (byte)res;
            if (user.isHasMobileSecurity()) return (byte)res;
            UserServiceImpl userService = new UserServiceImpl();
            if (!userService.checkMobileSecurity(mobile)) {
                SecurityDaoImpl dao = new SecurityDaoImpl();
                if (!dao.updateUserInfo(user.getId(), mobile, 4)) return (byte)res;
                user.setMobile(mobile);
                userMap.put(nickname, user);
                res = 0;
                return (byte)res;
            } else {
                res = 3;
            }
            return (byte)res;
        }
        catch (Exception e) {
            logger.debug(("updateMobile error: " + e.getMessage()));
            return (byte)res;
        }
        finally {
             userMap.unlock(nickname);
        }
    }

    private boolean checkSecureInfo(UserCacheModel user) {
        return user != null && user.getMobile() != null && !user.getMobile().isEmpty() && user.getEmail() != null && !user.getEmail().isEmpty() && user.getIdentification() != null && !user.getIdentification().isEmpty();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public byte updateNewMobile(String nickname, String mobile, boolean check) {
        int res = 1;
        if (!UserValidaton.validateMobileVN((String)mobile)) return (byte)2;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) return (byte)res;
        try {
             userMap.lock(nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            if (user.getMobile() == null) return (byte)res;
            if (!user.isHasMobileSecurity()) return (byte)res;
            if (!mobile.equals(user.getMobile())) {
                UserServiceImpl userService = new UserServiceImpl();
                if (!userService.checkMobileSecurity(mobile)) {
                    SecurityDaoImpl dao = new SecurityDaoImpl();
                    if (check) {
                        if (!dao.updateNewMobile(nickname, mobile, user.getMobile())) return (byte)res;
                        res = 0;
                        return (byte)res;
                    } else {
                        if (!dao.updateUserInfo(user.getId(), mobile, 4)) return (byte)res;
                        user.setMobile(mobile);
                        user.setSecurityTime(new Date());
                        userMap.put(nickname, user);
                        res = 0;
                    }
                    return (byte)res;
                } else {
                    res = 4;
                }
                return (byte)res;
            } else {
                res = 3;
            }
            return (byte)res;
        }
        catch (Exception e) {
            logger.debug(("updateNewMobile error: " + e.getMessage()));
            return (byte)res;
        }
        finally {
             userMap.unlock(nickname);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public byte updateUserInfo(String nickname, String identification, String email, String mobile) {
        int res = 1;
       // if (!UserValidaton.validateIdentification((String)identification)) return (byte)2;
       // if (!UserValidaton.validateEmail((String)email)) return (byte)3;
        if (!UserValidaton.validateMobileVN((String)mobile)) return (byte)4;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) return (byte)res;
        try {
             userMap.lock(nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            if (user.isHasEmailSecurity()) return (byte)res;
            if (user.isHasMobileSecurity()) return (byte)res;
            UserServiceImpl userService = new UserServiceImpl();
            if (!userService.checkMobileSecurity(mobile)) {
                if (!userService.checkEmailSecurity(email)) {
                    SecurityDaoImpl dao = new SecurityDaoImpl();
                    if (!dao.updateUserInfos(user.getId(), identification, email, mobile)) return (byte)res;
                    user.setMobile(mobile);
                    user.setEmail(email);
                    user.setIdentification(identification);
                    userMap.put(nickname, user);
                    res = 0;
                    return (byte)res;
                } else {
                    res = 6;
                }
                return (byte)res;
            } else {
                res = 5;
            }
            return (byte)res;
        }
        catch (Exception e) {
            logger.debug(("updateUserInfo error: " + e.getMessage()));
            return (byte)res;
        }
        finally {
             userMap.unlock(nickname);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public int updateUserVipInfo(String nickname, String birthday, String gender, String address) {
        int res = 1;
        boolean bGender = gender.equals("1");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        SecurityDaoImpl dao = new SecurityDaoImpl();
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (!dao.updateUserVipInfo(user.getId(), birthday, bGender, address)) return res;
                user.setBirthday(birthday);
                user.setGender(bGender);
                user.setAddress(address);
                userMap.put(nickname, user);
                res = 0;
                return res;
            }
            catch (Exception e) {
                logger.debug(("updateUserVipInfo error: " + e.getMessage()));
                return res;
            }
            finally {
                 userMap.unlock(nickname);
            }
        } else {
            UserDaoImpl userDao = new UserDaoImpl();
            try {
                int userId = userDao.getIdByNickname(nickname);
                if (userId <= 0) return 2;
                if (!dao.updateUserVipInfo(userId, birthday, bGender, address)) return res;
                return 0;
            }
            catch (Exception e2) {
                logger.debug(("updateUserVipInfo error: " + e2.getMessage()));
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte activeMobile(String nickname, boolean check) {
        int res = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.getMobile() != null && !user.isHasMobileSecurity()) {
                    UserServiceImpl userService = new UserServiceImpl();
                    if (!userService.checkMobileSecurity(user.getMobile())) {
                        if (check) {
                            logger.debug("this.activeMobile(user, dataCmd);" + user.getMobile());
                            res = 0;
                        } else {
                            int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)4, (String)"1");
                            SecurityDaoImpl dao = new SecurityDaoImpl();
                            if (dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 13)) {
                                user.setStatus(statusNew);
                                user.setHasMobileSecurity(true);
                                user.setSecurityTime(new Date());
                                userMap.put(nickname, user);
                                res = 0;
                            }
                        }
                    } else {
                        res = 2;
                    }
                }
            }
            catch (Exception e) {
                logger.debug(("activeMobile error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return (byte)res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte activeEmail(String nickname) {
        int res = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.getEmail() != null && !user.isHasEmailSecurity()) {
                    UserServiceImpl userService = new UserServiceImpl();
                    if (!userService.checkEmailSecurity(user.getEmail())) {
                        String datetime = VinPlayUtils.getCurrentDateTime();
                        String checksum = nickname + user.getEmail() + datetime + "@VinPlay#6102$817";
                        checksum = VinPlayUtils.getMD5Hash((String)checksum);
                        String token = VinPlayUtils.encodeBase64((String)new EmailActive(nickname, user.getEmail(), datetime, checksum).toJson());
                        String url = GameCommon.getValueStr("URL_ACTIVE_EMAIL") + token;
                        AlertServiceImpl alertService = new AlertServiceImpl();
                        ArrayList<String> receives = new ArrayList<String>();
                        receives.add(user.getEmail());
                        String subject = String.format("[CGame] Activate security by email - %s (UTC)", DateTimeUtils.getFormatTime("yyyy-MM-dd HH:mm:ss", new Date()));
                        JsonObject params = new JsonObject();
                        params.addProperty("URL", url);
                        params.addProperty("Logo", String.format("%s/res/common/mail/logo.png?v=1", GameCommon.getValueStr((String)"WEB")));
                        if (alertService.sendEmail(subject, "activate-email-security", params.toString(), receives)) {
                            res = 0;
                        }
                    } else {
                        res = 2;
                    }
                }
            }
            catch (Exception e) {
                logger.debug(("activeEmail error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return (byte)res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String receiveActiveEmail(String token) {
        String res;
        block9 : {
            res = "Li\u00ean k\u1ebft n\u00e0y \u0111\u00e3 qu\u00e1 th\u1eddi gian x\u1eed l\u00fd";
            try {
                if (token == null) break block9;
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                ObjectMapper mapper = new ObjectMapper();
                token = VinPlayUtils.decodeBase64((String)token);
                EmailActive emailActive = (EmailActive)mapper.readValue(token, EmailActive.class);
                String nickname = emailActive.getNickname();
                String email = emailActive.getEmail();
                Date datetime = VinPlayUtils.getDateTime((String)emailActive.getTime());
                String checksum = nickname + email + emailActive.getTime() + "@VinPlay#6102$817";
                if (!(checksum = VinPlayUtils.getMD5Hash((String)checksum)).equals(emailActive.getChecksum()) || !userMap.containsKey(nickname) || VinPlayUtils.emailTimeout((Date)datetime)) break block9;
                try {
                    UserServiceImpl userService;
                     userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    if (user.getEmail().equals(email) && user.getEmail() != null && !user.isHasEmailSecurity() && !(userService = new UserServiceImpl()).checkEmailSecurity(email)) {
                        int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)5, (String)"1");
                        SecurityDaoImpl dao = new SecurityDaoImpl();
                        if (dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7)) {
                            user.setStatus(statusNew);
                            user.setHasEmailSecurity(true);
                            userMap.put(nickname, user);
                            String web = GameCommon.getValueStr("WEB");
                            res = "B\u1ea1n \u0111\u00e3 k\u00edch ho\u1ea1t b\u1ea3o m\u1eadt email th\u00e0nh c\u00f4ng. Vui l\u00f2ng \u0111\u0103ng nh\u1eadp <a href='" + web + "'>" + web + "</a> \u0111\u1ec3 ki\u1ec3m tra.";
                        }
                    }
                }
                catch (Exception e) {
                    logger.debug(("receiveActiveEmail error: " + e.getMessage()));
                }
                finally {
                     userMap.unlock(nickname);
                }
            }
            catch (Exception e2) {
                logger.debug(("receiveActiveEmail error: " + e2.getMessage()));
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse sendMoneyToSafe(String nickname, long money, boolean check) {
        IMap userMap;
        HazelcastInstance client;
        logger.debug(("Request sendMoneyToSafe: nickname: " + nickname + ", money: " + money));
        MoneyResponse response = new MoneyResponse(false, "1001");
        if (money > 0L && (userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                long moneyUser = user.getVin();
                long currentMoney = user.getVinTotal();
                long safeMoney = user.getSafe();
                if (moneyUser < money) {
                    response.setErrorCode("1002");
                } else if (check) {
                    response.setSuccess(true);
                    response.setErrorCode("0");
                } else {
                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                    context.beginTransaction();
                    try {
                        user.setVin(moneyUser -= money);
                        user.setVinTotal(currentMoney -= money);
                        user.setSafe(safeMoney += money);
                        SafeMoneyMessage message = new SafeMoneyMessage(VinPlayUtils.genMessageId(), user.getId(), nickname, "SafeMoney", moneyUser, currentMoney, money, "vin", safeMoney);
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)message, (int)14);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "SafeMoney", "\u0110\u00f3ng b\u0103ng Vin", currentMoney, -money, "vin", "Chuy\u1ec3n Vin v\u00e0o k\u00e9t s\u1eaft", 0L, false, user.isBot());
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                        userMap.put(nickname, user);
                        context.commitTransaction();
                        response.setSuccess(true);
                        response.setErrorCode("0");
                    }
                    catch (Exception e) {
                        logger.debug(("sendMoneyToSafe error: " + e.getMessage()));
                        context.rollbackTransaction();
                    }
                }
                response.setCurrentMoney(currentMoney);
                response.setSafeMoney(safeMoney);
                response.setMoneyUse(moneyUser);
            }
            catch (Exception e2) {
                logger.debug(("sendMoneyToSafe error: " + e2.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        logger.debug(("Response sendMoneyToSafe: " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse takeMoneyInSafe(String nickname, long money, boolean check) {
        IMap userMap;
        HazelcastInstance client;
        logger.debug(("Request takeMoneyInSafe: nickname: " + nickname + ", money: " + money));
        MoneyResponse response = new MoneyResponse(false, "1001");
        if (money > 0L && (userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                long moneyUser = user.getVin();
                long currentMoney = user.getVinTotal();
                long safeMoney = user.getSafe();
                if (safeMoney < money) {
                    response.setErrorCode("1002");
                } else if (check) {
                    response.setSuccess(true);
                    response.setErrorCode("0");
                } else {
                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                    context.beginTransaction();
                    try {
                        user.setVin(moneyUser += money);
                        user.setVinTotal(currentMoney += money);
                        user.setSafe(safeMoney -= money);
                        SafeMoneyMessage message = new SafeMoneyMessage(VinPlayUtils.genMessageId(), user.getId(), nickname, "SafeMoney", moneyUser, currentMoney, money, "vin", safeMoney);
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)message, (int)14);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "SafeMoney", "M\u1edf \u0111\u00f3ng b\u0103ng Vin", currentMoney, money, "vin", "R\u00fat Vin t\u1eeb k\u00e9t s\u1eaft", 0L, false, user.isBot());
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                        userMap.put(nickname, user);
                        context.commitTransaction();
                        response.setSuccess(true);
                        response.setErrorCode("0");
                    }
                    catch (Exception e) {
                        logger.debug(("takeMoneyInSafe error: " + e.getMessage()));
                        context.rollbackTransaction();
                    }
                }
                response.setCurrentMoney(currentMoney);
                response.setSafeMoney(safeMoney);
                response.setMoneyUse(moneyUser);
            }
            catch (Exception e2) {
                logger.debug(("takeMoneyInSafe error: " + e2.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        logger.debug(("Response takeMoneyInSafe: " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public BaseResponseModel updateAvatar(String nickname, String avatar) {
        BaseResponseModel res = new BaseResponseModel(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                SecurityDaoImpl dao = new SecurityDaoImpl();
                if (dao.updateUserInfo(user.getId(), avatar, 1)) {
                    user.setAvatar(avatar);
                    userMap.put(nickname, user);
                    res.setErrorCode("0");
                    res.setSuccess(true);
                }
            }
            catch (Exception e) {
                logger.debug(("updateAvatar error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public DoipassResponse changePassword(String nickname, String oldPassword, String newPassword, boolean check) {
        DoipassResponse response = new DoipassResponse();
        response.setResult((byte) 1);
        response.setIsAuth((byte) 0);
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.getFacebookId() != null && !user.getFacebookId().isEmpty()) {
                    response.setResult((byte) 4);
                } else if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) {
                    response.setResult((byte) 5);
                } else /*if (user.isHasMobileSecurity())*/ {
                    if (user.getPassword().equals(oldPassword)) {
                        if (check && user.isHasMobileSecurity()) {
                            response.setResult((byte) 0);
                            response.setIsAuth((byte) 1);
                        } else {
                            SecurityDaoImpl dao = new SecurityDaoImpl();
                            if (dao.updateUserInfo(user.getId(), newPassword, 2)) {
                                user.setPassword(newPassword);
                                userMap.put(nickname, user);
                                response.setResult((byte) 0);
                            }
                        }
                    } else {
                        response.setResult((byte) 3);
                    }
                } /*else {
                    res = 2;
                }*/
            }
            catch (Exception e) {
                logger.debug(("changePassword error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean checkMobileSecurity(String nickname) {
        boolean res = false;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.getMobile() != null && !user.getMobile().isEmpty() && user.isHasMobileSecurity()) {
                    res = true;
                }
            }
            catch (Exception e) {
                logger.debug(("checkMobileSecurity error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte loginWithOTP(String nickname, long money, byte type) {
        IMap userMap;
        HazelcastInstance client;
        byte res = 1;
        if (money >= 0L && (type == 0 || type == 1) && (userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.getMobile() != null && !user.getMobile().isEmpty() && user.isHasMobileSecurity()) {
                    SecurityDaoImpl dao = new SecurityDaoImpl();
                    if (type == 0) {
                        int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)7, (String)"0");
                        if (user.isHasLoginSecurity() && dao.updateUserInfo(user.getId(), "-1," + statusNew, 12)) {
                            user.setHasLoginSecurity(false);
                            user.setLoginOtp(-1L);
                            user.setStatus(statusNew);
                            userMap.put(nickname, user);
                            res = 0;
                        }
                    } else {
                        int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)7, (String)"1");
                        if (dao.updateUserInfo(user.getId(), String.valueOf(money) + "," + statusNew, 12)) {
                            user.setHasLoginSecurity(true);
                            user.setLoginOtp(money);
                            user.setStatus(statusNew);
                            userMap.put(nickname, user);
                            res = 0;
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.debug(("loginWithOTP error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte configGame(String nickname, String lisrGame, boolean check) {
        IMap userMap;
        HazelcastInstance client;
        byte res = 1;
        if (lisrGame != null && !lisrGame.isEmpty() && (userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (check && user.getMobile() != null && !user.getMobile().isEmpty() && user.isHasMobileSecurity()) return (byte) 2;

                ObjectMapper mapper = new ObjectMapper();
                List<ConfigGame> configGame = (List)mapper.readValue(lisrGame, (TypeReference)new TypeReference<List<ConfigGame>>(){});
                String[] listGame = GameCommon.getValueStr("LIST_GAME_BAI").split(",");
                if (configGame != null) {
                    SecurityDaoImpl dao;
                    int statusNew = user.getStatus();
                    boolean ck = true;
                    for (ConfigGame game : configGame) {
                        if (!Arrays.asList(listGame).contains(String.valueOf(game.getId()))) {
                            ck = false;
                            break;
                        }
                        String newBit = game.getStatus() == 1 ? "1" : "0";
                        statusNew = StatusUser.changeStatus((int)statusNew, (int)game.getId(), (String)newBit);
                    }
                    if (ck && (dao = new SecurityDaoImpl()).updateUserInfo(user.getId(), String.valueOf(statusNew), 7)) {
                        user.setStatus(statusNew);
                        userMap.put(nickname, user);
                        res = 0;
                    }
                }
            }
            catch (Exception e) {
                logger.debug(("configGame error: " + e.getMessage()));
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean updateStatusUser(String nickname, int action, String type) {
        IMap userMap;
        HazelcastInstance client;
        boolean res = false;
        SecurityDaoImpl dao = new SecurityDaoImpl();
        if (type == null) return res;
        if (!type.equals("0")) {
            if (!type.equals("1")) return res;
        }
        if ((userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)action, (String)type);
                if (!dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7)) return res;
                user.setStatus(statusNew);
                boolean ban = false;
                if (type.equals("1")) {
                    ban = true;
                }
                switch (action) {
                    case 0: {
                        user.setBanLogin(ban);
                        break;
                    }
                    case 1: {
                        user.setBanCashOut(ban);
                        break;
                    }
                    case 2: {
                        user.setCanLoginSandbox(ban);
                        break;
                    }
                    case 3: {
                        user.setBanTransferMoney(ban);
                    }
                }
                userMap.put(nickname, user);
                res = true;
                return res;
            }
            catch (Exception e) {
                logger.debug(("updateStatusUser error: " + e.getMessage()));
                return res;
            }
            finally {
                 userMap.unlock(nickname);
            }
        } else {
            try {
                UserModel user2 = dao.getStatus(nickname);
                if (user2 == null) return res;
                int statusNew = StatusUser.changeStatus((int)user2.getStatus(), (int)action, (String)type);
                if (!dao.updateUserInfo(user2.getId(), String.valueOf(statusNew), 7)) return res;
                return true;
            }
            catch (Exception e) {
                logger.debug(("updateStatusUser error: " + e.getMessage()));
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean changeSecurityUser(String nickname, int action, String type) {
        IMap userMap;
        HazelcastInstance client;
        boolean res = false;
        SecurityDaoImpl dao = new SecurityDaoImpl();
        if (action != 4) {
            if (action != 5) return res;
        }
        if ((userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)action, (String)type);
                if (!dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7)) return res;
                user.setStatus(statusNew);
                switch (action) {
                    case 4: {
                        if (type.equals("0")) {
                            user.setHasMobileSecurity(false);
                            break;
                        }
                        user.setHasMobileSecurity(true);
                        break;
                    }
                    case 5: {
                        if (type.equals("0")) {
                            user.setHasEmailSecurity(false);
                            break;
                        }
                        user.setHasEmailSecurity(true);
                    }
                }
                userMap.put(nickname, user);
                res = true;
                return res;
            }
            catch (Exception e) {
                logger.debug(("changeSecurityUser error: " + e.getMessage()));
                return res;
            }
            finally {
                 userMap.unlock(nickname);
            }
        } else {
            try {
                UserModel user2 = dao.getStatus(nickname);
                if (user2 == null) return res;
                int statusNew = StatusUser.changeStatus((int)user2.getStatus(), (int)action, (String)type);
                if (!dao.updateUserInfo(user2.getId(), String.valueOf(statusNew), 7)) return res;
                return true;
            }
            catch (Exception e) {
                logger.debug(("changeSecurityUser error: " + e.getMessage()));
            }
        }
        return res;
    }

    @Override
    public List<ConfigGame> getListGameBai(int status) throws KeyNotFoundException {
        String[] listGame = GameCommon.getValueStr("LIST_GAME_BAI").split(",");
        ArrayList<ConfigGame> listGameBai = new ArrayList<ConfigGame>();
        for (String sid : listGame) {
            int id = Integer.parseInt(sid);
            Games game = Games.findGameById((int)id);
            ConfigGame configGame = new ConfigGame(id, game.getDescription(), (byte)0);
            if (StatusUser.checkStatus((int)status, (int)id)) {
                configGame.setStatus((byte)1);
            }
            listGameBai.add(configGame);
        }
        return listGameBai;
    }

    public boolean saveLoginInfo(int userId, String username, String nickname, String ip, String agent, int type, String platform) {
        return saveLoginInfo(userId, username, nickname, ip, agent, type, platform, "", "", "", System.currentTimeMillis());
    }

    @Override
    public boolean saveLoginInfo(int userId, String username, String nickname, String ip, String agent, int type, String platform, String deviceName, String location, String loginMethod, long createdAtTs) {
        LoginPortalInfoMsg msg = new LoginPortalInfoMsg(userId, username, nickname, ip, agent, type, platform, deviceName, location, loginMethod, createdAtTs);
        try {
            MessageBusFactory.get("queue_login_info").publish("queue_login_info", (BaseMessage)msg, (int)50);
        }
        catch (Exception ex2) {
            Exception e = ex2;
            return false;
        }
        return true;
    }

}

