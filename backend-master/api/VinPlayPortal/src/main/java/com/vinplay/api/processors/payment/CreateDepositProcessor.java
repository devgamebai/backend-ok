/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.entities.agent.UserAgentModel
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.entities.Bank
 *  com.vinplay.payment.entities.PaymentConfig
 *  com.vinplay.payment.entities.UserBank
 *  com.vinplay.payment.service.impl.PaymentConfigServiceImpl
 *  com.vinplay.payment.service.impl.RechargeManualBankServiceImpl
 *  com.vinplay.usercore.service.impl.UserBankServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.Bank;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.UserBank;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.service.impl.RechargeManualBankServiceImpl;
import com.vinplay.usercore.service.impl.UserBankServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class CreateDepositProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static Map<String, Long> mapCache = new ConcurrentHashMap<String, Long>();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        long amount = 0L;
        try {
            amount = Long.parseLong(request.getParameter("am"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        String agentCode = request.getParameter("code");
        if (StringUtils.isBlank((CharSequence)agentCode)) {
            return BaseResponse.error((String)"5", (String)"Agent code can not empty");
        }
        String agentBankNumber = request.getParameter("abn");
        if (StringUtils.isBlank((CharSequence)agentBankNumber)) {
            return BaseResponse.error((String)"5", (String)"Bank of agent can not empty");
        }
        String cartId = request.getParameter("cid");
        if (StringUtils.isBlank((CharSequence)cartId)) {
            return BaseResponse.error((String)"5", (String)"Id of banking transaction can not empty");
        }
        String nickName = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String providerName = "manualbank";
        String ip = this.getIpAddress(request);
        logger.info(("ipaddress :" + ip));
        String clientIp = "";
        if (ip != null && !"".equals(ip)) {
            String[] arrayIp = ip.split(",");
            for (int i = 0; i < (arrayIp.length > 2 ? 2 : arrayIp.length); ++i) {
                if (arrayIp[i].length() > 40) continue;
                clientIp = arrayIp[i].trim();
                break;
            }
        }
        if (!CreateDepositProcessor.validateRequest(nickName)) {
            return BaseResponse.error((String)"15", (String)("Please wait 5 seconds to make the next transaction, NickName =" + nickName));
        }
        logger.info(("Deposit request nickName: " + nickName + ", accessToken: " + accessToken + ", providerName: " + providerName + ",ipaddress=" + clientIp));
        UserServiceImpl userService = new UserServiceImpl();
        try {
            if (StringUtils.isBlank((CharSequence)nickName) || StringUtils.isBlank((CharSequence)accessToken)) {
                return BaseResponse.error((String)"5", (String)"input parameter is null or empty");
            }
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (isToken) {
                long minAmount;
                PaymentConfigServiceImpl payConfig = new PaymentConfigServiceImpl();
                PaymentConfig config = payConfig.getConfigByKey(providerName);
                long l = minAmount = config == null ? 1000L : (long)config.getConfig().getMinMoney().intValue();
                if (amount < minAmount) {
                    return BaseResponse.error((String)"1", (String)("Money is greater than " + minAmount));
                }
                if (amount > 300000000L) {
                    return BaseResponse.error((String)"16", (String)"Money must be less than than 300M ");
                }
                UserModel user = userService.getUserByNickName(nickName);
                String userId = user.getId() + "";
                String username = user.getUsername();
                if (user.isBanLogin() || user.isBanTransferMoney() || user.isBot()) {
                    return BaseResponse.error((String)"12", (String)"You can not allow access this feature");
                }
                if (user.getDaily() == 1) {
                    UserAgentModel agentModel;
                    AgentDAOImpl agentDao = new AgentDAOImpl();
                    try {
                        agentModel = agentDao.DetailUserAgentByNickName(nickName);
                    }
                    catch (SQLException e1) {
                        e1.printStackTrace();
                        agentModel = null;
                    }
                    if (agentModel == null) {
                        return BaseResponse.error((String)"5", (String)"Agent account is not exist");
                    }
                    if (agentModel.getActive() == 0) {
                        return BaseResponse.error((String)"5", (String)"Agent is inactive");
                    }
                }
                RechargeManualBankServiceImpl service = new RechargeManualBankServiceImpl();
                String bankAccountNum = request.getParameter("bn");
                if (StringUtils.isBlank((CharSequence)bankAccountNum)) {
                    return BaseResponse.error((String)"5", (String)"Bank number can not empty");
                }
                UserBankServiceImpl bankService = new UserBankServiceImpl();
                UserBank userBank = bankService.getByDetail(nickName, bankAccountNum);
                if (userBank == null) {
                    return BaseResponse.error((String)"12", (String)"Bank number is invalid");
                }
                Bank bank = GameCommon.LIST_BANK_NAME.stream().filter(x -> userBank.getBankName().trim().toLowerCase().contains(x.getBank_name().trim().toLowerCase())).findAny().orElse(null);
                RechargePaywellResponse rs = null;
                rs = service.create(userId, username, nickName, bank == null ? userBank.getBankName() : bank.getBank_short_name(), bank == null ? userBank.getBankName() : bank.getCode(), bankAccountNum, userBank.getCustomerName(), agentCode, agentBankNumber, amount, cartId);
                if (rs == null) {
                    return BaseResponse.error((String)"2", (String)"Create transaction deposit failure");
                }
                logger.info(("Deposit response nickName: " + nickName + ", response : " + rs.toJson()));
                if (0 == rs.getCode()) {
                    return new BaseResponse().success(rs.getData());
                }
                return BaseResponse.error((String)(rs.getCode() + ""), (String)rs.getData());
            }
            return BaseResponse.error((String)"4", (String)"Your trading session has expried, please reload the page and login again.");
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static boolean validateRequest(String orderID) {
        if (mapCache.isEmpty()) {
            long t1 = new Date().getTime();
            mapCache.put(orderID, t1);
        } else {
            if (mapCache.containsKey(orderID)) {
                long t1 = mapCache.get(orderID);
                long t2 = new Date().getTime();
                if (t2 - t1 > 5000L) {
                    mapCache.put(orderID, t2);
                    return true;
                }
                return false;
            }
            long t1 = new Date().getTime();
            mapCache.put(orderID, t1);
        }
        return true;
    }
}

