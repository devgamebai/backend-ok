/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.service.impl;

import com.vinplay.dichvuthe.dao.ExchangeDao;
import com.vinplay.dichvuthe.dao.impl.ExchangeDaoImpl;
import com.vinplay.dichvuthe.service.ExchangeService;

public class ExchangeServiceImpl
implements ExchangeService {
    private ExchangeDao exDao = new ExchangeDaoImpl();

    @Override
    public long getExchangeMoney(String merchantId, String nickname, String startTime, String endTime) {
        return this.exDao.getExchangeMoney(merchantId, nickname, startTime, endTime);
    }
}

