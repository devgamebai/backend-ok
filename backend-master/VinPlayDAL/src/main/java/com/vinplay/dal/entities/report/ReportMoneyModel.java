/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.report;

import com.vinplay.dal.entities.report.MoneyInOut;
import com.vinplay.dal.entities.report.ReportMoneySystemModelNew;
import java.util.HashMap;
import java.util.List;

public class ReportMoneyModel {
    public List<ReportMoneySystemModelNew> listReportSlotMiniGame;
    public List<ReportMoneySystemModelNew> listReportGameBai;
    public List<ReportMoneySystemModelNew> listReportGameKhac;
    public List<MoneyInOut> ListUserIn;
    public List<MoneyInOut> ListUserInEvent;
    public List<MoneyInOut> ListUserOut;
    public List<MoneyInOut> ListOther;
    public HashMap<String, Long> UserMoney;
    public HashMap<String, Long> AgentMoney;
    public MoneyInOut AgentMoneyIn;
    public MoneyInOut AgentMoneyOut;

    public ReportMoneyModel(List<ReportMoneySystemModelNew> listReportSlotMiniGame, List<ReportMoneySystemModelNew> listReportGameBai, List<ReportMoneySystemModelNew> listReportGameKhac, List<MoneyInOut> listUserIn, List<MoneyInOut> listUserInEvent, List<MoneyInOut> listUserOut, List<MoneyInOut> listOther) {
        this.listReportSlotMiniGame = listReportSlotMiniGame;
        this.listReportGameBai = listReportGameBai;
        this.listReportGameKhac = listReportGameKhac;
        this.ListUserIn = listUserIn;
        this.ListUserInEvent = listUserInEvent;
        this.ListUserOut = listUserOut;
        this.ListOther = listOther;
    }
}

