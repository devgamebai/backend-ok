/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum Vippoint {
    DA(1, 80, 0),
    DONG(2, 800, 405),
    BAC(3, 4500, 972),
    VANG(4, 8600, 1350),
    BACH_KIM_1(5, 12000, 1890),
    BACH_KIM_2(6, 50000, 2160),
    KIM_CUONG_1(7, 100000, 2430),
    KIM_CUONG_2(8, 200000, 2565),
    KIM_CUONG_3(9, -1, 2700);
    
    private int id;
    private int money;
    private int ratio;

    private Vippoint(int id, int money, int ratio) {
        this.id = id;
        this.money = money;
        this.ratio = ratio;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMoney() {
        return this.money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRatio() {
        return this.ratio;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }
}

