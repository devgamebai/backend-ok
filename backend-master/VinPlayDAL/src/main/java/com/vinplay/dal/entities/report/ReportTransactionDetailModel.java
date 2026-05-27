/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.report;

import java.io.Serializable;
import java.util.Date;

public class ReportTransactionDetailModel
implements Serializable {
    private static final long serialVersionUID = -6144824116891162639L;
    private String action_name;
    private String date;
    private String nick_name;
    private Date create_time;
    private Long fee;
    private Long money_lost;
    private Long money_other;
    private Long money_win;
    private String time_log;

    public ReportTransactionDetailModel() {
    }

    public ReportTransactionDetailModel(String action_name, String date, String nick_name, Date create_time, Long fee, Long money_lost, Long money_other, Long money_win, String time_log) {
        this.action_name = action_name;
        this.date = date;
        this.nick_name = nick_name;
        this.create_time = create_time;
        this.fee = fee;
        this.money_lost = money_lost;
        this.money_other = money_other;
        this.money_win = money_win;
        this.time_log = time_log;
    }

    public String getAction_name() {
        return this.action_name;
    }

    public void setAction_name(String action_name) {
        this.action_name = action_name;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public Date getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public Long getFee() {
        return this.fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public Long getMoney_lost() {
        return this.money_lost;
    }

    public void setMoney_lost(Long money_lost) {
        this.money_lost = money_lost;
    }

    public Long getMoney_other() {
        return this.money_other;
    }

    public void setMoney_other(Long money_other) {
        this.money_other = money_other;
    }

    public Long getMoney_win() {
        return this.money_win;
    }

    public void setMoney_win(Long money_win) {
        this.money_win = money_win;
    }

    public String getTime_log() {
        return this.time_log;
    }

    public void setTime_log(String time_log) {
        this.time_log = time_log;
    }
}

