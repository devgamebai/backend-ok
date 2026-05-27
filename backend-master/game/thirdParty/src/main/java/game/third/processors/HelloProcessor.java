/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.processors;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import game.third.processors.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;

public class HelloProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> params) {
        BaseResponse baseResponse = new BaseResponse(false, "ServerError");
        baseResponse.setData("Hello");
        return baseResponse.toJson();
    }
}

