/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import javax.servlet.http.HttpServletRequest;

public class GetGameCommonProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        try {
            return GameCommon.getValueStr((String)"COMMONS");
        }
        catch (KeyNotFoundException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}

