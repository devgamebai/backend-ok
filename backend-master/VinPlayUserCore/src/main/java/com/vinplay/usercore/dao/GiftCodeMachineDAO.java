/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GiftCodeMachineMessage
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.response.GiftCodeMachineMessage;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import java.sql.SQLException;

public interface GiftCodeMachineDAO {
    public boolean exportGiftCodeMachine(GiftCodeMachineMessage var1);

    public GiftCodeUpdateResponse updateGiftCode(String var1, String var2) throws SQLException;
}

