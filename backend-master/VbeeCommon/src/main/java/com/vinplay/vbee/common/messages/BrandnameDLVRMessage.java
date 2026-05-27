/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class BrandnameDLVRMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private int smsStatus;
    private int count;
    private String statusDesc;
    private String sentDate;

    public BrandnameDLVRMessage(String requestId, int smsStatus, int count, String statusDesc, String sentDate) {
        this.requestId = requestId;
        this.smsStatus = smsStatus;
        this.count = count;
        this.statusDesc = statusDesc;
        this.sentDate = sentDate;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getSmsStatus() {
        return this.smsStatus;
    }

    public void setSmsStatus(int smsStatus) {
        this.smsStatus = smsStatus;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getStatusDesc() {
        return this.statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getSentDate() {
        return this.sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }
}

