/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import java.util.ArrayList;
import java.util.List;

public class MoneyTotalRechargeByCardReponse {
    public String name;
    public long value;
    public List<MoneyTotalFollowFaceValue> trans = new ArrayList<MoneyTotalFollowFaceValue>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return this.value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public List<MoneyTotalFollowFaceValue> getTrans() {
        return this.trans;
    }

    public void setTrans(List<MoneyTotalFollowFaceValue> trans) {
        this.trans = trans;
    }
}

