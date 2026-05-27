/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.core.hook.AbstractHookProcessor
 *  com.payment.core.hook.Param
 *  com.payment.model.Code
 *  com.payment.model.Result
 *  com.payment.service.impl.ProviderServiceImpl
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.hook.callback;

import com.payment.core.hook.AbstractHookProcessor;
import com.payment.core.hook.Param;
import com.payment.model.Code;
import com.payment.model.Result;
import com.payment.service.impl.ProviderServiceImpl;
import javax.servlet.http.HttpServletRequest;

public class CallbackCardIn
extends AbstractHookProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> params) {
        try {
            Result result = ProviderServiceImpl.getInstance().hookCardIn("default", params);
            if (result.getCode() == Code.SUCCESS) {
                params.setStatus(200);
            } else {
                params.setStatus(400);
            }
            if (result.getDataRaw() == null || result.getDataRaw().isEmpty()) {
                return (String)result.getData();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}

