/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponse
 */
package com.vinplay.report.service;

import com.vinplay.dal.entities.report.LogCountUserPlay;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.List;

public interface CountReportService {
    public BaseResponse<List<LogCountUserPlay>> getLogReportModelSQL(String var1, String var2, String var3, int var4, int var5);
}

