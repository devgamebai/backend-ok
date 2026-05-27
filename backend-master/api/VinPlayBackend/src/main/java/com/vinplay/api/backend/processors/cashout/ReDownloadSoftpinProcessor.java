/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.dal.service.impl.CashOutByCardServiceImpl
 *  com.vinplay.dichvuthe.client.VinplayClient
 *  com.vinplay.dichvuthe.encode.RSA
 *  com.vinplay.dichvuthe.entities.SoftpinObj
 *  com.vinplay.epay.EpayClient
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.ProviderType
 *  com.vinplay.vbee.common.response.CashOutByCardResponse
 *  com.vinplay.vtc.VTCClient
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.cashout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.service.impl.CashOutByCardServiceImpl;
import com.vinplay.dichvuthe.client.VinplayClient;
import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.dichvuthe.entities.SoftpinObj;
import com.vinplay.epay.EpayClient;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.ProviderType;
import com.vinplay.vbee.common.response.CashOutByCardResponse;
import com.vinplay.vtc.VTCClient;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ReDownloadSoftpinProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String transId = request.getParameter("tid");
        if (transId != null && !transId.isEmpty()) {
            CashOutByCardServiceImpl service = new CashOutByCardServiceImpl();
            List trans = service.searchCashOutByCard((String)null, (String)null, (String)null, (String)null, (String)null, (String)null, 1, transId, (String)null);
            if (trans != null && trans.size() > 0) {
                CashOutByCardResponse coRes = (CashOutByCardResponse)trans.get(0);
                if (coRes == null || coRes.partner == null || coRes.partner.isEmpty()) {
                    return "Th\u1ea5t b\u1ea1i";
                }
                try {
                    String partner;
                    if (coRes.softpin != null && !coRes.softpin.isEmpty()) {
                        return coRes.softpin;
                    }
                    boolean success = false;
                    SoftpinObj spObj = null;
                    switch (partner = coRes.partner) {
                        case "dvt": {
                            String sign = RSA.sign((String)coRes.referenceId, (String)GameCommon.getValueStr((String)"DVT_PRIVATE_KEY"));
                            spObj = VinplayClient.reCheckCashOutByCard((String)coRes.referenceId, (String)sign);
                            success = spObj.getStatus() == 0;
                            break;
                        }
                        case "vtc": {
                            spObj = VTCClient.getCards((String)ProviderType.getProviderByName((String)coRes.provider).getValue(), (String)String.valueOf(coRes.amount), (int)coRes.quantity, (String)coRes.referenceId);
                            success = spObj.getStatus() == 1;
                            break;
                        }
                        case "1pay": {
                            break;
                        }
                        case "epay": {
                            spObj = EpayClient.reDownloadSoftpin((String)coRes.referenceId, (String)ProviderType.getProviderByName((String)coRes.provider).getValue(), (int)coRes.amount, (int)coRes.quantity);
                            success = spObj.getStatus() == 0;
                            break;
                        }
                    }
                    if (success) {
                        ObjectMapper mp = new ObjectMapper();
                        return mp.writeValueAsString((Object)spObj.getSoftpinList());
                    }
                    return spObj.getMessage();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }
            return "Giao d\u1ecbch kh\u00f4ng t\u1ed3n t\u1ea1i";
        }
        return "Th\u1ea5t b\u1ea1i";
    }
}

