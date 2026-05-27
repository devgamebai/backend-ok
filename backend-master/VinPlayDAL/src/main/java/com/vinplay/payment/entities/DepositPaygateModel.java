/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

public class DepositPaygateModel {
    public String Id;
    public String CreatedAt;
    public String ModifiedAt;
    public Boolean IsDeleted;
    public String CartId;
    public String ReferenceId;
    public String UserId;
    public String Username;
    public String Nickname;
    public String RequestTime;
    public String BankCode;
    public String BankName;
    public String PaymentType;
    public String MerchantCode;
    public String ProviderName;
    public long Amount;
    public long AmountFee;
    public int Status;
    public String BankAccountNumber;
    public String BankAccountName;
    public String Description;
    public String UserApprove;
    public String AgentBankCode;
    public String AgentBankAccountNumber;
    public String AgentBankAccountName;
    public String Content;

    public String getPayTypeStr() {
        switch (this.ProviderName) {
            case "paywell": {
                if ("IB_ONLINE".equals(this.PaymentType)) {
                    return "ONLINE";
                }
                return "OFFLINE";
            }
            case "royalpay": {
                if ("IB_ONLINE".equals(this.PaymentType)) {
                    return "ONLINE";
                }
                return "OFFLINE";
            }
            case "clickpay": {
                if ("IB_ONLINE".equals(this.PaymentType)) {
                    return "ONLINE";
                }
                return "OFFLINE";
            }
            case "princepay": {
                if ("907".equals(this.PaymentType)) {
                    return "ONLINE";
                }
                if (!"908".equals(this.PaymentType)) break;
                return "OFFLINE";
            }
            default: {
                return "";
            }
        }
        return "";
    }

    public DepositPaygateModel() {
    }

    public DepositPaygateModel(String id, String createdAt, String modifiedAt, Boolean isDeleted, String cartId, String referenceId, String userId, String username, String nickname, String requestTime, String bankCode, String bankName, String paymentType, String merchantCode, String providerName, long amount, long amountFee, int status, String bankAccountNumber, String bankAccountName, String description, String userApprove) {
        this.Id = id;
        this.CreatedAt = createdAt;
        this.ModifiedAt = modifiedAt;
        this.IsDeleted = isDeleted;
        this.CartId = cartId;
        this.ReferenceId = referenceId;
        this.UserId = userId;
        this.Username = username;
        this.Nickname = nickname;
        this.RequestTime = requestTime;
        this.BankName = bankName;
        this.BankCode = bankCode;
        this.ProviderName = providerName;
        this.PaymentType = paymentType;
        this.MerchantCode = merchantCode;
        this.Amount = amount;
        this.AmountFee = amountFee;
        this.Status = status;
        this.BankAccountNumber = bankAccountNumber;
        this.BankAccountName = bankAccountName;
        this.Description = description;
        this.UserApprove = userApprove;
    }

    public DepositPaygateModel(String cartId, String nickname, String requestTime, String bankCode, String merchantCode, String providerName, int status) {
        this.CartId = cartId;
        this.Nickname = nickname;
        this.RequestTime = requestTime;
        this.BankCode = bankCode;
        this.MerchantCode = merchantCode;
        this.ProviderName = providerName;
        this.Status = status;
    }
}

