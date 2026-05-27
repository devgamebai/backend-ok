package game.third.usecase.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.vinplay.dal.config.CacheConfig;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import game.third.usecase.service.UserMoneyService;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class UserMoneyServiceImpl implements UserMoneyService {

    private static final Logger logger = Logger.getLogger("service");
    UserService userService = new UserServiceImpl();

    public long getMoneyFromUsersMap(String nickname, String moneyType) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            UserCacheModel user = (UserCacheModel) userMap.get(nickname);
            // Must be current spendable wallet balance, not lifetime PnL.
            // getTotalPnl returned accumulated profit/loss which is usually
            // negative for active players, caused GSC BalanceProcess to feed
            // GSC a negative balance so live-casino bets looked "rejected" /
            // balance display stayed stuck. getMoney is what bet() already
            // uses on the debit path, so both sides now agree.
            return user.getMoney(moneyType);
        }
        return 0L;
    }

    @Override
    public long getBalance(String nickname) {
        return getMoneyFromUsersMap(nickname, Consts.MONEY_VIN);
    }

    @Override
    public boolean bet(String nickname, long money, long fee, String gameName, String serviceName, String description, long referenceId, Map<String, Object> logMetadata) {
        return bet(nickname, money, fee, gameName, serviceName, description, referenceId, logMetadata, 0L);
    }

    @Override
    public boolean bet(String nickname, long money, long fee, String gameName, String serviceName, String description, long referenceId, Map<String, Object> logMetadata, long validBetAmount) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                long currentBalance = user.getMoney(Consts.MONEY_VIN);
                if (currentBalance >= Math.abs(money)) {
                    if (money > 0) {
                        money = -money;
                    }
                    logMetadata.put("nickname", nickname);
                    logMetadata.put("money", money);
                    logMetadata.put("gameName", gameName);
                    logMetadata.put("serviceName", serviceName);
                    logMetadata.put("action", "bet");
                    if (validBetAmount > 0L) {
                        logMetadata.put("validBetAmount", validBetAmount);
                    }
                    // Log the bet details

                    logger.debug("Bet placed: " + logMetadata);
                    // Call updateMoney with the optional commission-volume
                    // override. SUN-1205/1206: hedge-bet handling.
                    MoneyResponse response = this.userService.updateMoney(
                            nickname,
                            money,
                            Consts.MONEY_VIN,
                            gameName,
                            serviceName,
                            description,
                            fee,
                            referenceId,
                            TransType.START_TRANS,
                            validBetAmount
                    );
                    return response.isSuccess();
                } else {
                    System.out.println("Insufficient balance for user: " + nickname);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while betting: " + e.getMessage());
                return false;
            }
            return true;
        } else {
            logger.trace("User not found: " + nickname);
        }
        return false;
    }

    @Override
    public boolean reward(String nickname, long money, String gameName, String serviceName, String description, Map<String, Object> logMetadata) {
        return reward(nickname, money, gameName, serviceName, description, logMetadata, false);
    }

    @Override
    public boolean reward(String nickname, long money, String gameName, String serviceName, String description, Map<String, Object> logMetadata, boolean self) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel) userMap.get(nickname);

                logMetadata.put("nickname", nickname);
                logMetadata.put("money", money);
                logMetadata.put("gameType", gameName);
                logMetadata.put("serviceName", serviceName);
                logMetadata.put("action", "reward");
                // Log the reward details
                logger.debug("Reward given: " + logMetadata);
                // Call updateMoney
                MoneyResponse response;
                if (self) {
                    response = updateMoney(
                            nickname,
                            money,
                            Consts.MONEY_VIN,
                            gameName,
                            serviceName,
                            description,
                            0, // No fee for reward
                            0L, // No referenceId for reward
                            TransType.END_TRANS
                    );
                } else {
                    response = this.userService.updateMoney(
                            nickname,
                            money,
                            Consts.MONEY_VIN,
                            gameName,
                            serviceName,
                            description,
                            0, // No fee for reward
                            0L, // No referenceId for reward
                            TransType.END_TRANS
                    );
                }
                if (!response.isSuccess()) {
                    System.out.printf("Error while rewarding: {} \n", response);
                }
                return response.isSuccess();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while rewarding: " + e.getMessage());
                return false;
            }
        } else {
            logger.trace("User not found: " + nickname);
        }
        return false;
    }


    @Override
    public boolean isUser(String nickname) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            return true;
        } else {
            logger.trace("User not found: " + nickname);
        }
        return false;
    }

    @Override
    public boolean isUserAndCheckBalance(String nickname, double money) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                long currentBalance = user.getMoney(Consts.MONEY_VIN);
                if (currentBalance >= Math.abs(money)) {
                    return true;
                } else {
                    System.out.println("Insufficient balance for user: " + nickname);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logger.trace("User not found: " + nickname);
        }
        return false;
    }

    @Override
    public MoneyResponse updateMoney(String nickname, long money, String moneyType, String gameName, String serviceName, String description, long fee, Long transId, TransType type) {
        //logger.debug(("Request updateMoney:  nickname: " + nickname + ", money: " + money + ", moneyType: " + moneyType + ", gameName: " + gameName + ", serviceName: " + serviceName + ", description: " + description + ", fee: " + fee + ", transId: " + transId + ", TransType: " + type.getId()));
        MoneyResponse response = new MoneyResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030", "can not connect hazelcast");
            response.setErrorCode("1030");
            return response;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                if (user.isBot()) {
                    response.setSuccess(true);
                    response.setErrorCode("0");
                    return response;
                }
                long moneyUser = user.getMoney(moneyType);
                long currentMoney = user.getTotalPnl(moneyType);
                if (money != 0L) {
                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                    context.beginTransaction();
                    try {
                        user.setMoney(moneyType, moneyUser += money);
                        user.setTotalPnl(moneyType, currentMoney += money);
                        long moneyVP = VippointUtils.calculateMoneyVP(moneyType, transId, client, nickname, gameName, money, type);
                        int vp = 0;
                        int moneyVPs = 0;
                        int vpAddEvent = 0;
                        if (moneyVP > 0L) {
                            List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(), (long) user.getMoneyVP() + moneyVP, false);
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
                                VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(), nickname, vpReal, vpEvent, 0, 0, 0, 0, place, placeMax, 0, 0);
                                MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage, 801);
                            }
                        }
                        boolean playgame = false;
                        if (transId != null || type == TransType.VIPPOINT) {
                            playgame = true;
                        }
                        MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, money, moneyType, fee, moneyVPs, vp);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName, serviceName, currentMoney, money, moneyType, description, fee, playgame, user.isBot());
                        messageLog.setReferralCode(user.getReferralCode());
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, 16);
                        if (!isUserRateHigh(nickname, client)) {
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                        }
                        userMap.put(nickname, user, 1L, java.util.concurrent.TimeUnit.HOURS);
                        context.commitTransaction();
                        response.setSuccess(true);
                        response.setErrorCode("0");
                    } catch (Exception e) {
                        context.rollbackTransaction();
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1031", "error rmq: " + e.getMessage());
                        response.setErrorCode("1031");
                    }
                } else {
                    if (moneyType.equals("vin") && transId != null && type.getId() == TransType.END_TRANS.getId()) {
                        IMap vpCache = client.getMap("VPMinigame");
                        String vpCacheId = nickname + gameName + transId;
                        long moneyVP2 = 0L;
                        if (vpCache.containsKey(vpCacheId)) {
                            moneyVP2 = Math.abs((Long) vpCache.get(vpCacheId));
                            vpCache.remove(vpCacheId);
                            if (moneyVP2 > 0L) {
                                List<Integer> vpLst2 = VippointUtils.calculateVP(client, user.getNickname(), (long) user.getMoneyVP() + moneyVP2, false);
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
                                    VippointEventMessage vpEventMessage2 = new VippointEventMessage(user.getId(), nickname, vpReal2, vpEvent2, 0, 0, 0, 0, place2, placeMax2, 0, 0);
                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage2, 801);
                                }
                                VippointMessage message2 = new VippointMessage(user.getId(), nickname, moneyVPs2, vp2);
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message2, 18);
                                userMap.put(nickname, user, 1L, java.util.concurrent.TimeUnit.HOURS);
                            }
                        }
                    }
                    response.setSuccess(true);
                    response.setErrorCode("0");
                }
                response.setCurrentMoney(currentMoney);
            } catch (Exception e2) {
                logger.debug(e2);
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030", "error hazelcast: " + e2.getMessage());
                response.setErrorCode("1030");
            } finally {
                userMap.unlock(nickname);
            }
        }
        //logger.debug(("Response updateMoney:" + response.toJson()));
        return response;
    }

    private boolean isUserRateHigh(String nickName, HazelcastInstance client) {
        IMap userMap = client.getMap(CacheConfig.CACHE_USER_WIN);
        return userMap.containsKey(nickName) && (Boolean) userMap.get(nickName);
    }
}