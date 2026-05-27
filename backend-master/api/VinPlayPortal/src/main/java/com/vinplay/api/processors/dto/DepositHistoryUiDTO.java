/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.processors.dto;

public class DepositHistoryUiDTO {
    private String Id;
    private String CreatedAt;
    private String Nickname;
    private String PaymentType;
    private long Amount;
    private long AmountFee;
    private String BankCode;
    private int Status;
    private String BankAccountNumber;
    private String BankAccountName;
    private String Description;

    public String getId() {
        return this.Id;
    }

    public void setId(String id) {
        this.Id = id;
    }

    public String getCreatedAt() {
        return this.CreatedAt;
    }

    public void setCreatedAt(String createdAt) {
        this.CreatedAt = createdAt;
    }

    public String getNickname() {
        return this.Nickname;
    }

    public void setNickname(String nickname) {
        this.Nickname = nickname;
    }

    public String getPaymentType() {
        return this.PaymentType;
    }

    public void setPaymentType(String paymentType) {
        this.PaymentType = paymentType;
    }

    public long getAmount() {
        return this.Amount;
    }

    public void setAmount(long amount) {
        this.Amount = amount;
    }

    public long getAmountFee() {
        return this.AmountFee;
    }

    public void setAmountFee(long amountFee) {
        this.AmountFee = amountFee;
    }

    public String getBankCode() {
        return this.BankCode;
    }

    public void setBankCode(String bankCode) {
        this.BankCode = bankCode;
    }

    public int getStatus() {
        return this.Status;
    }

    public void setStatus(int status) {
        this.Status = status;
    }

    public String getBankAccountNumber() {
        return this.BankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.BankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountName() {
        return this.BankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.BankAccountName = bankAccountName;
    }

    public String getDescription() {
        return this.Description;
    }

    public void setDescription(String description) {
        this.Description = description;
    }
}

