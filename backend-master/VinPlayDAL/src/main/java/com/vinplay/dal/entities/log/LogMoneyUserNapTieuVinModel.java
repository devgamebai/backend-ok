/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.log;

import java.io.Serializable;
import java.util.Date;

public class LogMoneyUserNapTieuVinModel
implements Serializable {
    private Long trans_id;
    private Integer user_id;
    private String nick_name;
    private String service_name;
    private Long current_money;
    private Long money_exchange;
    private String description;
    private String trans_time;
    private String action_name;
    private Long fee;
    private Date create_time;

    public LogMoneyUserNapTieuVinModel() {
    }

    public LogMoneyUserNapTieuVinModel(Long trans_id, Integer user_id, String nick_name, String service_name, Long current_money, Long money_exchange, String description, String trans_time, String action_name, Long fee, Date create_time) {
        this.trans_id = trans_id;
        this.user_id = user_id;
        this.nick_name = nick_name;
        this.service_name = service_name;
        this.current_money = current_money;
        this.money_exchange = money_exchange;
        this.description = description;
        this.trans_time = trans_time;
        this.action_name = action_name;
        this.fee = fee;
        this.create_time = create_time;
    }

    public Long getTrans_id() {
        return this.trans_id;
    }

    public void setTrans_id(Long trans_id) {
        this.trans_id = trans_id;
    }

    public Integer getUser_id() {
        return this.user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getService_name() {
        return this.service_name;
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public Long getCurrent_money() {
        return this.current_money;
    }

    public void setCurrent_money(Long current_money) {
        this.current_money = current_money;
    }

    public Long getMoney_exchange() {
        return this.money_exchange;
    }

    public void setMoney_exchange(Long money_exchange) {
        this.money_exchange = money_exchange;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTrans_time() {
        return this.trans_time;
    }

    public void setTrans_time(String trans_time) {
        this.trans_time = trans_time;
    }

    public String getAction_name() {
        return this.action_name;
    }

    public void setAction_name(String action_name) {
        this.action_name = action_name;
    }

    public Long getFee() {
        return this.fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public Date getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}

