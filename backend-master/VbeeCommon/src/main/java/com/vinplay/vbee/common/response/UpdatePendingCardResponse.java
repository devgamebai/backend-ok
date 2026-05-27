/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class UpdatePendingCardResponse
extends BaseResponseModel {
    private RechargeByCardMessage rechargeByCardMessage = new RechargeByCardMessage();

    public UpdatePendingCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public RechargeByCardMessage getRechargeByCardMessage() {
        return this.rechargeByCardMessage;
    }

    public void setRechargeByCardMessage(RechargeByCardMessage rechargeByCardMessage) {
        this.rechargeByCardMessage = rechargeByCardMessage;
    }
}

