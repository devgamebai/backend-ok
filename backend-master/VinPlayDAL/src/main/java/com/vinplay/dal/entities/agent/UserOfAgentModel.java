/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.io.Serializable;
import java.util.Date;

public class UserOfAgentModel
implements Serializable {
    private Integer id;
    private String nick_name;
    private Date create_time;
    private Long balance;
    private Long total_nap;
    private Long total_rut;
    private Long total_bonus;
    private Long doanh_thu;

    public UserOfAgentModel() {
    }

    public UserOfAgentModel(Integer id, String nick_name, Date create_time, Long balance, Long total_nap, Long total_rut, Long total_bonus, Long doanh_thu) {
        this.id = id;
        this.nick_name = nick_name;
        this.create_time = create_time;
        this.balance = balance;
        this.total_nap = total_nap;
        this.total_rut = total_rut;
        this.total_bonus = total_bonus;
        this.doanh_thu = doanh_thu;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Long getBalance() {
        return this.balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getTotal_nap() {
        return this.total_nap;
    }

    public void setTotal_nap(Long total_nap) {
        this.total_nap = total_nap;
    }

    public Long getTotal_rut() {
        return this.total_rut;
    }

    public void setTotal_rut(Long total_rut) {
        this.total_rut = total_rut;
    }

    public Long getTotal_bonus() {
        return this.total_bonus;
    }

    public void setTotal_bonus(Long total_bonus) {
        this.total_bonus = total_bonus;
    }

    public Long getDoanh_thu() {
        return this.doanh_thu;
    }

    public void setDoanh_thu(Long doanh_thu) {
        this.doanh_thu = doanh_thu;
    }
}

