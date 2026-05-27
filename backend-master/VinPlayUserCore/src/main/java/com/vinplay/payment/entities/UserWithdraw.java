/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.utils.VinPlayUtils;

public class UserWithdraw {
    public String Id;
    public String Username;
    public long Amount;
    public long AmountReal;
    public String BankAccountNumber;
    public String BankAccountName;
    public String BankName;
    public String CreatedAt;
    public String UpdatedAt;
    public String Status;
    public int Version;
    public String UserProve;

    public UserWithdraw(String id, String username, long amount, String bankAccountNumber, String bankAccountName, String bankName, String createdAt, String updatedAt, String status, int version) {
        this.Id = id;
        this.Username = username;
        this.Amount = amount;
        this.BankAccountNumber = bankAccountNumber;
        this.BankAccountName = bankAccountName;
        this.BankName = bankName;
        this.CreatedAt = createdAt;
        this.UpdatedAt = updatedAt;
        this.Status = status;
        this.Version = version;
    }

    public UserWithdraw(String username, long amount, String bankAccountNumber, String bankAccountName, String bankName) {
        this.Id = String.valueOf(VinPlayUtils.generateTransId());
        this.Username = username;
        this.Amount = amount;
        this.BankAccountNumber = bankAccountNumber;
        this.BankAccountName = bankAccountName;
        this.BankName = bankName;
        this.CreatedAt = VinPlayUtils.getCurrentDateTime();
        this.UpdatedAt = VinPlayUtils.getCurrentDateTime();
        this.Status = "pending";
        this.UserProve = "";
        this.Version = 0;
        try {
            double feeWithdraw = GameCommon.getValueDouble("RATIO_CASHOUT_BANK");
            this.AmountReal = (long)(feeWithdraw * (double)amount);
        }
        catch (Exception e) {
            this.AmountReal = 0L;
        }
    }

    public UserWithdraw(String Id, String username, String bankAccountNumber, String bankAccountName, String bankName, String Status2) {
        this.Id = Id;
        this.Username = username;
        this.BankAccountNumber = bankAccountNumber;
        this.BankAccountName = bankAccountName;
        this.BankName = bankName;
        this.CreatedAt = VinPlayUtils.getCurrentDateTime();
        this.UpdatedAt = VinPlayUtils.getCurrentDateTime();
        this.Status = Status2;
    }

    public UserWithdraw(String id, String username, int amount, int amountReal, String bankAccountNumber, String bankAccountName, String bankName, String createdAt, String updatedAt, String status, int version, String userProve) {
        this.Id = id;
        this.Username = username;
        this.Amount = amount;
        this.AmountReal = amountReal;
        this.BankAccountNumber = bankAccountNumber;
        this.BankAccountName = bankAccountName;
        this.BankName = bankName;
        this.CreatedAt = createdAt;
        this.UpdatedAt = updatedAt;
        this.Status = status;
        this.Version = version;
        this.UserProve = userProve;
    }
}

