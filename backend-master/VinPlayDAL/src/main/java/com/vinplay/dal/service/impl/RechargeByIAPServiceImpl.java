/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.RechardByIAPResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeByIAPDAOImpl;
import com.vinplay.dal.service.RechargeByIAPService;
import com.vinplay.vbee.common.response.RechardByIAPResponse;
import java.util.List;

public class RechargeByIAPServiceImpl
implements RechargeByIAPService {
    @Override
    public List<RechardByIAPResponse> ListRechargeIAP(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId, int page) {
        RechargeByIAPDAOImpl dao = new RechargeByIAPDAOImpl();
        return dao.ListRechargeIAP(nickName, code, timeStart, timeEnd, amount, orderId, page);
    }

    @Override
    public long totalMoney(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId) {
        RechargeByIAPDAOImpl dao = new RechargeByIAPDAOImpl();
        return dao.totalMoney(nickName, code, timeStart, timeEnd, amount, orderId);
    }

    @Override
    public long countListRechargeIAP(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId) {
        RechargeByIAPDAOImpl dao = new RechargeByIAPDAOImpl();
        return dao.countListRechargeIAP(nickName, code, timeStart, timeEnd, amount, orderId);
    }
}

