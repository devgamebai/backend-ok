/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 */
package com.vinplay.api.processors.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class BasePortalResponse {
    public static final int Error = 1;
    public static final int Success = 0;
    private String msg;
    private int status;
    private Object data;

    public static BasePortalResponse New(int status) {
        BasePortalResponse base = new BasePortalResponse();
        base.setStatus(status);
        return base;
    }

    public static BasePortalResponse New(int status, String msg) {
        BasePortalResponse base = new BasePortalResponse();
        base.setStatus(status);
        base.setMsg(msg);
        return base;
    }

    public static BasePortalResponse Success(int status, Object data) {
        BasePortalResponse base = new BasePortalResponse();
        base.setStatus(status);
        base.setData(data);
        return base;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("status", (Number)this.status);
        json.addProperty("msg", this.msg);
        if (this.data != null) {
            if (this.data instanceof String) {
                json.addProperty("data", (String)this.data);
            } else {
                json.add("data", new Gson().toJsonTree(this.data));
            }
        }
        return json.toString();
    }

    public void setData(Object data) {
        this.data = data;
    }
}

