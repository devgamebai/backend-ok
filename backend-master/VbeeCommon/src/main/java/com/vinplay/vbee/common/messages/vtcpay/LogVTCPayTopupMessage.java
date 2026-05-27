/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.vtcpay;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogVTCPayTopupMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String referenceId;
    private String partnerTransId;
    private int userId;
    private String userName;
    private String nickname;
    private int price;
    private long moneyUser;
    private int statusRes;
    private String responseCode;
    private String responseDes;
    private String timeResponse;
    private String timeRequest;

    public LogVTCPayTopupMessage(String referenceId, String partnerTransId, int userId, String userName, String nickname, int price, long moneyUser, int statusRes, String responseCode, String responseDes, String timeRequest, String timeResponse) {
        this.referenceId = referenceId;
        this.partnerTransId = partnerTransId;
        this.userId = userId;
        this.userName = userName;
        this.nickname = nickname;
        this.price = price;
        this.moneyUser = moneyUser;
        this.statusRes = statusRes;
        this.responseCode = responseCode;
        this.responseDes = responseDes;
        this.timeResponse = timeResponse;
        this.timeRequest = timeRequest;
    }

    public LogVTCPayTopupMessage() {
    }

    public String getTimeResponse() {
        return this.timeResponse;
    }

    public void setTimeResponse(String timeResponse) {
        this.timeResponse = timeResponse;
    }

    public String getTimeRequest() {
        return this.timeRequest;
    }

    public void setTimeRequest(String timeRequest) {
        this.timeRequest = timeRequest;
    }

    public long getMoneyUser() {
        return this.moneyUser;
    }

    public void setMoneyUser(long moneyUser) {
        this.moneyUser = moneyUser;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getStatusRes() {
        return this.statusRes;
    }

    public void setStatusRes(int statusRes) {
        this.statusRes = statusRes;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDes() {
        return this.responseDes;
    }

    public void setResponseDes(String responseDes) {
        this.responseDes = responseDes;
    }
}

