/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

public class I2BResponse {
    private String url;
    private int code;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public I2BResponse(String url, int code) {
        this.url = url;
        this.code = code;
    }
}

