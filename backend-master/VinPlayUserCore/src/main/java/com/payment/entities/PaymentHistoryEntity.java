/*
 * Decompiled with CFR 0.152.
 */
package com.payment.entities;

public class PaymentHistoryEntity {
    private String type;
    private String fid;
    private String request_id;
    private String nick_name;
    private String serial;
    private String code;
    private int cash;
    private int cash_real;
    private int status;
    private String text;
    private int day;
    private int month;
    private int year;
    private long time;
    private int number;

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFid() {
        return this.fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getRequest_id() {
        return this.request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCash() {
        return this.cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public int getCash_real() {
        return this.cash_real;
    }

    public void setCash_real(int cash_real) {
        this.cash_real = cash_real;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
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

    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}

