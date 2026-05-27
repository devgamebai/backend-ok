/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package game.third.processors.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class BaseResponse
extends BaseResponseModel {
    private Object data;

    public BaseResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public BaseResponse(boolean success, String errorCode, Object data) {
        super(success, errorCode, data);
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"success\":false,\"errorCode\":\"1001\",\"totalData\":\"0\"}";
        }
    }

    public Object getData() {
        return this.data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

