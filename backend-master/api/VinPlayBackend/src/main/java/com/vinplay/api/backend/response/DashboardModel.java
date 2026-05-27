package com.vinplay.api.backend.response;

import java.util.List;

public class DashboardModel {
    public int newUsers;
    public long totalDeposit;
    public long totalWithdraw;
    public long totalCommission;
    public long profit;
    public List<TopUserModel> top10Win;
    public List<TopUserModel> top10Loss;
    public List<TopUserModel> top10AgencyCommission;

    public DashboardModel(int newUsers, long totalDeposit, long totalWithdraw, long totalCommission, long profit,
            List<TopUserModel> top10Win, List<TopUserModel> top10Loss, List<TopUserModel> top10AgencyCommission) {
        this.newUsers = newUsers;
        this.totalDeposit = totalDeposit;
        this.totalWithdraw = totalWithdraw;
        this.totalCommission = totalCommission;
        this.profit = profit;
        this.top10Win = top10Win;
        this.top10Loss = top10Loss;
        this.top10AgencyCommission = top10AgencyCommission;
    }
}
