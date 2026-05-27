/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.messages.GiftCodeMessage;
import java.sql.SQLException;
import java.util.List;

public interface GiftCodeXCDao {
    public List<String> loadAllGiftcode() throws SQLException;

    public void insertGiftcodeStore(GiftCodeMessage var1) throws SQLException;
}

