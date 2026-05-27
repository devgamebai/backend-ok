/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame.baucua;

import com.vinplay.vbee.common.messages.BaseMessage;
import java.io.Serializable;

public class TransactionBauCuaMsg
extends BaseMessage
implements Serializable {
    private static final long serialVersionUID = 1L;
    public long referenceId;
    public int room;
    public String dices;
    public String username;
    public String transactionCode;
    public long[] betValues;
    public long[] prizes;
    public long totalExchange;
    public byte moneyType;
}

