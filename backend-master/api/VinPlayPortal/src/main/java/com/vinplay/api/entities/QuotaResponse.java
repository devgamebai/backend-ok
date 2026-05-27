/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.entities;

public class QuotaResponse {
    private int code;
    private long total_giftcode_money;
    private long total_user_receive;
    private long total_agency_receive;
    private long total_recharge_card_money;
    private long total_user_transfer;
    private long total_agency_transfer;
    private long total_bet_money;
    private long total_win_money;

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getTotal_giftcode_money() {
        return this.total_giftcode_money;
    }

    public void setTotal_giftcode_money(long total_giftcode_money) {
        this.total_giftcode_money = total_giftcode_money;
    }

    public long getTotal_user_receive() {
        return this.total_user_receive;
    }

    public void setTotal_user_receive(long total_user_receive) {
        this.total_user_receive = total_user_receive;
    }

    public long getTotal_agency_receive() {
        return this.total_agency_receive;
    }

    public void setTotal_agency_receive(long total_agency_receive) {
        this.total_agency_receive = total_agency_receive;
    }

    public long getTotal_recharge_card_money() {
        return this.total_recharge_card_money;
    }

    public void setTotal_recharge_card_money(long total_recharge_card_money) {
        this.total_recharge_card_money = total_recharge_card_money;
    }

    public long getTotal_user_transfer() {
        return this.total_user_transfer;
    }

    public void setTotal_user_transfer(long total_user_transfer) {
        this.total_user_transfer = total_user_transfer;
    }

    public long getTotal_agency_transfer() {
        return this.total_agency_transfer;
    }

    public void setTotal_agency_transfer(long total_agency_transfer) {
        this.total_agency_transfer = total_agency_transfer;
    }

    public long getTotal_bet_money() {
        return this.total_bet_money;
    }

    public void setTotal_bet_money(long total_bet_money) {
        this.total_bet_money = total_bet_money;
    }

    public long getTotal_win_money() {
        return this.total_win_money;
    }

    public void setTotal_win_money(long total_win_money) {
        this.total_win_money = total_win_money;
    }
}

