/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;

public class AgentModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickName;
    private int percentBonusVincard;

    public AgentModel() {
    }

    public AgentModel(String nickName, int percentBonusVincard) {
        this.nickName = nickName;
        this.percentBonusVincard = percentBonusVincard;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getPercentBonusVincard() {
        return this.percentBonusVincard;
    }

    public void setPercentBonusVincard(int percentBonusVincard) {
        this.percentBonusVincard = percentBonusVincard;
    }
}

