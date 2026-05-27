/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.payment.entities;

public class MerchantInfo {
    private String merchantId;
    private String merchantKey;
    private String moneyType;
    private double feeToVin;
    private double feeFromVin;
    private long moneyMin;
    private long moneyMax;
    private long moneyLimitSystem;
    private long moneyLimitUser;
    private long moneyInDay;
    private int updateDay;
    private int numAlertInDay;

    public MerchantInfo(String merchantId, String merchantKey, String moneyType, double feeToVin, double feeFromVin, long moneyMin, long moneyMax, long moneyLimitSystem, long moneyLimitUser, long moneyInDay, int updateDay, int numAlertInDay) {
        this.merchantId = merchantId;
        this.merchantKey = merchantKey;
        this.moneyType = moneyType;
        this.feeToVin = feeToVin;
        this.feeFromVin = feeFromVin;
        this.moneyMin = moneyMin;
        this.moneyMax = moneyMax;
        this.moneyLimitSystem = moneyLimitSystem;
        this.moneyLimitUser = moneyLimitUser;
        this.moneyInDay = moneyInDay;
        this.updateDay = updateDay;
        this.numAlertInDay = numAlertInDay;
    }

    public MerchantInfo() {
    }

    public int getNumAlertInDay() {
        return this.numAlertInDay;
    }

    public void setNumAlertInDay(int numAlertInDay) {
        this.numAlertInDay = numAlertInDay;
    }

    public long getMoneyInDay() {
        return this.moneyInDay;
    }

    public void setMoneyInDay(long moneyInDay) {
        this.moneyInDay = moneyInDay;
    }

    public int getUpdateDay() {
        return this.updateDay;
    }

    public void setUpdateDay(int updateDay) {
        this.updateDay = updateDay;
    }

    public String getMerchantId() {
        return this.merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantKey() {
        return this.merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }

    public double getFeeToVin() {
        return this.feeToVin;
    }

    public void setFeeToVin(double feeToVin) {
        this.feeToVin = feeToVin;
    }

    public double getFeeFromVin() {
        return this.feeFromVin;
    }

    public void setFeeFromVin(double feeFromVin) {
        this.feeFromVin = feeFromVin;
    }

    public long getMoneyMin() {
        return this.moneyMin;
    }

    public void setMoneyMin(long moneyMin) {
        this.moneyMin = moneyMin;
    }

    public long getMoneyLimitSystem() {
        return this.moneyLimitSystem;
    }

    public void setMoneyLimitSystem(long moneyLimitSystem) {
        this.moneyLimitSystem = moneyLimitSystem;
    }

    public long getMoneyLimitUser() {
        return this.moneyLimitUser;
    }

    public void setMoneyLimitUser(long moneyLimitUser) {
        this.moneyLimitUser = moneyLimitUser;
    }

    public long getMoneyMax() {
        return this.moneyMax;
    }

    public void setMoneyMax(long moneyMax) {
        this.moneyMax = moneyMax;
    }
}

