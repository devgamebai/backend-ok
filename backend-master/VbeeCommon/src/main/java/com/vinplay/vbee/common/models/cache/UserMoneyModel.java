/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class UserMoneyModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String nickname;
    private boolean isBot;
    private long vin;
    private long xu;
    private long vinTotal;
    private long xuTotal;
    private long safe;
    private long rechargeMoney;
    private int vippoint;
    private int vippointSave;
    private int moneyVP;

    public UserMoneyModel(int id, String username, String nickname, boolean isBot, long vin, long xu, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int vippointSave, int moneyVP) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.isBot = isBot;
        this.vin = vin;
        this.xu = xu;
        this.vinTotal = vinTotal;
        this.xuTotal = xuTotal;
        this.safe = safe;
        this.rechargeMoney = rechargeMoney;
        this.vippoint = vippoint;
        this.vippointSave = vippointSave;
        this.moneyVP = moneyVP;
    }

    public UserMoneyModel() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isBot() {
        return this.isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public long getVin() {
        return this.vin;
    }

    public void setVin(long vin) {
        this.vin = vin;
    }

    public long getXu() {
        return this.xu;
    }

    public void setXu(long xu) {
        this.xu = xu;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        this.vinTotal = vinTotal;
    }

    public long getXuTotal() {
        return this.xuTotal;
    }

    public void setXuTotal(long xuTotal) {
        this.xuTotal = xuTotal;
    }

    public long getSafe() {
        return this.safe;
    }

    public void setSafe(long safe) {
        this.safe = safe;
    }

    public long getRechargeMoney() {
        return this.rechargeMoney;
    }

    public void setRechargeMoney(long rechargeMoney) {
        this.rechargeMoney = rechargeMoney;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }

    public int getVippointSave() {
        return this.vippointSave;
    }

    public void setVippointSave(int vippointSave) {
        this.vippointSave = vippointSave;
    }

    public int getMoneyVP() {
        return this.moneyVP;
    }

    public void setMoneyVP(int moneyVP) {
        this.moneyVP = moneyVP;
    }

    public long getMoney(String moneyType) {
        return moneyType.equalsIgnoreCase("vin") ? this.getVin() : this.getXu();
    }

    public void setMoney(String moneyType, long money) {
        if (moneyType.equalsIgnoreCase("vin")) {
            this.setVin(money);
        } else {
            this.setXu(money);
        }
    }

    /** See UserModel#getTotalPnl — returns vin_total/xu_total (cumulative P&L), NOT current balance. */
    public long getTotalPnl(String moneyType) {
        return moneyType.equalsIgnoreCase("vin") ? this.getVinTotal() : this.getXuTotal();
    }

    /** See UserModel#setTotalPnl — sets vin_total/xu_total (cumulative P&L), NOT current balance. */
    public void setTotalPnl(String moneyType, long money) {
        if (moneyType.equalsIgnoreCase("vin")) {
            this.setVinTotal(money);
        } else {
            this.setXuTotal(money);
        }
    }
}

