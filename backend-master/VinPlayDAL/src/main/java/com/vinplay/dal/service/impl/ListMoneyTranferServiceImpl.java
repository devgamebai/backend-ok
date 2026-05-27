/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TranferMoneyResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.ListMoneyTranferDAOImpl;
import com.vinplay.dal.service.ListMoneyTranferService;
import com.vinplay.vbee.common.response.TranferMoneyResponse;
import java.util.List;

public class ListMoneyTranferServiceImpl
implements ListMoneyTranferService {
    @Override
    public List<TranferMoneyResponse> listMoneyTranfer(String nickName, int isFreezeMoney, int page, int timeSearch) {
        ListMoneyTranferDAOImpl dao = new ListMoneyTranferDAOImpl();
        return dao.listMoneyTranfer(nickName, isFreezeMoney, page, timeSearch);
    }

    @Override
    public TranferMoneyResponse getMoneyTranferByTransNo(String transNo) {
        ListMoneyTranferDAOImpl dao = new ListMoneyTranferDAOImpl();
        return dao.getMoneyTranferByTransNo(transNo);
    }

    @Override
    public int countTotalRecord(String nickName, int isFreezeMoney, int page, int timeSearch) {
        ListMoneyTranferDAOImpl dao = new ListMoneyTranferDAOImpl();
        return dao.countTotalRecord(nickName, isFreezeMoney, page, timeSearch);
    }
}

