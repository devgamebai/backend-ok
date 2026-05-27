/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.dvt;

import com.vinplay.vbee.common.messages.BaseMessage;

public class RefundFeeAgentMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private long fee1;
    private double ratio1;
    private long fee2;
    private double ratio2;
    private long fee2More;
    private double ratio2More;
    private long fee;
    private String month;
    private int code;
    private String description;
    private long feeVinplayCard;
    private long feeVinCash;
    private int percent;
    private String endTime;
    private String startTime;

    public RefundFeeAgentMessage() {
    }

    public RefundFeeAgentMessage(String nickname, long fee1, double ratio1, long fee2, double ratio2, long fee2More, double ratio2More, long fee, String month, int code, String description, long feeVinplayCard, long feeVinCash, int percent, String endTime, String startTime) {
        this.nickname = nickname;
        this.fee1 = fee1;
        this.ratio1 = ratio1;
        this.fee2 = fee2;
        this.ratio2 = ratio2;
        this.fee2More = fee2More;
        this.ratio2More = ratio2More;
        this.fee = fee;
        this.month = month;
        this.code = code;
        this.description = description;
        this.feeVinplayCard = feeVinplayCard;
        this.feeVinCash = feeVinCash;
        this.percent = percent;
        this.endTime = endTime;
        this.startTime = startTime;
    }

    public RefundFeeAgentMessage(String nickname, long fee1, double ratio1, long fee2, double ratio2, long fee2More, double ratio2More, long fee, String month, int code, String description, long feeVinplayCard, long feeVinCash, int percent) {
        this.nickname = nickname;
        this.fee1 = fee1;
        this.ratio1 = ratio1;
        this.fee2 = fee2;
        this.ratio2 = ratio2;
        this.fee2More = fee2More;
        this.ratio2More = ratio2More;
        this.fee = fee;
        this.month = month;
        this.code = code;
        this.description = description;
        this.feeVinplayCard = feeVinplayCard;
        this.feeVinCash = feeVinCash;
        this.percent = percent;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getFee2More() {
        return this.fee2More;
    }

    public void setFee2More(long fee2More) {
        this.fee2More = fee2More;
    }

    public double getRatio2More() {
        return this.ratio2More;
    }

    public void setRatio2More(double ratio2More) {
        this.ratio2More = ratio2More;
    }

    public int getPercent() {
        return this.percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public long getFeeVinplayCard() {
        return this.feeVinplayCard;
    }

    public void setFeeVinplayCard(long feeVinplayCard) {
        this.feeVinplayCard = feeVinplayCard;
    }

    public long getFeeVinCash() {
        return this.feeVinCash;
    }

    public void setFeeVinCash(long feeVinCash) {
        this.feeVinCash = feeVinCash;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getFee1() {
        return this.fee1;
    }

    public void setFee1(long fee1) {
        this.fee1 = fee1;
    }

    public double getRatio1() {
        return this.ratio1;
    }

    public void setRatio1(double ratio1) {
        this.ratio1 = ratio1;
    }

    public long getFee2() {
        return this.fee2;
    }

    public void setFee2(long fee2) {
        this.fee2 = fee2;
    }

    public double getRatio2() {
        return this.ratio2;
    }

    public void setRatio2(double ratio2) {
        this.ratio2 = ratio2;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getMonth() {
        return this.month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

