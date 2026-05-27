/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonParseException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.JsonMappingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.commons.codec.binary.Hex
 *  org.apache.commons.lang.time.DateUtils
 *  org.apache.http.HttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.impl.client.DefaultHttpClient
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.LogFishDaoImpl;
import com.vinplay.dal.dao.impl.LogFishTransactionDaoImpl;
import com.vinplay.dal.entities.fish.FishGameRecord;
import com.vinplay.dal.entities.fish.FishTransaction;
import com.vinplay.shotfish.entites.ShotfishConfig;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ShotFishUtils {
    private static final Logger logger = Logger.getLogger((String)"api portal");

    public static ORDERSTATUS valueOf(int code) throws IllegalArgumentException {
        return Arrays.stream(ORDERSTATUS.values()).filter(x -> ((ORDERSTATUS)x).code == code).findFirst().orElseThrow(() -> new IllegalArgumentException("unknown code: " + code));
    }

    public static <T extends Enum<T>> T valueOfIgnoreCase(Class<T> enumeration, String code) {
        for (Enum enumValue : (Enum[])enumeration.getEnumConstants()) {
            if (!enumValue.name().equalsIgnoreCase(code)) continue;
            return (T)enumValue;
        }
        throw new IllegalArgumentException(String.format("There is no value with code '%s' in Enum %s", code, enumeration.getName()));
    }

    public static <T extends Enum<T>> T valueOfIgnoreCase(Class<T> enumeration, int code) {
        for (Enum enumValue : (Enum[])enumeration.getEnumConstants()) {
            if (!enumValue.name().equalsIgnoreCase(String.valueOf(code))) continue;
            return (T)enumValue;
        }
        throw new IllegalArgumentException(String.format("There is no value with code '%s' in Enum %s", code, enumeration.getName()));
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static SecretKeySpec createKey(String secret) {
        byte[] data = null;
        if (secret == null) {
            secret = "";
        }
        StringBuffer sb = new StringBuffer(16);
        sb.append(secret);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) {
            sb.setLength(16);
        }
        try {
            data = sb.toString().getBytes();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }

    public static String encrypt(String strToEncrypt, String secret) {
        try {
            SecretKeySpec secretKey = ShotFishUtils.createKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(1, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
        }
        catch (Exception e) {
            logger.debug(e);
            System.out.println("Error while encrypting: " + e.toString());
            return null;
        }
    }

    public static String decrypt(String strToDecrypt, String secret) {
        try {
            SecretKeySpec secretKey = ShotFishUtils.createKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e) {
            logger.debug(e);
            System.out.println("Error while decrypting: " + e.toString());
            return null;
        }
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getHMACSHA1(String value, String key) throws Exception {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(value.getBytes());
            byte[] hexBytes = new Hex().encode(rawHmac);
            return new String(hexBytes, "UTF-8");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getHMACSHA256(String key, String data) throws Exception {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("ASCII"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString((byte[])sha256_HMAC.doFinal(data.getBytes("ASCII")));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static long getCurrentTimeStamp() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        return utc.toEpochSecond();
    }

    public static String getCurrentTime(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "yyyyMMddHHmmssSSS";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }

    public static String getRequest(String url) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        StringBuffer result = new StringBuffer();
        String line = "";
        try {
            HttpResponse response = client.execute((HttpUriRequest)get);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        catch (Exception e) {
            return "";
        }
        return result.toString();
    }

    public static boolean updateBalance(String nickName, Double amount, int direction) throws SQLException {
        MoneyInGameServiceImpl moneyService = new MoneyInGameServiceImpl();
        MoneyResponse moneyResponse = null;
        if (direction == 1) {
            UserServiceImpl userService = new UserServiceImpl();
            UserModel u = userService.getUserByNickName(nickName);
            if (u.getVin() < amount.longValue()) {
                return false;
            }
            moneyResponse = moneyService.updateMoneyGame3rdUser(nickName, amount.longValue() * -1L, "vin", "fish", "FISH_DEPOSIT", "N\u1ea0P TI\u1ec0N FISH", 0L, false);
        } else {
            moneyResponse = moneyService.updateMoneyGame3rdUser(nickName, amount.longValue(), "vin", "fish", "FISH_WITHDRAW", "R\u00daT TI\u1ec0N FISH", 0L, false);
        }
        return moneyResponse != null && "0".equals(moneyResponse.getErrorCode());
    }

    public static BaseResponse<Object> CheckUserInfo(String nickname, String accessToken, Long money, boolean ischeckBalance) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        try {
            UserServiceImpl userService = new UserServiceImpl();
            boolean isToken = userService.isActiveToken(nickname, accessToken);
            if (!isToken) {
                res.setData("Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
                res.setErrorCode("4");
                return res;
            }
            UserModel userModel = null;
            try {
                userModel = userService.getUserByNickName(nickname);
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            if (userModel == null) {
                res.setData("T\u00e0i kho\u1ea3n kh\u00f4ng \u0111\u00fang.");
                res.setErrorCode(String.valueOf(11));
                return res;
            }
            if (ischeckBalance && money > 0L) {
                if (userModel.isBanLogin() || userModel.isBot()) {
                    res.setData("T\u00e0i kho\u1ea3n b\u1ecb kho\u00e1.");
                    res.setErrorCode("13");
                    return res;
                }
                long balance = userModel.getVin();
                if (balance < money) {
                    res.setData("S\u1ed1 d\u01b0 kh\u00f4ng \u0111\u1ee7.");
                    res.setErrorCode("1");
                    return res;
                }
            }
        }
        catch (Exception e) {
            logger.error(("[CHEKUSERINFO FISH] Exception: " + e.getMessage()));
            return res;
        }
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("0");
        res.setMessage("success");
        res.setSuccess(true);
        return res;
    }

    public static ShotfishConfig getConfig() {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap configCache = instance.getMap("cacheConfig");
            String value = ((String)configCache.get("SHOTFISHCONFIGCACHE")).toString();
            Type type = new TypeToken<ShotfishConfig>(){}.getType();
            ShotfishConfig shotfishConfig = (ShotfishConfig)new Gson().fromJson(value, type);
            return shotfishConfig;
        }
        catch (Exception e) {
            logger.error(("[GETCONFIG FISH] Exception: " + e.getMessage()));
            return null;
        }
    }

    public static BaseResponse<Object> LoginGame(String nickname, String accessToken, String clientIP, Long money) {
        BaseResponse<Object> valid = ShotFishUtils.CheckUserInfo(nickname, accessToken, money, true);
        if (!valid.isSuccess()) {
            return valid;
        }
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        LogFishTransactionDaoImpl logFishTransactionDao = new LogFishTransactionDaoImpl();
        FishTransaction fishTransaction = new FishTransaction();
        fishTransaction.setPrefix(config.prefix);
        fishTransaction.setNickname(nickname);
        fishTransaction.setAction("LOGIN");
        fishTransaction.setMoney(money);
        String orderId = config.agentId + ShotFishUtils.getCurrentTime(null) + config.prefix + nickname;
        fishTransaction.setOrderId(orderId);
        CharSequence[] params = new String[]{"s=0", "account=" + config.prefix + nickname, "money=" + money, "orderid=" + orderId, "ip=" + clientIP, "lineCode=" + config.envCode, "KindID=" + config.kindId, "showReturn=0"};
        String param = "";
        param = String.join((CharSequence)"&", params);
        fishTransaction.setParam(param);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[LOGIN FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        fishTransaction.setTimeStamp(timeStamp);
        fishTransaction.setKey(pKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            boolean result;
            fishTransaction.setUrlApi(url);
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                logger.error(("[LOGIN FISH] Error response data: " + data));
                return res;
            }
            if (money > 0L && !(result = ShotFishUtils.updateBalance(nickname, money.doubleValue(), 1))) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                ShotFishUtils.WithDraw(nickname, accessToken, money);
                res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (deposit when login). Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1eed l\u1ea1i l\u1ea7n n\u1eefa ho\u1eb7c li\u00ean h\u1ec7 v\u1edbi b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3.");
                return res;
            }
            fishTransaction.setStatus(ORDERSTATUS.SUCCESS.name());
            logFishTransactionDao.Save(fishTransaction);
            res.setData(jsonObj.getJSONObject("d").getString("url"));
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[LOGIN FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> CheckUserBalance(String nickname) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        CharSequence[] params = new String[]{"s=1", "account=" + config.prefix + nickname};
        String param = "";
        param = String.join((CharSequence)"&", params);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[CHECKUSERBALANCE FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                logger.error(("[CHECKUSERBALANCE FISH] Error response data: " + data));
                return res;
            }
            res.setData(Double.valueOf(jsonObj.getJSONObject("d").getDouble("money")).longValue());
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[CHECKUSERBALANCE FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> CheckOrderStatus(String orderId) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        CharSequence[] params = new String[]{"s=4", "orderid=" + orderId};
        String param = "";
        param = String.join((CharSequence)"&", params);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[CHECKORDERSTATUS FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                logger.error(("[CHECKORDERSTATUS FISH] Error response data: " + data));
                return res;
            }
            ORDERSTATUS status = ShotFishUtils.valueOf(jsonObj.getJSONObject("d").getInt("status"));
            res.setData(jsonObj.getJSONObject("d").toString());
            if (status == ORDERSTATUS.SUCCESS) {
                res.setErrorCode(String.valueOf(status.getCode()));
                res.setMessage("success");
                res.setSuccess(true);
            } else {
                res.setErrorCode(String.valueOf(status.getCode()));
                res.setMessage("error");
                res.setSuccess(false);
            }
            return res;
        }
        catch (Exception e) {
            logger.error(("[CHECKORDERSTATUS FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> CheckUserInGame(String nickname) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        CharSequence[] params = new String[]{"s=10", "account=" + config.prefix + nickname};
        String param = "";
        param = String.join((CharSequence)"&", params);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[CHECKUSERINGAME FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                logger.error(("[CHECKUSERINGAME FISH] Error response data: " + data));
                return res;
            }
            res.setData(jsonObj.getJSONObject("d").getBoolean("status"));
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[CHECKUSERINGAME FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> Deposit(String nickname, String accessToken, Long money) {
        BaseResponse<Object> valid = ShotFishUtils.CheckUserInfo(nickname, accessToken, money, true);
        if (!valid.isSuccess()) {
            return valid;
        }
        valid = ShotFishUtils.CheckUserBalance(nickname);
        if (!valid.isSuccess()) {
            return valid;
        }
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        if (money < 0L) {
            res.setMessage("S\u1ed1 ti\u1ec1n n\u1ea1p kh\u00f4ng \u0111\u00fang");
            return res;
        }
        ShotfishConfig config = ShotFishUtils.getConfig();
        LogFishTransactionDaoImpl logFishTransactionDao = new LogFishTransactionDaoImpl();
        FishTransaction fishTransaction = new FishTransaction();
        fishTransaction.setPrefix(config.prefix);
        fishTransaction.setNickname(nickname);
        fishTransaction.setAction("DEPOSIT");
        fishTransaction.setMoney(money);
        String orderId = config.agentId + ShotFishUtils.getCurrentTime(null) + config.prefix + nickname;
        fishTransaction.setOrderId(orderId);
        CharSequence[] params = new String[]{"s=2", "account=" + config.prefix + nickname, "money=" + money, "orderid=" + orderId};
        String param = "";
        param = String.join((CharSequence)"&", params);
        fishTransaction.setParam(param);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[DEPOSIT FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        fishTransaction.setTimeStamp(timeStamp);
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        fishTransaction.setKey(pKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            fishTransaction.setUrlApi(url);
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                logger.error(("[DEPOSIT FISH] Error response data: " + data));
                return res;
            }
            boolean result = ShotFishUtils.updateBalance(nickname, money.doubleValue(), 1);
            if (!result) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                ShotFishUtils.WithDraw(nickname, accessToken, money);
                res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (deposit). Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1eed l\u1ea1i l\u1ea7n n\u1eefa ho\u1eb7c li\u00ean h\u1ec7 v\u1edbi b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3.");
                return res;
            }
            fishTransaction.setStatus(ORDERSTATUS.SUCCESS.name());
            logFishTransactionDao.Save(fishTransaction);
            res.setData(Double.valueOf(jsonObj.getJSONObject("d").getDouble("money")).longValue());
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[DEPOSIT FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> WithDraw(String nickname, String accessToken, Long money) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        if (money < 0L) {
            res.setMessage("S\u1ed1 ti\u1ec1n n\u1ea1p kh\u00f4ng \u0111\u00fang");
            return res;
        }
        BaseResponse<Object> valid = ShotFishUtils.CheckUserInfo(nickname, accessToken, money, false);
        if (!valid.isSuccess()) {
            return valid;
        }
        valid = ShotFishUtils.CheckUserBalance(nickname);
        if (!valid.isSuccess()) {
            return valid;
        }
        Long moneyReal = 0L;
        try {
            moneyReal = Long.parseLong(valid.getData().toString());
        }
        catch (Exception e) {
            res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (deposit). Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1eed l\u1ea1i l\u1ea7n n\u1eefa ho\u1eb7c li\u00ean h\u1ec7 v\u1edbi b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3.");
            return res;
        }
        if (moneyReal < money) {
            res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (withdraw). Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i qu\u1ef9 v\u00e0 \u0111\u1ea3m b\u1ea3o s\u1ed1 d\u01b0 lu\u00f4n l\u1edbn h\u01a1n s\u1ed1 ti\u1ec1n mu\u1ed1n chuy\u1ec3n");
            return res;
        }
        ShotfishConfig config = ShotFishUtils.getConfig();
        LogFishTransactionDaoImpl logFishTransactionDao = new LogFishTransactionDaoImpl();
        FishTransaction fishTransaction = new FishTransaction();
        fishTransaction.setPrefix(config.prefix);
        fishTransaction.setNickname(nickname);
        fishTransaction.setAction("DEPOSIT");
        fishTransaction.setMoney(money);
        String orderId = config.agentId + ShotFishUtils.getCurrentTime(null) + config.prefix + nickname;
        fishTransaction.setOrderId(orderId);
        CharSequence[] params = new String[]{"s=3", "account=" + config.prefix + nickname, "money=" + money, "orderid=" + orderId};
        String param = "";
        param = String.join((CharSequence)"&", params);
        fishTransaction.setParam(param);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[WITHDRAW FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        fishTransaction.setTimeStamp(timeStamp);
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        fishTransaction.setKey(pKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            fishTransaction.setUrlApi(url);
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                logger.error(("[WITHDRAW FISH] Error response data: " + data));
                res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (withdraw). Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i qu\u1ef9 v\u00e0 \u0111\u1ea3m b\u1ea3o s\u1ed1 d\u01b0 lu\u00f4n l\u1edbn h\u01a1n s\u1ed1 ti\u1ec1n mu\u1ed1n chuy\u1ec3n");
                return res;
            }
            Double balance = 0.0;
            try {
                balance = jsonObj.getJSONObject("d").getDouble("money");
            }
            catch (Exception e) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                logger.error(("[WITHDRAW FISH] Error response data: " + data));
                res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (withdraw). Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i qu\u1ef9 v\u00e0 \u0111\u1ea3m b\u1ea3o s\u1ed1 d\u01b0 lu\u00f4n l\u1edbn h\u01a1n s\u1ed1 ti\u1ec1n mu\u1ed1n chuy\u1ec3n");
                return res;
            }
            boolean result = ShotFishUtils.updateBalance(nickname, money.doubleValue(), 0);
            if (!result) {
                fishTransaction.setStatus(ORDERSTATUS.ERROR.name());
                logFishTransactionDao.Save(fishTransaction);
                ShotFishUtils.Deposit(nickname, accessToken, money);
                res.setMessage("L\u1ed7i chuy\u1ec3n qu\u1ef9 (withdraw). Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1eed l\u1ea1i l\u1ea7n n\u1eefa ho\u1eb7c li\u00ean h\u1ec7 v\u1edbi b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3.");
                return res;
            }
            fishTransaction.setStatus(ORDERSTATUS.SUCCESS.name());
            logFishTransactionDao.Save(fishTransaction);
            HashMap<String, Long> map = new HashMap<String, Long>();
            map.put("Transfer", money);
            map.put("Balance", balance.longValue());
            res.setData(map);
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[WITHDRAW FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> History(String startTime, String endTime) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        CharSequence[] params = new String[]{"s=6", "startTime=" + startTime, "endTime=" + endTime};
        String param = "";
        param = String.join((CharSequence)"&", params);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[HISTORY FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                logger.error(("[HISTORY FISH] Error response data: " + data));
                return res;
            }
            res.setData(jsonObj.getJSONObject("d").getString("list"));
            res.setTotalRecords(jsonObj.getJSONObject("d").getLong("count"));
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[HISTORY FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    private static List<FishGameRecord> convertJsontoListObject(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List result = (List)mapper.readValue(jsonString, (TypeReference)new TypeReference<List<FishGameRecord>>(){});
            return result;
        }
        catch (JsonParseException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
        catch (JsonMappingException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
    }

    private static FishGameRecord convertJsontoObject(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            FishGameRecord result = (FishGameRecord)mapper.readValue(jsonString, (TypeReference)new TypeReference<FishGameRecord>(){});
            return result;
        }
        catch (JsonParseException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
        catch (JsonMappingException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.error(("[CONVERTJSONTOLIST FISH] Error response data: " + jsonString));
            return null;
        }
    }

    public static BaseResponse<Object> synchronizeHistory(String startTime, String endTime) {
        BaseResponse<Object> res = new BaseResponse<Object>();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        ShotfishConfig config = ShotFishUtils.getConfig();
        CharSequence[] params = new String[]{"s=6", "startTime=" + startTime, "endTime=" + endTime};
        String param = "";
        param = String.join((CharSequence)"&", params);
        param = ShotFishUtils.encrypt(param, config.secretKey);
        try {
            param = URLEncoder.encode(param, "UTF-8");
        }
        catch (UnsupportedEncodingException e1) {
            logger.error(("[HISTORY FISH] Error encrypt param: " + e1.getMessage()));
            e1.printStackTrace();
            return res;
        }
        Long timeStamp = ShotFishUtils.getCurrentTimeStamp();
        String pKey = ShotFishUtils.getMd5(config.agentId + String.valueOf(timeStamp) + config.secretKey);
        String url = config.urlApi + "agent=" + config.agentId + "&timestamp=" + timeStamp + "&param=" + param + "&key=" + pKey;
        try {
            String data = ShotFishUtils.getRequest(url);
            JSONObject jsonObj = new JSONObject(data);
            int code = jsonObj.getJSONObject("d").getInt("code");
            if (code != 0) {
                logger.error(("[HISTORY FISH] Error response data: " + data));
                return res;
            }
            String json = "";
            json = jsonObj.getJSONObject("d").getString("list");
            List fishGameRecords = new ArrayList();
            fishGameRecords = ShotFishUtils.convertJsontoListObject(json);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            int countSuccess = 0;
            if (fishGameRecords != null) {
                LogFishDaoImpl logFishDao = new LogFishDaoImpl();
                for (Object _fgr : fishGameRecords) {
                    FishGameRecord fishGameRecord = (FishGameRecord) _fgr;
                    Date endTimeChange = simpleDateFormat.parse(fishGameRecord.getEndtime());
                    endTimeChange = DateUtils.addHours((Date)new Date(), (int)-1);
                    fishGameRecord.setEndtime(simpleDateFormat.format(endTimeChange));
                    if (logFishDao.findItem(fishGameRecord.getId(), fishGameRecord.getRoomid(), fishGameRecord.getGid(), fishGameRecord.getMuid(), fishGameRecord.getEndtime()) == null) {
                        logFishDao.insert(fishGameRecord);
                        ++countSuccess;
                        LogMoneyUserMessage message = new LogMoneyUserMessage(0, fishGameRecord.getMuid().replace(config.prefix, ""), "FISH", Games.SHOT_FISH.getId() + "", 0L, -fishGameRecord.getBetcoin().longValue(), "vin", "", 0L, false, false);
                        RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                        taskReportUser.start();
                        RMQPublishTask taskExtra1 = new RMQPublishTask(message, "queue_log_money_extra", 1001);
                        taskExtra1.start();
                        if (fishGameRecord.getCoin().longValue() <= 0L) continue;
                        LogMoneyUserMessage message2 = new LogMoneyUserMessage(0, fishGameRecord.getMuid().replace(config.prefix, ""), "FISH", Games.SHOT_FISH.getId() + "", 0L, Math.abs(fishGameRecord.getCoin().longValue()), "vin", "", 0L, false, false);
                        RMQPublishTask taskReportUser2 = new RMQPublishTask(message2, "queue_log_report_user_balance", 602);
                        taskReportUser2.start();
                        RMQPublishTask taskExtra2 = new RMQPublishTask(message2, "queue_log_money_extra", 1001);
                        taskExtra2.start();
                        continue;
                    }
                    logFishDao.update(fishGameRecord);
                    ++countSuccess;
                }
            }
            int totalRecord = 0;
            totalRecord = jsonObj.getJSONObject("d").getInt("count");
            HashMap<String, Integer> map = new HashMap<String, Integer>();
            map.put("countSuccess", countSuccess);
            map.put("countError", totalRecord - countSuccess);
            res.setData(map);
            res.setTotalRecords(totalRecord);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[HISTORY FISH] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> synchronizeHistory() {
        LogFishDaoImpl logFishDao = new LogFishDaoImpl();
        String startTime = String.valueOf(logFishDao.getLastUpdateTime());
        String endTime = String.valueOf(DateUtils.addHours((Date)new Date(), (int)1).getTime());
        return ShotFishUtils.synchronizeHistory(startTime, endTime);
    }

    public static enum ORDERSTATUS {
        NOT_EXIT(2, "Not exist"),
        SUCCESS(0, "Success"),
        ERROR(-1, "Error");

        private int code;
        private String description;

        private ORDERSTATUS(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        public int getCode() {
            return this.code;
        }
    }
}

