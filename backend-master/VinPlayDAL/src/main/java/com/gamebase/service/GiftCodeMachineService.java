/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GiftCodeMachineMessage
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 */
package com.gamebase.service;

import com.vinplay.vbee.common.response.GiftCodeMachineMessage;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import java.sql.SQLException;

public interface GiftCodeMachineService {
    public boolean exportGiftCodeMachine(GiftCodeMachineMessage var1);

    public GiftCodeUpdateResponse updateGiftCode(String var1, String var2) throws SQLException;
}

