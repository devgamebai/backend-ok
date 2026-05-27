/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.ResultGameConfigResponse;
import java.util.ArrayList;
import java.util.List;

public class GameConfigResponse
extends BaseResponseModel {
    private List<ResultGameConfigResponse> gameconfig = new ArrayList<ResultGameConfigResponse>();

    public GameConfigResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<ResultGameConfigResponse> getGameconfig() {
        return this.gameconfig;
    }

    public void setGameconfig(List<ResultGameConfigResponse> gameconfig) {
        this.gameconfig = gameconfig;
    }
}

