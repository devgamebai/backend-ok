/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.dao.impl.RechargePaygateDaoImpl
 *  com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl
 *  com.vinplay.payment.entities.WithDrawPaygateModel
 *  com.vinplay.payment.service.impl.WithDrawOneClickPayServiceImpl
 *  com.vinplay.payment.utils.PayCommon$PAYSTATUS
 *  org.apache.log4j.Logger
 */
package com.vinplay.schedule.task;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.service.impl.WithDrawOneClickPayServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class TaskAutoPayment
extends TimerTask {
    private static final Logger LOGGER = Logger.getLogger(TaskAutoPayment.class);

    @Override
    public void run() {
        LOGGER.info((Object)("Start Job Update bank status automation , time=" + LocalDateTime.now()));
        RechargePaygateDaoImpl rechard = new RechargePaygateDaoImpl();
        try {
            rechard.updatePendingStatusToFailedAfterMinus(60, "all");
        }
        catch (Exception e) {
            LOGGER.error((Object)e);
        }
        WithDrawOneClickPayServiceImpl withdrawOneClickPayService = new WithDrawOneClickPayServiceImpl();
        WithDrawPaygateDaoImpl withDraw = new WithDrawPaygateDaoImpl();
        try {
            ArrayList lstRecords = new ArrayList();
            lstRecords = withDraw.GetRecevied(Integer.valueOf(10));
            if (lstRecords == null || lstRecords.isEmpty()) {
                return;
            }
            for (Object _wd : lstRecords) {
                WithDrawPaygateModel wd = (WithDrawPaygateModel) _wd;
                RechargePaywellResponse res = withdrawOneClickPayService.checkStatus(wd.CartId);
                if (res.getCode() == 0) {
                    withdrawOneClickPayService.notify(wd, PayCommon.PAYSTATUS.SUCCESS.getId().intValue());
                    continue;
                }
                withdrawOneClickPayService.notify(wd, PayCommon.PAYSTATUS.FAILED.getId().intValue());
            }
        }
        catch (Exception e) {
            LOGGER.error((Object)e);
        }
    }
}

