package com.vinplay.usercore.service.impl;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.vinplay.utils.TelegramAlert;
import org.apache.log4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.MoneyInGameDaoImpl;
import com.vinplay.usercore.entities.LogTransferAgentModel;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.model.GetCardGameDataInfo;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.enums.FreezeInGame;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.FreezeMoneyMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInGame;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.FreezeMoneyResponse;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;

public class MoneyInGameServiceImpl
        implements MoneyInGameService {
    private static final Logger logger = Logger.getLogger("user_core");

    @Override
    public FreezeModel getFreeze(String sessionId) throws SQLException {
        FreezeModel freeze = null;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap freezeMap = client.getMap("freeze");
        if (freezeMap.containsKey((Object) sessionId)) {
            freeze = (FreezeModel) freezeMap.get((Object) sessionId);
        } else {
            MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
            freeze = dao.getFreeze(sessionId);
        }
        return freeze;
    }

    @Override
    public boolean pushFreezeToCache(FreezeModel model) {
        HazelcastInstance client;
        IMap freezeMap;
        boolean res = false;
        if (model != null && !(freezeMap = (client = HazelcastClientFactory.getInstance()).getMap("freeze")).containsKey((Object) model.getSessionId())) {
            freezeMap.put((Object) model.getSessionId(), (Object) model);
            res = true;
        }
        return res;
    }

    @Override
    public List<FreezeModel> getListFreezeMoneyAgentTranfer(String gameName, String nickName, String moneyType, String startTime, String endTime, int page, String status) throws SQLException {
        MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
        return dao.getListFreezeMoneyAgentTranfer(gameName, nickName, moneyType, startTime, endTime, page, status);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public FreezeMoneyResponse freezeMoneyTranferAgent(String agentLevel1, String nickReceive, String gameName, long money, String moneyType, String transNo) {
        FreezeMoneyResponse response;
        block34:
        {
            logger.debug((Object) ("Start request FreezeMoneyTranferAgent: nickReceive: " + nickReceive + ", gameName: " + gameName + ", money: " + money + ", moneyType: " + moneyType + ", transactionNo: " + transNo));
            response = new FreezeMoneyResponse(false, "1001");
            if (money > 0L) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1030", "can not connect hazelcast");
                    response.setErrorCode("1030");
                    return response;
                }
                IMap<String, UserModel> userMap = client.getMap("users");
                if (userMap.containsKey((Object) nickReceive)) {
                    try {
                        userMap.lock(nickReceive);
                        UserCacheModel user = (UserCacheModel) userMap.get((Object) nickReceive);
                        long currentMoney = user.getTotalPnl(moneyType);
                        response.setCurrentMoney(currentMoney);
                        long moneyUse = user.getMoney(moneyType);
                        long moneySafe = user.getSafe();
                        long moneyCanFreeze = moneyUse + moneySafe;
                        if (moneyCanFreeze <= 0L) {
                            response.setErrorCode("1042");
                            FreezeMoneyResponse freezeMoneyResponse = response;
                            return freezeMoneyResponse;
                        }
                        if (moneyCanFreeze < money) {
                            money = moneyCanFreeze;
                        }
                        String sessionId = VinPlayUtils.genSessionId((int) user.getId(), (String) gameName);
                        response.setSessionId(sessionId);
                        IMap freezeMap = client.getMap("freeze");
                        if (freezeMap.containsKey((Object) sessionId)) {
                            response.setErrorCode("1004");
                            MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1004", "sessionId duplicate");
                        }
                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                        context.beginTransaction();
                        try {
                            FreezeModel freeze = new FreezeModel(sessionId, nickReceive, gameName, "", money, moneyType, new Date(), transNo, 0);
                            long moneySubSafe = 0L;
                            if (money > moneyUse) {
                                moneySubSafe = money - moneyUse;
                                moneySafe -= moneySubSafe;
                                moneyUse = 0L;
                            } else {
                                moneyUse -= money;
                            }
                            user.setMoney(moneyType, moneyUse);
                            user.setSafe(moneySafe);
                            FreezeMoneyMessage message = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId, user.getId(), nickReceive, gameName, "", money, moneyUse, currentMoney, moneyType, transNo);
                            MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish((String) "queue_log_chuyen_tien_dai_ly", (BaseMessage) message, (int) 11);
                            if (moneySubSafe > 0L) {
                                MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
                                dao.updateSafeMoney(moneySubSafe, user.getId());
                            }
                            freezeMap.put((Object) sessionId, (Object) freeze);
                            userMap.put(nickReceive, user);
                            context.commitTransaction();
                            response.setSuccess(true);
                            response.setErrorCode("0");
                        } catch (Exception e) {
                            context.rollbackTransaction();
                            MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1031", "error rmq: " + e.getMessage());
                            response.setErrorCode("1031");
                        }
                    } catch (Exception e2) {
                        MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                        response.setErrorCode("1030");
                    } finally {
                        userMap.unlock(nickReceive);
                    }
                } else {
                    try {
                        MoneyInGameDaoImpl dao2 = new MoneyInGameDaoImpl();
                        UserCacheModel user2 = dao2.getUserByNickName(nickReceive);
                        long currentMoney2 = user2.getTotalPnl(moneyType);
                        response.setCurrentMoney(currentMoney2);
                        long moneyUse2 = user2.getMoney(moneyType);
                        long moneySafe2 = user2.getSafe();
                        long moneyCanFreeze2 = moneyUse2 + moneySafe2;
                        if (moneyCanFreeze2 <= 0L) {
                            response.setErrorCode("1042");
                            return response;
                        }
                        if (moneyCanFreeze2 < money) {
                            money = moneyCanFreeze2;
                        }
                        String sessionId2 = VinPlayUtils.genSessionId((int) user2.getId(), (String) gameName);
                        response.setSessionId(sessionId2);
                        IMap freezeMap2 = client.getMap("freeze");
                        if (freezeMap2.containsKey((Object) sessionId2)) {
                            response.setErrorCode("1004");
                            MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1004", "sessionId duplicate");
                        } else {
                            TransactionContext context2 = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                            context2.beginTransaction();
                            try {
                                FreezeModel freeze2 = new FreezeModel(sessionId2, nickReceive, gameName, "", money, moneyType, new Date(), transNo, 0);
                                long moneySubSafe2 = 0L;
                                if (money > moneyUse2) {
                                    moneySubSafe2 = money - moneyUse2;
                                    moneySafe2 -= moneySubSafe2;
                                    moneyUse2 = 0L;
                                } else {
                                    moneyUse2 -= money;
                                }
                                user2.setMoney(moneyType, moneyUse2);
                                user2.setSafe(moneySafe2);
                                FreezeMoneyMessage message2 = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId2, user2.getId(), nickReceive, gameName, "", money, moneyUse2, currentMoney2, moneyType, transNo);
                                MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish((String) "queue_log_chuyen_tien_dai_ly", (BaseMessage) message2, (int) 11);
                                if (moneySubSafe2 > 0L) {
                                    dao2.updateSafeMoney(moneySubSafe2, user2.getId());
                                }
                                freezeMap2.put((Object) sessionId2, (Object) freeze2);
                                context2.commitTransaction();
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            } catch (Exception e3) {
                                context2.rollbackTransaction();
                                MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1031", "error rmq: " + e3.getMessage());
                                response.setErrorCode("1031");
                            }
                        }
                    } catch (Exception e2) {
                        MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                        response.setErrorCode("1030");
                    }
                }
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String alertContent = "[THONG BAO] Dai ly " + agentLevel1 + " da dong bang " + money + " vin cua user " + nickReceive + ". Thuc hien luc: " + format.format(new Date());
                    AlertServiceImpl alertService = new AlertServiceImpl();
                    if (GameCommon.getValueStr("FREEZE_MONEY_GROUP_NUMBER").contains(",")) {
                        String[] arr = GameCommon.getValueStr("FREEZE_MONEY_GROUP_NUMBER").split(",");
                        ArrayList<String> mList = new ArrayList<String>();
                        for (String m : arr) {
                            m = m.trim();
                            mList.add(m);
                        }
                        alertService.alert2List(mList, alertContent, false);
                        break block34;
                    }
                    alertService.alert2One(GameCommon.getValueStr("FREEZE_MONEY_GROUP_NUMBER"), alertContent, false);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } else {
                response.setErrorCode("1017");
                MoneyLogger.log(nickReceive, gameName, money, 0L, moneyType, "Dong bang tien", "1017", "money <= 0");
            }
        }
        logger.debug((Object) ("Finish request FreezeMoneyTranferAgent, Response: " + response.toJson()));
        return response;
    }

    @Override
    public boolean restoreFreezeTranferAgent(String sessionId) throws SQLException {
        boolean response = false;
        MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
        String nickName = dao.getNickNameFreezeMoneyAgentTranferBySessionId(sessionId);
        if (nickName == null || nickName.isEmpty()) {
            return response;
        }
        logger.debug((Object) ("Start Request restoreFreezeTranferAgent: sessionId: " + sessionId + ", nickname: " + nickName + ", gameName: FreezeMoneyTranferAgent, moneyType: vin"));
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "can not connect hazelcast");
            response = false;
        }
        IMap freezeMap = client.getMap("freeze");
        boolean isCacheFreeze = freezeMap.containsKey((Object) sessionId);
        IMap<String, UserModel> userMap = client.getMap("users");
        boolean isCacheUser = userMap.containsKey((Object) nickName);
        if (isCacheUser && isCacheFreeze) {
            response = this.restoreFreezeTranferAgentTH1(sessionId, nickName);
        } else if (isCacheUser && !isCacheFreeze) {
            response = this.restoreFreezeTranferAgentTH2(sessionId, nickName);
        } else if (!isCacheUser && isCacheFreeze) {
            response = this.restoreFreezeTranferAgentTH3(sessionId, nickName);
        } else if (!isCacheUser && !isCacheFreeze) {
            response = this.restoreFreezeTranferAgentTH4(sessionId, nickName);
        }
        logger.debug((Object) "Finish restoreFreezeTranferAgent");
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean restoreFreezeTranferAgentTH1(String sessionId, String nickName) {
        boolean response;
        block14:
        {
            response = true;
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            try {
                userMap.lock(nickName);
                UserCacheModel user = (UserCacheModel) userMap.get((Object) nickName);
                long currentMoney = user.getTotalPnl("vin");
                IMap freezeMap = client.getMap("freeze");
                if (freezeMap.containsKey((Object) sessionId)) {
                    try {
                        freezeMap.lock((Object) sessionId);
                        FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                        context.beginTransaction();
                        try {
                            // find log agent transaction
                            MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
                            LogTransferAgentModel model = dao.getMoneyAgentTranferBySessionId(sessionId);
                            if (model != null) {
                                //find sender user
                                if (userMap.containsKey(model.getNick_name_send())) {
                                    UserCacheModel sender = (UserCacheModel) userMap.get((Object) model.getNick_name_send());
                                    if (sender.getDaily() > 0) {
                                        userMap.lock(sender.getNickname());
                                        long moneyUse = user.getMoney("vin");
                                        if (freeze.getMoney() > 0L) {
                                            user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                                        }
                                        // cong lai tien cho dai ly
                                        UserServiceImpl service = new UserServiceImpl();
                                        service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                                        freezeMap.remove((Object) sessionId);
                                        userMap.put(nickName, user);
                                    }
                                } else {
                                    // tim torng mysql
                                    UserCacheModel sender = dao.getUserByNickName(model.getNick_name_send());
                                    if (sender != null && sender.getDaily() > 0) {
                                        userMap.lock(sender.getNickname());
                                        long moneyUse = user.getMoney("vin");
                                        if (freeze.getMoney() > 0L) {
                                            user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                                        }
                                        // cong lai tien cho dai ly
                                        UserServiceImpl service = new UserServiceImpl();
                                        service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                                        freezeMap.remove((Object) sessionId);
                                        userMap.put(nickName, user);
                                    }
                                }
                            }
                            response = false;
                        } catch (Exception e) {
                            context.rollbackTransaction();
                            MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                            response = false;
                        }
                        break block14;
                    } catch (Exception e2) {
                        MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                        response = false;
                        break block14;
                    } finally {
                        freezeMap.unlock((Object) sessionId);
                    }
                }
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1003", "sessionId not exist: ");
                response = false;
            } catch (Exception e3) {
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e3.getMessage());
                response = false;
            } finally {
                userMap.unlock(nickName);
            }
        }
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean restoreFreezeTranferAgentTH2(String sessionId, String nickName) {
        boolean response = true;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        try {
            userMap.lock(nickName);
            UserCacheModel user = (UserCacheModel) userMap.get((Object) nickName);
            long currentMoney = user.getTotalPnl("vin");
            MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
            FreezeModel freeze = dao.getFreezeMoneyAgentTranferBySessionId(sessionId);
            TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
            context.beginTransaction();
            try {
                // find log agent transaction
                MoneyInGameDaoImpl moneyDao = new MoneyInGameDaoImpl();
                LogTransferAgentModel model = moneyDao.getMoneyAgentTranferBySessionId(sessionId);
                if (model != null) {
                    //find sender user
                    if (userMap.containsKey(model.getNick_name_send())) {
                        UserCacheModel sender = (UserCacheModel) userMap.get((Object) model.getNick_name_send());
                        if (sender.getDaily() > 0) {
                            userMap.lock(sender.getNickname());
                            long moneyUse = user.getMoney("vin");
                            if (freeze.getMoney() > 0L) {
                                user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                            }
                            // cong lai tien cho dai ly
                            UserServiceImpl service = new UserServiceImpl();
                            service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                            userMap.put(nickName, user);
                        }
                    } else {
                        // tim torng mysql
                        UserCacheModel sender = dao.getUserByNickName(model.getNick_name_send());
                        if (sender != null && sender.getDaily() > 0) {
                            userMap.lock(sender.getNickname());
                            long moneyUse = user.getMoney("vin");
                            if (freeze.getMoney() > 0L) {
                                user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                            }
                            // cong lai tien cho dai ly
                            UserServiceImpl service = new UserServiceImpl();
                            service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                            userMap.put(nickName, user);
                        }
                    }
                }
                response = false;
            } catch (Exception e) {
                context.rollbackTransaction();
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                response = false;
            }
        } catch (Exception e2) {
            MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
            response = false;
        } finally {
            userMap.unlock(nickName);
        }
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean restoreFreezeTranferAgentTH3(String sessionId, String nickName) {
        boolean response;
        block11:
        {
            response = true;
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            try {
                MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
                UserCacheModel user = dao.getUserByNickName(nickName);
                long currentMoney = user.getTotalPnl("vin");
                IMap freezeMap = client.getMap("freeze");
                if (freezeMap.containsKey((Object) sessionId)) {
                    try {
                        freezeMap.lock((Object) sessionId);
                        FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                        context.beginTransaction();
                        try {
                            // find log agent transaction
                            MoneyInGameDaoImpl moneyDao = new MoneyInGameDaoImpl();
                            LogTransferAgentModel model = moneyDao.getMoneyAgentTranferBySessionId(sessionId);
                            if (model != null) {
                                //find sender user
                                if (userMap.containsKey(model.getNick_name_send())) {
                                    UserCacheModel sender = (UserCacheModel) userMap.get((Object) model.getNick_name_send());
                                    if (sender.getDaily() > 0) {
                                        userMap.lock(sender.getNickname());
                                        long moneyUse = user.getMoney("vin");
                                        if (freeze.getMoney() > 0L) {
                                            user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                                        }
                                        // cong lai tien cho dai ly
                                        UserServiceImpl service = new UserServiceImpl();
                                        service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                                        userMap.put(nickName, user);
                                    }
                                } else {
                                    // tim torng mysql
                                    UserCacheModel sender = dao.getUserByNickName(model.getNick_name_send());
                                    if (sender != null && sender.getDaily() > 0) {
                                        userMap.lock(sender.getNickname());
                                        long moneyUse = user.getMoney("vin");
                                        if (freeze.getMoney() > 0L) {
                                            user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                                        }
                                        // cong lai tien cho dai ly
                                        UserServiceImpl service = new UserServiceImpl();
                                        service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                                        userMap.put(nickName, user);
                                    }
                                }
                            }
                            response = false;
                        } catch (Exception e) {
                            context.rollbackTransaction();
                            MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                            response = false;
                        }
                        break block11;
                    } catch (Exception e2) {
                        MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                        response = false;
                        break block11;
                    } finally {
                        freezeMap.unlock((Object) sessionId);
                    }
                }
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1003", "sessionId not exist: ");
                response = false;
            } catch (Exception e3) {
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e3.getMessage());
                response = false;
            }
        }
        return response;
    }

    private boolean restoreFreezeTranferAgentTH4(String sessionId, String nickName) {
        boolean response = true;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        try {
            MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
            UserCacheModel user = dao.getUserByNickName(nickName);
            long currentMoney = user.getTotalPnl("vin");
            FreezeModel freeze = dao.getFreezeMoneyAgentTranferBySessionId(sessionId);
            TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
            context.beginTransaction();
            try {
                // find log agent transaction
                MoneyInGameDaoImpl moneyDao = new MoneyInGameDaoImpl();
                LogTransferAgentModel model = moneyDao.getMoneyAgentTranferBySessionId(sessionId);
                if (model != null) {
                    //find sender user
                    if (userMap.containsKey(model.getNick_name_send())) {
                        UserCacheModel sender = (UserCacheModel) userMap.get((Object) model.getNick_name_send());
                        if (sender.getDaily() > 0) {
                            userMap.lock(sender.getNickname());
                            long moneyUse = user.getMoney("vin");
                            if (freeze.getMoney() > 0L) {
                                user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                            }
                            // cong lai tien cho dai ly
                            UserServiceImpl service = new UserServiceImpl();
                            service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                            userMap.put(nickName, user);
                        }
                    } else {
                        // tim torng mysql
                        UserCacheModel sender = dao.getUserByNickName(model.getNick_name_send());
                        if (sender != null && sender.getDaily() > 0) {
                            userMap.lock(sender.getNickname());
                            long moneyUse = user.getMoney("vin");
                            if (freeze.getMoney() > 0L) {
                                user.setTotalPnl("vin", currentMoney -= freeze.getMoney());
                            }
                            // cong lai tien cho dai ly
                            UserServiceImpl service = new UserServiceImpl();
                            service.updateMoneyFromAdmin(model.getNick_name_send(), model.getMoney_send(), "vin", "UnfreezeAgentTransferMoney", "Admin", "Chuyen lai tien bi dong bang");
                            userMap.put(nickName, user);
                        }
                    }
                }
                response = false;
            } catch (Exception e) {
                context.rollbackTransaction();
                MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                response = false;
            }
        } catch (Exception e2) {
            MoneyLogger.log(nickName, "FreezeMoneyTranferAgent", 0L, 0L, "vin", "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
            response = false;
        }
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public FreezeMoneyResponse restoreMoneyTranferAgent(String sessionId, String nickname, String gameName, String moneyType) {
        FreezeMoneyResponse response;
        block28:
        {
            logger.debug((Object) ("Start Request restoreMoneyTranferAgent: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", moneyType: " + moneyType));
            response = new FreezeMoneyResponse(false, "1001");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        try {
                            freezeMap.lock((Object) sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                            TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                            context.beginTransaction();
                            try {
                                long moneyUse = user.getMoney(moneyType);
                                if (freeze.getMoney() > 0L) {
                                    user.setMoney(moneyType, moneyUse += freeze.getMoney());
                                }
                                FreezeMoneyMessage message = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId, user.getId(), nickname, gameName, "", freeze.getMoney(), moneyUse, currentMoney, moneyType, freeze.getTransNo());
                                MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish((String) "queue_log_chuyen_tien_dai_ly", (BaseMessage) message, (int) 15);
                                freezeMap.remove((Object) sessionId);
                                userMap.put(nickname, user);
                                context.commitTransaction();
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            } catch (Exception e) {
                                context.rollbackTransaction();
                                MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                                response.setErrorCode("1031");
                            }
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                        } finally {
                            freezeMap.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1003", "sessionId not exist: ");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            } else {
                try {
                    MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
                    UserCacheModel user2 = dao.getUserByNickName(nickname);
                    long currentMoney2 = user2.getTotalPnl(moneyType);
                    response.setCurrentMoney(currentMoney2);
                    IMap freezeMap2 = client.getMap("freeze");
                    if (freezeMap2.containsKey((Object) sessionId)) {
                        try {
                            freezeMap2.lock((Object) sessionId);
                            FreezeModel freeze2 = (FreezeModel) freezeMap2.get((Object) sessionId);
                            TransactionContext context2 = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                            context2.beginTransaction();
                            try {
                                long moneyUse2 = user2.getMoney(moneyType);
                                if (freeze2.getMoney() > 0L) {
                                    user2.setMoney(moneyType, moneyUse2 += freeze2.getMoney());
                                }
                                FreezeMoneyMessage message2 = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId, user2.getId(), nickname, gameName, "", freeze2.getMoney(), moneyUse2, currentMoney2, moneyType, freeze2.getTransNo());
                                MessageBusFactory.get("queue_log_chuyen_tien_dai_ly").publish((String) "queue_log_chuyen_tien_dai_ly", (BaseMessage) message2, (int) 15);
                                freezeMap2.remove((Object) sessionId);
                                context2.commitTransaction();
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            } catch (Exception e4) {
                                context2.rollbackTransaction();
                                MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1031", "error rmq: " + e4.getMessage());
                                response.setErrorCode("1031");
                            }
                            break block28;
                        } catch (Exception e5) {
                            MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e5.getMessage());
                            response.setErrorCode("1030");
                            break block28;
                        } finally {
                            freezeMap2.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1003", "sessionId not exist: ");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                }
            }
        }
        response.setSessionId(sessionId);
        logger.debug((Object) ("Finish Response restoreMoneyTranferAgent: " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public FreezeMoneyResponse freezeMoneyInGame(String nickname, String gameName, String roomId, long money, String moneyType) {
        Debug.trace((Object) ("Request freezeMoneyInGame: nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", money: " + money + ", moneyType: " + moneyType));
        FreezeMoneyResponse response = new FreezeMoneyResponse(false, "1001");
        if (money > 0L) {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setCurrentMoney(currentMoney);
                    if (user.getMoney(moneyType) < money) {
                        response.setErrorCode("1002");
                        MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1002", "Khong du tien");
                        CommonHandle.writeInfoLog("TLMN check money 729 " + user.getMoney(moneyType) + " " + money);
                        return response;
                    }
                    String sessionId = VinPlayUtils.genSessionId((int) user.getId(), (String) gameName);
                    response.setSessionId(sessionId);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        response.setErrorCode("1004");
                        MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1002", "sessionId duplicate");

                        return response;
                    }
                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                    context.beginTransaction();
                    try {
                        FreezeModel freeze = new FreezeModel(sessionId, nickname, gameName, roomId, money, moneyType, new Date(), (String) null, 0);
                        long moneyUse = user.getMoney(moneyType) - money;
                        user.setMoney(moneyType, moneyUse);
                        FreezeMoneyMessage message = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId, user.getId(), nickname, gameName, roomId, money, moneyUse, currentMoney, moneyType, (String) null);
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 12);
                        freezeMap.put((Object) sessionId, (Object) freeze);
                        userMap.put(nickname, user);
                        context.commitTransaction();
                        response.setSuccess(true);
                        response.setErrorCode("0");
                        CommonHandle.writeInfoLog("TLMN check money 754 " + user.getMoney(moneyType) + " " + money);
                    } catch (Exception e) {
                        context.rollbackTransaction();
                        MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1031", "error rmq: " + e.getMessage());
                        response.setErrorCode("1031");
                    }
                } catch (Exception e2) {
                    MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        } else if (money == 0L) {
            response.setSuccess(true);
            response.setErrorCode("0");
        } else {
            response.setErrorCode("1017");
            MoneyLogger.log(nickname, gameName, money, 0L, moneyType, "Dong bang tien", "1017", "money < 0");
        }
        logger.debug((Object) ("Response freezeMoneyInGame: nickname: " + nickname + ", gameName: " + gameName + " " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public FreezeMoneyResponse restoreMoneyInGame(String sessionId, String nickname, String gameName, String roomId, String moneyType) {
        FreezeMoneyResponse response;
        block16:
        {
            logger.debug((Object) ("Request restoreMoneyInGame: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", moneyType: " + moneyType));
            response = new FreezeMoneyResponse(false, "1001");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        try {
                            freezeMap.lock((Object) sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                            TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                            context.beginTransaction();
                            try {
                                long moneyUse = user.getMoney(moneyType);
                                if (freeze.getMoney() > 0L) {
                                    user.setMoney(moneyType, moneyUse += freeze.getMoney());
                                }
                                FreezeMoneyMessage message = new FreezeMoneyMessage(VinPlayUtils.genMessageId(), sessionId, user.getId(), nickname, gameName, roomId, freeze.getMoney(), moneyUse, currentMoney, moneyType, (String) null);
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 13);
                                freezeMap.remove((Object) sessionId);
                                userMap.put(nickname, user);
                                context.commitTransaction();
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            } catch (Exception e) {
                                context.rollbackTransaction();
                                MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1031", "error rmq: " + e.getMessage());
                                response.setErrorCode("1031");
                            }
                            break block16;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block16;
                        } finally {
                            freezeMap.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1003", "sessionId not exist: ");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, 0L, 0L, moneyType, "Mo dong bang tien", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        response.setSessionId(sessionId);
        logger.debug((Object) ("Response restoreMoneyInGame: " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse addingMoneyInGame(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, long maxFreeze, String matchId, long fee) {
        MoneyResponse response;
        block26:
        {
            logger.debug("Request addingMoneyInGame: sessionId: " + sessionId + ", nickname: " +
                    nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", matchId: " + matchId + ", money: " + money +
                    ", moneyType: " + moneyType + ", maxFreeze: " + maxFreeze + ", fee: " + fee);
            response = new MoneyResponse(false, "1001");
            boolean bBuyIn = this.getGameType(gameName);
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tien", "1030"
                        , "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey(nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey(sessionId)) {
                        try {
                            freezeMap.lock(sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get(sessionId);
                            long freezeMoney = freeze.getMoney();
                            response.setFreezeMoney(freezeMoney);
                            if (money > 0L) {
                                long addMoneyUser = 0L;
                                long addMoneyFreeze = 0L;
                                if (bBuyIn) {
                                    addMoneyFreeze = money;
                                } else if (freezeMoney >= maxFreeze) {
                                    addMoneyUser = money;
                                } else if (freezeMoney + money > maxFreeze) {
                                    addMoneyUser = freezeMoney + money - maxFreeze;
                                    addMoneyFreeze = maxFreeze - freezeMoney;
                                } else {
                                    addMoneyFreeze = money;
                                }
                                TransactionContext context = client.newTransactionContext(new
                                        TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                context.beginTransaction();
                                try {
                                    user.setMoney(moneyType, moneyUser += addMoneyUser);
                                    user.setTotalPnl(moneyType, currentMoney += money);
                                    long fMoney = -1L;
                                    if (addMoneyFreeze > 0L) {
                                        freeze.setMoney(freezeMoney += addMoneyFreeze);
                                        fMoney = freezeMoney;
                                    }
                                    int vp = 0;
                                    int moneyVPs = 0;
                                    int vpAddEvent = 0;
                                    if (moneyType.equals("vin") && !bBuyIn) {
                                        List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(),
                                                (long) user.getMoneyVP() + Math.abs(money), false);
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
                                            VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(), nickname,
                                                    vpReal, vpEvent, 0, 0, 0, 0, place, placeMax,
                                                    0, 0);
                                            MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", vpEventMessage, 801);
                                        }
                                    }
                                    MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(), user.getId(),
                                            nickname, gameName, moneyUser, currentMoney, money, moneyType, fMoney, sessionId,
                                            fee, moneyVPs, vp);
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, (int) 10);
                                    //TODO
                                    String des = "";
                                    if ((Games.XOC_DIA.getName().equals(gameName) || Games.BA_CAY.getName().equals(gameName)) && user.isBot()) {
                                        logger.info("XOC_DIA not write bot logs ");
                                    } else {
                                        des = GetCardGameDataInfo.getCardGameDataInfo(
                                                VinPlayUtils.getGameID(gameName), roomId, matchId);
                                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                                VinPlayUtils.getGameID(gameName), currentMoney, money, moneyType, des
                                                , fee, true, user.isBot());
                                        messageLog.setReferralCode(user.getReferralCode());
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                                    }

                                    freezeMap.put(sessionId, freeze);
                                    userMap.put(nickname, user);
                                    context.commitTransaction();
                                    response.setSuccess(true);
                                    response.setErrorCode("0");
                                    response.setCurrentMoney(currentMoney);
                                    response.setFreezeMoney(freezeMoney);
                                    response.setMoneyUse(moneyUser);
                                    if (!user.isBot())
                                        TelegramAlert.sendUserPlayGame(nickname, String.valueOf(money), gameName, des);
                                } catch (Exception e) {
                                    context.rollbackTransaction();
                                    MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tien", "1031",
                                            "error rmq: " + e.getMessage());
                                    response.setErrorCode("1031");
                                }
                            } else {
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            }
                            break block26;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tien", "1030",
                                    "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block26;
                        } finally {
                            freezeMap.unlock(sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tien",
                            "1003", "session not exist");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tien", "1030",
                            "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        logger.debug("Response addingMoneyInGame: sessionId: " + sessionId + " " + response.toJson());
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse subtractMoneyInGame(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, String matchId) {
        MoneyResponse response;
        block32:
        {
            /*Debug.trace("Request subtractMoneyInGame 1: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName
                    + ", roomId: " + roomId + ", matchId: " + matchId + ", money: " + money + ", moneyType: " + moneyType);*/
            response = new MoneyResponse(false, "1001");
            boolean bBuyIn = this.getGameType(gameName);
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030",
                        "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }

            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey(nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    long subtractMoney = 0L;
                    long subtractMoneyUser = 0L;
                    long subtractMoneyFreeze = 0L;
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");

                    if (freezeMap.containsKey(sessionId)) {
                        try {
                            freezeMap.lock(sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get(sessionId);
                            long freezeMoney = freeze.getMoney();
                            response.setFreezeMoney(freezeMoney);

                            if (money > 0L) {
                                if (moneyUser + freezeMoney >= money) {
                                    if (bBuyIn) {
                                        if (freezeMoney >= money) {
                                            subtractMoneyFreeze = money;
                                        } else if (moneyUser + freezeMoney >= money) {
                                            subtractMoneyFreeze = freezeMoney;
                                            subtractMoneyUser = money - freezeMoney;
                                        } else {
                                            subtractMoneyUser = moneyUser;
                                            subtractMoneyFreeze = freezeMoney;
                                        }
                                    } else if (moneyUser >= money) {
                                        subtractMoneyUser = money;
                                    } else if (moneyUser + freezeMoney >= money) {
                                        subtractMoneyUser = moneyUser;
                                        subtractMoneyFreeze = money - moneyUser;
                                    } else {
                                        subtractMoneyUser = moneyUser;
                                        subtractMoneyFreeze = freezeMoney;
                                    }

                                    subtractMoney = subtractMoneyUser + subtractMoneyFreeze;

                                    if (subtractMoney > 0L) {
                                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                        context.beginTransaction();
                                        try {
                                            user.setMoney(moneyType, moneyUser -= subtractMoneyUser);
                                            user.setTotalPnl(moneyType, currentMoney -= subtractMoney);
                                            long fMoney = -1L;
                                            if (subtractMoneyFreeze > 0L) {
                                                freeze.setMoney(freezeMoney -= subtractMoneyFreeze);
                                                fMoney = freezeMoney;
                                            }
                                            int vp = 0;
                                            int moneyVPs = 0;
                                            int vpAddEvent = 0;
                                            if (moneyType.equals("vin") && !bBuyIn) {
                                                List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(),
                                                        (long) user.getMoneyVP() + Math.abs(money), false);
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
                                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event",
                                                            vpEventMessage, (int) 801);
                                                }
                                            }

                                            MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(),
                                                    user.getId(), nickname, gameName, moneyUser, currentMoney, subtractMoney, moneyType,
                                                    fMoney, sessionId, 0L, moneyVPs, vp);
                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, 10);

                                            //todo su lai o day nhe
                                            if ((Games.XOC_DIA.getName().equals(gameName) || Games.BA_CAY.getName().equals(gameName)) && user.isBot()) {
                                                logger.info("XOC_DIA not write bot logs ");
                                            } else {
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                                        VinPlayUtils.getGameID(gameName), currentMoney, -subtractMoney, moneyType,
                                                        GetCardGameDataInfo.getCardGameDataInfo(VinPlayUtils.getGameID(gameName),
                                                                roomId, matchId), 0L, true, user.isBot());
                                                messageLog.setReferralCode(user.getReferralCode());
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                                            }
                                            freezeMap.put(sessionId, freeze);
                                            userMap.put(nickname, user);
                                            context.commitTransaction();
                                            response.setSuccess(true);
                                            response.setErrorCode("0");
                                            response.setCurrentMoney(currentMoney);
                                            response.setSubtractMoney(subtractMoney);
                                            response.setFreezeMoney(freezeMoney);
                                            response.setMoneyUse(moneyUser);
                                        } catch (Exception e) {
                                            context.rollbackTransaction();
                                            MoneyLogger.log(nickname, gameName, -money, 0L, moneyType,
                                                    "Tru tien", "1031", "error rmq: " + e.getMessage());
                                            response.setErrorCode("1031");
                                        }
                                    } else {
                                        response.setErrorCode("1002");
                                        MoneyLogger.log(nickname, gameName, -money, 0L, moneyType,
                                                "Tru tien", "1002", "Khong du tien");
                                    }
                                } else {
                                    response.setErrorCode("1002");
                                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1002", "Khong du tien");
                                }

                            } else {
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            }
                            break block32;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien",
                                    "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block32;
                        } finally {
                            freezeMap.unlock(sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1003",
                            "session not exist");
                    response.setErrorCode("1003");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030",
                            "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        logger.debug("Response subtractMoneyInGame: sessionId: " + sessionId + " " + response.toJson());
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse updateMoneyInGameByFreeze(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, String matchId, long fee) {
        MoneyResponse response;
        block20:
        {
            logger.debug((Object) ("Request updateMoneyInGameByFreeze: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", matchId: " + matchId + ", money: " + money + ", moneyType: " + moneyType + ", fee: " + fee));
            boolean bBuyIn = this.getGameType(gameName);
            response = new MoneyResponse(false, "1001");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        try {
                            freezeMap.lock((Object) sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                            long freezeMoney = freeze.getMoney();
                            response.setFreezeMoney(freezeMoney);
                            if (money != 0L) {
                                if (freezeMoney + money >= 0L) {
                                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                    context.beginTransaction();
                                    try {
                                        user.setTotalPnl(moneyType, currentMoney += money);
                                        freeze.setMoney(freezeMoney += money);
                                        int vp = 0;
                                        int moneyVPs = 0;
                                        int vpAddEvent = 0;
                                        if (moneyType.equals("vin") && !bBuyIn) {
                                            List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(), (long) user.getMoneyVP() + Math.abs(money), false);
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
                                                MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage, (int) 801);
                                            }
                                        }
                                        MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, money, moneyType, freezeMoney, sessionId, fee, moneyVPs, vp);
                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 10);
                                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                                VinPlayUtils.getGameID(gameName), currentMoney, money, moneyType,
                                                GetCardGameDataInfo.getCardGameDataInfo(
                                                        VinPlayUtils.getGameID(gameName), roomId, matchId), fee, true, user.isBot());
                                        messageLog.setReferralCode(user.getReferralCode());
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                                        freezeMap.put((Object) sessionId, (Object) freeze);
                                        userMap.put(nickname, user);
                                        context.commitTransaction();
                                        response.setSuccess(true);
                                        response.setErrorCode("0");
                                        response.setCurrentMoney(currentMoney);
                                        response.setFreezeMoney(freezeMoney);
                                        response.setMoneyUse(moneyUser);
                                        if (money < 0L) {
                                            response.setSubtractMoney(Math.abs(money));
                                        }
                                        break block20;
                                    } catch (Exception e) {
                                        context.rollbackTransaction();
                                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1031", "error rmq: " + e.getMessage());
                                        response.setErrorCode("1031");
                                    }
                                    break block20;
                                }
                                response.setErrorCode("1002");
                                MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1002", "Khong du tien");
                                break block20;
                            }
                            response.setSuccess(true);
                            response.setErrorCode("0");
                            break block20;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block20;
                        } finally {
                            freezeMap.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1003", "session not exist");
                    response.setErrorCode("1003");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, money, fee, moneyType, "Cong tru tien dong bang", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        logger.debug((Object) ("Response updateMoneyInGameByFreeze: sessionId: " + sessionId + " " + response.toJson()));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse subtractMoneyInGameExactly(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, String matchId) {
        MoneyResponse response;
        block34:
        {
//            CommonHandle.writeInfoLog(("Request subtractMoneyInGameExactly: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", matchId: " + matchId + ", money: " + money + ", moneyType: " + moneyType));
            response = new MoneyResponse(false, "1001");
            boolean bBuyIn = this.getGameType(gameName);
            HazelcastInstance client = HazelcastClientFactory.getInstance();

            if (client == null) {
                MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }

            IMap<String, UserModel> userMap = client.getMap("users");

            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    long subtractMoney = 0L;
                    long subtractMoneyUser = 0L;
                    long subtractMoneyFreeze = 0L;
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        try {
                            freezeMap.lock((Object) sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                            long freezeMoney = freeze.getMoney();
                            response.setFreezeMoney(freezeMoney);

                            if (money > 0L) {
                                if (moneyUser + freezeMoney >= money) {
                                    if (bBuyIn) {
                                        if (freezeMoney >= money) {
                                            subtractMoneyFreeze = money;
                                        } else if (moneyUser + freezeMoney >= money) {
                                            subtractMoneyFreeze = freezeMoney;
                                            subtractMoneyUser = money - freezeMoney;
                                        } else {
                                            subtractMoneyUser = moneyUser;
                                            subtractMoneyFreeze = freezeMoney;
                                        }
                                    } else if (moneyUser >= money) {
                                        subtractMoneyUser = money;
                                    } else if (moneyUser + freezeMoney >= money) {
                                        subtractMoneyUser = moneyUser;
                                        subtractMoneyFreeze = money - moneyUser;
                                    } else {
                                        subtractMoneyUser = moneyUser;
                                        subtractMoneyFreeze = freezeMoney;
                                    }

                                    subtractMoney = subtractMoneyUser + subtractMoneyFreeze;

                                    if (subtractMoney > 0L) {
                                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                        context.beginTransaction();
                                        try {
                                            user.setMoney(moneyType, moneyUser -= subtractMoneyUser);
                                            user.setTotalPnl(moneyType, currentMoney -= subtractMoney);
                                            long fMoney = -1L;
                                            if (subtractMoneyFreeze > 0L) {
                                                freeze.setMoney(freezeMoney -= subtractMoneyFreeze);
                                                fMoney = freezeMoney;
                                            }
                                            int vp = 0;
                                            int moneyVPs = 0;
                                            int vpAddEvent = 0;
                                            if (moneyType.equals("vin") && !bBuyIn) {
                                                List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(), (long) user.getMoneyVP() + Math.abs(money), false);
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
                                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage, (int) 801);
                                                }
                                            }
                                            MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, subtractMoney, moneyType, fMoney, sessionId, 0L, moneyVPs, vp);
                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, 10);
                                            if (Games.XOC_DIA.getName().equals(gameName) && user.isBot()) {
                                                CommonHandle.writeInfoLog("XOC_DIA not write bot logs ");
                                            } else {
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName, VinPlayUtils.getServiceName((String) gameName), currentMoney, -subtractMoney, moneyType, "Ph\u00f2ng: " + roomId + ", B\u00e0n: " + matchId, 0L, true, user.isBot());
                                                messageLog.setReferralCode(user.getReferralCode());
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                                            }
                                            freezeMap.put(sessionId, freeze);
                                            userMap.put(nickname, user);
                                            context.commitTransaction();
                                            response.setSuccess(true);
                                            response.setErrorCode("0");
                                            response.setCurrentMoney(currentMoney);
                                            response.setSubtractMoney(subtractMoney);
                                            response.setFreezeMoney(freezeMoney);
                                            response.setMoneyUse(moneyUser);
                                        } catch (Exception e) {
                                            context.rollbackTransaction();
                                            MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1031", "error rmq: " + e.getMessage());
                                            response.setErrorCode("1031");
                                        }
                                    } else {
                                        response.setErrorCode("1002");
                                        MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1002", "Khong du tien");
                                    }
                                } else {
                                    response.setErrorCode("1002");
                                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1002", "Khong du tien");
                                }
                            } else {
                                response.setSuccess(true);
                                response.setErrorCode("0");
                            }
                            break block34;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block34;
                        } finally {
                            freezeMap.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1003", "session not exist");
                    response.setErrorCode("1003");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        logger.debug((Object) ("Response subtractMoneyInGameExactly: sessionId: " + sessionId + " " + response.toJson()));
        return response;
    }

    @Override
    public List<FreezeModel> getListFreeze(String gamename, String nickname, String moneyType, String startTime, String endTime, int page) throws ParseException {
        int numStart = (page - 1) * 50;
        int numEnd = numStart + 50;
        ArrayList<FreezeModel> res = new ArrayList<FreezeModel>();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, FreezeModel> freezeMap = client.getMap("freeze");
        long start = 0L;
        long end = 0L;
        if (!startTime.isEmpty()) {
            start = VinPlayUtils.getDateTime((String) startTime).getTime();
        }
        if (!endTime.isEmpty()) {
            end = VinPlayUtils.getDateTime((String) endTime).getTime();
        }
        int i = 0;
        List<String> sessionList = new ArrayList();
        if (gamename.isEmpty() || gamename.equals("XocDia")) {
            try {
                XocDiaServiceImpl xdSer = new XocDiaServiceImpl();
                sessionList = xdSer.getListSessionActive();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry entry : freezeMap.entrySet()) {
            FreezeModel model = (FreezeModel) entry.getValue();
            if (sessionList.contains(model.getSessionId()) || !gamename.isEmpty() && !model.getGameName().equals(gamename) || !nickname.isEmpty() && !model.getNickname().equals(nickname) || !moneyType.isEmpty() && !model.getMoneyType().equals(moneyType) || (start != 0L || end != 0L) && (model.getCreateTime().getTime() < start || model.getCreateTime().getTime() > end))
                continue;
            if (numStart < 0 || i >= numStart && i <= numEnd) {
                res.add(model);
            }
            ++i;
        }
        return res;
    }

    @Override
    public boolean restoreFreeze(String nickname, String gamename, String startTime, String endTime) throws ParseException {
        if (nickname == null || gamename == null || startTime == null || endTime == null || nickname.isEmpty() || gamename.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            return false;
        }
        if (nickname.equals("*")) {
            nickname = "";
        }
        if (gamename.equals("*")) {
            return false;
        }
        if (startTime.equals("*")) {
            startTime = "";
        }
        if (endTime.equals("*")) {
            endTime = "";
        }
        List<FreezeModel> list = this.getListFreeze(gamename, nickname, "", startTime, endTime, 0);
        for (FreezeModel model : list) {
            this.restoreFreeze(model.getSessionId());
        }
        return true;
    }

    @Override
    public boolean restoreFreeze(String sessionId) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap freezeMap = client.getMap("freeze");
        if (freezeMap.containsKey((Object) sessionId)) {
            FreezeModel model = (FreezeModel) freezeMap.get((Object) sessionId);
            this.restoreMoneyInGame(sessionId, model.getNickname(), model.getGameName(), "", model.getMoneyType());
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void addVippoint(String nickname, long money, String moneyType) {
        HazelcastInstance client;
        IMap userMap;
        logger.debug((Object) ("Request addVippoint: nickname: " + nickname + ", money: " + money + ", moneyType: " + moneyType));
        if (moneyType.equals("vin") && (userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey((Object) nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                int vp = 0;
                int moneyVPs = 0;
                int vpAddEvent = 0;
                List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(), (long) user.getMoneyVP() + Math.abs(money), false);
                vp = vpLst.get(0);
                moneyVPs = vpLst.get(1);
                vpAddEvent = vpLst.get(2);
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
                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage, (int) 801);
                }
                user.setVippoint(user.getVippoint() + vp);
                user.setVippointSave(user.getVippointSave() + vp);
                user.setMoneyVP(moneyVPs);
                MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
                if (dao.updateVippoint(nickname, vp, moneyVPs)) {
                    userMap.put(nickname, user);
                }
            } catch (Exception e) {
                logger.debug((Object) e);
            } finally {
                userMap.unlock(nickname);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse addFreezeMoneyInGame(String sessionId, String nickname, String gameName, String roomId, String matchId, long addFreezeMoney, String moneyType, FreezeInGame type) {
        MoneyResponse response;
        block28:
        {
            logger.debug((Object) ("Request addFreezeMoneyInGame: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", matchId: " + matchId + ", addFreezeMoney: " + addFreezeMoney + ", moneyType: " + moneyType + ", type: " + type.getId()));
            response = new MoneyResponse(false, "1001");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1030", "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey((Object) nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    IMap freezeMap = client.getMap("freeze");
                    if (freezeMap.containsKey((Object) sessionId)) {
                        try {
                            freezeMap.lock((Object) sessionId);
                            FreezeModel freeze = (FreezeModel) freezeMap.get((Object) sessionId);
                            long freezeMoney = freeze.getMoney();
                            response.setFreezeMoney(freezeMoney);
                            if (addFreezeMoney > 0L) {
                                long moneyUserExchange = 0L;
                                long moneyFreezeExchange = 0L;
                                boolean next = false;
                                if (type == FreezeInGame.MORE) {
                                    if (moneyUser >= addFreezeMoney) {
                                        moneyUserExchange = -addFreezeMoney;
                                        next = true;
                                    }
                                } else if (type == FreezeInGame.SET) {
                                    if (moneyUser + freezeMoney >= addFreezeMoney) {
                                        moneyUserExchange = freezeMoney - addFreezeMoney;
                                        next = true;
                                    }
                                } else if (type == FreezeInGame.ALL_MIN) {
                                    if (moneyUser + freezeMoney >= addFreezeMoney) {
                                        moneyUserExchange = -moneyUser;
                                        next = true;
                                    }
                                } else if (type == FreezeInGame.ALL) {
                                    moneyUserExchange = -moneyUser;
                                    next = true;
                                }
                                moneyFreezeExchange = -moneyUserExchange;
                                if (next) {
                                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                    context.beginTransaction();
                                    try {
                                        user.setMoney(moneyType, moneyUser += moneyUserExchange);
                                        freeze.setMoney(freezeMoney += moneyFreezeExchange);
                                        MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, 0L, moneyType, freezeMoney, sessionId, 0L, user.getMoneyVP(), 0);
                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 10);
                                        freezeMap.put((Object) sessionId, (Object) freeze);
                                        userMap.put(nickname, user);
                                        context.commitTransaction();
                                        response.setSuccess(true);
                                        response.setErrorCode("0");
                                        response.setCurrentMoney(currentMoney);
                                        response.setFreezeMoney(freezeMoney);
                                        response.setMoneyUse(moneyUser);
                                    } catch (Exception e) {
                                        context.rollbackTransaction();
                                        MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1031", "error rmq: " + e.getMessage());
                                        response.setErrorCode("1031");
                                    }
                                } else {
                                    response.setErrorCode("1002");
                                    MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1002", "Khong du tien");
                                }
                            }
                            break block28;
                        } catch (Exception e2) {
                            MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1030", "error hazelcast: " + e2.getMessage());
                            response.setErrorCode("1030");
                            break block28;
                        } finally {
                            freezeMap.unlock((Object) sessionId);
                        }
                    }
                    response.setErrorCode("1003");
                    MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1003", "session not exist");
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, addFreezeMoney, 0L, moneyType, "Dong bang them tien", "1030", "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            }
        }
        logger.debug((Object) ("Response addFreezeMoneyInGame:  sessionId: " + sessionId + " " + response.toJson()));
        return response;
    }

    private boolean getGameType(String gameName) {
        boolean bBuyIn = false;
        if (gameName.equals("Poker") || gameName.equals("Lieng") || gameName.equals("XiTo") || gameName.equals("XocDia")) {
            bBuyIn = true;
        }
        return bBuyIn;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public MoneyResponse updateMoneyUser(String nickname, long money, String moneyType, String gameName, String serviceName, String description, long fee, Long transId, TransType type, boolean playGame) {
        logger.debug((Object) ("Request updateMoneyUser:  nickname: " + nickname + ", money: " + money + ", moneyType: " + moneyType + ", gameName: " + gameName + ", serviceName: " + serviceName + ", description: " + description + ", fee: " + fee + ", transId: " + transId + ", TransType: " + type.getId()));
        MoneyResponse response = new MoneyResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030", "can not connect hazelcast");
            response.setErrorCode("1030");
            return response;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey((Object) nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel) userMap.get((Object) nickname);
                long moneyUser = user.getMoney(moneyType);
                long currentMoney = user.getTotalPnl(moneyType);
                if (money != 0L) {
                    if (moneyUser + money >= 0L) {
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
                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage, (int) 801);
                                }
                            }
                            MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, money, moneyType, fee, moneyVPs, vp);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName, serviceName, currentMoney, money, moneyType, description, fee, playGame, user.isBot());
                            messageLog.setReferralCode(user.getReferralCode());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                            userMap.put(nickname, user);
                            context.commitTransaction();
                            response.setSuccess(true);
                            response.setErrorCode("0");
                        } catch (Exception e) {
                            context.rollbackTransaction();
                            MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1031", "error rmq: " + e.getMessage());
                            response.setErrorCode("1031");
                        }
                    } else {
                        response.setErrorCode("1002");
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1002", "khong du tien");
                        response.setErrorCode("1002");
                    }
                } else {
                    if (moneyType.equals("vin") && transId != null && type.getId() == TransType.END_TRANS.getId()) {
                        IMap vpCache = client.getMap("VPMinigame");
                        String vpCacheId = nickname + gameName + transId;
                        long moneyVP2 = 0L;
                        if (vpCache.containsKey((Object) vpCacheId)) {
                            moneyVP2 = Math.abs((Long) vpCache.get((Object) vpCacheId));
                            vpCache.remove((Object) vpCacheId);
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
                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage) vpEventMessage2, (int) 801);
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
                response.setCurrentMoney(currentMoney);
            } catch (Exception e2) {
                logger.debug((Object) e2);
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030", "error hazelcast: " + e2.getMessage());
                response.setErrorCode("1030");
            } finally {
                userMap.unlock(nickname);
            }
        }
        logger.debug((Object) ("Response updateMoneyUser:" + response.toJson()));
        return response;
    }

    @Override
    public MoneyResponse updateMoneyGame3rdUser(String nickname, long money, String moneyType, String gameName,
                                                String serviceName, String description, long fee, boolean playGame) {
        logger.debug("Request updateMoneyUser:  nickname: " + nickname + ", money: " + money + ", moneyType: "
                + moneyType + ", gameName: " + gameName + ", serviceName: " + serviceName + ", description: "
                + description + ", fee: " + fee);
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
                long moneyUser = user.getMoney(moneyType);
                long currentMoney = user.getTotalPnl(moneyType);
                if (money != 0L) {
                    if (moneyUser + money >= 0L) {
                        TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                        context.beginTransaction();
                        try {
                            user.setMoney(moneyType, moneyUser += money);
                            user.setTotalPnl(moneyType, currentMoney += money);
                            int vp = 0;
                            int moneyVPs = 0;
                            MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(),
                                    user.getId(), nickname, gameName, moneyUser, currentMoney, money, moneyType, fee, moneyVPs, vp);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                    serviceName, currentMoney, money, moneyType, description, fee, playGame, user.isBot());
                            messageLog.setReferralCode(user.getReferralCode());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, 16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                            userMap.put(nickname, user);
                            context.commitTransaction();
                            response.setSuccess(true);
                            response.setErrorCode("0");
                        } catch (Exception e) {
                            context.rollbackTransaction();
                            MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1031", "error rmq: " + e.getMessage());
                            response.setErrorCode("1031");
                        }
                    } else {
                        MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1002", "khong du tien");
                        response.setErrorCode("1002");
                    }
                }
                response.setCurrentMoney(currentMoney);
            } catch (Exception e2) {
                logger.info(e2);
                MoneyLogger.log(nickname, gameName, money, fee, moneyType, serviceName, "1030", "error hazelcast: " + e2.getMessage());
                response.setErrorCode("1030");
            } finally {
                userMap.unlock(nickname);
            }
        }
        logger.debug(("Response updateMoneyUser:" + response.toJson()));
        return response;
    }

    @Override
    public MoneyResponse subtractMoneyInGameXocDia(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, String matchId, boolean isBot) {
        MoneyResponse response;
        block32:
        {
            /*Debug.trace("Request subtractMoneyInGame 1: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName
                    + ", roomId: " + roomId + ", matchId: " + matchId + ", money: " + money + ", moneyType: " + moneyType);*/
            response = new MoneyResponse(false, "1001");
            boolean bBuyIn = this.getGameType(gameName);
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030",
                        "can not connect hazelcast");
                response.setErrorCode("1030");
                return response;
            }

            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey(nickname)) {
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                    long moneyUser = user.getMoney(moneyType);
                    long currentMoney = user.getTotalPnl(moneyType);
                    long subtractMoney = 0L;
                    long subtractMoneyUser = 0L;
                    long subtractMoneyFreeze = 0L;
                    response.setMoneyUse(moneyUser);
                    response.setCurrentMoney(currentMoney);
                    try {
                        long freezeMoney = 0;
                        response.setFreezeMoney(0);

                        if (money > 0L) {
                            if (isBot && moneyUser + freezeMoney < money) {
                                moneyUser = 5000000L;
                            }
                            if (moneyUser + freezeMoney >= money) {
                                if (bBuyIn) {
                                    if (freezeMoney >= money) {
                                        subtractMoneyFreeze = money;
                                    } else if (moneyUser + freezeMoney >= money) {
                                        subtractMoneyFreeze = freezeMoney;
                                        subtractMoneyUser = money - freezeMoney;
                                    } else {
                                        subtractMoneyUser = moneyUser;
                                        subtractMoneyFreeze = freezeMoney;
                                    }
                                } else if (moneyUser >= money) {
                                    subtractMoneyUser = money;
                                } else if (moneyUser + freezeMoney >= money) {
                                    subtractMoneyUser = moneyUser;
                                    subtractMoneyFreeze = money - moneyUser;
                                } else {
                                    subtractMoneyUser = moneyUser;
                                    subtractMoneyFreeze = freezeMoney;
                                }

                                subtractMoney = subtractMoneyUser + subtractMoneyFreeze;

                                if (subtractMoney > 0L) {
                                    TransactionContext context = client.newTransactionContext(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.ONE_PHASE));
                                    context.beginTransaction();
                                    try {
                                        user.setMoney(moneyType, moneyUser -= subtractMoneyUser);
                                        user.setTotalPnl(moneyType, currentMoney -= subtractMoney);
                                        long fMoney = -1L;
                                        if (subtractMoneyFreeze > 0L) {
                                            fMoney = freezeMoney;
                                        }
                                        int vp = 0;
                                        int moneyVPs = 0;
                                        int vpAddEvent = 0;
//                                            if (moneyType.equals("vin") && !bBuyIn) {
//                                                List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(),
//                                                        (long)user.getMoneyVP() + Math.abs(money), false);
//                                                vp = vpLst.get(0);
//                                                moneyVPs = vpLst.get(1);
//                                                vpAddEvent = vpLst.get(2);
//                                                user.setVippoint(user.getVippoint() + vp);
//                                                user.setVippointSave(user.getVippointSave() + vp);
//                                                user.setMoneyVP(moneyVPs);
//                                                if (vpAddEvent > 0) {
//                                                    int vpReal = user.getVpEventReal();
//                                                    int vpEvent = user.getVpEvent();
//                                                    int place = VippointUtils.calculatePlace(vpEvent += vpAddEvent);
//                                                    int placeMax = place > user.getPlace() ? place : user.getPlace();
//                                                    user.setVpEventReal(vpReal += vpAddEvent);
//                                                    user.setVpEvent(vpEvent);
//                                                    user.setPlace(place);
//                                                    user.setPlaceMax(placeMax);
//                                                    VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(), nickname, vpReal, vpEvent, 0, 0, 0, 0, place, placeMax, 0, 0);
//                                                    CompletableFuture.runAsync(() -> {
//                                                        try {
//                                                            RMQApi.publishMessage((String)"queue_vippoint_event",
//                                                                    vpEventMessage, (int)801);
//                                                        } catch (Exception e) {
//                                                            logger.error("Error publishMessage queue_vippoint_event: " + e.getMessage(), (Throwable)e);
//                                                        }
//                                                    });
//                                                }
//                                            }

                                        MoneyMessageInGame message = new MoneyMessageInGame(VinPlayUtils.genMessageId(),
                                                user.getId(), nickname, gameName, moneyUser, currentMoney, subtractMoney, moneyType,
                                                fMoney, sessionId, 0L, moneyVPs, vp);
                                        CompletableFuture.runAsync(() -> {
                                            try {
                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", message, 10);
                                            } catch (Exception e) {
                                                logger.error("Error publishMessagePayment: " + e.getMessage(), (Throwable) e);
                                            }
                                        });

//                                            //todo su lai o day nhe
                                        if ((Games.XOC_DIA.getName().equals(gameName) || Games.BA_CAY.getName().equals(gameName)) && user.isBot()) {
                                            logger.info("XOC_DIA not write bot logs ");
                                        } else {
                                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName,
                                                    VinPlayUtils.getGameID(gameName), currentMoney, -subtractMoney, moneyType,
                                                    GetCardGameDataInfo.getCardGameDataInfo(VinPlayUtils.getGameID(gameName),
                                                            roomId, matchId), 0L, true, user.isBot());
                                            messageLog.setReferralCode(user.getReferralCode());
                                            CompletableFuture.runAsync(() -> {
                                                try {
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                                                } catch (Exception e) {
                                                    logger.error("Error publishMessageLogMoney: " + e.getMessage(), (Throwable) e);
                                                }
                                            });
                                        }
                                        userMap.put(nickname, user);
                                        context.commitTransaction();
                                        response.setSuccess(true);
                                        response.setErrorCode("0");
                                        response.setCurrentMoney(currentMoney);
                                        response.setSubtractMoney(subtractMoney);
                                        response.setFreezeMoney(0);
                                        response.setMoneyUse(moneyUser);
                                    } catch (Exception e) {
                                        context.rollbackTransaction();
                                        MoneyLogger.log(nickname, gameName, -money, 0L, moneyType,
                                                "Tru tien", "1031", "error rmq: " + e.getMessage());
                                        response.setErrorCode("1031");
                                    }
                                } else {
                                    response.setErrorCode("1002");
                                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType,
                                            "Tru tien", "1002", "Khong du tien");
                                }
                            } else {
                                response.setErrorCode("1002");
                                MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1002", "Khong du tien");
                            }

                        } else {
                            response.setSuccess(true);
                            response.setErrorCode("0");
                        }
                        break block32;
                    } catch (Exception e2) {
                        MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien",
                                "1030", "error hazelcast: " + e2.getMessage());
                        response.setErrorCode("1030");
                        break block32;
                    }
                } catch (Exception e3) {
                    MoneyLogger.log(nickname, gameName, -money, 0L, moneyType, "Tru tien", "1030",
                            "error hazelcast: " + e3.getMessage());
                    response.setErrorCode("1030");
                } finally {
                    userMap.unlock(nickname);
                }
            } else {
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        }
        logger.debug("Response subtractMoneyInGame: sessionId: " + sessionId + " " + response.toJson());
        return response;
    }
}

