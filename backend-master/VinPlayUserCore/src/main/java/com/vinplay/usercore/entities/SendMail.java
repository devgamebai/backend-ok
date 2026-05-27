/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

public class SendMail {
    public static final int TypeRegister = 1;
    private int id;
    private String title;
    private String message;
    private String extra_data;
    private int type;
    private int status;
    private String created_at;
    private String updated_at;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return this.updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getExtra_data() {
        return this.extra_data;
    }

    public void setExtra_data(String extra_data) {
        this.extra_data = extra_data;
    }
}

