/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.giftcode;

import com.vinplay.vbee.common.response.giftcode.GiftcodeFollowFaceValue;
import java.util.ArrayList;
import java.util.List;

public class GiftcodeStatisticObj {
    public String name;
    public List<GiftcodeFollowFaceValue> trans = new ArrayList<GiftcodeFollowFaceValue>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GiftcodeFollowFaceValue> getTrans() {
        return this.trans;
    }

    public void setTrans(List<GiftcodeFollowFaceValue> trans) {
        this.trans = trans;
    }
}

