/*
 * Decompiled with CFR 0.152.
 */
package com.payment.config;

import com.payment.config.Config;
import java.util.ArrayList;
import java.util.List;

public class PaymentConfig {
    private String default_provider_bank;
    private String default_provider_card;
    private String default_provider_bank_out;
    private Integer min_amount = 20000;
    private List<Config> providers = new ArrayList<Config>();

    public String getDefault_provider_bank() {
        return this.default_provider_bank;
    }

    public void setDefault_provider_bank(String default_provider_bank) {
        this.default_provider_bank = default_provider_bank;
    }

    public String getDefault_provider_card() {
        return this.default_provider_card;
    }

    public void setDefault_provider_card(String default_provider_card) {
        this.default_provider_card = default_provider_card;
    }

    public String getDefault_provider_bank_out() {
        return this.default_provider_bank_out;
    }

    public void setDefault_provider_bank_out(String default_provider_bank_out) {
        this.default_provider_bank_out = default_provider_bank_out;
    }

    public List<Config> getProviders() {
        return this.providers;
    }

    public void setProviders(List<Config> providers) {
        this.providers = providers;
    }

    public Config getProvider(String key) {
        for (Config provider : this.providers) {
            if (!provider.getKey().equals(key)) continue;
            return provider;
        }
        return null;
    }

    public Integer getMin_amount() {
        return this.min_amount;
    }

    public void setMin_amount(Integer min_amount) {
        this.min_amount = min_amount;
    }
}

