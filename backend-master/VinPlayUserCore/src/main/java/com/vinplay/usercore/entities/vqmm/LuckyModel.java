/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities.vqmm;

import java.util.Map;

public class LuckyModel {
    private Map<String, Integer> slot;
    private Map<String, Integer> vin;
    private Map<String, Integer> xu;

    public LuckyModel() {
    }

    public LuckyModel(Map<String, Integer> slot, Map<String, Integer> vin, Map<String, Integer> xu) {
        this.slot = slot;
        this.vin = vin;
        this.xu = xu;
    }

    public Map<String, Integer> getSlot() {
        return this.slot;
    }

    public void setSlot(Map<String, Integer> slot) {
        this.slot = slot;
    }

    public Map<String, Integer> getVin() {
        return this.vin;
    }

    public void setVin(Map<String, Integer> vin) {
        this.vin = vin;
    }

    public Map<String, Integer> getXu() {
        return this.xu;
    }

    public void setXu(Map<String, Integer> xu) {
        this.xu = xu;
    }
}

