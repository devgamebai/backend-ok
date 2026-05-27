/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserBonusModel {
    private Long id;
    private String nick_name;
    private Integer bonus_type;
    private Double amount;
    private String create_date;
    private String ip;
    private String bonusName;

    public UserBonusModel(String nick_name, Integer bonus_type, Double amount, String create_date, String ip, String bonusName) {
        this.nick_name = nick_name;
        this.bonus_type = bonus_type;
        this.amount = amount;
        this.create_date = create_date;
        this.ip = ip;
        this.bonusName = bonusName;
    }

    public String getBonusName() {
        return this.bonusName;
    }

    public void setBonusName(String bonusName) {
        this.bonusName = bonusName;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public Integer getBonus_type() {
        return this.bonus_type;
    }

    public void setBonus_type(Integer bonus_type) {
        this.bonus_type = bonus_type;
    }

    public Double getAmount() {
        return this.amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCreate_date() {
        return this.create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public UserBonusModel(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.nick_name = rs.getString("nick_name");
        this.bonus_type = rs.getInt("bonus_type");
        this.create_date = rs.getString("create_date");
        this.amount = rs.getDouble("amount");
        this.ip = rs.getString("ip");
        this.bonusName = rs.getString("bonus_name");
    }

    public UserBonusModel() {
    }
}

