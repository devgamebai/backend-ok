/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Bank
implements Serializable {
    private static final long serialVersionUID = 7192864024306831721L;
    private Long id;
    private String bank_name;
    private String bank_short_name;
    private Integer status;
    private String create_date;
    private String update_date;
    private String code;
    private String logo;
    private String addby;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBank_name() {
        return this.bank_name;
    }

    public void setBank_name(String bank_name) {
        this.bank_name = bank_name;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreate_date() {
        return this.create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public String getUpdate_date() {
        return this.update_date;
    }

    public void setUpdate_date(String update_date) {
        this.update_date = update_date;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLogo() {
        return this.logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAddby() {
        return this.addby;
    }

    public void setAddby(String addby) {
        this.addby = addby;
    }

    public Bank(Long id, String bank_name, String bank_short_name, Integer status, String create_date, String update_date, String code, String logo, String addby) {
        this.id = id;
        this.bank_name = bank_name;
        this.bank_short_name = bank_short_name;
        this.status = status;
        this.create_date = create_date;
        this.update_date = update_date;
        this.code = code;
        this.logo = logo;
        this.addby = addby;
    }

    public Bank(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.bank_name = rs.getString("bank_name").trim();
        this.status = rs.getInt("status");
        this.create_date = rs.getString("create_date");
        this.update_date = rs.getString("update_date");
        this.code = rs.getString("code").trim();
        this.logo = rs.getString("logo");
        this.addby = rs.getString("add_by");
        this.bank_short_name = rs.getString("bank_short_name");
    }

    public Bank() {
    }

    public String getBank_short_name() {
        return this.bank_short_name;
    }

    public void setBank_short_name(String bank_short_name) {
        this.bank_short_name = bank_short_name;
    }
}

