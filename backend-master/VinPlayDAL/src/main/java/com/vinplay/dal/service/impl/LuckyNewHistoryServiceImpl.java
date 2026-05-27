/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LuckyNewHistoryResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LuckyNewHistoryDAOImpl;
import com.vinplay.dal.service.LuckyNewHistoryService;
import com.vinplay.vbee.common.response.LuckyNewHistoryResponse;
import java.util.List;

public class LuckyNewHistoryServiceImpl
implements LuckyNewHistoryService {
    @Override
    public List<LuckyNewHistoryResponse> searchLuckyNewHistory(String nickName, String timeStart, String timeEnd, int page) {
        LuckyNewHistoryDAOImpl dao = new LuckyNewHistoryDAOImpl();
        return dao.searchLuckyNewHistory(nickName, timeStart, timeEnd, page);
    }
}

