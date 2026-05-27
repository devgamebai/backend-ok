/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

import com.vinplay.vbee.common.models.SpecialGiftCode;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class SpecialGiftCodeResponse
extends BaseResponseModel {
    private SpecialGiftCode giftCode;

    public SpecialGiftCodeResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public SpecialGiftCode getGiftCode() {
        return this.giftCode;
    }

    public void setGiftCode(SpecialGiftCode giftCode) {
        this.giftCode = giftCode;
    }
}

