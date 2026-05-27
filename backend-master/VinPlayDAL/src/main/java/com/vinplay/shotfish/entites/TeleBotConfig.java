/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.shotfish.entites;

import java.io.Serializable;

public class TeleBotConfig
implements Serializable {
    public String nameBot;
    public String secretKey;

    public TeleBotConfig() {
    }

    public TeleBotConfig(String nameBot, String secretKey) {
        this.nameBot = nameBot;
        this.secretKey = secretKey;
    }
}

