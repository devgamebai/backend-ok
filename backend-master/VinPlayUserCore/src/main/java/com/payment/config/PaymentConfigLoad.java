/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.vinplay.usercore.dao.impl.GameConfigDaoImpl
 *  org.apache.log4j.Logger
 */
package com.payment.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.payment.config.BanMayTinhConfig;
import com.payment.config.Config;
import com.payment.config.MPay247Config;
import com.payment.config.OneVnPayConfig;
import com.payment.config.PaymentConfig;
import com.payment.config.SieuTocConfig;
import com.payment.config.TheSieuTocConfig;
import com.payment.provider.Provider;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import java.lang.reflect.Type;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class PaymentConfigLoad {
    private static SieuTocConfig sieuTocConfig;
    private static OneVnPayConfig oneVnPayConfig;
    private static TheSieuTocConfig theSieuTocConfig;
    private static PaymentConfig paymentConfig;
    private static BanMayTinhConfig banMayTinhConfig;
    private static MPay247Config mPay247Config;
    private static final Logger logger;

    public static PaymentConfig getPaymentConfig() {
        if (paymentConfig == null) {
            GameConfigDaoImpl gameConfigDao = new GameConfigDaoImpl();
            try {
                String jsonInput = gameConfigDao.getGameCommon("payment_provider");
                logger.debug(("jsonInput: " + jsonInput));
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Config.class, new ConfigDeserializer());
                Gson gson = gsonBuilder.create();
                paymentConfig = (PaymentConfig)gson.fromJson(jsonInput, PaymentConfig.class);
            }
            catch (Exception e) {
                e.printStackTrace();
                paymentConfig = new PaymentConfig();
                paymentConfig.setDefault_provider_bank("default");
                paymentConfig.setDefault_provider_card("default");
                paymentConfig.setDefault_provider_bank_out("default");
            }
        }
        return paymentConfig;
    }

    public static SieuTocConfig getSieuTocConfig() {
        if (sieuTocConfig == null) {
            try {
                Config cfg = PaymentConfigLoad.getPaymentConfig().getProvider("SieuToc");
                if (cfg == null) {
                    return null;
                }
                sieuTocConfig = (SieuTocConfig)new Gson().fromJson(cfg.getConfig(), SieuTocConfig.class);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sieuTocConfig;
    }

    public static OneVnPayConfig getOneVnPayConfig() {
        if (oneVnPayConfig == null) {
            try {
                Config cfg = PaymentConfigLoad.getPaymentConfig().getProvider("OneVnPay");
                if (cfg == null) {
                    return null;
                }
                oneVnPayConfig = (OneVnPayConfig)new Gson().fromJson(cfg.getConfig(), OneVnPayConfig.class);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return oneVnPayConfig;
    }

    public static BanMayTinhConfig getBanMayTinhConfig() {
        logger.debug("getBanMayTinhConfig: ");
        if (banMayTinhConfig == null) {
            try {
                Config cfg = PaymentConfigLoad.getPaymentConfig().getProvider("BanMayTinh");
                if (cfg == null) {
                    logger.debug("getBanMayTinhConfig1: NULL  000");
                    return null;
                }
                logger.debug(("getBanMayTinhConfig2: " + cfg.getConfig()));
                banMayTinhConfig = (BanMayTinhConfig)new Gson().fromJson(cfg.getConfig(), BanMayTinhConfig.class);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug(("getBanMayTinhConfig3: " + e.getMessage()));
            }
        }
        return banMayTinhConfig;
    }

    public static TheSieuTocConfig getTheSieuTocConfig() {
        if (theSieuTocConfig == null) {
            try {
                Config cfg = PaymentConfigLoad.getPaymentConfig().getProvider("TheSieuToc");
                if (cfg == null) {
                    return null;
                }
                theSieuTocConfig = (TheSieuTocConfig)new Gson().fromJson(cfg.getConfig(), TheSieuTocConfig.class);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return theSieuTocConfig;
    }

    public static MPay247Config getMPay247Config() {
        logger.debug("getMPay247Config: ");
        if (mPay247Config == null) {
            try {
                Config cfg = PaymentConfigLoad.getPaymentConfig().getProvider("MPay247");
                if (cfg == null) {
                    logger.debug("getMPay247Config1: NULL  000");
                    return null;
                }
                logger.debug(("getMPay247Config2: " + cfg.getConfig()));
                mPay247Config = (MPay247Config)new Gson().fromJson(cfg.getConfig(), MPay247Config.class);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug(("getMPay247Config3: " + e.getMessage()));
            }
        }
        return mPay247Config;
    }

    static {
        logger = Logger.getLogger(Provider.class);
    }

    static class ConfigDeserializer
    implements JsonDeserializer<Config> {
        ConfigDeserializer() {
        }

        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Config config = new Config();
            config.setKey(jsonObject.get("key").getAsString());
            config.setName(jsonObject.get("name").getAsString());
            config.setEnable(jsonObject.get("enable").getAsBoolean());
            JsonArray availableArray = jsonObject.getAsJsonArray("available");
            ArrayList<String> availableList = new ArrayList<String>();
            for (JsonElement element : availableArray) {
                availableList.add(element.getAsString());
            }
            config.setAvailable(availableList);
            JsonElement configElement = jsonObject.get("config");
            if (configElement != null) {
                if (configElement.isJsonObject()) {
                    config.setConfig(configElement.toString());
                } else {
                    config.setConfig(configElement.getAsString());
                }
            } else {
                config.setConfig("");
            }
            return config;
        }
    }
}

