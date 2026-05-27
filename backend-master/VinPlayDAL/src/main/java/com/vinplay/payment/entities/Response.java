/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 */
package com.vinplay.payment.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Response {
    private String message;
    private int code;
    private String data;

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.setMessage(code == 0 ? "success" : "failed");
        this.code = code;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static Response error(int code, String message) {
        return new Response(message, code, null);
    }

    public Response(String message, int code, String data) {
        this.message = message;
        this.code = code;
        this.data = data;
    }

    public Response(String message, int code) {
        this.message = message;
        this.code = code;
        this.data = null;
    }

    public Response(int code, String data) {
        this.message = code == 1 ? "failed" : "success";
        this.code = code;
        this.data = data;
    }

    public String toJson() {
        ObjectWriter ow = new ObjectMapper().writer();
        ow.with(SerializationFeature.INDENT_OUTPUT);
        try {
            String json = ow.writeValueAsString(this);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

