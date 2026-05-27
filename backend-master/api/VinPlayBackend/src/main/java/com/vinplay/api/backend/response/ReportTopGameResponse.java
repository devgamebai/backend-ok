/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ReportTopGameResponse
extends BaseResponseModel {
    public List<TopCaoThu> topUserWin = new ArrayList<TopCaoThu>();
    public List<TopCaoThu> topUserLost = new ArrayList<TopCaoThu>();
    public List<TopCaoThu> topBotWin = new ArrayList<TopCaoThu>();
    public List<TopCaoThu> topBotLost = new ArrayList<TopCaoThu>();

    public ReportTopGameResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

