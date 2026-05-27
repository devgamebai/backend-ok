/*
 * Decompiled with CFR 0.152.
 */
package com.payment.entities;

import java.time.ZoneId;
import java.util.Date;

public class HistoryApplyForEntity {
    public static final int StatusInProgress = 1;
    public static final int StatusSuccess = 2;
    public static final int StatusError = 3;
    private int id;
    private String fid;
    private String keyId;
    private String nickName;
    private long cash;
    private long cashReal;
    private String type;
    private String text;
    private int status;
    private int day;
    private int month;
    private int year;
    private long time;
    private long cashBack;

    public HistoryApplyForEntity() {
    }

    public HistoryApplyForEntity(String request_id) {
        this.keyId = request_id;
        Date date = new Date();
        this.day = date.toInstant().atZone(ZoneId.systemDefault()).getDayOfMonth();
        this.month = date.toInstant().atZone(ZoneId.systemDefault()).getMonthValue();
        this.year = date.toInstant().atZone(ZoneId.systemDefault()).getYear();
        this.time = date.getTime() / 1000L;
        this.status = 1;
        this.text = "";
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFid() {
        return this.fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getKeyId() {
        return this.keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getCash() {
        return this.cash;
    }

    public void setCash(long cash) {
        this.cash = cash;
    }

    public long getCashReal() {
        return this.cashReal;
    }

    public void setCashReal(long cashReal) {
        this.cashReal = cashReal;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setDay(byte day) {
        this.day = day;
    }

    public void setMonth(byte month) {
        this.month = month;
    }

    public void setYear(short year) {
        this.year = year;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getDay() {
        return this.day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return this.month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getCashBack() {
        return this.cashBack;
    }

    public void setCashBack(long cashBack) {
        this.cashBack = cashBack;
    }
}

