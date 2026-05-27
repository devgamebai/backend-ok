/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;

public interface RechargeDao {
    public void logRechargeByCard(RechargeByCardMessage var1);

    public void logRechargeByVinCard(RechargeByCardMessage var1);

    public void logRechargeByBank(RechargeByBankMessage var1);

    public void logRefundFeeAgent(RefundFeeAgentMessage var1);

    public void logRechargeByMegaCard(RechargeByCardMessage var1);
}

