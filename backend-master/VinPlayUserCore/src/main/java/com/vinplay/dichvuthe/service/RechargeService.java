/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.ProviderType
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 */
package com.vinplay.dichvuthe.service;

import com.vinplay.dichvuthe.response.I2BResponse;
import com.vinplay.dichvuthe.response.RechargeApiOTPResponse;
import com.vinplay.dichvuthe.response.RechargeIAPResponse;
import com.vinplay.dichvuthe.response.RechargeResponse;
import com.vinplay.vbee.common.enums.ProviderType;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import java.util.Map;

public interface RechargeService {
    public RechargeResponse rechargeByCard(String var1, ProviderType var2, String var3, String var4,String var6, String var5) throws Exception;

    public RechargeResponse rechargeByGachThe(String nickname, ProviderType provider, String serial, String pin, String amount, String platform, int UserId) throws Exception;

    public Map<String, Long> reCheckRechargeByCard() throws Exception;

    public I2BResponse rechargeByBank(String var1, long var2, byte var4, String var5, String var6) throws Exception;

    public void receiveResultFromBank(Map<String, String[]> var1);

    public byte checkRechargeIAP(String var1, int var2);

    public RechargeIAPResponse rechargeIAP(String var1, String var2, String var3);

    public String smsPlusCheckMO(Map<String, String[]> var1);

    public String sms8xRequest(Map<String, String[]> var1);

    public String smsPlusRequest(Map<String, String[]> var1);

    public RechargeApiOTPResponse sendRequestChargingOTP(String var1, String var2, int var3);

    public RechargeApiOTPResponse sendConfirmChargingOTP(String var1, String var2, String var3);

    public String receiveConfirmChargingOTP(Map<String, String[]> var1);

    public RechargeResponse rechargeByVinCard(String var1, String var2, String var3, String var4) throws Exception;

    public RechargeByCardMessage updatePendingCardStatus(String var1, String var2) throws Exception;

    public Map<String, Long> updatePendingCardStatus(String var1, String var2, String var3) throws Exception;

    public RechargeResponse rechargeByMegaCard(String var1, String var2, String var3, String var4) throws Exception;

    public RechargeResponse rechargeByVcoin(String var1, String var2, String var3, String var4) throws Exception;

}

