/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service.impl;

import com.gamebase.dao.impl.GiftCodeMachineDAOImpl;
import com.gamebase.service.GiftCodeMachineService;
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

