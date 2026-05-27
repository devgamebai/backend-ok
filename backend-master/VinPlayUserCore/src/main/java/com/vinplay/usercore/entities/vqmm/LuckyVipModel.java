/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities.vqmm;

import java.util.Map;

public class LuckyVipModel {
    private Map<Integer, Integer> vin;
    private Map<Integer, Integer> multi;
    private int numRotate;

    public LuckyVipModel(Map<Integer, Integer> vin, Map<Integer, Integer> multi, int numRotate) {
        this.vin = vin;
        this.multi = multi;
        this.numRotate = numRotate;
    }

    public LuckyVipModel() {
    }

    public Map<Integer, Integer> getVin() {
        return this.vin;
    }

    public void setVin(Map<Integer, Integer> vin) {
        this.vin = vin;
    }

    public Map<Integer, Integer> getMulti() {
        return this.multi;
    }

    public void setMulti(Map<Integer, Integer> multi) {
        this.multi = multi;
    }

    public int getNumRotate() {
        return this.numRotate;
    }

    public void setNumRotate(int numRotate) {
        this.numRotate = numRotate;
    }
}

