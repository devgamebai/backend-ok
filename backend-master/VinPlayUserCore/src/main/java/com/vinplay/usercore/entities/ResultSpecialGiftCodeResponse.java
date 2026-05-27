/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

import com.vinplay.vbee.common.models.SpecialGiftCode;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultSpecialGiftCodeResponse
extends BaseResponseModel {
    private List<SpecialGiftCode> giftCodes = new ArrayList<SpecialGiftCode>();

    public ResultSpecialGiftCodeResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<SpecialGiftCode> getGiftCodes() {
        return this.giftCodes;
    }

    public void setGiftCodes(List<SpecialGiftCode> giftCodes) {
        this.giftCodes = giftCodes;
    }
}

