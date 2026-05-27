/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dichvuthe.entities;

public class DepositBankModel {
    public String Id;
    public String Nickname;
    public String CreatedAt;
    public String UpdatedAt;
    public String Amount;
    public int Status;
    public String Description;
    public String UserApprove;

    public DepositBankModel(String id, String nickname, int status, String amount) {
        this.Id = id;
        this.Nickname = nickname;
        this.Amount = amount;
        this.Status = status;
    }

    public DepositBankModel(String id, String nickname, String createdAt, String updatedAt, String amount, int status, String description, String userApprove) {
        this.Id = id;
        this.Nickname = nickname;
        this.CreatedAt = createdAt;
        this.UpdatedAt = updatedAt;
        this.Amount = amount;
        this.Status = status;
        this.Description = description;
        this.UserApprove = userApprove;
    }
}

