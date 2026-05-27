/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class UpdateDealerProfitMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public long phienid;
    public int result;
    public long total_money_tai = 0L;
    public long total_money_xiu = 0L;
    public long total_profit = 0L;
    public long last_balance = 0L;
}

