/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.ReportModel
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.models.cache.ReportModel;
import java.sql.SQLException;
import java.text.ParseException;

public interface ReportDao {
    public boolean saveLogMoneyForReport(String var1, String var2, String var3, ReportModel var4, boolean var5) throws ParseException;

    public boolean checkBot(String var1) throws SQLException;
}

