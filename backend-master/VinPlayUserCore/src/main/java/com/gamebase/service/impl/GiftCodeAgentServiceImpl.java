/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.service.impl;

import com.gamebase.dao.impl.GiftCodeAgentDaoImpl;
import com.gamebase.service.GiftCodeAgentService;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;
import org.apache.log4j.Logger;

public class GiftCodeAgentServiceImpl
implements GiftCodeAgentService {
    private static final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public GiftCodeAgentResponse exportGiftCode(GiftCodeMessage msg, long curentMoney, String nickName) {
        GiftCodeAgentDaoImpl dao = new GiftCodeAgentDaoImpl();
        return dao.exportGiftCode(msg, curentMoney, nickName);
    }
}

