/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.response.GiftCodeAgentResponse
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;

public interface GiftCodeAgentDao {
    public GiftCodeAgentResponse exportGiftCode(GiftCodeMessage var1, long var2, String var4);
}

