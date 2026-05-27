/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service;

import java.sql.SQLException;
import java.util.List;

public interface GiftCodeXCService {
    public List<String> loadAllGiftcode() throws SQLException;
}

