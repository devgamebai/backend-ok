/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.util.Date;

public class OtpModel {
    private String id;
    private String mobile;
    private String otp;
    private Date otpTime;
    private String commandCode;
    private String type;
    private String sender;
    private String nickname;
    private int count;

    public OtpModel() {
    }

    public OtpModel(String mobile, String otp, Date otpTime, String commandCode) {
        this.mobile = mobile;
        this.otp = otp;
        this.otpTime = otpTime;
        this.commandCode = commandCode;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOtp() {
        return this.otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public Date getOtpTime() {
        return this.otpTime;
    }

    public void setOtpTime(Date otpTime) {
        this.otpTime = otpTime;
    }

    public String getCommandCode() {
        return this.commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = commandCode;
    }

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return this.type; }
    public void setType(String type) { this.type = type; }
    public String getSender() { return this.sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getNickname() { return this.nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getCount() { return this.count; }
    public void setCount(int count) { this.count = count; }
}

