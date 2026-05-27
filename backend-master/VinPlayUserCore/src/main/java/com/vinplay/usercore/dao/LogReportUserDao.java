/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.models.LogReportModel;
import java.util.List;

public interface LogReportUserDao {
    public boolean updateDepositLogReportByDateAndUser(String var1, String var2, Long var3);

    public List<LogReportModel> getAllDepositLogReportByDateAndUser(String var1);
}

