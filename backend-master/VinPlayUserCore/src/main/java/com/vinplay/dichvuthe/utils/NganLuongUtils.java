/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.I2BNLType
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.dichvuthe.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.client.HttpURLClient;
import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.entities.NganLuongModel;
import com.vinplay.dichvuthe.response.I2BResponse;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.I2BNLType;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class NganLuongUtils {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    public static I2BResponse setExpressCheckout(String nickname, long money, byte bank, String ip) {
        int code = 1;
        I2BResponse res = new I2BResponse("", code);
        try {
            I2BNLType bankType = I2BNLType.getBankById((int)bank);
            if (bankType != null) {
                String url = GameCommon.getValueStr("NL_URL");
                String merchantId = GameCommon.getValueStr("NL_MERCHANT_ID");
                String merchantPassword = VinPlayUtils.getMD5Hash((String)GameCommon.getValueStr("NL_MERCHANT_PASSWORD"));
                String version = GameCommon.getValueStr("NL_VERSION");
                String function = "SetExpressCheckout";
                String receiverEmail = GameCommon.getValueStr("NL_RECEIVER_EMAIL");
                UserDaoImpl userDao = new UserDaoImpl();
                int userId = userDao.getIdByNickname(nickname);
                String orderCode = "VP_" + userId + System.currentTimeMillis();
                Integer totalAmount = (int)money;
                String paymentMethod = GameCommon.getValueStr("NL_PAYMENT_METHOD");
                String bankCode = bankType.getValue();
                String paymentType = "1";
                String orderDescription = "Nap vin qua ngan hang";
                String taxAmount = "0";
                String discountAmount = "0";
                String feeShipping = "0";
                String returnUrl = GameCommon.getValueStr("NL_RETURN_URL");
                String cancelUrl = GameCommon.getValueStr("NL_CANCEL_URL");
                String timeLimit = GameCommon.getValueStr("NL_TIME_LIMIT");
                String buyerEmail = GameCommon.getValueStr("EMAIL");
                String buyerMobile = GameCommon.getValueStr("HOT_LINE");
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                if (userMap.containsKey(nickname)) {
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    if (user.getEmail() != null && !user.getEmail().isEmpty() && user.isHasEmailSecurity()) {
                        buyerEmail = user.getEmail();
                    }
                    if (user.getMobile() != null && !user.getMobile().isEmpty() && user.isHasMobileSecurity()) {
                        buyerMobile = user.getMobile();
                    }
                }
                String buyerAddress = "Ha Noi";
                String affiliateCode = "";
                String totalItem = "1";
                String itemName1 = "";
                String itemQuantity1 = "";
                String itemAmount1 = "";
                String itemUrl1 = "";
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("merchant_id", merchantId);
                fields.put("merchant_password", merchantPassword);
                fields.put("version", version);
                fields.put("function", "SetExpressCheckout");
                fields.put("receiver_email", receiverEmail);
                fields.put("order_code", orderCode);
                fields.put("total_amount", String.valueOf(totalAmount));
                fields.put("payment_method", paymentMethod);
                fields.put("bank_code", bankCode);
                fields.put("payment_type", "1");
                fields.put("order_description", "Nap vin qua ngan hang");
                fields.put("tax_amount", "0");
                fields.put("discount_amount", "0");
                fields.put("fee_shipping", "0");
                fields.put("return_url", returnUrl);
                fields.put("cancel_url", cancelUrl);
                fields.put("time_limit", timeLimit);
                fields.put("buyer_fullname", nickname);
                fields.put("buyer_email", buyerEmail);
                fields.put("buyer_mobile", buyerMobile);
                fields.put("buyer_address", "Ha Noi");
                fields.put("affiliate_code", "");
                fields.put("total_item", "1");
                fields.put("item_name1", "");
                fields.put("item_quantity1", "");
                fields.put("item_amount1", "");
                fields.put("item_url1", "");
                String result = HttpURLClient.sendPOST(url, fields);
                if (result != null && !result.isEmpty()) {
                    RechargeDaoImpl dao;
                    DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(result));
                    Document doc = dBuilder.parse(is);
                    doc.getDocumentElement().normalize();
                    NodeList nodeList = doc.getElementsByTagName("result");
                    Element el = (Element)nodeList.item(0);
                    String errorCodeSend = el.getElementsByTagName("error_code").item(0).getTextContent();
                    String descVP = NganLuongUtils.getDescriptionError(errorCodeSend);
                    String token = el.getElementsByTagName("token").item(0).getTextContent();
                    String checkoutUrl = el.getElementsByTagName("checkout_url").item(0).getTextContent();
                    logger.debug(("Ngan Luong Request: " + checkoutUrl));
                    String timeLimits = el.getElementsByTagName("time_limit").item(0).getTextContent();
                    String descNL = "";
                    if (el.getElementsByTagName("user") != null) {
                        descNL = el.getElementsByTagName("description").item(0).getTextContent();
                    }
                    if ((dao = new RechargeDaoImpl()).logRechargeByNL(nickname, buyerEmail, buyerMobile, ip, orderCode, totalAmount, Integer.parseInt("0"), Integer.parseInt("0"), Integer.parseInt("0"), paymentMethod, bankType.getName(), errorCodeSend, descVP, token, checkoutUrl, timeLimits, descNL) && errorCodeSend.equals("00") && !checkoutUrl.isEmpty()) {
                        code = 0;
                        res.setUrl(checkoutUrl);
                    }
                } else {
                    logger.debug(("Ngan Luong Request ERROR: nickname: " + nickname + ", money: " + money + ", bank: " + bankType.getName() + ", ip: " + ip));
                }
            } else {
                code = 3;
            }
        }
        catch (Exception e) {
            logger.debug(("Ngan Luong Request ERROR: nickname: " + nickname + ", money: " + money + ", bank: " + bank + ", ip: " + ip + " error:" + e));
            e.printStackTrace();
        }
        res.setCode(code);
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static synchronized void receiveResultFromBank(Map<String, String[]> request) {
        block11 : {
            try {
                StringBuilder log = new StringBuilder("");
                HashMap<String, String> fields = new HashMap<String, String>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) continue;
                    fields.put(fieldName, fieldValue);
                }
                logger.debug(("Ngan Luong Response: " + log.toString()));
                if (fields.size() <= 0) break block11;
                String errorCode = (String)fields.get("error_code");
                String token = (String)fields.get("token");
                RechargeDaoImpl dao = new RechargeDaoImpl();
                if (errorCode == null || token == null || !errorCode.equals("00")) break block11;
                String desc = NganLuongUtils.getDescriptionError(errorCode);
                NganLuongModel model = dao.getNLTrans(token);
                if (model != null) {
                    String url = GameCommon.getValueStr("NL_URL");
                    String merchantId = GameCommon.getValueStr("NL_MERCHANT_ID");
                    String merchantPassword = VinPlayUtils.getMD5Hash((String)GameCommon.getValueStr("NL_MERCHANT_PASSWORD"));
                    String version = GameCommon.getValueStr("NL_VERSION");
                    String function = "GetTransactionDetail";
                    HashMap<String, String> fieldsCheck = new HashMap<String, String>();
                    fieldsCheck.put("merchant_id", merchantId);
                    fieldsCheck.put("merchant_password", merchantPassword);
                    fieldsCheck.put("version", version);
                    fieldsCheck.put("function", "GetTransactionDetail");
                    fieldsCheck.put("token", token);
                    String result = HttpURLClient.sendPOST(url, fieldsCheck);
                    if (result == null || result.isEmpty()) break block11;
                    DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(result));
                    Document doc = dBuilder.parse(is);
                    doc.getDocumentElement().normalize();
                    NodeList nodeList = doc.getElementsByTagName("result");
                    Element el = (Element)nodeList.item(0);
                    String errorCodeCheck = el.getElementsByTagName("error_code").item(0).getTextContent();
                    String transactionStatus = el.getElementsByTagName("transaction_status").item(0).getTextContent();
                    if (errorCodeCheck == null || transactionStatus == null) break block11;
                    errorCode = errorCodeCheck;
                    desc = NganLuongUtils.getDescriptionError(errorCode);
                    boolean addMoney = false;
                    if (errorCode.equals("00") && (transactionStatus.equals("00") || transactionStatus.equals("01"))) {
                        addMoney = true;
                    }
                    if (!dao.updateRechargeByNL(token, errorCode, desc) || !addMoney) break block11;
                    String nickname = model.getNickname();
                    long money = model.getAmount();
                    money = Math.round((double)money * GameCommon.getValueDouble("RATIO_RECHARGE_BANK"));
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    if (client == null) {
                        MoneyLogger.log(nickname, "RechargeByBank", money, 0L, "vin", "Nap vin qua ngan hang", "1030", "can not connect hazelcast");
                        return;
                    }
                    IMap<String, UserModel> userMap = client.getMap("users");
                    if (!userMap.containsKey(nickname)) break block11;
                    try {
                         userMap.lock(nickname);
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();
                        long rechargeMoney = user.getRechargeMoney();
                        user.setVin(moneyUser += money);
                        user.setVinTotal(currentMoney += money);
                        user.setRechargeMoney(rechargeMoney += money);
                        String descs = "M\u00e3 GD: " + model.getOrderCode() + ". Ng\u00e2n h\u00e0ng: " + model.getBank();
                        MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByBank", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByBank", "N\u1ea1p Vin qua ng\u00e2n h\u00e0ng", currentMoney, money, "vin", descs, 0L, false, user.isBot());
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                        userMap.put(nickname, user);
                        break block11;
                    }
                    catch (Exception e) {
                        logger.debug(e);
                        MoneyLogger.log(nickname, "RechargeByBank", money, 0L, "vin", "Nap vin qua ngan hang", "1031", "rmq error: " + e.getMessage());
                        break block11;
                    }
                    finally {
                         userMap.unlock(nickname);
                    }
                }
                dao.logRechargeByNLError(token, errorCode, desc);
            }
            catch (Exception e2) {
                logger.debug(e2);
            }
        }
    }

    public static String getDescriptionError(String errorCode) {
        String res = "";
        switch (errorCode) {
            case "00": {
                res = "Th\u00e0nh c\u00f4ng";
                break;
            }
            case "02": {
                res = "\u0110\u1ecba ch\u1ec9 IP c\u1ee7a merchant g\u1ecdi t\u1edbi NganLuong.vn kh\u00f4ng \u0111\u01b0\u1ee3c ch\u1ea5p nh\u1eadn";
                break;
            }
            case "03": {
                res = "Sai tham s\u1ed1 g\u1eedi t\u1edbi NganLuong.vn (c\u00f3 tham s\u1ed1 sai t\u00ean ho\u1eb7c ki\u1ec3u d\u1eef li\u1ec7u)";
                break;
            }
            case "04": {
                res = "T\u00ean h\u00e0m API do merchant g\u1ecdi t\u1edbi kh\u00f4ng h\u1ee3p l\u1ec7 (kh\u00f4ng t\u1ed3n t\u1ea1i)";
                break;
            }
            case "05": {
                res = "Sai version c\u1ee7a API";
                break;
            }
            case "06": {
                res = "M\u00e3 merchant kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c ch\u01b0a \u0111\u01b0\u1ee3c k\u00edch ho\u1ea1t";
                break;
            }
            case "07": {
                res = "Sai m\u1eadt kh\u1ea9u c\u1ee7a merchant";
                break;
            }
            case "08": {
                res = "T\u00e0i kho\u1ea3n ng\u01b0\u1eddi b\u00e1n h\u00e0ng kh\u00f4ng t\u1ed3n t\u1ea1i";
                break;
            }
            case "09": {
                res = "T\u00e0i kho\u1ea3n ng\u01b0\u1eddi nh\u1eadn ti\u1ec1n \u0111ang b\u1ecb phong t\u1ecfa";
                break;
            }
            case "10": {
                res = "H\u00f3a \u0111\u01a1n thanh to\u00e1n kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "11": {
                res = "S\u1ed1 ti\u1ec1n thanh to\u00e1n kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "12": {
                res = "\u0110\u01a1n v\u1ecb ti\u1ec1n t\u1ec7 kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "29": {
                res = "Token kh\u00f4ng t\u1ed3n t\u1ea1i";
                break;
            }
            case "80": {
                res = "Kh\u00f4ng th\u00eam \u0111\u01b0\u1ee3c \u0111\u01a1n h\u00e0ng";
                break;
            }
            case "81": {
                res = "\u0110\u01a1n h\u00e0ng ch\u01b0a \u0111\u01b0\u1ee3c thanh to\u00e1n";
                break;
            }
            case "99": {
                res = "L\u1ed7i kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ecbnh ngh\u0129a ho\u1eb7c kh\u00f4ng r\u00f5 nguy\u00ean nh\u00e2n";
                break;
            }
            case "110": {
                res = "\u0110\u1ecba ch\u1ec9 email t\u00e0i kho\u1ea3n nh\u1eadn ti\u1ec1n kh\u00f4ng ph\u1ea3i email ch\u00ednh";
                break;
            }
            case "111": {
                res = "T\u00e0i kho\u1ea3n nh\u1eadn ti\u1ec1n \u0111ang b\u1ecb kh\u00f3a";
                break;
            }
            case "113": {
                res = "T\u00e0i kho\u1ea3n nh\u1eadn ti\u1ec1n ch\u01b0a c\u1ea5u h\u00ecnh l\u00e0 ng\u01b0\u1eddi b\u00e1n n\u1ed9i dung s\u1ed1";
                break;
            }
            case "114": {
                res = "Giao d\u1ecbch \u0111ang th\u1ef1c hi\u1ec7n, ch\u01b0a k\u1ebft th\u00fac";
                break;
            }
            case "115": {
                res = "Giao d\u1ecbch b\u1ecb h\u1ee7y";
                break;
            }
            case "118": {
                res = "tax_amount kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "119": {
                res = "discount_amount kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "120": {
                res = "fee_shipping kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "121": {
                res = "return_url kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "122": {
                res = "cancel_url kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "123": {
                res = "items kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "124": {
                res = "transaction_info kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "125": {
                res = "quantity kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "126": {
                res = "order_description kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "127": {
                res = "affiliate_code kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "128": {
                res = "time_limit kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "129": {
                res = "buyer_fullname kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "130": {
                res = "buyer_email kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "131": {
                res = "buyer_mobile kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "132": {
                res = "buyer_address kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "133": {
                res = "total_item kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "134": {
                res = "payment_method, bank_code kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "135": {
                res = "L\u1ed7i k\u1ebft n\u1ed1i t\u1edbi h\u1ec7 th\u1ed1ng ng\u00e2n h\u00e0ng";
                break;
            }
            case "140": {
                res = "\u0110\u01a1n h\u00e0ng kh\u00f4ng h\u1ed7 tr\u1ee3 thanh to\u00e1n tr\u1ea3 g\u00f3p";
                break;
            }
            default: {
                res = "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            }
        }
        return res;
    }
}

