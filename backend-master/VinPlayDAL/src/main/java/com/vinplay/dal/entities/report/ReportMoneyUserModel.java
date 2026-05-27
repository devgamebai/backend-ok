/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.report;

import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import java.util.Map;

public class ReportMoneyUserModel {
    public String nickname;
    public boolean isBot;
    public Map<String, ReportMoneySystemModel> actionGame;
    public Map<String, Long> actionOther;
    public long currentMoney;
    public long safeMoney;
    public long totalMoney;
}

