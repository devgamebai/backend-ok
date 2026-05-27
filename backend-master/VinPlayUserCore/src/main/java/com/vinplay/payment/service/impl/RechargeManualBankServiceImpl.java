/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.http.util.TextUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.payment.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.RechargeManualBankService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.usercore.entities.AgentBank;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.impl.AgentBankServiceImpl;
import com.vinplay.utils.Utils;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;

public class RechargeManualBankServiceImpl
implements RechargeManualBankService {
    private static final Logger logger = Logger.getLogger(RechargeManualBankServiceImpl.class);
    private static final String PAYMENTNAME = "manualbank";

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
    }

    @Override
    public RechargePaywellResponse create(String userId, String username, String nickname, String bankName, String bankCode, String bankNumber, String bankAccount, String agentCode, String agentBankNumber, long amount, String cartId) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)nickname) || amount <= 0L || StringUtils.isBlank((CharSequence)bankCode)) {
                return res;
            }
            PaymentConfig paymentConfig = this.getPaymentConfig();
            if (amount < (long)(paymentConfig == null ? 1000 : paymentConfig.getConfig().getMinMoney())) {
                res.setData("Money recharge less than minimum");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            if (rechargeDao.CheckPending(nickname, PAYMENTNAME)) {
                res.setCode(8);
                res.setData("Please wait for last request is completed.");
                return res;
            }
            AgentBankServiceImpl agentBankService = new AgentBankServiceImpl();
            AgentBank agentBank = agentBankService.getByBankNumber(agentBankNumber);
            if (agentBank == null) {
                res.setCode(21);
                res.setData("Bank number of agent is invalid");
                return res;
            }
            if (!agentCode.equals(agentBank.getAgent_code())) {
                res.setCode(22);
                res.setData("Code of agent is not match bank number");
                return res;
            }
            AgentDAOImpl agentDao = new AgentDAOImpl();
            UserAgentModel userAgentModel = new UserAgentModel();
            userAgentModel = agentDao.DetailUserAgentByCode(agentCode);
            if (userAgentModel == null) {
                res.setCode(22);
                res.setData("Code of agent is invalid");
                return res;
            }
            DepositPaygateModel modelCheck = new DepositPaygateModel();
            modelCheck = rechargeDao.GetByReferenceId(cartId);
            if (modelCheck != null) {
                res.setCode(23);
                res.setData("Transaction code is exist");
                return res;
            }
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = amount;
            model.BankAccountName = bankAccount;
            model.BankAccountNumber = bankNumber;
            model.BankName = bankName;
            model.BankCode = bankCode;
            model.CartId = cartId;
            model.CreatedAt = "";
            model.Description = "";
            model.IsDeleted = false;
            model.PaymentType = "IB_OFFLINE";
            model.MerchantCode = agentCode;
            model.ProviderName = PAYMENTNAME;
            model.ModifiedAt = "";
            model.UserId = userId;
            model.Username = username;
            model.Nickname = nickname;
            model.ReferenceId = cartId;
            model.RequestTime = VinPlayUtils.getCurrentDateTime();
            model.Status = PayCommon.PAYSTATUS.PENDING.getId();
            model.UserApprove = "";
            model.AgentBankAccountName = agentBank.getBank_acount();
            model.AgentBankAccountNumber = agentBank.getBank_number();
            model.AgentBankCode = agentBank.getBank_code();
            model.Content = "Recharge " + amount + " for nickname: " + nickname + " from: " + bankNumber + " to: " + agentBank.getBank_number() + " with TransactionId: " + cartId;
            long id = rechargeDao.Add(model);
            if (id == 0L) {
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            logger.error(("create " + e.getMessage()));
            return null;
        }
    }

    @Override
    public RechargePaywellResponse topupByCash(String userId, String username, String nickname, String agentCode, long amount, long amountFee, String note) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)nickname) || amount <= 0L) {
                return res;
            }
            AgentDAOImpl agentDao = new AgentDAOImpl();
            UserAgentModel userAgentModel = new UserAgentModel();
            userAgentModel = agentDao.DetailUserAgentByCode(agentCode);
            if (userAgentModel == null) {
                res.setCode(22);
                res.setData("Code of agent is invalid");
                return res;
            }
            if (username.equals(userAgentModel.getUsername())) {
                res.setCode(22);
                res.setData("You can not topup money for owner");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                res.setCode(Integer.valueOf("99"));
                res.setData("Can not connect cached svr");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = amount + amountFee;
            model.AmountFee = amountFee;
            model.BankAccountName = "CASH";
            model.BankAccountNumber = "CASH";
            model.BankCode = "CASH";
            model.CartId = String.valueOf(VinPlayUtils.generateTransId());
            model.CreatedAt = VinPlayUtils.getCurrentDateTime();
            model.IsDeleted = false;
            model.PaymentType = "IB_OFFLINE";
            model.MerchantCode = agentCode;
            model.ProviderName = PAYMENTNAME;
            model.ModifiedAt = VinPlayUtils.getCurrentDateTime();
            model.UserId = userId;
            model.Username = username;
            model.Nickname = nickname;
            model.ReferenceId = model.CartId;
            model.RequestTime = VinPlayUtils.getCurrentDateTime();
            model.Status = PayCommon.PAYSTATUS.COMPLETED.getId();
            model.UserApprove = userAgentModel.getNickname();
            model.AgentBankAccountName = "CASH";
            model.AgentBankAccountNumber = "CASH";
            model.AgentBankCode = "CASH";
            model.Content = note;
            model.Description = "Recharge " + amount + " for nickname: " + nickname + " via CASH by agent: " + userAgentModel.getNickname();
            long id = rechargeDao.topupByCash(model);
            if (id == 0L) {
                return res;
            }
            res = this.discountMoney(model);
            if (res.getCode() != 0) {
                rechargeDao.delete(String.valueOf(id));
                return res;
            }
            model.Amount = amount;
            res = this.addMoney(model, note);
            if (res.getCode() != 0) {
                model = rechargeDao.GetById(String.valueOf(id));
                if (model != null) {
                    rechargeDao.UpdateStatus(String.valueOf(id), PayCommon.PAYSTATUS.FAILED.getId(), "system");
                    this.addMoneyComeBackForAgent(model);
                }
                res.setData("Add point for player failure. Please contact customer care suport for help.");
                return res;
            }
            if (userAgentModel.getLevel() == 2) {
                UserAgentModel userAgentParentModel = new UserAgentModel();
                userAgentParentModel = agentDao.DetailUserAgent(userAgentModel.getParentid());
                model.Amount = amountFee;
                model.AmountFee = 0L;
                model.Nickname = userAgentParentModel.getNickname();
                res = this.addMoneyAgentProfit(model, "Hoa h\u1ed3ng C2");
                if (res.getCode() != 0) {
                    // empty if block
                }
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            logger.error(("topupByCash " + e.getMessage()));
            return null;
        }
    }

    @Override
    public RechargePaywellResponse update(String id, int status, String userApproved) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)id)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("TransactionId can not empty");
                return res;
            }
            if (StringUtils.isBlank((CharSequence)userApproved)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("User approved can not empty");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = new DepositPaygateModel();
            model = rechargeDao.GetById(id);
            if (model == null) {
                res.setCode(23);
                res.setData("Transaction can not found");
                return res;
            }
            if (model.Status != 0 && model.Status != 1) {
                res.setCode(24);
                res.setData("Status is invalid");
                return res;
            }
            String description = "Change status from " + (PayCommon.PAYSTATUS.getById(model.Status)) + " to " + (PayCommon.PAYSTATUS.getById(status)) + " by " + userApproved;
            model.Status = status;
            model.Description = description;
            Boolean rs = rechargeDao.Update(model);
            if (!rs.booleanValue()) {
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            logger.error(("update " + e.getMessage()));
            return null;
        }
    }

    @Override
    public RechargePaywellResponse Approved(String id, String userApproved) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)id)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("TransactionId can not empty");
                return res;
            }
            if (StringUtils.isBlank((CharSequence)userApproved)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("User approved can not empty");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = new DepositPaygateModel();
            model = rechargeDao.GetById(id);
            if (model == null) {
                res.setCode(23);
                res.setData("Transaction can not found");
                return res;
            }
            if (model.Status != 0 && model.Status != 1) {
                res.setCode(24);
                res.setData("Status is invalid");
                return res;
            }
            AgentDAOImpl agentDao = new AgentDAOImpl();
            UserAgentModel userAgentModel = new UserAgentModel();
            userAgentModel = agentDao.DetailUserAgentByCode(model.MerchantCode);
            if (userAgentModel == null) {
                res.setCode(22);
                res.setData("Code of agent is invalid");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                res.setCode(Integer.valueOf("99"));
                res.setData("Can not connect cached svr");
                return res;
            }
            IMap userMap = client.getMap("users");
            if (!userMap.containsKey(userAgentModel.getNickname())) {
                res.setCode(Integer.valueOf("4"));
                res.setData("Agent must be login on lobby before process topup request");
                return res;
            }
            if (!userMap.containsKey(model.Nickname)) {
                res.setCode(Integer.valueOf("4"));
                res.setData("User must be login on lobby before request topup");
                return res;
            }
            int statusBefore = model.Status;
            String descriptionBefore = model.Description;
            String userApprovedBefore = model.UserApprove;
            String description = "Change status from " + (PayCommon.PAYSTATUS.getById(model.Status)) + " to " + (PayCommon.PAYSTATUS.COMPLETED) + " by " + userApproved;
            model.Status = PayCommon.PAYSTATUS.COMPLETED.getId();
            model.Description = description;
            model.UserApprove = userApproved;
            Boolean rs = rechargeDao.Update(model);
            if (!rs.booleanValue()) {
                return res;
            }
            res = this.discountMoney(model);
            if (res.getCode() != 0) {
                model.Status = statusBefore;
                model.Description = descriptionBefore;
                model.UserApprove = userApprovedBefore;
                rechargeDao.Update(model);
                return res;
            }
            res = this.addMoney(model, "");
            if (res.getCode() != 0) {
                res.setData("Add point for player failure. Please contact customer care suport for help.");
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            logger.error(("Approved " + e.getMessage()));
            return null;
        }
    }

    @Override
    public RechargePaywellResponse Reject(String id, String userApproved) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)id)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("TransactionId can not empty");
                return res;
            }
            if (StringUtils.isBlank((CharSequence)userApproved)) {
                res.setCode(Integer.parseInt("5"));
                res.setData("User approved can not empty");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = new DepositPaygateModel();
            model = rechargeDao.GetById(id);
            if (model == null) {
                res.setCode(23);
                res.setData("Transaction can not found");
                return res;
            }
            if (model.Status != 0 && model.Status != 1) {
                res.setCode(24);
                res.setData("Status is invalid");
                return res;
            }
            AgentDAOImpl agentDao = new AgentDAOImpl();
            UserAgentModel userAgentModel = new UserAgentModel();
            userAgentModel = agentDao.DetailUserAgentByCode(model.MerchantCode);
            if (userAgentModel == null) {
                res.setCode(22);
                res.setData("Code of agent is invalid");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                res.setCode(Integer.valueOf("99"));
                res.setData("Can not connect cached svr");
                return res;
            }
            IMap userMap = client.getMap("users");
            if (!userMap.containsKey(userAgentModel.getNickname())) {
                res.setCode(Integer.valueOf("4"));
                res.setData("Agent must be login on lobby before process topup request");
                return res;
            }
            if (!userMap.containsKey(model.Nickname)) {
                res.setCode(Integer.valueOf("4"));
                res.setData("User must be login on lobby before request topup");
                return res;
            }
            String description = "Change status from " + (PayCommon.PAYSTATUS.getById(model.Status)) + " to " + (PayCommon.PAYSTATUS.FAILED) + " by " + userApproved;
            model.Status = PayCommon.PAYSTATUS.FAILED.getId();
            model.Description = description;
            model.UserApprove = userApproved;
            Boolean rs = rechargeDao.Update(model);
            if (!rs.booleanValue()) {
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            logger.error(("Approved " + e.getMessage()));
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse discountMoney(DepositPaygateModel depositPaygateModel) {
        AgentDAOImpl agentDao = new AgentDAOImpl();
        UserAgentModel userAgentModel = new UserAgentModel();
        try {
            userAgentModel = agentDao.DetailUserAgentByCode(depositPaygateModel.MerchantCode);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(userAgentModel.getNickname(), "REQUEST_CASHOUT", depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "Cannot connect hazelcast");
            return res;
        }
        String username = "";
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(userAgentModel.getNickname())) {
            return res;
        }
        try {
            userMap.lock(userAgentModel.getNickname());
            UserCacheModel user = (UserCacheModel)userMap.get(userAgentModel.getNickname());
            username = user.getUsername();
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            int cashoutMoney = user.getCashout();
            if (moneyUser < depositPaygateModel.Amount || currentMoney < depositPaygateModel.Amount) {
                res.setData("Balance of agent is not enough");
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            user.setVin(moneyUser -= depositPaygateModel.Amount);
            user.setVinTotal(currentMoney -= depositPaygateModel.Amount);
            user.setCashout(cashoutMoney += (int)depositPaygateModel.Amount);
            user.setCashoutTime(new Date());
            String desc = "Topup by agent code: " + userAgentModel.getCode();
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), userAgentModel.getNickname(), "REQUEST_CASHOUT", moneyUser, currentMoney, depositPaygateModel.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), userAgentModel.getNickname(), "REQUEST_CASHOUT", "Cashout", currentMoney, depositPaygateModel.Amount * -1L, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(userAgentModel.getNickname(), user);
            res.setCode(0);
            res.setData("");
            res.setCurrentMoney(currentMoney);
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(username, "REQUEST_CASHOUT", depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(userAgentModel.getNickname());
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoney(DepositPaygateModel depositPaygateModel, String note) {
        AgentDAOImpl agentDao = new AgentDAOImpl();
        UserAgentModel userAgentModel = new UserAgentModel();
        try {
            userAgentModel = agentDao.DetailUserAgentByCode(depositPaygateModel.MerchantCode);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(depositPaygateModel.Nickname, PAYMENTNAME, depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(depositPaygateModel.Nickname)) {
            return res;
        }
        try {
            userMap.lock(depositPaygateModel.Nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(depositPaygateModel.Nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += depositPaygateModel.Amount);
            user.setVinTotal(currentMoney += depositPaygateModel.Amount);
            user.setRechargeMoney(rechargeMoney += depositPaygateModel.Amount);
            String desc = "Nh\u1eadn ti\u1ec1n t\u1eeb \u0111\u1ea1i l\u00fd: " + userAgentModel.getNickname() + " " + (TextUtils.isEmpty((CharSequence)note) ? "" : note);
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), depositPaygateModel.Nickname, "RechargeByBank", moneyUser, currentMoney, depositPaygateModel.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), depositPaygateModel.Nickname, "RechargeByBank", PAYMENTNAME, currentMoney, depositPaygateModel.Amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(depositPaygateModel.Nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(depositPaygateModel.Nickname, PAYMENTNAME, depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositPaygateModel.Nickname);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public RechargePaywellResponse addMoneyAgentProfit(DepositPaygateModel depositPaygateModel, String note) {
        AgentDAOImpl agentDao = new AgentDAOImpl();
        UserAgentModel userAgentModel = new UserAgentModel();
        try {
            userAgentModel = agentDao.DetailUserAgentByCode(depositPaygateModel.MerchantCode);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(depositPaygateModel.Nickname, PAYMENTNAME, depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(depositPaygateModel.Nickname)) {
            try {
                UserCacheModel userSend2 = Utils.genUserCacheModel(depositPaygateModel.Nickname);
                if (userSend2 == null) {
                    MoneyLogger.log(depositPaygateModel.Nickname, "Chuy\u1ec3n Kho\u1ea3n hoa h\u1ed3ng C2", depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "L\u1ed7i kh\u00f4ng t\u1ea1o UserCacheModel");
                    return res;
                }
                userMap.put(depositPaygateModel.Nickname, userSend2);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            userMap.lock(depositPaygateModel.Nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(depositPaygateModel.Nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += depositPaygateModel.Amount);
            user.setVinTotal(currentMoney += depositPaygateModel.Amount);
            user.setRechargeMoney(rechargeMoney += depositPaygateModel.Amount);
            String desc = "Nh\u1eadn ti\u1ec1n hoa h\u1ed3ng t\u1eeb \u0111\u1ea1i l\u00fd: " + userAgentModel.getNickname() + " " + (TextUtils.isEmpty((CharSequence)note) ? "" : note);
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), depositPaygateModel.Nickname, "RechargeByAgentProfit", moneyUser, currentMoney, depositPaygateModel.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), depositPaygateModel.Nickname, "RechargeByAgentProfit", "Chuy\u1ec3n kho\u1ea3n hoa h\u1ed3ng", currentMoney, depositPaygateModel.Amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(depositPaygateModel.Nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(depositPaygateModel.Nickname, PAYMENTNAME, depositPaygateModel.Amount, 0L, "vin", "Topup for player " + depositPaygateModel.Nickname + " by agent: " + userAgentModel.getNickname(), "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositPaygateModel.Nickname);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoneyComeBackForAgent(DepositPaygateModel model) {
        AgentDAOImpl agentDao = new AgentDAOImpl();
        UserAgentModel userAgentModel = new UserAgentModel();
        try {
            userAgentModel = agentDao.DetailUserAgentByCode(model.MerchantCode);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(userAgentModel.getNickname(), PAYMENTNAME, model.Amount, 0L, "vin", "Add money comeback " + model.Amount + " from transaction(deposit_paygate) id: " + String.valueOf(model.Id) + " nickname " + model.Nickname + " agent code: " + model.MerchantCode, "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(userAgentModel.getNickname())) {
            return res;
        }
        try {
            userMap.lock(userAgentModel.getNickname());
            UserCacheModel user = (UserCacheModel)userMap.get(userAgentModel.getNickname());
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += model.Amount);
            user.setVinTotal(currentMoney += model.Amount);
            user.setRechargeMoney(rechargeMoney += model.Amount);
            String desc = "Add money comeback (CASH) " + userAgentModel.getCode() + " from transaction(deposit_paygate) id: " + String.valueOf(model.Id);
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), userAgentModel.getNickname(), "RechargeByBank", moneyUser, currentMoney, model.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), userAgentModel.getNickname(), "RechargeByBank", PAYMENTNAME, currentMoney, model.Amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(userAgentModel.getNickname(), user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(userAgentModel.getNickname(), PAYMENTNAME, model.Amount, 0L, "vin", "Add money comeback " + model.Amount + " from transaction(deposit_paygate) id: " + String.valueOf(model.Id) + " nickname " + model.Nickname + " agent code: " + model.MerchantCode, "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(userAgentModel.getNickname());
        }
        return res;
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        Map<String, Object> data = new HashMap();
        try {
            RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
            data = dao.FindTransaction(nickname, status, page, maxItem, fromTime, endTime, providerName);
            return data;
        }
        catch (Exception e) {
            logger.error(("FindTransaction " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, String agentCode, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        Map<String, Object> data = new HashMap();
        try {
            RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
            data = dao.FindTransaction(nickname, agentCode, status, page, maxItem, fromTime, endTime, providerName);
            return data;
        }
        catch (Exception e) {
            logger.error(("FindTransaction " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }

    @Override
    public Map<String, Object> FindTransactionUserToAgent(String agentNickName, String userNickName, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        Map<String, Object> data = new HashMap();
        try {
            RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
            data = dao.FindTransactionUserToAgent(agentNickName, userNickName, status, page, maxItem, fromTime, endTime, providerName);
            return data;
        }
        catch (Exception e) {
            logger.error(("FindTransaction " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }

    @Override
    public boolean UpdateTransactionDetail(String agentNickname, String txid, String content, int status) {
        try {
            RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
            return dao.UpdateTransactionDetail(agentNickname, txid, content, status);
        }
        catch (Exception e) {
            logger.error(("FindTransaction " + e.getMessage()));
            return false;
        }
    }
}

