/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.models.SoftpinJson
 *  org.json.simple.JSONArray
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay._1pay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay._1pay.BuyCardRequestObj;
import com.vinplay._1pay.BuyCardResponseObj;
import com.vinplay._1pay.ReCheckBuyCardRequestObj;
import com.vinplay._1pay.ReCheckBuyCardResponseObj;
import com.vinplay._1pay.SerialsObj;
import com.vinplay._1pay.StatusObj;
import com.vinplay._1pay.TransactionObj;
import com.vinplay.dichvuthe.client.HttpClient;
import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.dichvuthe.entities.SoftpinObj;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.SoftpinJson;
import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class _1PayClient {
    public static SoftpinObj buyCard(String id, String provider, String price, int quantity) throws Exception {
        BuyCardRequestObj reqObj = new BuyCardRequestObj();
        reqObj.setUsername(GameCommon.getValueStr("_1PAY_USER"));
        reqObj.setApiCode(GameCommon.getValueStr("_1PAY_CODE_API"));
        reqObj.setApiUsername(GameCommon.getValueStr("_1PAY_USER_API"));
        reqObj.setRequestId(id);
        reqObj.setServiceCode(_1PayClient.mappingServiceCodeSoftpin(provider));
        reqObj.setPrice(price);
        reqObj.setQuantity(quantity);
        String data = reqObj.getUsername() + "|" + reqObj.getApiCode() + "|" + reqObj.getApiUsername() + "|" + reqObj.getServiceCode() + "|" + reqObj.getRequestId();
        String sign = RSA.sign(data, GameCommon.getValueStr("_1PAY_PRIVATE_KEY"));
        reqObj.setDataSign(sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("_1PAY_URL"), mapper.writeValueAsString(reqObj));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        BuyCardResponseObj result = _1PayClient.convertToBuyCardResponseObj(json);
        SoftpinObj response = new SoftpinObj();
        response.setId(id);
        response.setProvider(provider);
        response.setAmount(Integer.parseInt(price));
        response.setQuantity(quantity);
        response.setStatus(Integer.parseInt(result.getStatus().getCode()));
        response.setMessage(result.getStatus().getValue());
        response.setSign(result.getDataSign());
        response.setPartnerTransId(result.getTransactionId());
        if (Integer.parseInt(result.getStatus().getCode()) == 0) {
            String listCard = _1PayClient.decrypt(GameCommon.getValueStr("_1PAY_PASS"), result.getEncryptCards());
            String[] split = listCard.split(";", -1);
            ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
            for (int i = 1; i < split.length - 1; ++i) {
                String card = split[i];
                String[] splitCard = card.split("\\|", -1);
                SoftpinJson softpinJson = new SoftpinJson();
                softpinJson.setProvider(provider);
                softpinJson.setAmount(Integer.parseInt(price));
                softpinJson.setPin(splitCard[2]);
                softpinJson.setSerial(splitCard[3]);
                softpinJson.setExpire(splitCard[4]);
                softpinList.add(softpinJson);
            }
            response.setSoftpinList(softpinList);
        }
        return response;
    }

    public static SoftpinObj reCheckBuyCard(String requestId, String transactionId, String provider, String price, int quantity) throws Exception {
        ReCheckBuyCardRequestObj reqObj = new ReCheckBuyCardRequestObj();
        reqObj.setUsername(GameCommon.getValueStr("_1PAY_USER"));
        reqObj.setApiCode(GameCommon.getValueStr("_1PAY_CODE_API"));
        reqObj.setApiUsername(GameCommon.getValueStr("_1PAY_USER_API"));
        reqObj.setRequestId(requestId);
        reqObj.setServiceCode("1PAY0061");
        reqObj.setTxnRequestId(transactionId);
        String data = reqObj.getUsername() + "|" + reqObj.getApiCode() + "|" + reqObj.getApiUsername() + "|" + reqObj.getServiceCode() + "|" + reqObj.getRequestId();
        String sign = RSA.sign(data, GameCommon.getValueStr("_1PAY_PRIVATE_KEY"));
        reqObj.setDataSign(sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("_1PAY_URL"), mapper.writeValueAsString(reqObj));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        ReCheckBuyCardResponseObj result = _1PayClient.convertToReCheckBuyCardResponseObj(json);
        SoftpinObj response = new SoftpinObj();
        response.setId(requestId);
        response.setProvider(provider);
        response.setAmount(Integer.parseInt(price));
        response.setQuantity(quantity);
        response.setSign(result.getDataSign());
        response.setPartnerTransId(transactionId);
        if (result.getTransaction() != null && result.getTransaction().getSerials() != null && result.getTransaction().getErrorCode() != null && result.getTransaction().getErrorCode().equals("0")) {
            List<SerialsObj> serialsList = result.getTransaction().getSerials();
            ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
            for (int i = 0; i < serialsList.size(); ++i) {
                SoftpinJson softpinJson = new SoftpinJson();
                softpinJson.setProvider(provider);
                softpinJson.setAmount(Integer.parseInt(price));
                softpinJson.setPin(serialsList.get(i).getPin());
                softpinJson.setSerial(serialsList.get(i).getSerial());
                softpinJson.setExpire(serialsList.get(i).getExpiredDate());
                softpinList.add(softpinJson);
            }
            response.setSoftpinList(softpinList);
            response.setStatus(0);
            response.setMessage("Th\u00e0nh c\u00f4ng");
        } else {
            response.setStatus(1);
            response.setMessage("Giao d\u1ecbch th\u1ea5t b\u1ea1i");
        }
        return response;
    }

    private static ReCheckBuyCardResponseObj convertToReCheckBuyCardResponseObj(JSONObject jData) throws Exception {
        JSONObject json;
        ReCheckBuyCardResponseObj response = new ReCheckBuyCardResponseObj();
        if (jData.get("status") != null) {
            json = (JSONObject)new JSONParser().parse(String.valueOf(jData.get("status")));
            StatusObj status = new StatusObj();
            status.setCode(String.valueOf(json.get("code")));
            status.setValue(String.valueOf(json.get("value")));
            response.setStatus(status);
        }
        if (jData.get("transaction") != null) {
            json = (JSONObject)new JSONParser().parse(String.valueOf(jData.get("transaction")));
            TransactionObj transaction = new TransactionObj();
            transaction.setId(String.valueOf(json.get("id")));
            transaction.setCif(String.valueOf(json.get("cif")));
            transaction.setAmount(String.valueOf(json.get("amount")));
            transaction.setCommision(String.valueOf(json.get("commision")));
            transaction.setFee(String.valueOf(json.get("fee")));
            transaction.setPrice(String.valueOf(json.get("price")));
            transaction.setQuantity(String.valueOf(json.get("quantity")));
            transaction.setRealAmount(String.valueOf(json.get("realAmount")));
            transaction.setLastBalance(String.valueOf(json.get("lastBalance")));
            transaction.setCurrentBalance(String.valueOf(json.get("currentBalance")));
            transaction.setInfo(String.valueOf(json.get("info")));
            transaction.setServiceType(String.valueOf(json.get("serviceType")));
            transaction.setSubject(String.valueOf(json.get("subject")));
            transaction.setErrorCode(String.valueOf(json.get("errorCode")));
            transaction.setErrorMessage(String.valueOf(json.get("errorMessage")));
            transaction.setTransactionStatus(String.valueOf(json.get("transactionStatus")));
            transaction.setCreationDate(String.valueOf(json.get("creationDate")));
            transaction.setServiceCode(String.valueOf(json.get("serviceCode")));
            transaction.setServiceName(String.valueOf(json.get("serviceName")));
            transaction.setTelcoType(String.valueOf(json.get("telcoType")));
            if (json.get("serials") != null) {
                JSONArray arr = (JSONArray)json.get("serials");
                ArrayList<SerialsObj> serialsList = new ArrayList<SerialsObj>();
                for (int i = 0; i < arr.size(); ++i) {
                    JSONObject obj = (JSONObject)arr.get(i);
                    SerialsObj serials = new SerialsObj();
                    serials.setSerial(String.valueOf(obj.get("serial")));
                    serials.setPin(String.valueOf(obj.get("pin")));
                    Double d = Double.parseDouble(String.valueOf(obj.get("price")));
                    serials.setPrice(d.intValue());
                    serials.setCardType(String.valueOf(obj.get("cardType")));
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                    long milliSeconds = Long.parseLong(String.valueOf(obj.get("expiredDate")));
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(milliSeconds);
                    serials.setExpiredDate(formatter.format(calendar.getTime()));
                    serialsList.add(serials);
                }
                transaction.setSerials(serialsList);
            }
            response.setTransaction(transaction);
        }
        if (jData.get("dataSign") != null) {
            response.setDataSign(String.valueOf(jData.get("dataSign")));
        }
        return response;
    }

    private static BuyCardResponseObj convertToBuyCardResponseObj(JSONObject jData) throws Exception {
        BuyCardResponseObj response = new BuyCardResponseObj();
        if (jData.get("status") != null) {
            JSONObject json = (JSONObject)new JSONParser().parse(String.valueOf(jData.get("status")));
            StatusObj status = new StatusObj();
            status.setCode(String.valueOf(json.get("code")));
            status.setValue(String.valueOf(json.get("value")));
            response.setStatus(status);
        }
        if (jData.get("transactionId") != null) {
            response.setTransactionId(String.valueOf(jData.get("transactionId")));
        }
        if (jData.get("encryptCards") != null) {
            response.setEncryptCards(String.valueOf(jData.get("encryptCards")));
        }
        if (jData.get("dataSign") != null) {
            response.setDataSign(String.valueOf(jData.get("dataSign")));
        }
        return response;
    }

    private static String decrypt(String key, String data) throws Exception {
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

    private static String mappingServiceCodeSoftpin(String provider) {
        switch (provider) {
            case "vtt": {
                return "1PAY0050";
            }
            case "vnp": {
                return "1PAY0051";
            }
            case "vms": {
                return "1PAY0052";
            }
            case "vnm": {
                return "1PAY0055";
            }
            case "gate": {
                return "1PAY0057";
            }
            case "zing": {
                return "1PAY0056";
            }
            case "vcoin": {
                return "1PAY0058";
            }
            case "sfone": {
                return "1PAY0053";
            }
            case "gtel": {
                return "1PAY0054";
            }
            case "garena": {
                return "1PAY0059";
            }
        }
        return null;
    }

    public static void main(String[] args) {
    }
}

