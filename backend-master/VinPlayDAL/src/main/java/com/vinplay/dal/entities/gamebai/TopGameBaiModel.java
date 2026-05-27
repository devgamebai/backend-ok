/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.dal.entities.gamebai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TopGameBaiModel {
    private String nickName;
    private String avatar;
    private int winCount;
    private int winCountToday;
    private int winCountThisWeek;
    private int winCountThisMonth;
    private int winCountThisYear;
    private int lostCount;
    private int lostCountToday;
    private int lostCountThisWeek;
    private int lostCountThisMonth;
    private int lostCountThisYear;
    private long moneyWin;
    private long moneyWinToday;
    private long moneyWinThisWeek;
    private long moneyWinThisMonth;
    private long moneyWinThisYear;
    private long moneyLost;
    private long moneyLostToday;
    private long moneyLostThisWeek;
    private long moneyLostThisMonth;
    private long moneyLostThisYear;
    private int lastDay;
    private int lastWeek;
    private int lastMonth;
    private int lastYear;

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }

    public TopGameBaiModel() {
    }

    public TopGameBaiModel(String nickName, String avatar, int winCount, int winCountToday, int winCountThisWeek, int winCountThisMonth, int winCountThisYear, int lostCount, int lostCountToday, int lostCountThisWeek, int lostCountThisMonth, int lostCountThisYear, long moneyWin, long moneyWinToday, long moneyWinThisWeek, long moneyWinThisMonth, long moneyWinThisYear, long moneyLost, long moneyLostToday, long moneyLostThisWeek, long moneyLostThisMonth, long moneyLostThisYear, int lastDay, int lastWeek, int lastMonth, int lastYear) {
        this.nickName = nickName;
        this.avatar = avatar;
        this.winCount = winCount;
        this.winCountToday = winCountToday;
        this.winCountThisWeek = winCountThisWeek;
        this.winCountThisMonth = winCountThisMonth;
        this.winCountThisYear = winCountThisYear;
        this.lostCount = lostCount;
        this.lostCountToday = lostCountToday;
        this.lostCountThisWeek = lostCountThisWeek;
        this.lostCountThisMonth = lostCountThisMonth;
        this.lostCountThisYear = lostCountThisYear;
        this.moneyWin = moneyWin;
        this.moneyWinToday = moneyWinToday;
        this.moneyWinThisWeek = moneyWinThisWeek;
        this.moneyWinThisMonth = moneyWinThisMonth;
        this.moneyWinThisYear = moneyWinThisYear;
        this.moneyLost = moneyLost;
        this.moneyLostToday = moneyLostToday;
        this.moneyLostThisWeek = moneyLostThisWeek;
        this.moneyLostThisMonth = moneyLostThisMonth;
        this.moneyLostThisYear = moneyLostThisYear;
        this.lastDay = lastDay;
        this.lastWeek = lastWeek;
        this.lastMonth = lastMonth;
        this.lastYear = lastYear;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getWinCount() {
        return this.winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getWinCountToday() {
        return this.winCountToday;
    }

    public void setWinCountToday(int winCountToday) {
        this.winCountToday = winCountToday;
    }

    public int getWinCountThisWeek() {
        return this.winCountThisWeek;
    }

    public void setWinCountThisWeek(int winCountThisWeek) {
        this.winCountThisWeek = winCountThisWeek;
    }

    public int getWinCountThisMonth() {
        return this.winCountThisMonth;
    }

    public void setWinCountThisMonth(int winCountThisMonth) {
        this.winCountThisMonth = winCountThisMonth;
    }

    public int getWinCountThisYear() {
        return this.winCountThisYear;
    }

    public void setWinCountThisYear(int winCountThisYear) {
        this.winCountThisYear = winCountThisYear;
    }

    public int getLostCount() {
        return this.lostCount;
    }

    public void setLostCount(int lostCount) {
        this.lostCount = lostCount;
    }

    public int getLostCountToday() {
        return this.lostCountToday;
    }

    public void setLostCountToday(int lostCountToday) {
        this.lostCountToday = lostCountToday;
    }

    public int getLostCountThisWeek() {
        return this.lostCountThisWeek;
    }

    public void setLostCountThisWeek(int lostCountThisWeek) {
        this.lostCountThisWeek = lostCountThisWeek;
    }

    public int getLostCountThisMonth() {
        return this.lostCountThisMonth;
    }

    public void setLostCountThisMonth(int lostCountThisMonth) {
        this.lostCountThisMonth = lostCountThisMonth;
    }

    public int getLostCountThisYear() {
        return this.lostCountThisYear;
    }

    public void setLostCountThisYear(int lostCountThisYear) {
        this.lostCountThisYear = lostCountThisYear;
    }

    public long getMoneyWin() {
        return this.moneyWin;
    }

    public void setMoneyWin(long moneyWin) {
        this.moneyWin = moneyWin;
    }

    public long getMoneyWinToday() {
        return this.moneyWinToday;
    }

    public void setMoneyWinToday(long moneyWinToday) {
        this.moneyWinToday = moneyWinToday;
    }

    public long getMoneyWinThisWeek() {
        return this.moneyWinThisWeek;
    }

    public void setMoneyWinThisWeek(long moneyWinThisWeek) {
        this.moneyWinThisWeek = moneyWinThisWeek;
    }

    public long getMoneyWinThisMonth() {
        return this.moneyWinThisMonth;
    }

    public void setMoneyWinThisMonth(long moneyWinThisMonth) {
        this.moneyWinThisMonth = moneyWinThisMonth;
    }

    public long getMoneyWinThisYear() {
        return this.moneyWinThisYear;
    }

    public void setMoneyWinThisYear(long moneyWinThisYear) {
        this.moneyWinThisYear = moneyWinThisYear;
    }

    public long getMoneyLost() {
        return this.moneyLost;
    }

    public void setMoneyLost(long moneyLost) {
        this.moneyLost = moneyLost;
    }

    public long getMoneyLostToday() {
        return this.moneyLostToday;
    }

    public void setMoneyLostToday(long moneyLostToday) {
        this.moneyLostToday = moneyLostToday;
    }

    public long getMoneyLostThisWeek() {
        return this.moneyLostThisWeek;
    }

    public void setMoneyLostThisWeek(long moneyLostThisWeek) {
        this.moneyLostThisWeek = moneyLostThisWeek;
    }

    public long getMoneyLostThisMonth() {
        return this.moneyLostThisMonth;
    }

    public void setMoneyLostThisMonth(long moneyLostThisMonth) {
        this.moneyLostThisMonth = moneyLostThisMonth;
    }

    public long getMoneyLostThisYear() {
        return this.moneyLostThisYear;
    }

    public void setMoneyLostThisYear(long moneyLostThisYear) {
        this.moneyLostThisYear = moneyLostThisYear;
    }

    public int getLastDay() {
        return this.lastDay;
    }

    public void setLastDay(int lastDay) {
        this.lastDay = lastDay;
    }

    public int getLastWeek() {
        return this.lastWeek;
    }

    public void setLastWeek(int lastWeek) {
        this.lastWeek = lastWeek;
    }

    public int getLastMonth() {
        return this.lastMonth;
    }

    public void setLastMonth(int lastMonth) {
        this.lastMonth = lastMonth;
    }

    public int getLastYear() {
        return this.lastYear;
    }

    public void setLastYear(int lastYear) {
        this.lastYear = lastYear;
    }
}

