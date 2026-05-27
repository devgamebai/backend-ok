/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.service.impl.PaymentServiceImpl
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.api.processors.response.TransInfoResponse;
import com.vinplay.payment.service.impl.PaymentServiceImpl;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class QueryTransactionProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(QueryTransactionProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        int errorCode = 1;
        TransInfoResponse res = new TransInfoResponse();
        HttpServletRequest request = (HttpServletRequest)param.get();
        long time = System.currentTimeMillis();
        String transId = null;
        String ip = this.getIpAddress(request);
        ExchangeMessage message = null;
        try {
            String command = request.getParameter("c");
            transId = request.getParameter("tid");
            String merchantId = request.getParameter("mid");
            String checkSum = request.getParameter("cks");
            logger.debug((Object)("Request QueryTransaction id: " + time + ", command: " + command + ", merchantTransId: " + transId + ", merchantId: " + merchantId + ", checkSum: " + checkSum + ", ip: " + ip));
            if (command != null && transId != null && merchantId != null && checkSum != null) {
                res.setTransId(transId);
                if (PayUtils.checkMerchantId((String)merchantId)) {
                    String merchantKey = PayUtils.getMerchantKey((String)merchantId);
                    String value = String.valueOf(command) + transId + merchantId + merchantKey;
                    String ck = VinPlayUtils.getMD5Hash((String)value);
                    if (ck.equals(checkSum)) {
                        ExchangeMessage messageCk = this.getTrans(merchantId, transId);
                        if (messageCk != null) {
                            message = messageCk;
                            errorCode = 0;
                        } else {
                            errorCode = 13;
                        }
                    } else {
                        errorCode = 3;
                    }
                } else {
                    errorCode = 2;
                }
            } else {
                errorCode = 4;
            }
        }
        catch (Exception e) {
            logger.error((Object)e);
        }
        res.setTrans(message);
        res.setErrorCode(errorCode);
        logger.debug((Object)("Response QueryTransaction id: " + time + ", res: " + res.toJson()));
        logger.error((Object)("QueryTransaction " + transId + " " + (System.currentTimeMillis() - time)));
        return res.toJson();
    }

    private synchronized ExchangeMessage getTrans(String merchantId, String transId) throws Exception {
        PaymentServiceImpl ser = new PaymentServiceImpl();
        List listEx = ser.getExchangeMoney("", merchantId, transId, "", "", -1, "", "");
        if (listEx.size() > 0) {
            return (ExchangeMessage)listEx.get(0);
        }
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

