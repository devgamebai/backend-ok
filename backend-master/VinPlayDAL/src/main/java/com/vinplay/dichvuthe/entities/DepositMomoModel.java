/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dichvuthe.entities;

public class DepositMomoModel {
    public String Id;
    public String Nickname;
    public String CreatedAt;
    public String UpdatedAt;
    public long Amount;
    public int Status;
    public String ReceivedPhoneNumber;
    public String ReceivedName;
    public String SendFromNumber;
    public String Description;
    public String UserApprove;

    public DepositMomoModel(String id, String nickname, String createdAt, String updatedAt, long amount, int status, String receivedPhoneNumber, String receivedName, String sendFromNumber, String description) {
        this.Id = id;
        this.Nickname = nickname;
        this.CreatedAt = createdAt;
        this.UpdatedAt = updatedAt;
        this.Amount = amount;
        this.Status = status;
        this.ReceivedPhoneNumber = receivedPhoneNumber;
        this.ReceivedName = receivedName;
        this.SendFromNumber = sendFromNumber;
        this.Description = description;
    }

    public DepositMomoModel(String id, String nickname, String createdAt, String updatedAt, long amount, int status, String receivedPhoneNumber, String receivedName, String sendFromNumber, String description, String userApprove) {
        this.Id = id;
        this.Nickname = nickname;
        this.CreatedAt = createdAt;
        this.UpdatedAt = updatedAt;
        this.Amount = amount;
        this.Status = status;
        this.ReceivedPhoneNumber = receivedPhoneNumber;
        this.ReceivedName = receivedName;
        this.SendFromNumber = sendFromNumber;
        this.Description = description;
        this.UserApprove = userApprove;
    }

    public DepositMomoModel(String nickname, long amount, String receivedPhoneNumber, String receivedName, String sendFromNumber) {
        this.Nickname = nickname;
        this.Amount = amount;
        this.ReceivedPhoneNumber = receivedPhoneNumber;
        this.ReceivedName = receivedName;
        this.SendFromNumber = sendFromNumber;
    }

    public DepositMomoModel(String id, String nickname, String status, String sendFromNumber) {
        this.Id = id;
        this.Nickname = nickname;
        if (!status.isEmpty()) {
            this.Status = Integer.parseInt(status);
        }
        this.SendFromNumber = sendFromNumber;
    }
}

