/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gamebase.service.impl.BannerServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.banner;

import com.gamebase.service.impl.BannerServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ShowListBannerProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            BannerServiceImpl service = new BannerServiceImpl();
            List banners = service.getListActive();
            return BaseResponse.success(banners, (long)banners.size());
        }
        catch (Exception e) {
            return BaseResponse.error((String)"-1", (String)e.getMessage());
        }
    }
}

