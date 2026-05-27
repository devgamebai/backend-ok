/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class UpdatePotMessage
        extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public String potName;
    public long newValue;
    public boolean isMD5=false;
}

