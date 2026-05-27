package com.vinplay.api.backend.response;

import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportTXModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.Map;

public class ReportMoneySystemResponse
extends BaseResponseModel {
    public ReportTXModel taiXiu;
    public ReportTXModel taiXiuBot;
    public Map<String, ReportMoneySystemModel> actionGame;
    public Map<String, Long> vinInUser;
    public Map<String, Long> vinInEvent;
    public long totalInUser;
    public long totalInEvent;
    public long totalIn;
    public Map<String, Long> vinOutUser;
    public Map<String, Long> vinOutAgent;
    public long totalOutUser;
    public long totalOutAgent;
    public long totalOut;
    public double ratioCashout;
    public Map<String, Long> vinOther;
    public Map<String, Long> user;
    public Map<String, ReportMoneySystemModel> actionGameBot;
    public Map<String, Long> bot;
    public DashboardModel newDashboard;

    public ReportMoneySystemResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public ReportMoneySystemResponse(boolean success, String errorCode, ReportTXModel taiXiu, ReportTXModel taiXiuBot, Map<String, ReportMoneySystemModel> actionGame, Map<String, Long> vinInUser, Map<String, Long> vinInEvent, long totalInUser, long totalInEvent, long totalIn, Map<String, Long> vinOutUser, Map<String, Long> vinOutAgent, long totalOutUser, long totalOutAgent, long totalOut, double ratioCashout, Map<String, Long> vinOther, Map<String, Long> user, Map<String, ReportMoneySystemModel> actionGameBot, Map<String, Long> bot) {
        super(success, errorCode);
        this.taiXiu = taiXiu;
        this.taiXiuBot = taiXiuBot;
        this.actionGame = actionGame;
        this.vinInUser = vinInUser;
        this.vinInEvent = vinInEvent;
        this.totalInUser = totalInUser;
        this.totalInEvent = totalInEvent;
        this.totalIn = totalIn;
        this.vinOutUser = vinOutUser;
        this.vinOutAgent = vinOutAgent;
        this.totalOutUser = totalOutUser;
        this.totalOutAgent = totalOutAgent;
        this.totalOut = totalOut;
        this.ratioCashout = ratioCashout;
        this.vinOther = vinOther;
        this.user = user;
        this.actionGameBot = actionGameBot;
        this.bot = bot;
    }

    public ReportMoneySystemResponse(boolean success, String errorCode, ReportTXModel taiXiu, ReportTXModel taiXiuBot, Map<String, ReportMoneySystemModel> actionGame, Map<String, Long> vinInUser, Map<String, Long> vinInEvent, long totalInUser, long totalInEvent, long totalIn, Map<String, Long> vinOutUser, Map<String, Long> vinOutAgent, long totalOutUser, long totalOutAgent, long totalOut, double ratioCashout, Map<String, Long> vinOther, Map<String, Long> user, Map<String, ReportMoneySystemModel> actionGameBot, Map<String, Long> bot, DashboardModel newDashboard) {
        super(success, errorCode);
        this.taiXiu = taiXiu;
        this.taiXiuBot = taiXiuBot;
        this.actionGame = actionGame;
        this.vinInUser = vinInUser;
        this.vinInEvent = vinInEvent;
        this.totalInUser = totalInUser;
        this.totalInEvent = totalInEvent;
        this.totalIn = totalIn;
        this.vinOutUser = vinOutUser;
        this.vinOutAgent = vinOutAgent;
        this.totalOutUser = totalOutUser;
        this.totalOutAgent = totalOutAgent;
        this.totalOut = totalOut;
        this.ratioCashout = ratioCashout;
        this.vinOther = vinOther;
        this.user = user;
        this.actionGameBot = actionGameBot;
        this.bot = bot;
        this.newDashboard = newDashboard;
    }
}
