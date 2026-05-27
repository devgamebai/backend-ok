/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 */
package com.vinplay.api.processors.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vinplay.api.processors.dto.ConfigUIDto;
import java.io.Serializable;

public class PaymentConfigUiDto
implements Serializable {
    private String providerName;
    private ConfigUIDto providerConfig;

    public String getProviderName() {
        return this.providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public ConfigUIDto getProviderConfig() {
        return this.providerConfig;
    }

    public void setProviderConfig(ConfigUIDto providerConfig) {
        this.providerConfig = providerConfig;
    }

    public PaymentConfigUiDto(String providerName, ConfigUIDto providerConfig) {
        this.providerName = providerName;
        this.providerConfig = providerConfig;
    }

    public PaymentConfigUiDto() {
    }

    public String toString() {
        ObjectWriter ow = new ObjectMapper().writer();
        ow.with(SerializationFeature.INDENT_OUTPUT);
        try {
            String json = ow.writeValueAsString(this);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

