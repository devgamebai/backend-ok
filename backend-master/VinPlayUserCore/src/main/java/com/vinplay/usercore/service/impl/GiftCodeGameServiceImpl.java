/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GiftCodeGameResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GiftCodeGameDaoImpl;
import com.vinplay.usercore.service.GiftCodeGameService;
import com.vinplay.vbee.common.response.GiftCodeGameResponse;
import java.util.List;

public class GiftCodeGameServiceImpl
implements GiftCodeGameService {
    @Override
    public boolean exportGiftCodeStore(GiftCodeGameResponse msg) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.exportGiftCodeStore(msg);
    }

    @Override
    public boolean exportGiftCode(GiftCodeGameResponse msg) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.exportGiftCode(msg);
    }

    @Override
    public List<GiftCodeGameResponse> searchAllGiftCode(String nickName, String giftcode, String surfing, String source, String timeStart, String timeEnd, String userName, String block, String giftuse, int page, int totalRecord) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.searchAllGiftCode(nickName, giftcode, surfing, source, timeStart, timeEnd, userName, block, giftuse, page, totalRecord);
    }

    @Override
    public List<GiftCodeGameResponse> searchAllGiftCodeAdmin(String surfing, String source, String timeStart, String timeEnd, String giftuse, int page, int totalRecord) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.searchAllGiftCodeAdmin(surfing, source, timeStart, timeEnd, giftuse, page, totalRecord);
    }

    @Override
    public long countSearchAllGiftCode(String nickName, String giftcode, String price, String source, String timeStart, String timeEnd, String userName, String block, String giftuse) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.countSearchAllGiftCode(nickName, giftcode, price, source, timeStart, timeEnd, userName, block, giftuse);
    }

    @Override
    public long countSearchAllGiftCodeAdmin(String surfing, String source, String timeStart, String timeEnd, String giftuse) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.countSearchAllGiftCodeAdmin(surfing, source, timeStart, timeEnd, giftuse);
    }

    @Override
    public boolean blockGiftCode(String giftCode, String block) {
        GiftCodeGameDaoImpl dao = new GiftCodeGameDaoImpl();
        return dao.blockGiftCode(giftCode, block);
    }
}

