/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.entities;

import java.io.Serializable;
import java.util.Date;

public class EventMission
implements Serializable {
    private static final long serialVersionUID = -1235632346L;
    private int id;
    private String name;
    private String content;
    private int status;
    private boolean show;
    private Date expiredAt;
    private Date createdAt;
    private Date updatedAt;

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

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isShow() {
        return this.show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public Date getExpiredAt() {
        return this.expiredAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isExpired() {
        return this.expiredAt.before(new Date());
    }

    public boolean isHidden() {
        return !this.show;
    }
}

