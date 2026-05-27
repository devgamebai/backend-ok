/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.payment.service.impl;

import com.payment.config.PaymentConfigLoad;
import com.payment.core.hook.Param;
import com.payment.dao.HistoryApplyForDao;
import com.payment.dao.HistoryBankDao;
import com.payment.dao.HistoryBankLiveDao;
import com.payment.dao.Impl.HistoryApplyForDaoImpl;
import com.payment.dao.Impl.HistoryBankDaoImpl;
import com.payment.dao.Impl.HistoryBankLiveDaoImpl;
import com.payment.dao.Impl.PaymentStatisticsDaoImpl;
import com.payment.dao.Impl.TopUpDaoImpl;
import com.payment.dao.Impl.TopUpLiveDaoImpl;
import com.payment.dao.TopUpDao;
import com.payment.dao.TopUpLiveDao;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
import com.payment.entities.PaymentSummaryEntity;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.model.Result;
import com.payment.provider.Provider;
import com.payment.provider.banmaytinh.BanMayTinhProvider;
import com.payment.provider.mock.MockProvider;
import com.payment.provider.mpay247.MPay247Provider;
import com.payment.provider.oneVnPay.OneVnPayProvider;
import com.payment.provider.sieutoc.BaseResponse;
import com.payment.provider.sieutoc.SieuTocProvider;
import com.payment.provider.thesieutoc.TheSieuTocProvider;
import com.payment.response.Bank;
import com.payment.response.BankInResult;
import com.payment.response.BankInfo;
import com.payment.response.BankListResult;
import com.payment.response.BankOutResult;
import com.payment.response.CardInResult;
import com.payment.response.CardInfo;
import com.payment.response.CardListResult;
import com.payment.response.HookBankInResult;
import com.payment.response.HookBankOutResult;
import com.payment.response.HookCardInResult;
import com.payment.service.ProviderService;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.liveUser.service.LiveUserDepositService;
import com.vinplay.liveUser.service.impl.LiveUserDepositServiceImpl;
import com.vinplay.payment.dao.WithDrawPaygateDao;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.service.PaymentManualService;
import com.vinplay.payment.service.impl.PaymentManualServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.utils.TelegramAlert;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ProviderServiceImpl
implements ProviderService {
    Map<String, Provider> providers = new HashMap<String, Provider>();
    WithDrawPaygateDao withdrawDao = new WithDrawPaygateDaoImpl();
    LiveUserDepositService liveUserDepositService = new LiveUserDepositServiceImpl();
    UserService userService = new UserServiceImpl();
    HistoryBankDao historyBankDao = new HistoryBankDaoImpl();
    HistoryBankLiveDao historyBankLiveDao = new HistoryBankLiveDaoImpl();
    TopUpDao topUpDao = new TopUpDaoImpl();
    TopUpLiveDao topUpLiveDao = new TopUpLiveDaoImpl();
    HistoryApplyForDao historyApplyForDao = new HistoryApplyForDaoImpl();
    PaymentManualService withdrawManual = new PaymentManualServiceImpl();
    boolean greenMoney = false;
    private static ProviderService _instance;
    private static final ScheduledExecutorService scheduler;
    private static final Random random;
    private static final Logger logger;

    @Override
    public void add(Provider provider) {
        this.providers.put(provider.name(), provider);
    }

    private Provider getProvider(String providerName, ProviderType type) {
        if (providerName.toLowerCase().contains("default")) {
            switch (type) {
                case PROVIDER_BANK: {
                    providerName = PaymentConfigLoad.getPaymentConfig().getDefault_provider_bank();
                    break;
                }
                case PROVIDER_CART: {
                    providerName = PaymentConfigLoad.getPaymentConfig().getDefault_provider_card();
                    break;
                }
                case PROVIDER_BANK_OUT: {
                    providerName = PaymentConfigLoad.getPaymentConfig().getDefault_provider_bank_out();
                    break;
                }
                default: {
                    return null;
                }
            }
        }
        if (this.providers.containsKey(providerName)) {
            return this.providers.get(providerName);
        }
        return null;
    }

    public static ProviderService getInstance() {
        if (_instance == null) {
            _instance = new ProviderServiceImpl();
            _instance.add(new MockProvider());
            _instance.add(new SieuTocProvider());
            _instance.add(new OneVnPayProvider());
            _instance.add(new TheSieuTocProvider());
            _instance.add(new BanMayTinhProvider());
            _instance.add(new MPay247Provider());
        }
        return _instance;
    }

    @Override
    public Result<BankInfo> bankIn(String providerName, UserModel userInfo, String type, int amount) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        BankInResult result = provider.BankIn(userInfo, type, amount);
        if (result.getCode() == Code.ERROR) {
            return Result.error(result.getMsg());
        }
        if (result.getCode() == Code.NOT_SUCCESS) {
            return Result.error(result.getMsg());
        }
        HistoryBankEntity historyBank = result.getHistoryBank();
        if (this.greenMoney) {
            String msg = "N\u1ea1p Ti\u1ec1n Th\u00e0nh C\u00f4ng | " + amount + " | " + type + " | " + result.getHistoryBank().getId();
            boolean ok = this.liveUserDepositService.checkAndCreateDeposit(userInfo.getNickname(), amount, "BankCode", userInfo.getId(), "vin", msg);
            if (ok) {
                historyBank.setNumber(1);
                historyBank.setStatus(2);
            }
        }
        BankInfo bankInfo = result.getBankInfo();
        historyBank.setText(bankInfo.getQr());
        if (!userInfo.isLive()) {
            boolean success = this.historyBankDao.create(historyBank);
            if (!success) {
                result.setHistoryBank(null);
                return Result.error("Create history bank error");
            }
        } else {
            this.historyBankLiveDao.create(historyBank);
            int delaySeconds = 70 + random.nextInt(51);
            String nickname = userInfo.getNickname();
            long finalAmount = (long)amount * 135L / 100L;
            String msg = "N\u1ea1p Ti\u1ec1n Th\u00e0nh C\u00f4ng | " + amount + "| " + historyBank.getType();
            String requestId = historyBank.getRequest_id();
            scheduler.schedule(() -> {
                try {
                    MoneyResponse mr = this.userService.updateMoneyFromAdmin(nickname, finalAmount, "vin", "BankCode", "BankCode", msg, 0L, false);
                    this.historyBankLiveDao.updateStatus(requestId, 2, amount);
                    logger.debug(("Delayed updateMoneyFromAdmin for live user " + nickname + " after " + delaySeconds + "s: " + (mr.isSuccess() ? "Success" : "Failed") + " msg: " + msg + " amount: " + finalAmount));
                }
                catch (Exception e) {
                    logger.error(("Error in delayed updateMoneyFromAdmin for live user " + nickname), (Throwable)e);
                }
            }, (long)delaySeconds, TimeUnit.SECONDS);
            logger.debug(("Scheduled updateMoneyFromAdmin for live user " + nickname + " after " + delaySeconds + "s"));
        }
        return Result.success(bankInfo);
    }

    @Override
    public Result<String> cardIn(String providerName, UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_CART);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        CardInResult result = provider.CardIn(userInfo, code, serial, type, amount);
        logger.debug(("cardIn3: " + (result.getCode())));
        if (result.getCode() == Code.ERROR) {
            return Result.error(result.getMsg());
        }
        if (result.getCode() == Code.NOT_SUCCESS) {
            return Result.error(result.getMsg());
        }
        TopUpEntity topUpEntity = result.getTopUpEntity();
        if (!userInfo.isLive()) {
            boolean success = this.topUpDao.create(topUpEntity);
            logger.debug(("cardIn4: " + success));
            if (!success) {
                return Result.error("Create top up error");
            }
        } else {
            boolean success = this.topUpLiveDao.create(topUpEntity);
            if (!success) {
                return Result.error("Create top up live error");
            }
            int delaySeconds = 70 + random.nextInt(51);
            String nickname = userInfo.getNickname();
            String msg = "N\u1ea1p Ti\u1ec1n Th\u00e0nh C\u00f4ng | " + amount + "| Card";
            String requestId = topUpEntity.getRequest_id();
            scheduler.schedule(() -> {
                try {
                    MoneyResponse mr = this.userService.updateMoneyFromAdmin(nickname, (long)((double)amount * 0.8), "vin", "TopupCard", "TopupCard", msg, 0L, false);
                    this.topUpLiveDao.updateStatus(requestId, 2, msg);
                    logger.debug(("Delayed updateMoneyFromAdmin for live user " + nickname + " after " + delaySeconds + "s: " + (mr.isSuccess() ? "Success" : "Failed") + " msg: " + msg + " amount: " + amount));
                }
                catch (Exception e) {
                    logger.error(("Error in delayed updateMoneyFromAdmin for live user " + nickname), (Throwable)e);
                }
            }, (long)delaySeconds, TimeUnit.SECONDS);
        }
        return Result.success(result.toResult());
    }

    @Override
    public synchronized Result<String> bankOut(String providerName, UserModel userInfo, String requestId, String nickName, String admin, String ip) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK_OUT);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        WithDrawPaygateModel withDrawPaygateModel = this.withdrawDao.GetById(requestId);
        if (withDrawPaygateModel == null) {
            return Result.error("Not found bank out");
        }
        if (!nickName.contains(withDrawPaygateModel.Nickname)) {
            return Result.error("User not match");
        }
        if (withDrawPaygateModel.Status != PayCommon.PAYSTATUS.REQUEST.getId()) {
            return Result.error("Can not request bank out");
        }
        if (withDrawPaygateModel.Amount < 200000L) {
            return Result.error("S\u1ed1 ti\u1ec1n r\u00fat ph\u1ea3i l\u1edbn h\u01a1n 200.000");
        }
        BankOutResult result = provider.BankOut(withDrawPaygateModel);
        if (result.getCode() == Code.ERROR) {
            return Result.error("Bank out error");
        }
        if (result.getCode() == Code.NOT_SUCCESS) {
            return Result.error(result.getMsg());
        }
        RechargePaywellResponse res = this.withdrawManual.withdrawal(withDrawPaygateModel.Id, admin, ip, PayCommon.PAYSTATUS.PENDING, provider.name());
        if (res.getCode() != 0) {
            return Result.error("Not approved Order Out");
        }
        HistoryApplyForEntity historyApplyFor = result.getHistoryApplyFor();
        historyApplyFor.setFid(withDrawPaygateModel.UserId);
        historyApplyFor.setText(requestId);
        boolean success = this.historyApplyForDao.create(historyApplyFor);
        if (!success) {
            return Result.error("Update status error");
        }
        return Result.success("Th\u00e0nh c\u00f4ng");
    }

    @Override
    public Result<List<Bank>> bankList(String providerName) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK);
        if (provider == null) {
            return Result.error("ProviderName not found " + providerName);
        }
        BankListResult result = provider.BankList();
        if (result.getCode() == Code.ERROR) {
            return Result.error("Bank list error");
        }
        return Result.success(result.getBanks());
    }

    @Override
    public Result<List<CardInfo>> cardList(String providerName) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        CardListResult result = provider.CardList();
        if (result.getCode() == Code.ERROR) {
            return Result.error("Card list error");
        }
        return Result.success(result.getCards());
    }

    @Override
    public Result<String> hookBankIn(String providerName, Param<HttpServletRequest> param) {
        logger.debug(("hookBankIn: " + providerName));
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        HookBankInResult result = provider.hookBankIn(param);
        if (result.getCode() == Code.ERROR) {
            return Result.error((String)result.getData());
        }
        try {
            HistoryBankEntity historyBank = this.historyBankDao.getByRequestId(result.getRequestId());
            if (historyBank == null) {
                return Result.error(provider, "Not found history bank");
            }
            logger.debug(("hookBankIn5: " + historyBank.getStatus()));
            if (historyBank.getStatus() != 1) {
                return Result.error(provider, "History bank status has finish");
            }
            String msg = "N\u1ea1p Ti\u1ec1n Th\u00e0nh C\u00f4ng | " + result.getAmount() + "| " + historyBank.getType() + " | " + result.getRequestId();
            MoneyResponse mr = this.userService.updateMoneyFromAdmin(historyBank.getNick_name(), result.getAmount(), "vin", "BankCode", "BankCode", msg, 0L, false);
            if (!mr.isSuccess()) {
                return Result.error(provider, "Update money error");
            }
            try {
                boolean success = this.historyBankDao.updateStatus(result.getRequestId(), 2, result.getAmount());
                if (!success) {
                    return Result.error(provider, "Update status error");
                }
                PaymentStatisticsDaoImpl paymentStatisticsDao = new PaymentStatisticsDaoImpl();
                PaymentSummaryEntity paymentSummaryEntity = paymentStatisticsDao.getPaymentSummaryByNickName(historyBank.getNick_name());
                if (historyBank.getType().equals("momo")) {
                    TelegramAlert.sendDepositMoMo(historyBank.getNick_name(), String.valueOf(param.get().getParameter("amount")), String.valueOf(mr.getCurrentMoney()), (String)result.getData(), String.valueOf(paymentSummaryEntity.getTotalDeposit()), String.valueOf(paymentSummaryEntity.getTotalWithdraw()));
                } else {
                    TelegramAlert.sendDepositBank(historyBank.getNick_name(), String.valueOf(param.get().getParameter("amount")), String.valueOf(mr.getCurrentMoney()), (String)result.getData(), String.valueOf(paymentSummaryEntity.getTotalDeposit()), String.valueOf(paymentSummaryEntity.getTotalWithdraw()));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return Result.error(provider, "Update status error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(provider, "Server Error");
        }
        return Result.success(provider, (String)result.getData());
    }

    @Override
    public Result<String> hookBankOut(String providerName, Param<HttpServletRequest> param) {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK_OUT);
        if (provider == null) {
            return Result.error("ProviderName not found");
        }
        HookBankOutResult result = provider.hookBankOut(param);
        if (result.getCode() == Code.ERROR) {
            return Result.error((String)result.getData());
        }
        try {
            boolean success;
            HistoryApplyForEntity historyApplyFor = this.historyApplyForDao.getByRequestId(result.getRequestId());
            if (historyApplyFor == null) {
                return Result.error(provider, "Not found top up");
            }
            if (historyApplyFor.getStatus() != 1) {
                return Result.error(provider, "Top up status has finish");
            }
            WithDrawPaygateModel withDrawPaygateModel = this.withdrawDao.GetById(result.getRequestId());
            if (withDrawPaygateModel == null) {
                return Result.error("Not found bank out");
            }
            if (result.isRollback()) {
                success = this.withdrawManual.withdrawalSystemNote(withDrawPaygateModel.Id, PayCommon.PAYSTATUS.FAILED, "L\u1ed7i giao \u0111\u1ecbch, h\u1ec7 th\u1ed1ng s\u1ebd ho\u00e0n ti\u1ec1n r\u00fat v\u1ec1 ng\u00e2n h\u00e0ng");
                if (!success) {
                    return Result.error("Not approved Order Out");
                }
                long amount = result.getAmount();
                String msg = "Ho\u00e0n Ti\u1ec1n R\u00fat V\u1ec1 Ng\u00e2n H\u00e0ng | " + amount + " | " + historyApplyFor.getType() + " | " + historyApplyFor.getFid();
                MoneyResponse mr = this.userService.updateMoneyFromAdmin(historyApplyFor.getNickName(), amount, "vin", "BankRefund", "BankRefund", msg, 0L, false);
                if (!mr.isSuccess()) {
                    return Result.error(provider, "User not found");
                }
            } else {
                success = this.withdrawManual.withdrawalSystemNote(withDrawPaygateModel.Id, PayCommon.PAYSTATUS.SUCCESS, "R\u00fat ti\u1ec1n th\u00e0nh c\u00f4ng");
                if (!success) {
                    return Result.error("Not approved Order Out");
                }
            }
            try {
                success = this.historyApplyForDao.updateStatus(result.getRequestId(), result.isRollback() ? 3 : 2, result.getAmount());
                if (!success) {
                    return Result.error(provider, "Update status error");
                }
                if (historyApplyFor.getCashBack() > 0L && !result.isRollback()) {
                    String msg = "Ho\u00e0n ti\u1ec1n r\u00fat 5% Th\u00e0nh C\u00f4ng | " + historyApplyFor.getCashBack();
                    MoneyResponse mr = this.userService.updateMoneyFromAdmin(withDrawPaygateModel.Nickname, historyApplyFor.getCashBack(), "vin", "BACK_BANK_OUT", "BACK_BANK_OUT", msg, 0L, false);
                    if (!mr.isSuccess()) {
                        return Result.error(provider, "Ho\u00e0n ti\u1ec1n r\u00fat 5% l\u1ed7i");
                    }
                }
                TelegramAlert.sendBankOut(historyApplyFor.getNickName(), String.valueOf(result.getAmount()), (String)result.getData());
            }
            catch (Exception e) {
                e.printStackTrace();
                return Result.error(provider, "Update status error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(provider, "Server error");
        }
        return Result.success(provider, (String)result.getData());
    }

    @Override
    public Result<String> hookCardIn(String providerName, Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        logger.debug(("[hookCardIn] providerName=" + providerName + " params: request_id=" + request.getParameter("request_id") + " status=" + request.getParameter("status") + " amount=" + request.getParameter("amount") + " message=" + request.getParameter("message") + " signature=" + (request.getParameter("signature") != null ? "***" : request.getParameter("sign"))));
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_CART);
        if (provider == null) {
            String resolvedName = PaymentConfigLoad.getPaymentConfig().getDefault_provider_card();
            return Result.error("ProviderName not found");
        }
        logger.debug(("[hookCardIn] using provider=" + provider.name()));
        HookCardInResult result = provider.hookCardIn(param);
        if (result.getCode() == Code.ERROR) {
            return Result.error((String)result.getData());
        }
        try {
            MoneyResponse mr;
            TopUpEntity topUpEntity = this.topUpDao.getByRequestId(result.getRequestId());
            if (topUpEntity == null) {
                return Result.error(BaseResponse.New(3).toJson());
            }
            if (topUpEntity.getStatus() != 1) {
                return Result.error(BaseResponse.New(4, "Th\u1ebb \u0111\u00e3 c\u1ed9ng r\u1ed3i.").toJson());
            }
            if (result.getCode() == Code.SUCCESS) {
                long amount = result.getAmount();
                String msg = result.getResult_message() + " | " + amount + " | " + topUpEntity.getType() + " | " + topUpEntity.getSerial();
                mr = this.userService.updateMoneyFromAdmin(topUpEntity.getNick_name(), amount, "vin", "TopupCard", "TopupCard", msg, 0L, false);
                if (!mr.isSuccess()) {
                    return Result.error(BaseResponse.New(4).toJson());
                }
                boolean success = this.topUpDao.updateStatus(result.getRequestId(), 2, result.getResult_message(), result.getAmount());
                if (!success) {
                    return Result.error(BaseResponse.New(4).toJson());
                }
            } else {
                boolean success = this.topUpDao.updateStatus(result.getRequestId(), 3, result.getResult_message());
                if (!success) {
                    return Result.error(BaseResponse.New(4).toJson());
                }
                return Result.error(result.getResult_message());
            }
            PaymentStatisticsDaoImpl paymentStatisticsDao = new PaymentStatisticsDaoImpl();
            PaymentSummaryEntity paymentSummaryEntity = paymentStatisticsDao.getPaymentSummaryByNickName(topUpEntity.getNick_name());
            TelegramAlert.sendDepositCard(topUpEntity.getNick_name(), String.valueOf(result.getAmount()), String.valueOf(mr.getCurrentMoney()), result.getResult_message(), String.valueOf(paymentSummaryEntity.getTotalDeposit()), String.valueOf(paymentSummaryEntity.getTotalWithdraw()));
        }
        catch (Exception e) {
            logger.error(("[hookCardIn] 400 exception. requestId=" + (result != null ? result.getRequestId() : "n/a")), (Throwable)e);
            e.printStackTrace();
            return Result.error(e.toString());
        }
        return Result.success(provider, (String)result.getData());
    }

    @Override
    public Result<List<Bank>> bankListOut(String providerName) throws Exception {
        Provider provider = this.getProvider(providerName, ProviderType.PROVIDER_BANK);
        if (provider == null) {
            return Result.error("ProviderName not found " + providerName);
        }
        BankListResult result = provider.BankListOut();
        if (result.getCode() == Code.ERROR) {
            return Result.error("Bank list error");
        }
        return Result.success(result.getBanks());
    }

    static {
        scheduler = Executors.newScheduledThreadPool(10);
        random = new Random();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    if (!scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                        Logger.getLogger((String)"payment").warn("Scheduler did not terminate");
                    }
                }
            }
            catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "ProviderService-Scheduler-Shutdown"));
        logger = Logger.getLogger(Provider.class);
    }

    private static enum ProviderType {
        PROVIDER_BANK,
        PROVIDER_CART,
        PROVIDER_BANK_OUT;

    }
}

