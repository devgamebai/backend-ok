/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GiftCodeXCDaoImpl;
import com.vinplay.usercore.service.GiftCodeXCService;
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

