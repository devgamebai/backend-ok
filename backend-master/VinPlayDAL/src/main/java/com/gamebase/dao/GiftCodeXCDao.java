/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 */
package com.gamebase.dao;

import com.vinplay.vbee.common.messages.GiftCodeMessage;
import java.sql.SQLException;
import java.util.List;

public interface GiftCodeXCDao {
    public List<String> loadAllGiftcode() throws SQLException;

    public void insertGiftcodeStore(GiftCodeMessage var1) throws SQLException;
}

