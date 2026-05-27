/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.api.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.api.entities.UserTransaction;

public class GetUserDetailResponse {
    private int code;
    private String message;
    private String nickname;
    private long current_balance;
    private long register_time;
    private long last_login_time;
    private long total_agency_transfer_money;
    private long total_agency_receive_money;
    private long total_card_charge_money;
    private long total_card_exchange_money;
    private long total_transfer_money;
    private long total_receive_money;
    private long total_play_money;
    private long total_win_money;
    private long total_gift_code_money;
    private UserTransaction last_agency_transaction;
    private UserTransaction last_transfer_transaction;
    private UserTransaction last_receive_transaction;
    private long quote;

    public GetUserDetailResponse(int _code, String _message) {
        this.code = _code;
        this.message = _message;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"code\":500,\"message\":\"error\"}";
        }
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getCurrent_balance() {
        return this.current_balance;
    }

    public void setCurrent_balance(long current_balance) {
        this.current_balance = current_balance;
    }

    public long getRegister_time() {
        return this.register_time;
    }

    public void setRegister_time(long register_time) {
        this.register_time = register_time;
    }

    public long getLast_login_time() {
        return this.last_login_time;
    }

    public void setLast_login_time(long last_login_time) {
        this.last_login_time = last_login_time;
    }

    public long getTotal_transfer_money() {
        return this.total_transfer_money;
    }

    public void setTotal_transfer_money(long total_transfer_money) {
        this.total_transfer_money = total_transfer_money;
    }

    public long getTotal_receive_money() {
        return this.total_receive_money;
    }

    public void setTotal_receive_money(long total_receive_money) {
        this.total_receive_money = total_receive_money;
    }

    public long getTotal_play_money() {
        return this.total_play_money;
    }

    public void setTotal_play_money(long total_play_money) {
        this.total_play_money = total_play_money;
    }

    public long getTotal_win_money() {
        return this.total_win_money;
    }

    public void setTotal_win_money(long total_win_money) {
        this.total_win_money = total_win_money;
    }

    public long getTotal_gift_code_money() {
        return this.total_gift_code_money;
    }

    public void setTotal_gift_code_money(long total_gift_code_money) {
        this.total_gift_code_money = total_gift_code_money;
    }

    public UserTransaction getLast_transfer_transaction() {
        return this.last_transfer_transaction;
    }

    public void setLast_transfer_transaction(UserTransaction last_transfer_transaction) {
        this.last_transfer_transaction = last_transfer_transaction;
    }

    public UserTransaction getLast_receive_transaction() {
        return this.last_receive_transaction;
    }

    public void setLast_receive_transaction(UserTransaction last_receive_transaction) {
        this.last_receive_transaction = last_receive_transaction;
    }

    public long getTotal_card_charge_money() {
        return this.total_card_charge_money;
    }

    public void setTotal_card_charge_money(long total_card_charge_money) {
        this.total_card_charge_money = total_card_charge_money;
    }

    public long getTotal_card_exchange_money() {
        return this.total_card_exchange_money;
    }

    public void setTotal_card_exchange_money(long total_card_exchange_money) {
        this.total_card_exchange_money = total_card_exchange_money;
    }

    public UserTransaction getLast_agency_transaction() {
        return this.last_agency_transaction;
    }

    public void setLast_agency_transaction(UserTransaction last_agency_transaction) {
        this.last_agency_transaction = last_agency_transaction;
    }

    public long getTotal_agency_transfer_money() {
        return this.total_agency_transfer_money;
    }

    public void setTotal_agency_transfer_money(long total_agency_transfer_money) {
        this.total_agency_transfer_money = total_agency_transfer_money;
    }

    public long getTotal_agency_receive_money() {
        return this.total_agency_receive_money;
    }

    public void setTotal_agency_receive_money(long total_agency_receive_money) {
        this.total_agency_receive_money = total_agency_receive_money;
    }

    public long getQuote() {
        return this.quote;
    }

    public void setQuote(long quote) {
        this.quote = quote;
    }
}

