/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.Filter
 *  javax.servlet.FilterChain
 *  javax.servlet.FilterConfig
 *  javax.servlet.ServletException
 *  javax.servlet.ServletRequest
 *  javax.servlet.ServletResponse
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 */
package com.vinplay.pay.server;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServerIPFilter
implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        String clientIp = this.getClientIpAddress(request);
        if (this.isServerIP(clientIp)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            response.setStatus(403);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().println("{\"error\":\"Access denied. This API can only be called from server.\"}");
        }
    }

    public void destroy() {
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getHeader("X-REAL-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            String[] ips = ipAddress.split(",");
            ipAddress = ips[0].trim();
        }
        return ipAddress;
    }

    private boolean isServerIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.equals("127.0.0.1") || ip.equals("localhost") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1");
    }
}

