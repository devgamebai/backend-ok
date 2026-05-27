/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service.impl;

import com.gamebase.dao.impl.GiftCodeXCDaoImpl;
import com.gamebase.service.GiftCodeXCService;
import java.sql.SQLException;
import java.util.List;

public class GiftCodeXCServiceImpl
implements GiftCodeXCService {
    @Override
    public List<String> loadAllGiftcode() throws SQLException {
        GiftCodeXCDaoImpl dao = new GiftCodeXCDaoImpl();
        return dao.loadAllGiftcode();
    }
}

