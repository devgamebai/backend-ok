/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class BrandnameMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String brandname;
    private String message;
    private String receives;
    private String code;

    public BrandnameMessage(String requestId, String brandname, String message, String receives, String code) {
        this.requestId = requestId;
        this.brandname = brandname;
        this.message = message;
        this.receives = receives;
        this.code = code;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getBrandname() {
        return this.brandname;
    }

    public void setBrandname(String brandname) {
        this.brandname = brandname;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReceives() {
        return this.receives;
    }

    public void setReceives(String receives) {
        this.receives = receives;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

