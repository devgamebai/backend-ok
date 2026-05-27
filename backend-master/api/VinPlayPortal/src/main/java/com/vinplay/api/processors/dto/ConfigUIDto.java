/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 *  com.vinplay.payment.entities.BankConfig
 *  com.vinplay.payment.entities.Config
 *  com.vinplay.payment.entities.PayType
 */
package com.vinplay.api.processors.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.PayType;
import java.util.List;

public class ConfigUIDto {
    private String currencyCode;
    private List<PayType> payType;
    private List<BankConfig> banks;
    private Integer minMoney;
    private Integer status;

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public ConfigUIDto(Config config) {
        this.currencyCode = config.getCurrencyCode();
        this.payType = config.getPayType();
        this.banks = config.getBanks();
        this.minMoney = config.getMinMoney();
        this.status = config.getStatus();
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public List<PayType> getPayType() {
        return this.payType;
    }

    public void setPayType(List<PayType> payType) {
        this.payType = payType;
    }

    public List<BankConfig> getBanks() {
        return this.banks;
    }

    public void setBanks(List<BankConfig> banks) {
        this.banks = banks;
    }

    public Integer getMinMoney() {
        return this.minMoney;
    }

    public void setMinMoney(Integer minMoney) {
        this.minMoney = minMoney;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public ConfigUIDto() {
    }

    public ConfigUIDto(String currencyCode, List<PayType> payType, List<BankConfig> banks, Integer minMoney, Integer status) {
        this.currencyCode = currencyCode;
        this.payType = payType;
        this.banks = banks;
        this.minMoney = minMoney;
        this.status = status;
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

