/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Hex
 *  org.apache.http.conn.ssl.X509HostnameVerifier
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 *  org.json.simple.parser.ParseException
 */
package com.vinplay.lucky79;

import com.vinplay.epay.MD5;
import com.vinplay.lucky79.ChargeLucky79Response;
import com.vinplay.lucky79.Lucky79Exception;
import com.vinplay.lucky79.ReCheckLucky79Response;
import com.vinplay.usercore.utils.GameCommon;

import java.io.*;
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

import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Lucky79Client {
    private String merchantId;
    private String secretKey;


    public Lucky79Client(String merchantId, String secretKey) {
        this.merchantId = merchantId;
        this.secretKey = secretKey;

    }

    public void installMyPolicy() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

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
        X509HostnameVerifier verifier = new X509HostnameVerifier() {

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
        HttpsURLConnection.setDefaultHostnameVerifier((HostnameVerifier) verifier);
    }

    public void installAllTrustManager() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

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
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String urlHostname, SSLSession _session) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TheCaoResponse doCharge(String cardType, String pin, String seri, String transId, String amount) throws Exception {


        this.validateCharge(this.luckyProviderMaxpay(cardType), pin, seri);

        String card = luckyProviderCard(cardType);

        String partner_id = merchantId;
        String partner_key = secretKey;
        String command = "charging";
       // long request_id = System.currentTimeMillis();
        JSONObject obj = new JSONObject();
        String sign = partner_key + pin + command + partner_id + transId + seri + card;

        obj.put("serial", seri);
        obj.put("code", pin);
        obj.put("telco", card);
        obj.put("amount", amount);
        obj.put("request_id", transId);
        obj.put("partner_id", partner_id);
        obj.put("command", command);
        obj.put("sign", MD5.hash(sign));

        StringWriter out = new StringWriter();
        obj.writeJSONString(out);

        String jsonText = out.toString();


        URL url = new URL(GameCommon.getValueStr("THE_CAO_URL"));
        HttpURLConnection request = (HttpURLConnection) url.openConnection();

        request.setConnectTimeout(90000);
        request.setUseCaches(false);
        request.setDoOutput(true);
        request.setDoInput(true);
        HttpURLConnection.setFollowRedirects(true);
        request.setInstanceFollowRedirects(true);
        request.setRequestProperty("Content-Type", "application/json;");
        request.setRequestProperty("Accept", "application/json");
        request.setRequestMethod("POST");
        try (OutputStream os = request.getOutputStream()) {
            byte[] input = jsonText.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        int responseCode = request.getResponseCode();

        BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String result = "";
        String line = "";
        while ((line = rd.readLine()) != null) {
            result = result.concat(line);
        }
        JSONObject json = (JSONObject) new JSONParser().parse(result);
        TheCaoResponse chargelucky = new TheCaoResponse();


        return this.convertToReCheckTheCao(json, chargelucky.getTrans_id());


    }

    public ReCheckLucky79Response doReCheck(String transId) throws Exception {
        try {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("merchant_txn_id", transId);
            map.put("merchant_id", this.merchantId);
            URL url = new URL("http://api.payviet.net/card?API-DOICARD-V1=947347756d8049d45c720df3696e6fa5&param=" + transId);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
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
            JSONObject json = (JSONObject) new JSONParser().parse(result);
            return this.convertToReCheckMaxpay(json, transId);
        } catch (Exception ex) {
            return null;
        }
    }

    public TheCaoResponse doReCheck1(RechargeByCardMessage transId) throws Exception {
        try {

            String card = luckyProviderCard(transId.getProvider());

            String partner_id = merchantId;
            String partner_key = secretKey;
            String command = "check";
            JSONObject obj = new JSONObject();
            String sign = partner_key + transId.getPin() + command + partner_id + transId.getReferenceId() + transId.getSerial() + card;

            obj.put("serial", transId.getSerial());
            obj.put("code", transId.getPin());
            obj.put("telco", card);
            obj.put("amount", transId.getAmount());
            obj.put("request_id", transId.getReferenceId());
            obj.put("partner_id", partner_id);
            obj.put("command", command);
            obj.put("sign", MD5.hash(sign));

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);

            String jsonText = out.toString();
            URL url = new URL(GameCommon.getValueStr("THE_CAO_URL"));
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setUseCaches(false);
            request.setDoOutput(true);
            request.setDoInput(true);
            HttpURLConnection.setFollowRedirects(true);
            request.setInstanceFollowRedirects(true);
            request.setRequestProperty("Content-Type", "application/json;");
            request.setRequestProperty("Accept", "application/json");
            request.setRequestMethod("POST");
            try (OutputStream os = request.getOutputStream()) {
                byte[] input = jsonText.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String result = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                result = result.concat(line);
            }
            JSONObject json = (JSONObject) new JSONParser().parse(result);
            return this.convertToReCheckTheCao(json, transId.getTranId());
        } catch (Exception ex) {
            return null;
        }
    }

    public ReCheckLucky79Response doReCheck2(String transId) throws Exception {
        URL url = new URL(GameCommon.getValueStr("LUCKY79_URL") + "/CheckCharge?apikey=" + this.secretKey + "&id=" + transId);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
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
        JSONObject json = (JSONObject) new JSONParser().parse(result);
        ReCheckLucky79Response ret = this.convertToReCheckMaxpay(json, transId);
        if (ret != null && (ret.getStatus().equals("waiting") || ret.getStatus().equals("processing"))) {
            return this.doReCheck(transId);
        }
        return null;
    }

    private PayVietCrypto convertToPayViet(JSONObject jData) {
        PayVietCrypto response = new PayVietCrypto();
        if (jData.get( "result") != null) {
            response.setResult(String.valueOf(jData.get( "result")));
        }
        if (jData.get( "status") != null) {
            response.setStatus(Boolean.valueOf(String.valueOf(jData.get( "status"))));
        }
        if (jData.get( "transaction_id") != null) {
            response.setTransaction_id(String.valueOf(jData.get( "transaction_id")));
        }
        return response;
    }

    private ChargeLucky79Response convertToChargeMaxpay(JSONObject jData) {
        ChargeLucky79Response response = new ChargeLucky79Response();
        if (jData.get( "stt") != null) {
            response.setCode(String.valueOf(jData.get( "stt")));
        }
        if (jData.get( "msg") != null) {
            response.setMessage(String.valueOf(jData.get( "msg")));
        }
        if (jData.get( "data") != null) {
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(String.valueOf(jData.get( "data")));
                if (json.get( "id") != null) {
                    response.setTxn_id(String.valueOf(json.get( "id")));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    private TheCaoResponse convertToReCheckTheCao(JSONObject jData, String transId) {
        TheCaoResponse response = new TheCaoResponse();
        if (jData.get( "status") != null) {
            response.setResponse_code(String.valueOf(jData.get( "status")));
        }
        if (jData.get( "message") != null) {
            response.setMessage(String.valueOf(jData.get( "message")));
        }


        //JSONObject json = (JSONObject) new JSONParser().parse(String.valueOf(jData.get( "result")));


        if (jData.get( "amount") != null) {
            response.setAmount(Double.parseDouble(String.valueOf(jData.get( "declared_value"))));
        }

        if (jData.get( "code") != null) {
            response.setCode(String.valueOf(jData.get( "code")));
        }

        if (jData.get( "status") != null) {
            response.setStatus(String.valueOf(jData.get( "status")));
        }
        if (jData.get( "request_id") != null) {
            response.setRequest_id(String.valueOf(jData.get( "request_id")));
        }
        if (jData.get( "serial") != null) {
            response.setSerial(String.valueOf(jData.get( "serial")));
        }
        if (jData.get( "telco") != null) {
            response.setTelco(String.valueOf(jData.get( "telco")));
        }
//                    if (json.get( "createDate") != null) {
//                        response.setDate(String.valueOf(json.get( "createDate")));
//                    }
        response.setTrans_id(String.valueOf(jData.get( "trans_id")));


        if (jData.get( "declared_value") != null) {
            response.setDeclared_value(Double.parseDouble(String.valueOf(jData.get( "declared_value"))));
        }

        return response;
    }

    private ReCheckLucky79Response convertToReCheckMaxpay(JSONObject jData, String transId) {
        ReCheckLucky79Response response = new ReCheckLucky79Response();
        if (jData.get( "status") != null) {
            response.setResponse_code(String.valueOf(jData.get( "status")));
        }
        if (jData.get( "msg") != null) {
            response.setResponse_message(String.valueOf(jData.get( "msg")));
        }
        if (jData.get( "result") != null) {
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(String.valueOf(jData.get( "result")));
                if (json != null) {

                    if (json.get( "amount") != null) {
                        response.setCard_amount(Double.parseDouble(String.valueOf(json.get( "amount"))));
                    }

                    if (json.get( "status") != null) {
                        response.setStatus(String.valueOf(json.get( "status")));
                    }
                    if (json.get( "id") != null) {
                        response.setTxn_id(String.valueOf(json.get( "id")));
                    }
                    if (json.get( "cardSerial") != null) {
                        response.setCardSerial(String.valueOf(json.get( "cardSerial")));
                    }
                    if (json.get( "cardType") != null) {
                        response.setCardType(String.valueOf(json.get( "cardType")));
                    }
                    if (json.get( "createDate") != null) {
                        response.setDate(String.valueOf(json.get( "createDate")));
                    }
                    response.setId(String.valueOf(json.get( "transaction_id")));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (jData.get( "net_amount") != null) {
                response.setNet_amount(Double.parseDouble(String.valueOf(jData.get( "net_amount"))));
            }
        }
        return response;
    }

    private String luckyProviderCard(String vinplayProvider) {
        switch (vinplayProvider) {
            case "Viettel": {
                return "VIETTEL";
            }
            case "Mobifone": {
                return "MOBIFONE";
            }
            case "Vinaphone": {
                return "VINAPHONE";
            }
            case "GATE": {
                return "GATE";
            }
        }
        return "Unknow";
    }

    private String luckyProviderMaxpay(String vinplayProvider) {
        switch (vinplayProvider) {
            case "vtt": {
                return "421";
            }
            case "vnp": {
                return "423";
            }
            case "vms": {
                return "422";
            }
            case "gate": {
                return "FPT";
            }
        }
        return vinplayProvider;
    }

    private boolean validateCharge(String cardType, String pin, String seri) throws Lucky79Exception {
        if (cardType == null || cardType.isEmpty()) {
            throw new Lucky79Exception("Ch\u01b0a c\u00f3 lo\u1ea1i th\u1ebb", 1);
        }
        String[] stringlistProvider = new String[]{"421", "Viettel", "Mobifone", "Vinaphone", "422", "423", "FPT"};
        if (!Arrays.asList(stringlistProvider).contains(cardType)) {
            throw new Lucky79Exception("Lo\u1ea1i th\u1ebb kh\u00f4ng h\u1ee3p l\u1ec7", 2);
        }
        if (pin == null || pin.isEmpty()) {
            throw new Lucky79Exception("Ch\u01b0a c\u00f3 m\u00e3 pin", 3);
        }
        if (seri == null || seri.isEmpty()) {
            throw new Lucky79Exception("Ch\u01b0a c\u00f3 serial", 4);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

