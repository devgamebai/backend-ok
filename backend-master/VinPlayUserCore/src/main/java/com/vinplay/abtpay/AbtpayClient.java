/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Hex
 *  org.apache.http.conn.ssl.X509HostnameVerifier
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.abtpay;

import com.vinplay.abtpay.AbtBuyCardResponse;
import com.vinplay.abtpay.AbtChargeResponse;
import com.vinplay.abtpay.AbtException;
import com.vinplay.abtpay.AbtRecheckResponse;
import com.vinplay.abtpay.AbtTopupResponse;
import com.vinplay.usercore.utils.GameCommon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class AbtpayClient {
    private String merchantId;
    private String secretKey;

    public AbtpayClient(String merchantId, String secretKey) {
        this.merchantId = merchantId;
        this.secretKey = secretKey;
    }

    public void installMyPolicy() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext sc = SSLContext.getInstance("SSL");
        X509HostnameVerifier verifier = new X509HostnameVerifier(){

            public void verify(String host, SSLSocket ssl) throws IOException {
            }

            public void verify(String host, X509Certificate cert) throws SSLException {
            }

            public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            }

            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((HostnameVerifier)verifier);
    }

    public void installAllTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){

                @Override
                public boolean verify(String urlHostname, SSLSession _session) {
                    return true;
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AbtBuyCardResponse buyCard(String transactionId, String cardType, String amount) throws Exception {
        this.installAllTrustManager();
        cardType = this.mapProvider(cardType);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_id", this.merchantId);
        map.put("merchant_txn_id", transactionId);
        map.put("card_type", cardType);
        map.put("card_amount", amount);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("ABT_TOP_URL") + "/card/get?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + transactionId + "&card_type=" + cardType + "&card_amount=" + amount);
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(30000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.mapBuyCardResponse(json);
    }

    public AbtTopupResponse topupTelco(String transactionId, String phoneNum, String amount) throws Exception {
        this.installAllTrustManager();
        String cardType = AbtpayClient.mapProviderByNumber(phoneNum);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_id", this.merchantId);
        map.put("merchant_txn_id", transactionId);
        map.put("card_type", cardType);
        map.put("card_amount", amount);
        map.put("account", phoneNum);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("ABT_TOP_URL") + "/topup/get?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + transactionId + "&card_type=" + cardType + "&card_amount=" + amount + "&account=" + phoneNum);
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(30000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.mapTopupResponse(json);
    }

    public AbtChargeResponse doCharge(String transactionId, String cardType, String pin, String seri) throws Exception {
        this.installAllTrustManager();
        cardType = this.mapProvider(cardType);
        this.doValidate(cardType, pin, seri);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_id", this.merchantId);
        map.put("pin", pin);
        map.put("seri", seri);
        map.put("card_type", cardType);
        map.put("merchant_txn_id", transactionId);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("ABT_PAY_URL") + "/charge?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + transactionId + "&card_type=" + cardType + "&pin=" + pin + "&seri=" + seri + "&format=json");
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(30000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.mapToResponse(json);
    }

    public AbtRecheckResponse doCheck(String txid) throws Exception {
        this.installAllTrustManager();
        this.validateCheckParam(txid);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_id", this.merchantId);
        map.put("merchant_txn_id", txid);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("ABT_PAY_URL") + "/recheck?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + txid + "&format=json");
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(30000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.mapToCheckResponse(json);
    }

    private AbtRecheckResponse mapToCheckResponse(JSONObject sample) {
        String code = sample.get("code") != null ? sample.get("code").toString() : "";
        String response_code = sample.get("response_code") != null ? sample.get("response_code").toString() : "";
        String response_message = sample.get("message") != null ? sample.get("message").toString() : "";
        String strAmount = sample.get("card_amount") != null ? sample.get("card_amount").toString() : "0";
        String strResponseAmount = sample.get("net_amount") != null ? sample.get("net_amount").toString() : "0";
        double amount = Double.parseDouble(strAmount);
        double responseAmount = Double.parseDouble(strResponseAmount);
        return new AbtRecheckResponse(response_code, response_message, code, amount, responseAmount);
    }

    private AbtBuyCardResponse mapBuyCardResponse(JSONObject data) throws Exception {
        AbtBuyCardResponse response = new AbtBuyCardResponse();
        String code = data.get("code") != null ? data.get("code").toString() : "";
        response.setCode(code);
        String card_amount = data.get("card_amount") != null ? data.get("card_amount").toString() : "";
        response.setCard_amount(Integer.parseInt(card_amount));
        String expired_at = data.get("expired_at") != null ? data.get("expired_at").toString() : "";
        response.setExpired_at(expired_at);
        String pin = data.get("pin") != null ? data.get("pin").toString() : "";
        response.setPin(AbtpayClient.decrypt(pin, this.secretKey));
        String seri = data.get("seri") != null ? data.get("seri").toString() : "";
        response.setSeri(AbtpayClient.decrypt(seri, this.secretKey));
        String txn_id = data.get("txn_id") != null ? data.get("txn_id").toString() : "";
        response.setTxn_id(txn_id);
        String checksum = data.get("checksum") != null ? data.get("checksum").toString() : "";
        response.setChecksum(checksum);
        return response;
    }

    private static String decrypt(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("TripleDES");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(key.getBytes(), 0, key.length());
        String keymd5 = new BigInteger(1, md5.digest()).toString(16).substring(0, 24);
        SecretKeySpec keyspec = new SecretKeySpec(keymd5.getBytes(), "TripleDES");
        cipher.init(2, keyspec);
        byte[] raw = Base64.getDecoder().decode(data);
        byte[] stringBytes = cipher.doFinal(raw);
        String result = new String(stringBytes);
        return result;
    }

    private AbtTopupResponse mapTopupResponse(JSONObject data) {
        AbtTopupResponse response = new AbtTopupResponse();
        String code = data.get("code") != null ? data.get("code").toString() : "";
        response.setCode(code);
        String message = data.get("message") != null ? data.get("message").toString() : "";
        response.setMessage(message);
        String txn_id = data.get("txn_id") != null ? data.get("txn_id").toString() : "";
        response.setTxn_id(txn_id);
        return response;
    }

    private AbtChargeResponse mapToResponse(JSONObject sample) {
        AbtChargeResponse response = new AbtChargeResponse();
        String code = sample.get("code") != null ? sample.get("code").toString() : "";
        response.setStatus(code);
        String message = sample.get("message") != null ? sample.get("message").toString() : "";
        response.setMessage(message);
        String txn_id = sample.get("txn_id") != null ? sample.get("txn_id").toString() : "";
        response.setTransactionId(txn_id);
        String strAmount = sample.get("card_amount") != null ? sample.get("card_amount").toString() : "0";
        double amount = Double.parseDouble(strAmount);
        response.setAmount(amount);
        String strNetAmount = sample.get("net_amount") != null ? sample.get("net_amount").toString() : "0";
        double netAmount = Double.parseDouble(strNetAmount);
        response.setNetAmount(netAmount);
        return response;
    }

    private void validateCheckParam(String txid) throws AbtException {
        if (txid == null || txid.isEmpty()) {
            throw new AbtException("Kh\u00f4ng c\u00f3 transaction id");
        }
    }

    private boolean doValidate(String cardType, String pin, String seri) throws AbtException {
        if (cardType == null || cardType.isEmpty()) {
            throw new AbtException("Ch\u01b0a c\u00f3 lo\u1ea1i th\u1ebb");
        }
        if (!Arrays.asList("VMS", "VNP", "VTE").contains(cardType)) {
            throw new AbtException("Lo\u1ea1i th\u1ebb kh\u00f4ng h\u1ee3p l\u1ec7");
        }
        if (pin == null || pin.isEmpty()) {
            throw new AbtException("Ch\u01b0a c\u00f3 m\u00e3 pin");
        }
        if (seri == null || seri.isEmpty()) {
            throw new AbtException("Ch\u01b0a c\u00f3 serial");
        }
        return true;
    }

    private String createChecksum(HashMap<String, String> args, String secretKey) {
        TreeMap abc = new TreeMap(Collections.reverseOrder());
        abc.putAll(args);
        Collection<String> allVal = abc.values();
        String tmp = "";
        for (String next : allVal) {
            tmp = "|" + next + tmp;
        }
        tmp = tmp.substring(1);
        return this.hmacSha1(tmp, secretKey);
    }

    private static String mapProviderByNumber(String number) {
        if (number == null) {
            return "null";
        }
        if (number.matches("^(096|097|098|0162|0163|0164|0165|0166|0167|0168|0169|086|082)[\\d]{7}$")) {
            return "VTE";
        }
        if (number.matches("^(091|094|0123|0124|0125|0127|0129|088)[\\d]{7}$")) {
            return "VNP";
        }
        if (number.matches("^(090|093|0120|0121|0122|0126|0128|089)[\\d]{7}$")) {
            return "VMS";
        }
        return "other";
    }

    private String hmacSha1(String value, String key) {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(value.getBytes());
            byte[] hexBytes = new Hex().encode(rawHmac);
            return new String(hexBytes, "UTF-8");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String mapProvider(String vinplayProvider) {
        switch (vinplayProvider) {
            case "vtt": {
                return "VTE";
            }
            case "vnp": {
                return "VNP";
            }
            case "vms": {
                return "VMS";
            }
        }
        return vinplayProvider;
    }

}

