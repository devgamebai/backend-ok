/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

public class UserFish {
    public int Id;
    public String Username;
    public String Nickname;
    public String Password;
    public int Active;
    public int Type;
    public long Cash;
    public long CashSafe;
    public long CashSilver;
    public long VipPoint;
    public String PhoneNumber;

    public UserFish(int id, String username, String nickname, int active, long cash) {
        this.Id = id;
        this.Username = username;
        this.Nickname = nickname;
        this.Active = active;
        this.Cash = cash;
    }

    public UserFish() {
    }

    public void setId(int id) {
        this.Id = id;
    }

    public void setUsername(String username) {
        this.Username = username;
    }

    public void setNickname(String nickname) {
        this.Nickname = nickname;
    }

    public void setPassword(String password) {
        this.Password = password;
    }

    public void setActive(int active) {
        this.Active = active;
    }

    public void setType(int type) {
        this.Type = type;
    }

    public void setCash(long cash) {
        this.Cash = cash;
    }

    public void setCashSafe(long cashSafe) {
        this.CashSafe = cashSafe;
    }

    public void setCashSilver(long cashSilver) {
        this.CashSilver = cashSilver;
    }

    public void setVipPoint(long vipPoint) {
        this.VipPoint = vipPoint;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.PhoneNumber = phoneNumber;
    }

    public int getId() {
        return this.Id;
    }

    public String getUsername() {
        return this.Username;
    }

    public String getNickname() {
        return this.Nickname;
    }

    public String getPassword() {
        return this.Password;
    }

    public int getActive() {
        return this.Active;
    }

    public int getType() {
        return this.Type;
    }

    public long getCash() {
        return this.Cash;
    }

    public long getCashSafe() {
        return this.CashSafe;
    }

    public long getCashSilver() {
        return this.CashSilver;
    }

    public long getVipPoint() {
        return this.VipPoint;
    }

    public String getPhoneNumber() {
        return this.PhoneNumber;
    }
}

