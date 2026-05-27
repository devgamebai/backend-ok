/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.shotfish.entites;

import java.io.Serializable;

public class ShotfishConfig
implements Serializable {
    public String urlApi;
    public String agentId;
    public String kindId;
    public String secretKey;
    public String envCode;
    public String prefix;

    public ShotfishConfig() {
    }

    public ShotfishConfig(String urlApi, String agentId, String kindId, String secretKey, String envCode, String prefix) {
        this.urlApi = urlApi;
        this.agentId = agentId;
        this.kindId = kindId;
        this.secretKey = secretKey;
        this.envCode = envCode;
        this.prefix = prefix;
    }
}

