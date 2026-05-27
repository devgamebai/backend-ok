/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.response.GiftCodeAgentResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GiftCodeAgentDaoImpl;
import com.vinplay.usercore.service.GiftCodeAgentService;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;

public class GiftCodeAgentServiceImpl
implements GiftCodeAgentService {
    @Override
    public GiftCodeAgentResponse exportGiftCode(GiftCodeMessage msg, long curentMoney, String nickName) {
        GiftCodeAgentDaoImpl dao = new GiftCodeAgentDaoImpl();
        return dao.exportGiftCode(msg, curentMoney, nickName);
    }
}

