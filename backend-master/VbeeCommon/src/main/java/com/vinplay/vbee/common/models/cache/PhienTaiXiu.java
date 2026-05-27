/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.cache.PotTaiXiu;

public class PhienTaiXiu {
    private long referenceId;
    private short moneyType;
    private PotTaiXiu potTai = new PotTaiXiu();
    private PotTaiXiu potXiu = new PotTaiXiu();

    public long getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(long referenceId) {
        this.referenceId = referenceId;
    }

    public short getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(short moneyType) {
        this.moneyType = moneyType;
    }

    public PotTaiXiu getPotTai() {
        return this.potTai;
    }

    public void setPotTai(PotTaiXiu potTai) {
        this.potTai = potTai;
    }

    public PotTaiXiu getPotXiu() {
        return this.potXiu;
    }

    public void setPotXiu(PotTaiXiu potXiu) {
        this.potXiu = potXiu;
    }
}

