/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Hex
 *  org.apache.http.conn.ssl.X509HostnameVerifier
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.maxpay;

import com.vinplay.maxpay.ChargeMaxpayResponse;
import com.vinplay.maxpay.MaxpayException;
import com.vinplay.maxpay.ReCheckMaxpayResponse;
import com.vinplay.usercore.utils.GameCommon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Key;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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

public class MaxpayClient {
    private String merchantId;
    private String secretKey;

    public MaxpayClient(String merchantId, String secretKey) {
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

    public void installAllTrustManager() throws Exception {
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

    public ChargeMaxpayResponse doCharge(String cardType, String pin, String seri, String transId) throws Exception {
        this.installAllTrustManager();
        this.validateCharge(this.mapProviderMaxpay(cardType), pin, seri);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_id", this.merchantId);
        map.put("pin", pin);
        map.put("seri", seri);
        map.put("card_type", this.mapProviderMaxpay(cardType));
        map.put("merchant_txn_id", transId);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("MAXPAY_URL") + "/charge?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + transId + "&card_type=" + this.mapProviderMaxpay(cardType) + "&pin=" + pin + "&seri=" + seri);
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(90000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.convertToChargeMaxpay(json);
    }

    public ReCheckMaxpayResponse doReCheck(String transId) throws Exception {
        this.installAllTrustManager();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("merchant_txn_id", transId);
        map.put("merchant_id", this.merchantId);
        String checkSum = this.createChecksum(map, this.secretKey);
        URL url = new URL(GameCommon.getValueStr("MAXPAY_URL") + "/recheck?checksum=" + checkSum + "&merchant_id=" + this.merchantId + "&merchant_txn_id=" + transId);
        HttpsURLConnection request = (HttpsURLConnection)url.openConnection();
        request.setConnectTimeout(10000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        request.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject)new JSONParser().parse(result);
        return this.convertToReCheckMaxpay(json);
    }

    private ChargeMaxpayResponse convertToChargeMaxpay(JSONObject jData) {
        ChargeMaxpayResponse response = new ChargeMaxpayResponse();
        if (jData.get("code") != null) {
            response.setCode(String.valueOf(jData.get("code")));
        }
        if (jData.get("message") != null) {
            response.setMessage(String.valueOf(jData.get("message")));
        }
        if (jData.get("txn_id") != null) {
            response.setTxn_id(String.valueOf(jData.get("txn_id")));
        }
        if (jData.get("card_amount") != null) {
            response.setCard_amount(Double.parseDouble(String.valueOf(jData.get("card_amount"))));
        }
        if (jData.get("net_amount") != null) {
            response.setNet_amount(Double.parseDouble(String.valueOf(jData.get("net_amount"))));
        }
        return response;
    }

    private ReCheckMaxpayResponse convertToReCheckMaxpay(JSONObject jData) {
        ReCheckMaxpayResponse response = new ReCheckMaxpayResponse();
        if (jData.get("response_code") != null) {
            response.setResponse_code(String.valueOf(jData.get("response_code")));
        }
        if (jData.get("response_message") != null) {
            response.setResponse_message(String.valueOf(jData.get("response_message")));
        }
        if (jData.get("code") != null) {
            response.setCode(String.valueOf(jData.get("code")));
        }
        if (jData.get("card_amount") != null) {
            response.setCard_amount(Double.parseDouble(String.valueOf(jData.get("card_amount"))));
        }
        if (jData.get("net_amount") != null) {
            response.setNet_amount(Double.parseDouble(String.valueOf(jData.get("net_amount"))));
        }
        return response;
    }

    private String mapProviderMaxpay(String vinplayProvider) {
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
            case "gate": {
                return "FPT";
            }
        }
        return vinplayProvider;
    }

    private boolean validateCharge(String cardType, String pin, String seri) throws MaxpayException {
        if (cardType == null || cardType.isEmpty()) {
            throw new MaxpayException("Ch\u01b0a c\u00f3 lo\u1ea1i th\u1ebb", 1);
        }
        if (!Arrays.asList("VTE", "VNP", "VMS", "FPT").contains(cardType)) {
            throw new MaxpayException("Lo\u1ea1i th\u1ebb kh\u00f4ng h\u1ee3p l\u1ec7", 2);
        }
        if (pin == null || pin.isEmpty()) {
            throw new MaxpayException("Ch\u01b0a c\u00f3 m\u00e3 pin", 3);
        }
        if (seri == null || seri.isEmpty()) {
            throw new MaxpayException("Ch\u01b0a c\u00f3 serial", 4);
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

}

