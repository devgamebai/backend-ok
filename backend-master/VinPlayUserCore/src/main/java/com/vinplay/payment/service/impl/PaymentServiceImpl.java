/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.response.LogExchangeMoneyResponse
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 */
package com.vinplay.payment.service.impl;

import com.vinplay.payment.dao.PaymentDao;
import com.vinplay.payment.dao.impl.PaymentDaoImpl;
import com.vinplay.payment.service.PaymentService;
import com.vinplay.usercore.response.LogExchangeMoneyResponse;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import java.util.List;

public class PaymentServiceImpl
implements PaymentService {
    private PaymentDao dao = new PaymentDaoImpl();

    @Override
    public boolean logExchangeMoney(ExchangeMessage message) {
        return this.dao.logExchangeMoney(message);
    }

    @Override
    public boolean checkMerchantTransId(String merchantId, String merchantTransId) {
        return this.dao.checkMerchantTransId(merchantId, merchantTransId);
    }

    @Override
    public LogExchangeMoneyResponse getLogExchangeMoney(String nickname, String merchantId, String transId, String transNo, String type, int code, String startTime, String endTime, int page) throws Exception {
        return this.dao.getLogExchangeMoney(nickname, merchantId, transId, transNo, type, code, startTime, endTime, page);
    }

    @Override
    public long getTotalMoney(String merchantId, String nickname, String startTime, String endTime, String type) throws Exception {
        return this.dao.getTotalMoney(merchantId, nickname, startTime, endTime, type);
    }

    @Override
    public List<ExchangeMessage> getExchangeMoney(String nickname, String merchantId, String transId, String transNo, String type, int code, String startTime, String endTime) throws Exception {
        return this.dao.getExchangeMoney(nickname, merchantId, transId, transNo, type, code, startTime, endTime);
    }
}

