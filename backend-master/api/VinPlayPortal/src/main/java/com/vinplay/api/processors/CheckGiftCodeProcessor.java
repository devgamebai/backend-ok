/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.GiftCodeDAOImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.api.processors;

import com.gamebase.dao.impl.GiftCodeDAOImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class CheckGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gc = request.getParameter("gc");
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        JSONObject giftcode = dao.GetGiftCode(gc);
        return giftcode.toString();
    }
}

