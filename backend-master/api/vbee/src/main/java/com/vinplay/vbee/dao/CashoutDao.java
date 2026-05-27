/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage;

public interface CashoutDao {
    public void logCashoutByCard(CashoutByCardMessage var1);

    public void logCashoutByTopUp(CashoutByTopUpMessage var1);

    public void logCashoutByBank(CashoutByBankMessage var1);
}

