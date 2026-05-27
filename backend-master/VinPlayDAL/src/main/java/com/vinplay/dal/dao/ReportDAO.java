/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.ReportModel
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportTotalMoneyModel;
import com.vinplay.vbee.common.models.cache.ReportModel;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ReportDAO {
    public List<String> getAllBot() throws SQLException;

    public ReportTotalMoneyModel getTotalMoney(String var1) throws SQLException;

    /** Returns vin_total (cumulative P&L counter), NOT current balance. Renamed from getCurrentMoney. */
    public long getTotalPnl(String var1) throws SQLException;

    public long getSafeMoney(String var1) throws SQLException;

    public boolean checkBot(String var1) throws SQLException;

    public Map<String, ReportMoneySystemModel> getReportMoneySystem(String var1, String var2, boolean var3) throws Exception;

    public Map<String, ReportMoneySystemModel> getReportMoneySystemMySQL(String var1, String var2, boolean var3) throws Exception;

    public HashMap<String, Long> getReportTopGame(String var1, String var2, String var3, boolean var4) throws Exception;

    public Map<String, ReportMoneySystemModel> getReportMoneyUser(String var1, String var2, String var3, boolean var4) throws Exception;

    public Map<String, ReportModel> getListReportModelByDay(String var1, boolean var2) throws Exception;

    public boolean saveLogTotalMoney(ReportTotalMoneyModel var1);

    public List<ReportTotalMoneyModel> getReportTotalMoney(int var1, String var2, String var3);

    public ReportTotalMoneyModel getReportTotalMoneyAtTime(String var1, boolean var2) throws ParseException;

    public boolean saveLogMoneyForReport(String var1, String var2, String var3, ReportModel var4) throws ParseException;

    public boolean saveTopCaoThu(String var1, String var2, long var3);

    public void saveReportMoneyVin(Map<String, ReportModel> var1);

    public Map<String, Long> getTopWinners(String var1, String var2) throws Exception;

    public Map<String, Long> getTopLosers(String var1, String var2) throws Exception;

    public Map<String, Long> getTopAgencyCommission(String var1, String var2) throws Exception;

    public long getTotalCommission(String var1, String var2) throws Exception;
}

