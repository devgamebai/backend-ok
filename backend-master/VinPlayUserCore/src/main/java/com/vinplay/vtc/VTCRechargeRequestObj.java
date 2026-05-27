/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtc;

public class VTCRechargeRequestObj {
    private String FunctionName;
    private String CardSerial;
    private String CardCode;
    private String PartnerCode;
    private String CardType;
    private String TransID;
    private String AccountName;
    private String ExtentionData;
    private String Amount;

    public VTCRechargeRequestObj(String functionName, String cardSerial, String cardCode, String partnerCode, String cardType, String transID, String accountName, String extentionData, String amount) {
        this.FunctionName = functionName;
        this.CardSerial = cardSerial;
        this.CardCode = cardCode;
        this.PartnerCode = partnerCode;
        this.CardType = cardType;
        this.TransID = transID;
        this.AccountName = accountName;
        this.ExtentionData = extentionData;
        this.Amount = amount;
    }

    public VTCRechargeRequestObj() {
    }

    public String getFunctionName() {
        return this.FunctionName;
    }

    public void setFunctionName(String functionName) {
        this.FunctionName = functionName;
    }

    public String getCardSerial() {
        return this.CardSerial;
    }

    public void setCardSerial(String cardSerial) {
        this.CardSerial = cardSerial;
    }

    public String getCardCode() {
        return this.CardCode;
    }

    public void setCardCode(String cardCode) {
        this.CardCode = cardCode;
    }

    public String getPartnerCode() {
        return this.PartnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.PartnerCode = partnerCode;
    }

    public String getCardType() {
        return this.CardType;
    }

    public void setCardType(String cardType) {
        this.CardType = cardType;
    }

    public String getTransID() {
        return this.TransID;
    }

    public void setTransID(String transID) {
        this.TransID = transID;
    }

    public String getAccountName() {
        return this.AccountName;
    }

    public void setAccountName(String accountName) {
        this.AccountName = accountName;
    }

    public String getExtentionData() {
        return this.ExtentionData;
    }

    public void setExtentionData(String extentionData) {
        this.ExtentionData = extentionData;
    }

    public String getAmount() {
        return this.Amount;
    }

    public void setAmount(String amount) {
        this.Amount = amount;
    }
}

