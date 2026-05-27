/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class ServerConfig {
    private int status;
    private String ip;
    private int port;

    public ServerConfig(int status, String ip, int port) {
        this.status = status;
        this.ip = ip;
        this.port = port;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

