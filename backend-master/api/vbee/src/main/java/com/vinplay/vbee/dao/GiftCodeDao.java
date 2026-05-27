/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.dao;

import java.sql.SQLException;

public interface GiftCodeDao {
    public void updateGiftCodeStore(String var1) throws SQLException;

    public void lockGiftCode(String var1) throws SQLException;
}

