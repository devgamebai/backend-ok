/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class AgentResponse {
    public String fullName;
    public String nickName;
    public String mobile;
    public String address;
    public int id;
    public int parentid;
    public int show;
    public int active;
    public int orderNo;
    public String facebook;
    public int percent;
    public String code;
    public String telegram;
    public String zalo;
    public double commissionRate;
    // Added for agent portal processors (c=9466 GetTotalTransaction + c=9537 ReportGeneral4Agency)
    public String parentNickName;
    public long total_transfer_in;
    public long total_transfer_out;
    public long total_transfer;
    public long balance;
}

