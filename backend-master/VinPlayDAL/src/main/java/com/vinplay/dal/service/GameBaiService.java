/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service;

import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import java.util.List;

public interface GameBaiService {
    public ReportMoneySystemModel getReportGameToday(String var1);

    public List<LogMoneyUserResponse> getLSGD(String var1, int var2);

    public List<LogMoneyUserResponse> getLSXD(String var1, int var2);
}

