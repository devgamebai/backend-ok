/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

import com.vinplay.payment.entities.Config;

public class PaymentConfig {
    private String name;
    private Config config;

    public PaymentConfig(String name, Config config) {
        this.name = name;
        this.config = config;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Config getConfig() {
        return this.config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public PaymentConfig() {
    }

    public String toString() {
        return "PaymentConfig [name=" + this.name + ", config=" + this.config + "]";
    }
}

