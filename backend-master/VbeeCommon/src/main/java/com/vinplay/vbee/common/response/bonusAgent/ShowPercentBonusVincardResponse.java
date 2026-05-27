/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.bonusAgent;

import com.vinplay.vbee.common.models.AgentModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class ShowPercentBonusVincardResponse
extends BaseResponseModel {
    private List<AgentModel> listPercentBonus;

    public ShowPercentBonusVincardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<AgentModel> getListPercentBonus() {
        return this.listPercentBonus;
    }

    public void setListPercentBonus(List<AgentModel> listPercentBonus) {
        this.listPercentBonus = listPercentBonus;
    }
}

