/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.log4j.Logger
 */
package com.vinplay.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.BankOneClick;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.Response;
import com.vinplay.payment.service.PaymentConfigService;
import com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.log4j.Logger;

public class PaymentConfigServiceImpl
implements PaymentConfigService {
    private static final Logger logger = Logger.getLogger((String)"PaymentConfigService");
    public static final String NAME = "payment_config";
    public static final String VERSION = "1.0.0";
    public static final String FLATFORM = "all";

    public static String initPayment() throws IOException {
        StringBuilder fullStr = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(Paths.get("./config/payment.json", new String[0]));){
            String line;
            while ((line = br.readLine()) != null) {
                fullStr.append(line);
            }
        }
        return fullStr.toString();
    }

    @Override
    public List<PaymentConfig> getConfig() {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap configCache = instance.getMap("cacheConfig");
            String configPayment = (String)configCache.get("PAYMENTCONFIGCACHE");
            if (configPayment == null || "".equals(configPayment)) {
                configPayment = PaymentConfigServiceImpl.initPayment();
            }
            Type listType = new TypeToken<List<PaymentConfig>>(){}.getType();
            List paymentConfigs = (List)new Gson().fromJson(configPayment, listType);
            return paymentConfigs;
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    @Override
    public PaymentConfig getConfigByKey(String key) {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap paymentConfigCache = instance.getMap("cacheConfig");
            String value = ((String)paymentConfigCache.get("PAYMENTCONFIGCACHE")).toString();
            Type listType = new TypeToken<List<PaymentConfig>>(){}.getType();
            List paymentConfigs = (List)new Gson().fromJson(value, listType);
            return (PaymentConfig) paymentConfigs.stream().filter(item -> ((PaymentConfig)item).getName().equals(key)).findFirst().orElse(null);
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    @Override
    public Response getConfig(String key) {
        Response res = new Response(1, "");
        try {
            List<PaymentConfig> paymentConfigs = this.getConfig();
            if (paymentConfigs == null || paymentConfigs.size() == 0) {
                res.setData("Config: payment_config not found");
                return res;
            }
            PaymentConfig paymentConfig = paymentConfigs.stream().filter(item -> item.getName().equals(key)).findFirst().orElse(null);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String payTypes = ow.writeValueAsString(paymentConfig.getConfig().getPayType());
            String data = "{\"minMoney\":" + paymentConfig.getConfig().getMinMoney() + "},";
            data = data + "{\"payType\":" + payTypes + "}";
            res.setData(data.toString());
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            res.setData("Get config provider " + key + " failed");
            return res;
        }
    }

    @Override
    public Response getBanks(String key) {
        Response res = new Response(1, "");
        try {
            RechargeOneClickPayServiceImpl rechargeOneClickPayService;
            List<BankOneClick> banks;
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            List<PaymentConfig> paymentConfigs = this.getConfig();
            if (paymentConfigs == null || paymentConfigs.size() == 0) {
                res.setData("Config: payment_config not found");
                return res;
            }
            PaymentConfig paymentConfig = paymentConfigs.stream().filter(item -> item.getName().equals(key)).findFirst().orElse(null);
            List<BankConfig> bankConfigs = paymentConfig.getConfig().getBanks();
            if (key.equals("clickpay") && (banks = (rechargeOneClickPayService = new RechargeOneClickPayServiceImpl()).getListBankSupport()) != null) {
                for (int i = 0; i < banks.size(); ++i) {
                    BankOneClick bankOneClick = banks.get(i);
                    int index = IntStream.range(0, bankConfigs.size()).filter(j -> ((BankConfig)bankConfigs.get(j)).getName().equalsIgnoreCase(bankOneClick.bank_name)).findFirst().orElse(-1);
                    if (index == -1) continue;
                    bankConfigs.get(index).setStatus(1);
                    bankConfigs.get(index).setIsWithdraw(1);
                }
            }
            String json = ow.writeValueAsString(bankConfigs);
            res.setData(json.toString());
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            res.setData("Get banks config provider " + key + " failed");
            return res;
        }
    }

    @Override
    public Response getBankWithdraw(String bankName, Integer isWithdraw) {
        Response res = new Response(1, "");
        try {
            HashMap result = new HashMap();
            List<PaymentConfig> paymentConfigs = this.getConfig();
            for (int i = 0; i < paymentConfigs.size(); ++i) {
                List bankConfigs = paymentConfigs.get(i).getConfig().getBanks().stream().filter(x -> x.getIsWithdraw() == isWithdraw && x.getName().equals(bankName)).collect(Collectors.toList());
                if (bankConfigs.size() <= 0) continue;
                result.put(paymentConfigs.get(i).getName(), bankConfigs);
            }
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(result);
            res.setData(json.toString());
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            res.setData(null);
            return res;
        }
    }
}

