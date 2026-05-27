/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package com.vinplay.response;

import com.google.gson.JsonObject;

public class BaseResponse {
    public static final int ErrorParams = 4;
    public static final int ErrorNotFound = 3;
    public static final int ErrorHasProcess = 2;
    public static final int Success = 1;
    private String msg;
    private int success;

    public static BaseResponse New(int success) {
        BaseResponse base = new BaseResponse();
        base.setSuccess(success);
        return base;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getSuccess() {
        return this.success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public String toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", (Number)this.success);
        jsonObject.addProperty("msg", this.msg);
        return jsonObject.toString();
    }
}

