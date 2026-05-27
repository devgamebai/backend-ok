/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GiftCodeMachineMessage
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GiftCodeMachineDAOImpl;
import com.vinplay.usercore.service.GiftCodeMachineService;
import com.vinplay.vbee.common.response.GiftCodeMachineMessage;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import java.sql.SQLException;

public class GiftCodeMachineServiceImpl
implements GiftCodeMachineService {
    @Override
    public boolean exportGiftCodeMachine(GiftCodeMachineMessage msg) {
        GiftCodeMachineDAOImpl dao = new GiftCodeMachineDAOImpl();
        return dao.exportGiftCodeMachine(msg);
    }

    @Override
    public GiftCodeUpdateResponse updateGiftCode(String nickName, String giftCode) throws SQLException {
        GiftCodeMachineDAOImpl dao = new GiftCodeMachineDAOImpl();
        return dao.updateGiftCode(nickName, giftCode);
    }
}

