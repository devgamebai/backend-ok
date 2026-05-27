/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LuckyVipHistoryResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LuckyVipHistoryDAOImpl;
import com.vinplay.dal.service.LuckyVipHistoryService;
import com.vinplay.vbee.common.response.LuckyVipHistoryResponse;
import java.util.List;

public class LuckyVipHistoryServiceImpl
implements LuckyVipHistoryService {
    @Override
    public List<LuckyVipHistoryResponse> searchLuckyVipHistory(String nickName, String timeStart, String timeEnd, int page) {
        LuckyVipHistoryDAOImpl dao = new LuckyVipHistoryDAOImpl();
        return dao.searchLuckyVipHistory(nickName, timeStart, timeEnd, page);
    }
}

