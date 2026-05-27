package com.vinplay.vbee.common.models;

import java.io.Serializable;

/**
 * Server-side session record for the agency portal (Next.js sunkr-nextagency).
 * Created by {@code LoginAgentProcessor} (c=9428) on successful login and
 * stored in Hazelcast map {@code agencySessions} keyed by agent id.
 *
 * <p>On every agency API call, backend validates the caller's sessionId
 * against the current value in {@code agencySessions[agentId]}. A fresh
 * login overwrites the value, so the old sessionId is no longer the
 * current one and every subsequent request from the old device fails.
 *
 * <p>SUN-767 — "kill old session when a new one logs in". Unlike the game
 * client path there is no persistent WebSocket, so the kick is lazy: it
 * takes effect on the old device's next HTTP request.
 */
public class AgencySession implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;   // UUID issued at login
    private long   loginAt;     // epoch millis
    private String ip;          // login IP
    private String userAgent;   // browser UA (truncated if too long)

    public AgencySession() {}

    public AgencySession(String sessionId, long loginAt, String ip, String userAgent) {
        this.sessionId = sessionId;
        this.loginAt = loginAt;
        this.ip = ip;
        this.userAgent = userAgent != null && userAgent.length() > 240
                ? userAgent.substring(0, 240)
                : userAgent;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public long getLoginAt() { return loginAt; }
    public void setLoginAt(long loginAt) { this.loginAt = loginAt; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
