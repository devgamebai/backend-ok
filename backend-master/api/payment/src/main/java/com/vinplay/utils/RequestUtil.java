/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.utils;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {
    public static String getIpAddress(HttpServletRequest request) {
        String[] arrayIp;
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        String clientIp = null;
        if (ipAddress != null && !"".equals(ipAddress) && (arrayIp = ipAddress.split(",")).length > 0) {
            clientIp = arrayIp[0].trim();
        }
        return clientIp;
    }
}

