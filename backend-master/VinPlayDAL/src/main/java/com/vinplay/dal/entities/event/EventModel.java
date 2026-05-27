/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.event;

import java.util.Date;

public class EventModel {
    private Integer id;
    private String name;
    private Date created_date;
    private Long amount;
    private Date expired_date;

    public EventModel() {
    }

    public EventModel(Integer id, String name, Date created_date, Long amount, Date expired_date) {
        this.id = id;
        this.name = name;
        this.created_date = created_date;
        this.amount = amount;
        this.expired_date = expired_date;
    }

    public EventModel(String name, Date created_date, Long amount, Date expired_date) {
        this.name = name;
        this.created_date = created_date;
        this.amount = amount;
        this.expired_date = expired_date;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated_date() {
        return this.created_date;
    }

    public void setCreated_date(Date created_date) {
        this.created_date = created_date;
    }

    public Long getAmount() {
        return this.amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Date getExpired_date() {
        return this.expired_date;
    }

    public void setExpired_date(Date expired_date) {
        this.expired_date = expired_date;
    }
}

