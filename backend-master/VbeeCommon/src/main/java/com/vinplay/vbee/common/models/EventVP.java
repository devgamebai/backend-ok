/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;
import java.util.Date;

public class EventVP
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private Date startTime;
    private Date endTime;
    private String description;
    private int status;

    public EventVP(int id, String name, Date startTime, Date endTime, String description, int status) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.status = status;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

