/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame.baucua;

import com.vinplay.vbee.common.messages.BaseMessage;
import java.io.Serializable;

public class TransactionBauCuaDetailMsg
extends BaseMessage
implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public long referenceId;
    public int room;
    public String transactionCode;
    public long[] betValues;
    public byte moneyType;
}

