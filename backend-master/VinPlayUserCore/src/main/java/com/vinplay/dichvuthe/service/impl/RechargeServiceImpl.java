/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.I2BType
 *  com.vinplay.vbee.common.enums.PhoneCardType
 *  com.vinplay.vbee.common.enums.ProviderType
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.ApiOtpModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.dichvuthe.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.client.DvtAlert;
import com.vinplay.dichvuthe.client.GachTheClient;
import com.vinplay.dichvuthe.client.HttpURLClient;
import com.vinplay.dichvuthe.client.VinplayClient;
import com.vinplay.dichvuthe.dao.RechargeDao;
import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.entities.ChargeObj;
import com.vinplay.dichvuthe.entities.ChargeVCObj;
import com.vinplay.dichvuthe.entities.Iwin99CallbackResponse;
import com.vinplay.dichvuthe.entities.SMSPlusResponse;
import com.vinplay.dichvuthe.response.I2BResponse;
import com.vinplay.dichvuthe.response.RechargeApiOTPResponse;
import com.vinplay.dichvuthe.response.RechargeIAPResponse;
import com.vinplay.dichvuthe.response.RechargeResponse;
import com.vinplay.dichvuthe.service.RechargeService;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.dichvuthe.utils.DvtConst;
import com.vinplay.dichvuthe.utils.DvtUtils;
import com.vinplay.dichvuthe.utils.NapasUtils;
import com.vinplay.dichvuthe.utils.NganLuongUtils;
import com.vinplay.epay.megacard.ChargeReponse;
import com.vinplay.epay.megacard.EpayMegaCardAlert;
import com.vinplay.epay.megacard.EpayMegaCardCharging;
import com.vinplay.iap.lib.Purchase;
import com.vinplay.iap.lib.Security;
import com.vinplay.lucky79.*;
import com.vinplay.maxpay.ChargeMaxpayResponse;
import com.vinplay.maxpay.MaxpayAlert;
import com.vinplay.maxpay.MaxpayClient;
import com.vinplay.maxpay.MaxpayException;
import com.vinplay.maxpay.ReCheckMaxpayResponse;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.IAPModel;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.PartnerConfig;
import com.vinplay.vbee.common.enums.I2BType;
import com.vinplay.vbee.common.enums.PhoneCardType;
import com.vinplay.vbee.common.enums.ProviderType;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.ApiOtpModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.UserValidaton;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vtc.VTCAlert;
import com.vinplay.vtc.VTCRechargeClient;
import com.vinplay.vtc.VTCRechargeResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.json.JSONObject;

public class RechargeServiceImpl
implements RechargeService {
    private static final Logger logger = Logger.getLogger((String)"recharge");

    @Override
    public RechargeResponse rechargeByCard(String nickname, ProviderType provider, String serial, String pin, String amount, String platform) throws Exception {
        String name;
        logger.debug(("Start rechargeByCard ham cha:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin + ", platform:" + platform));
        RechargeResponse response = null;
        block6 : switch (name = provider.getName()) {
            case "Viettel": {
                String valueStr2;
                String valueStr;
                String providername = GameCommon.getValueStr("RECHARGE_VTT_PRIMARY");
                switch (valueStr = GameCommon.getValueStr("RECHARGE_VTT_PRIMARY")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin, amount, platform);
                    }
                }
                if (response != null) break;
                switch (valueStr2 = GameCommon.getValueStr("RECHARGE_VTT_BACKUP")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break block6;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin,amount, platform);
                    }
                }
                break;
            }
            case "Mobifone": {
                String valueStr4;
                String valueStr3;
                switch (valueStr3 = GameCommon.getValueStr("RECHARGE_VMS_PRIMARY")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin,amount, platform);
                    }
                }
                if (response != null) break;
                switch (valueStr4 = GameCommon.getValueStr("RECHARGE_VMS_BACKUP")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break block6;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin,amount, platform);
                    }
                }
                break;
            }
            case "Vinaphone": {
                String valueStr5;
                String valueStr6;
                switch (valueStr5 = GameCommon.getValueStr("RECHARGE_VNP_PRIMARY")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin,amount, platform);
                    }
                }
                if (response != null) break;
                switch (valueStr6 = GameCommon.getValueStr("RECHARGE_VNP_BACKUP")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                        break block6;
                    }
                    case "lucky79": {
                        response = this.rechargeByCardLucky79(nickname, provider, serial, pin,amount, platform);
                    }
                }
                break;
            }
            case "Gate": {
                String valueStr7;
                String valueStr8;
                switch (valueStr7 = GameCommon.getValueStr("RECHARGE_GATE_PRIMARY")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                    }
                }
                if (response != null) break;
                switch (valueStr8 = GameCommon.getValueStr("RECHARGE_GATE_BACKUP")) {
                    case "maxpay": {
                        response = this.rechargeByCardMaxpay(nickname, provider, serial, pin, platform);
                    }
                }
                break;
            }
        }
        if (response == null) {
            response = new RechargeResponse(-1, 0L, 0, 0L);
            RechargeByCardMessage message = new RechargeByCardMessage(nickname, "", provider.getName(), serial, pin, 0, -1, "M\u1ea5t k\u1ebft n\u1ed1i t\u1ea5t c\u1ea3 c\u00e1c partner", -1, 0, (String)null, (String)null, "", platform, (String)null);
            MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
        }
        logger.debug(("Finish rechargeByCard ham cha:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin + ", platform:" + platform));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    private RechargeResponse rechargeByCardMaxpay(String nickname, ProviderType provider, String serial, String pin, String platform) throws Exception {
        RechargeServiceImpl.logger.debug(("Start rechargeByCard Maxpay:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin));
        int code = 1;
        HazelcastInstance client;
        IMap<String, UserModel> userMap;
        String description;
		String description1;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1 || provider == null) {
            RechargeServiceImpl.logger.debug("rechargeByCard: param fail");
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1030", "can not connect hazelcast");
            return res;
        }
        userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFail(user.getRechargeFail(), user.getRechargeFailTime());
                lbl129: // 8 sources:
                if (time <= 0L) {
                    if (UserValidaton.validateSerialPin((String)serial)) {
                        if (UserValidaton.validateSerialPin((String)pin)) {
                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                            ChargeObj obj = null;
                            ChargeMaxpayResponse response = null;
                            try {
                                MaxpayClient maxpay = new MaxpayClient(GameCommon.getValueStr("MAXPAY_MERCHANT_ID"), GameCommon.getValueStr("MAXPAY_SECRET_KEY"));
                                response = maxpay.doCharge(provider.getValue(), pin, serial, id);
                            }
                            catch (MaxpayException me) {
                                RechargeServiceImpl.logger.debug(me);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1034", "Loi inpput parameter: " + me.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++MaxpayAlert.maxpayDisconnect != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertMaxpayDisconnectTime, (int)1) == false) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong Maxpay dang bi mat ket noi!", true);
                                MaxpayAlert.alertMaxpayDisconnectTime = new Date();
                                return null;
                            }
                            catch (Exception e) {
                                RechargeServiceImpl.logger.debug(e);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1034", "Loi ket noi maxpay: " + e.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++MaxpayAlert.maxpayDisconnect != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertMaxpayDisconnectTime, (int)1) == false) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong Maxpay dang bi mat ket noi!", true);
                                MaxpayAlert.alertMaxpayDisconnectTime = new Date();
                                return null;
                            }
                            if (response != null) {
                                obj = new ChargeObj(response.getTxn_id(), provider.getValue(), serial, pin, (int)response.getCard_amount(), Integer.parseInt(response.getCode()), this.mapVinplayMessage(response.getCode()), "maxpay");
                            }
                            long money = 0L;
                            if (obj != null) {
                                money = Math.round((double)obj.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                                code = this.mapVinplayCode(String.valueOf(obj.getStatus()));
                                if (code == 0 && (obj.getAmount() == 0 || obj.getAmount() > PhoneCardType._5M.getValue() || obj.getAmount() % PhoneCardType._10K.getValue() != 0)) {
                                    code = 30;
                                }
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, obj.getAmount(), obj.getStatus(), obj.getMessage(), code, (int)money, (String)null, (String)null, "maxpay", platform, (String)null);
                            } else {
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, 0, -1, "L\u00e1\u00bb\u2014i k\u00e1\u00ba\u00bft n\u00e1\u00bb\u2018i maxpay", code, 0, (String)null, (String)null, "maxpay", platform, (String)null);
                                ++MaxpayAlert.maxpayDisconnect;
                            }
                            try {
                                userMap = client.getMap("users");
                                userMap.lock(nickname);
                                user = (UserCacheModel)userMap.get(nickname);
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                if (code == 0) {
                                    user.setVin(moneyUser += money);
                                    user.setVinTotal(currentMoney += money);
                                    user.setRechargeMoney(rechargeMoney += money);
                                    user.setRechargeFail(0);
                                    description = "Kết quả: Th\u00e0nh c\u00f4ng, Mã GD: " + id + ", Thẻ: " + provider.getName() + ", Mệnh giá: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin + "";
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "Nạp Win qua Thẻ", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    userMap.put(nickname, user);
                                    res.setCurrentMoney(currentMoney);
                                    MaxpayAlert.maxpayDisconnect = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        MaxpayAlert.viettelPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        MaxpayAlert.mobiPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        MaxpayAlert.vinaPending = 0;
                                    }
                                    if (!provider.getName().equals(ProviderType.GATE.getName())) break lbl129;
                                    MaxpayAlert.gatePending = 0;
                                }
                                if (code == 30) {
                                    description = "Kết quả: Đang xử lý, Serial: " + serial;
									description1 = "Thẻ: " + provider.getName() + " " + obj.getAmount() + ", Pin: " + pin;
                                    LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", description1, currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    MaxpayAlert.maxpayDisconnect = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        ++MaxpayAlert.viettelPending;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        ++MaxpayAlert.mobiPending;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        ++MaxpayAlert.vinaPending;
                                    }
                                    if (!provider.getName().equals(ProviderType.GATE.getName())) break lbl129;
                                    ++MaxpayAlert.gatePending;
                                }
                                if (code != 1) {
                                    cardFail = true;
                                }
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                            }
                            catch (Exception e2) {
                                code = 1;
                                RechargeServiceImpl.logger.debug(e2);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e2.getMessage());
                                return null;
//                                var21_32 = null;
//                                return var21_32;
                            }
                            finally {
                                userMap.unlock(nickname);
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }

                if (cardFail) {
                    try {
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        user = (UserCacheModel)userMap.get(nickname);
                        user.setRechargeFail(user.getRechargeFail() + 1);
                        user.setRechargeFailTime(new Date());
                        userMap.put(nickname, user);
                    }
                    catch (Exception e3) {
                        code = 1;
                        RechargeServiceImpl.logger.debug(e3);
                        MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e3.getMessage());
                        return null;
//                        obj = null;
//                        return obj;
                    }
                    finally {
                        userMap.unlock(nickname);
                    }
                }
                res.setFail(user.getRechargeFail());
            }
            catch (Exception e4) {
                code = 1;
                RechargeServiceImpl.logger.debug(e4);
                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e4.getMessage());
                return null;
            }
        }
        res.setCode(code);
        RechargeServiceImpl.logger.debug(("Finish rechargeByCard Maxpay, Response : " + code));
        if (MaxpayAlert.viettelPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertViettelPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong Maxpay tra ve pending qua 5 lan lien tiep Viettel!", true);
            MaxpayAlert.alertViettelPendingTime = new Date();
        }
        if (MaxpayAlert.mobiPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertMobiPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong Maxpay tra ve pending qua 5 lan lien tiep Mobifone!", true);
            MaxpayAlert.alertMobiPendingTime = new Date();
        }
        if (MaxpayAlert.vinaPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertVinaPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong Maxpay tra ve pending qua 5 lan lien tiep Vinaphone!", true);
            MaxpayAlert.alertVinaPendingTime = new Date();
        }
        if (MaxpayAlert.gatePending != GameCommon.getValueInt("COUNT_FAIL")) return res;
        if (VinPlayUtils.isAlertTimeout((Date)MaxpayAlert.alertGatePendingTime, (int)1) == false) return res;
        this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong Maxpay tra ve pending qua 5 lan lien tiep FPT Gate!", true);
        MaxpayAlert.alertGatePendingTime = new Date();
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    private RechargeResponse rechargeByCardLucky79(String nickname, ProviderType provider, String serial, String pin, String amount, String platform) throws Exception {
        RechargeServiceImpl.logger.debug(("Start rechargeByCard ozze:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin));
        int code = 1;
        String description;
		String description1;
        HazelcastInstance client;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1 || provider == null) {
            RechargeServiceImpl.logger.debug("rechargeByCard: param fail");
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1030", "can not connect hazelcast");
            return res;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFailLucky79(user.getRechargeFail(), user.getRechargeFailTime());
                lbl126: // 8 sources:
                if (time <= 0L) {
                    if (UserValidaton.validateSerialPin((String)serial)) {
                        if (UserValidaton.validateSerialPin((String)pin)) {
                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                            TheCaoResponse response = null;
                            try {
                                Lucky79Client maxpay = new Lucky79Client(GameCommon.getValueStr("THE_CAO_MERCHANT_ID"), GameCommon.getValueStr("THE_CAO_SECRET_KEY"));
                                response = maxpay.doCharge(provider.getName(), pin, serial, id, amount);
                            }
                            catch (Lucky79Exception me) {
                                RechargeServiceImpl.logger.debug(me);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap Win qua the", "1034", "Loi inpput parameter: " + me.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++Lucky79Alert.maxpayDisconnect != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertMaxpayDisconnectTime, (int)1) == false) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong nap the ozze dang bi mat ket noi!", true);
                                Lucky79Alert.alertMaxpayDisconnectTime = new Date();
                                return null;
                            }
                            catch (Exception e) {
                                RechargeServiceImpl.logger.debug(e);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1034", "Loi ket noi maxpay: " + e.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++Lucky79Alert.maxpayDisconnect != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertMaxpayDisconnectTime, (int)1) == false) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong nap the ozze dang bi mat ket noi!", true);
                                Lucky79Alert.alertMaxpayDisconnectTime = new Date();
                                return null;
                            }
                            long money = 0L;
                            if (response != null) {
                                money = Math.round(response.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                                code = this.mapPayVietToVinplayCode(response.getStatus());
                                if (code == 0 && (response.getAmount() == 0.0 || response.getAmount() > (double)PhoneCardType._5M.getValue() || Double.valueOf(amount) % (double)PhoneCardType._10K.getValue() != 0.0)) {
                                    code = 30;
                                }
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, (int)response.getDeclared_value(), this.mapLucky79ToVinplayCode(response.getStatus()), response.getMessage(), code, (int)money, response.getDate(), (String)null, "lucky79", platform, (String)null);
                                message.setTranId(response.getTrans_id());
                                message.setRequestId(response.getRequest_id());
                            } else {
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, 0, -1, "0", code, 0, (String)null, (String)null, "lucky79", platform, (String)null);
                                ++Lucky79Alert.maxpayDisconnect;
                            }
                            try {
                                userMap = client.getMap("users");
                                userMap.lock(nickname);
                                user = (UserCacheModel)userMap.get(nickname);
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                if (code == 0) {
                                    user.setVin(moneyUser += money);
                                    user.setVinTotal(currentMoney += money);
                                    user.setRechargeMoney(rechargeMoney += money);
                                    user.setRechargeFail(0);
                                    description = "Kết quả: Th\u00e0nh c\u00f4ng, Mã GD: " + id + ", Thẻ: " + provider.getName() + ", Mệnh giá: " + response.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "Nạp Win qua Thẻ", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    userMap.put(nickname, user);
                                    res.setCurrentMoney(currentMoney);
                                    Lucky79Alert.maxpayDisconnect = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        Lucky79Alert.viettelPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        Lucky79Alert.mobiPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        Lucky79Alert.vinaPending = 0;
                                    }
                                    if (!provider.getName().equals(ProviderType.GATE.getName())) break lbl126;
                                    Lucky79Alert.gatePending = 0;
                                }
                                if (code == 30) {
                                    description = "Kết quả: Đang xử lý, Serial: " + serial;
									description1 = "Thẻ: " + provider.getName() + " " + response.getAmount() + ", Pin: " + pin;
                                    LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", description1, currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    Lucky79Alert.maxpayDisconnect = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        ++Lucky79Alert.viettelPending;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        ++Lucky79Alert.mobiPending;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        ++Lucky79Alert.vinaPending;
                                    }
                                    if (!provider.getName().equals(ProviderType.GATE.getName())) break lbl126;
                                    ++Lucky79Alert.gatePending;
                                }
                                if (code != 1) {
                                    cardFail = true;
                                }
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                            }
                            catch (Exception e2) {
                                code = 1;
                                RechargeServiceImpl.logger.debug(e2);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap Win qua the", "1001", e2.getMessage());
                                return null;
//                                var20_31 = null;
//                                return var20_31;
                            }
                            finally {
                                userMap.unlock(nickname);
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }

                if (cardFail) {
                    try {
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        user = (UserCacheModel)userMap.get(nickname);
                        user.setRechargeFail(user.getRechargeFail() + 1);
                        user.setRechargeFailTime(new Date());
                        userMap.put(nickname, user);
                    }
                    catch (Exception e3) {
                        code = 1;
                        RechargeServiceImpl.logger.debug(e3);
                        MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e3.getMessage());
                        return null;
//                        response = null;
//                        return response;
                    }
                    finally {
                        userMap.unlock(nickname);
                    }
                }
                res.setFail(user.getRechargeFail());
            }
            catch (Exception e4) {
                code = 1;
                RechargeServiceImpl.logger.debug(e4);
                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e4.getMessage());
                return null;
            }
        }
        res.setCode(code);
        RechargeServiceImpl.logger.debug(("Finish rechargeByCard Lucky79, Response : " + code));
        if (Lucky79Alert.viettelPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertViettelPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong nap the ozze tra ve pending qua 5 lan lien tiep Viettel!", true);
            Lucky79Alert.alertViettelPendingTime = new Date();
        }
        if (Lucky79Alert.mobiPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertMobiPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong nap the ozze tra ve pending qua 5 lan lien tiep Mobifone!", true);
            Lucky79Alert.alertMobiPendingTime = new Date();
        }
        if (Lucky79Alert.vinaPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertVinaPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong nap the ozze tra ve pending qua 5 lan lien tiep Vinaphone!", true);
            Lucky79Alert.alertVinaPendingTime = new Date();
        }
        if (Lucky79Alert.gatePending != GameCommon.getValueInt("COUNT_FAIL")) return res;
        if (VinPlayUtils.isAlertTimeout((Date)Lucky79Alert.alertGatePendingTime, (int)1) == false) return res;
        this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong nap the ozze tra ve pending qua 5 lan lien tiep FPT Gate!", true);
        Lucky79Alert.alertGatePendingTime = new Date();
        return res;
    }

    private String mapVinplayLucky79Message(String lucky79Code) {
        String trim;
        switch (trim = lucky79Code.trim()) {
            case "waiting": {
                return "Thẻ \u0111ang ch\u1edd \u0111\u1ec3 Nạp";
            }
            case "processing": {
                return "Thẻ \u0111ang \u0111\u01b0\u1ee3c Nạp tr\u00ean thi\u1ebft b\u1ecb";
            }
            case "success": {
                return "Nạp Thẻ th\u00e0nh c\u00f4ng";
            }
            case "card_fail": {
                return "Nạp Kh\u00f4ng Thẻ th\u00e0nh c\u00f4ng";
            }
        }
        return "Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
    }


    private int mapPayVietToVinplayCode(String lucky79Code) {
        String trim;
        switch (trim = lucky79Code.trim()) {
            case "99": {
                return 30;
            }

        }
        return 35;
    }

    private int mapThacaoToVinplayCode(String lucky79Code) {
        String trim;
        switch (trim = lucky79Code.trim()) {
            case "1": {
                return 0;
            }


        }
        return 30;
    }

    private int mapLucky79ToVinplayCode(String lucky79Code) {
        String trim;
        switch (trim = lucky79Code.trim()) {
            case "success": {
                return 0;
            }
            case "card_fail": {
                return 35;
            }
            case "processing": {
                return 30;
            }
            case "waiting": {
                return 30;
            }
        }
        return 30;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private RechargeResponse rechargeVcoinCard(String nickname, String serial, String pin, String platform) throws Exception {
        HazelcastInstance client;
        logger.debug(("Start rechargeVcoinCard:  nickname: " + nickname + ", serial: " + serial + ", pin: " + pin));
        int code = 1;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1) {
            logger.debug("rechargeByCard: param fail");
            return res;
        }
        if (pin != null) {
            pin = pin.trim().toUpperCase();
        }
        if (serial != null) {
            serial = serial.trim().toUpperCase();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1030", "can not connect hazelcast");
            return res;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                ChargeObj obj;
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFail(user.getRechargeFail(), user.getRechargeFailTime());
                if (time <= 0L) {
                    if (UserValidaton.validateSerialPin((String)serial)) {
                        if (UserValidaton.validateSerialPin((String)pin)) {
                            String transId = String.valueOf(VinPlayUtils.generateTransId());
                            obj = null;
                            VTCRechargeResponse response = null;
                            try {
                                response = VTCRechargeClient.rechargeVcoinCard(transId, serial, pin, nickname);
                            }
                            catch (Exception e) {
                                logger.debug(e);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1034", "Loi ket noi VTC: " + e.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++VTCAlert.disconnectRecharge != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (!VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertDisconnectRechargeTime, (int)1)) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong gach the vcoin VTC dang bi mat ket noi!", true);
                                VTCAlert.alertDisconnectRechargeTime = new Date();
                                return null;
                            }
                            if (response != null) {
                                code = response.getCode();
                                obj = response.getStatus() == -1 ? new ChargeObj(transId, ProviderType.VCOIN.getValue(), serial, pin, response.getAmount(), Integer.parseInt(String.valueOf(response.getResponseCode())), response.getDescription(), "vtc") : new ChargeObj(transId, ProviderType.VCOIN.getValue(), serial, pin, response.getAmount(), response.getStatus(), response.getDescription(), "vtc");
                            }
                            long money = 0L;
                            if (obj != null) {
                                money = Math.round((double)obj.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                                if (code == 0 && (obj.getAmount() == 0 || obj.getAmount() > PhoneCardType._5M.getValue() || obj.getAmount() % PhoneCardType._10K.getValue() != 0)) {
                                    code = 30;
                                }
                                message = new RechargeByCardMessage(nickname, transId, ProviderType.VCOIN.getName(), serial, pin, obj.getAmount(), obj.getStatus(), obj.getMessage(), code, (int)money, (String)null, (String)null, "vtc", platform, (String)null);
                            } else {
                                message = new RechargeByCardMessage(nickname, transId, ProviderType.VCOIN.getName(), serial, pin, 0, -1, "L\u00e1\u00bb\u2014i k\u00e1\u00ba\u00bft n\u00e1\u00bb\u2018i VTC recharge", code, 0, (String)null, (String)null, "vtc", platform, (String)null);
                                ++VTCAlert.disconnectRecharge;
                            }
                            try {
                                userMap = client.getMap("users");
                                userMap.lock(nickname);
                                user = (UserCacheModel)userMap.get(nickname);
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                if (code == 0) {
                                    user.setVin(moneyUser += money);
                                    user.setVinTotal(currentMoney += money);
                                    user.setRechargeMoney(rechargeMoney += money);
                                    user.setRechargeFail(0);
                                    String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: Th\u00c3\u00a0nh c\u00c3\u00b4ng, M\u00c3\u00a3 GD: " + transId + ", Th\u00e1\u00ba\u00bb: " + ProviderType.VCOIN.getName() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "N\u00e1\u00ba\u00a1p Vin b\u00e1\u00ba\u00b1ng th\u00e1\u00ba\u00bb", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    userMap.put(nickname, user);
                                    res.setCurrentMoney(currentMoney);
                                    VTCAlert.disconnectRecharge = 0;
                                    VTCAlert.pendingRecharge = 0;
                                }
                                if (code == 30) {
                                    String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: \u00c4\ufffdang x\u00e1\u00bb\u00ad l\u00c3\u00bd, M\u00c3\u00a3 GD: " + transId + ", Th\u00e1\u00ba\u00bb: " + ProviderType.VCOIN.getName() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                    LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "N\u00e1\u00ba\u00a1p Vin b\u00e1\u00ba\u00b1ng th\u00e1\u00ba\u00bb", currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    VTCAlert.disconnectRecharge = 0;
                                    ++VTCAlert.pendingRecharge;
                                }
                                if (code != 1) {
                                    cardFail = true;
                                }
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                            }
                            catch (Exception e2) {
                                code = 1;
                                logger.debug(e2);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the Vcoin", "1001", e2.getMessage());
                                RechargeResponse rechargeResponse = null;
                                return rechargeResponse;
                            }
                            finally {
                                userMap.unlock(nickname);
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }
                if (cardFail) {
                    try {
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        user = (UserCacheModel)userMap.get(nickname);
                        user.setRechargeFail(user.getRechargeFail() + 1);
                        user.setRechargeFailTime(new Date());
                        userMap.put(nickname, user);
                    }
                    catch (Exception e3) {
                        code = 1;
                        logger.debug(e3);
                        MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the Vcoin", "1001", e3.getMessage());
                        return null;
//                        obj = null;
//                        return obj;
                    }
                    finally {
                        userMap.unlock(nickname);
                    }
                }
                res.setFail(user.getRechargeFail());
            }
            catch (Exception e4) {
                code = 1;
                logger.debug(e4);
                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the Vcoin", "1001", e4.getMessage());
                return null;
            }
        }
        res.setCode(code);
        logger.debug(("Finish rechargeVcoinCard, Response : " + code));
        if (VTCAlert.pendingRecharge != GameCommon.getValueInt("COUNT_FAIL")) return res;
        if (!VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertPendingRechargeTime, (int)1)) return res;
        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "[CANH BAO] He thong VTC tra ve pending qua 5 lan lien tiep Vcoin!", true);
        VTCAlert.alertPendingRechargeTime = new Date();
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    private RechargeResponse rechargeByCardDvt(String nickname, ProviderType provider, String serial, String pin, String platform) throws Exception {
        RechargeServiceImpl.logger.debug(("Start rechargeByCard Dvt:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin));
        int code = 1;
        HazelcastInstance client;
        String description;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1 || provider == null) {
            RechargeServiceImpl.logger.debug("rechargeByCard: param fail");
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1030", "can not connect hazelcast");
            return res;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFail(user.getRechargeFail(), user.getRechargeFailTime());
                lbl120: // 8 sources:
                if (time <= 0L) {
                    if (UserValidaton.validateSerialPin((String)serial)) {
                        if (UserValidaton.validateSerialPin((String)pin)) {
                            String id = String.valueOf(VinPlayUtils.generateTransId());
                            ChargeObj obj = null;
                            try {
                                obj = VinplayClient.rechargeByCard(id, provider.getValue(), serial, pin);
                            }
                            catch (Exception e) {
                                RechargeServiceImpl.logger.debug(e);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1034", "Loi ket noi dvt: " + e.getMessage());
                                DvtUtils.errorDvt(client, "RechargeByCard");
                                if (++DvtAlert.disconnectRecharge != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                if (VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertDisconnectRechargeTime, (int)1) == false) return null;
                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong dichvuthe dang bi mat ket noi!", true);
                                DvtAlert.alertDisconnectRechargeTime = new Date();
                                return null;
                            }
                            long money = 0L;
                            if (obj != null) {
                                money = Math.round((double)obj.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                                code = this.getErrorCode(obj.getStatus());
                                if (code == 0 && (obj.getAmount() == 0 || obj.getAmount() > PhoneCardType._5M.getValue() || obj.getAmount() % PhoneCardType._10K.getValue() != 0)) {
                                    code = 30;
                                }
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, obj.getAmount(), obj.getStatus(), obj.getMessage(), code, (int)money, (String)null, (String)null, obj.getChannel(), platform, (String)null);
                            } else {
                                message = new RechargeByCardMessage(nickname, id, provider.getName(), serial, pin, 0, -1, "L\u00e1\u00bb\u2014i k\u00e1\u00ba\u00bft n\u00e1\u00bb\u2018i D\u00e1\u00bb\u2039ch v\u00e1\u00bb\u00a5 th\u00e1\u00ba\u00bb", code, 0, (String)null, (String)null, "dvt", platform, (String)null);
                                ++DvtAlert.disconnectRecharge;
                            }
                            try {
                                userMap = client.getMap("users");
                                userMap.lock(nickname);
                                user = (UserCacheModel)userMap.get(nickname);
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                MoneyMessageInMinigame messageMoney;
                                if (code == 0) {
                                    user.setVin(moneyUser += money);
                                    user.setVinTotal(currentMoney += money);
                                    user.setRechargeMoney(rechargeMoney += money);
                                    user.setRechargeFail(0);
                                    description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: Th\u00c3\u00a0nh c\u00c3\u00b4ng, M\u00c3\u00a3 GD: " + id + ", Th\u00e1\u00ba\u00bb: " + provider.getName() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                    messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "N\u00e1\u00ba\u00a1p Vin b\u00e1\u00ba\u00b1ng th\u00e1\u00ba\u00bb", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    userMap.put(nickname, user);
                                    res.setCurrentMoney(currentMoney);
                                    DvtAlert.disconnectRecharge = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        DvtAlert.viettelPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        DvtAlert.mobiPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        DvtAlert.vinaPending = 0;
                                    }
                                    if (provider.getName().equals(ProviderType.GATE.getName())) {
                                        DvtAlert.gatePending = 0;
                                    }
                                    if (!provider.getName().equals(ProviderType.VCOIN.getName())) break lbl120;
                                    DvtAlert.vcoinPending = 0;
                                }
                                if (code == 30) {
                                    description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: \u00c4\ufffdang x\u00e1\u00bb\u00ad l\u00c3\u00bd, M\u00c3\u00a3 GD: " + id + ", Th\u00e1\u00ba\u00bb: " + provider.getName() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                    LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "N\u00e1\u00ba\u00a1p Vin b\u00e1\u00ba\u00b1ng th\u00e1\u00ba\u00bb", currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                    DvtAlert.disconnectRecharge = 0;
                                    if (provider.getName().equals(ProviderType.VIETTEL.getName())) {
                                        ++DvtAlert.viettelPending;
                                    }
                                    if (provider.getName().equals(ProviderType.MOBIFONE.getName())) {
                                        ++DvtAlert.mobiPending;
                                    }
                                    if (provider.getName().equals(ProviderType.VINAPHONE.getName())) {
                                        ++DvtAlert.vinaPending;
                                    }
                                    if (provider.getName().equals(ProviderType.GATE.getName())) {
                                        ++DvtAlert.gatePending;
                                    }
                                    if (!provider.getName().equals(ProviderType.VCOIN.getName())) break lbl120;
                                    ++DvtAlert.vcoinPending;
                                }
                                if (code != 1) {
                                    cardFail = true;
                                }
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                            }
                            catch (Exception e2) {
                                code = 1;
                                RechargeServiceImpl.logger.debug(e2);
                                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e2.getMessage());
                                return null;
//                                var20_29 = null;
//                                return var20_29;
                            }
                            finally {
                                userMap.unlock(nickname);
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }

                if (cardFail) {
                    try {
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        user = (UserCacheModel)userMap.get(nickname);
                        user.setRechargeFail(user.getRechargeFail() + 1);
                        user.setRechargeFailTime(new Date());
                        userMap.put(nickname, user);
                    }
                    catch (Exception e3) {
                        code = 1;
                        RechargeServiceImpl.logger.debug(e3);
                        MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e3.getMessage());
                        return null;
//                        obj = null;
//                        return obj;
                    }
                    finally {
                        userMap.unlock(nickname);
                    }
                }
                res.setFail(user.getRechargeFail());
            }
            catch (Exception e4) {
                code = 1;
                RechargeServiceImpl.logger.debug(e4);
                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e4.getMessage());
                return null;
            }
        }
        res.setCode(code);
        RechargeServiceImpl.logger.debug(("Finish rechargeByCard Dvt, Response : " + code));
        if (DvtAlert.viettelPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertViettelPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong DVT tra ve pending qua 5 lan lien tiep Viettel!", true);
            DvtAlert.alertViettelPendingTime = new Date();
        }
        if (DvtAlert.mobiPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertMobiPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong DVT tra ve pending qua 5 lan lien tiep Mobifone!", true);
            DvtAlert.alertMobiPendingTime = new Date();
        }
        if (DvtAlert.vinaPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertVinaPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong DVT tra ve pending qua 5 lan lien tiep Vinaphone!", true);
            DvtAlert.alertVinaPendingTime = new Date();
        }
        if (DvtAlert.gatePending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertGatePendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong DVT tra ve pending qua 5 lan lien tiep FPT Gate!", true);
            DvtAlert.alertGatePendingTime = new Date();
        }
        if (DvtAlert.vcoinPending != GameCommon.getValueInt("COUNT_FAIL")) return res;
        if (VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertVcoinPendingTime, (int)1) == false) return res;
        this.alert(GameCommon.getValueStr("PENDING_GROUP_NUMBER"), "[CANH BAO] He thong DVT tra ve pending qua 5 lan lien tiep FPT Gate!", true);
        DvtAlert.alertVcoinPendingTime = new Date();
        return res;
    }

    private String mapVinplayMessage(String maxpayCode) {
        switch (maxpayCode) {
            case "200": {
                return "G\u1ecdi api th\u00e0nh c\u00f4ng";
            }
            case "400": {
                return "D\u1eef li\u1ec7u g\u1eedi l\u00ean kh\u00f4ng ch\u00ednh x\u00e1c";
            }
            case "404": {
                return "Kh\u00f4ng t\u00ecm th\u1ea5y giao d\u1ecbch Thẻ";
            }
            case "1": {
                return "Nạp Thẻ th\u00e0nh c\u00f4ng";
            }
            case "2": {
                return "Thẻ sai ho\u1eb7c \u0111\u00e3 s\u1eed d\u1ee5ng";
            }
            case "3": {
                return "Thẻ b\u1ecb kh\u00f3a";
            }
            case "4": {
                return "S\u1ed1 l\u1ea7n Nạp Thẻ sai li\u00ean ti\u1ebfp v\u01b0\u1ee3t quy \u0111\u1ecbnh";
            }
            case "5": {
                return "Th\u00f4ng tin merchant sai";
            }
            case "6": {
                return "Ch\u01b0a truy\u1ec1n transaction id";
            }
            case "7": {
                return "Thẻ sai \u0111\u1ecbnh d\u1ea1ng";
            }
            case "8": {
                return "Kh\u00f4ng t\u00ecm th\u1ea5y nh\u00e0 cung c\u1ea5p Thẻ";
            }
            case "9": {
                return "Th\u00f4ng tin Session sai";
            }
            case "10": {
                return "Session timeout";
            }
            case "11": {
                return "L\u1ed7i h\u1ec7 th\u1ed1ng nh\u00e0 cung c\u1ea5p";
            }
            case "12": {
                return "Merchant b\u1ecb kh\u00f3a";
            }
            case "13": {
                return "Ip kh\u00f4ng h\u1ee3p l\u1ec7";
            }
            case "14": {
                return "Transaction id b\u1ecb tr\u00f9ng";
            }
            case "15": {
                return "Thẻ kh\u00f4ng h\u1ee3p l\u1ec7";
            }
            case "16": {
                return "Lo\u1ea1i Thẻ b\u1ecb kh\u00f3a";
            }
            case "17": {
                return "Thẻ \u0111ang x\u1eed l\u00fd";
            }
            case "96": {
                return "Nạp Thẻ th\u1ea5t b\u1ea1i";
            }
            case "98": {
                return "Ch\u1edd x\u1eed l\u00fd";
            }
            case "99": {
                return "Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            }
        }
        return "Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
    }

    private int mapVinplayCode(String maxpayCode) {
        switch (maxpayCode) {
            case "1": {
                return 0;
            }
            case "2": {
                return 31;
            }
            case "3": {
                return 32;
            }
            case "16": {
                return 32;
            }
            case "7": {
                return 35;
            }
            case "4": {
                return 1;
            }
            case "400": {
                return 1;
            }
            case "404": {
                return 1;
            }
            case "8": {
                return 1;
            }
            case "15": {
                return 1;
            }
            case "96": {
                return 1;
            }
            case "9": {
                return 1;
            }
            case "10": {
                return 1;
            }
            case "11": {
                return 1;
            }
            case "12": {
                return 1;
            }
            case "14": {
                return 1;
            }
            case "5": {
                return 1;
            }
            case "6": {
                return 1;
            }
            case "13": {
                return 1;
            }
            case "17": {
                return 30;
            }
            case "98": {
                return 30;
            }
            case "99": {
                return 30;
            }
        }
        return 30;
    }

    @Override
    public Map<String, Long> updatePendingCardStatus(String startTime, String endTime, String actor) throws Exception {
        HashMap<String, Long> mapRes = new HashMap<String, Long>();
        long totalRecord = 0L;
        long successRecord = 0L;
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1) {
            return mapRes;
        }
        RechargeDaoImpl dao = new RechargeDaoImpl();
        List<RechargeByCardMessage> listPending = dao.getListCardPending(startTime, endTime);
        totalRecord = listPending.size();
        for (RechargeByCardMessage message : listPending) {
            message.setError((String)null);
            if (message.getPartner().equals("maxpay") || message.getPartner().equals("dvt")) {
                if ((message = this.reCheckRechargeByCardMaxpay(message, actor)).getError() != null) continue;
                ++successRecord;
                continue;
            }
            if (!message.getPartner().equals("epay") || !message.getProvider().equals("MegaCard") || (message = this.reCheckRechargeByMegaCard(message, actor)).getError() != null) continue;
            ++successRecord;
        }
        mapRes.put("totalRecord", totalRecord);
        mapRes.put("successRecord", successRecord);
        return mapRes;
    }

    @Override
    public RechargeByCardMessage updatePendingCardStatus(String referenceId, String actor) throws Exception {
        RechargeDaoImpl dao = new RechargeDaoImpl();
        RechargeByCardMessage pendingCard = dao.getPendingCardByReferenceId(referenceId);
        if (pendingCard != null && pendingCard.getCode() == 30) {
            pendingCard.setError((String)null);
            if (pendingCard.getPartner().equals("maxpay")  || pendingCard.getPartner().equals("dvt")) {
                pendingCard = this.reCheckRechargeByCardMaxpay(pendingCard, actor);
            } else if (pendingCard.getPartner().equals("epay") && pendingCard.getProvider().equals("MegaCard")) {
                pendingCard = this.reCheckRechargeByMegaCard(pendingCard, actor);
            }else if(pendingCard.getPartner().equals("lucky79")){
                pendingCard = this.reCheckRechargeByCardLucky(pendingCard, actor);
            }
        }
        return pendingCard;
    }

    @Override
    public Map<String, Long> reCheckRechargeByCard() throws Exception {
        HashMap<String, Long> mapRes = new HashMap<String, Long>();
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1) {
            return mapRes;
        }
        RechargeDaoImpl dao = new RechargeDaoImpl();
        List<RechargeByCardMessage> listPending = dao.getListCardPending();
        for (RechargeByCardMessage message : listPending) {
            message.setError((String)null);
            if (message.getPartner().equals("dvt") || message.getPartner().equals("maxpay")) {
                this.reCheckRechargeByCardMaxpay(message, "system");
                continue;
            }
            if (!message.getPartner().equals("epay") || !message.getProvider().equals("MegaCard")) continue;
            this.reCheckRechargeByMegaCard(message, "system");
        }
        return mapRes;
    }


    private RechargeByCardMessage reCheckRechargeByCardLucky(RechargeByCardMessage message, String actor) {
        block19 : {
            try {
                String response_code;
                TheCaoResponse response = null;
                RechargeDaoImpl dao = new RechargeDaoImpl();
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                Lucky79Client maxpay = new Lucky79Client(GameCommon.getValueStr("THE_CAO_MERCHANT_ID"), GameCommon.getValueStr("THE_CAO_SECRET_KEY"));
                response = maxpay.doReCheck1(message);
                logger.debug(response);
                if (response == null) break block19;
                if ("1".equals(response.getResponse_code())) {
                    int code = this.mapThacaoToVinplayCode(response.getStatus());
                    if (code == 30) break block19;
                    long money = 0L;
                    if (code == 0) {
                        if (userMap.containsKey(message.getNickname())) {
                            money = Math.round(response.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                            try {
                                userMap.lock(message.getNickname());
                                UserCacheModel user = (UserCacheModel)userMap.get(message.getNickname());
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                user.setVin(moneyUser += money);
                                user.setVinTotal(currentMoney += money);
                                user.setRechargeMoney(rechargeMoney += money);
                                String description = "Mã GD: " + message.getReferenceId() + ", Thẻ: " + message.getProvider() + ", Mệnh giá: " + response.getAmount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), message.getNickname(), "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), message.getNickname(), "RechargeByCard", "Nạp VIN th\u00e0nh c\u00f4ng", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                userMap.put(message.getNickname(), user);
                            }
                            catch (Exception e) {
                                logger.debug(e);
                                code = 1;
                                message.setError("1037");
                            }
                            finally {
                                userMap.unlock(message.getNickname());
                            }
                            if (code == 0) {
                                ArrayList<String> nicknames = new ArrayList<String>();
                                nicknames.add(message.getNickname());
                                MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                                String content = "Nạp VIN th\u00e0nh c\u00f4ng. B\u1ea1n \u0111\u00e3 nh\u00e2n \u0111\u01b0\u1ee3c " + money + " Vin. Mã GD: " + message.getReferenceId() + ", Thẻ: " + message.getProvider() + ", Mệnh giá: " + response.getAmount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                                mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00f4ng b\u00e1o Nạp VIN th\u00e0nh c\u00f4ng", content);
                                dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getAmount()), String.valueOf(response.getCode()), this.mapVinplayMessage(response.getCode()), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                                dao.updateCard(message.getReferenceId(), (int)response.getAmount(), Integer.parseInt(response.getStatus()), this.mapVinplayMessage(response.getStatus()), code);
                                message.setAmount((int)response.getAmount());
                                message.setStatus(Integer.parseInt(response.getStatus()));
                                message.setMessage(this.mapVinplayMessage(response.getStatus()));
                                message.setCode(code);
                            }
                            break block19;
                        }
                        message.setError("1038");
                        break block19;
                    }
                    ArrayList<String> nicknames = new ArrayList<String>();
                    nicknames.add(message.getNickname());
                    MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                    String content = "Nạp VIN th\u1ea5t b\u1ea1i. Mã GD: " + message.getReferenceId() + ", Thẻ: " + message.getProvider() + ", Mệnh giá: " + response.getAmount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin() + ". L\u00fd do: " + this.mapVinplayMessage(response.getCode());
                    mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00f4ng b\u00e1o Nạp VIN th\u1ea5t b\u1ea1i", content);
                    dao.updateCard(message.getReferenceId(), (int)response.getAmount(), Integer.parseInt(response.getCode()), this.mapVinplayMessage(response.getCode()), code);
                    dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getAmount()), String.valueOf(response.getCode()), this.mapVinplayMessage(response.getCode()), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                    message.setAmount((int)response.getAmount());
                    message.setStatus(Integer.parseInt(response.getCode()));
                    message.setMessage(this.mapVinplayMessage(response.getCode()));
                    message.setCode(code);
                    break block19;
                }
                switch (response_code = response.getResponse_code()) {
                    case "400": {
                        message.setError("1039");
                        break;
                    }
                    case "404": {
                        message.setError("1040");
                        break;
                    }
                    default: {
                        message.setError("1041");
                    }
                }
                dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), "0", String.valueOf(response.getResponse_code()), this.mapVinplayMessage(response.getResponse_code()), String.valueOf(this.mapVinplayCode(response.getResponse_code())), message.getTimeLog(), "0", actor);
                dao.updateCard(message.getReferenceId(), 0, Integer.parseInt(response.getResponse_code()), this.mapVinplayMessage(response.getResponse_code()), this.mapVinplayCode(response.getResponse_code()));
            }
            catch (Exception e2) {
                e2.printStackTrace();
                logger.debug(e2);
            }
        }
        return message;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargeByCardMessage reCheckRechargeByCardMaxpay(RechargeByCardMessage message, String actor) {
        block19 : {
            try {
                String response_code;
                ReCheckMaxpayResponse response = null;
                RechargeDaoImpl dao = new RechargeDaoImpl();
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                MaxpayClient maxpay = new MaxpayClient(GameCommon.getValueStr("MAXPAY_MERCHANT_ID"), GameCommon.getValueStr("MAXPAY_SECRET_KEY"));
                response = maxpay.doReCheck(message.getReferenceId());
                if (response == null) break block19;
                if ("200".equals(response.getResponse_code())) {
                    int code = this.mapVinplayCode(response.getCode());
                    if (code == 30) break block19;
                    long money = 0L;
                    if (code == 0) {
                        if (userMap.containsKey(message.getNickname())) {
                            money = Math.round(response.getCard_amount() * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                            try {
                                 userMap.lock(message.getNickname());
                                UserCacheModel user = (UserCacheModel)userMap.get(message.getNickname());
                                long moneyUser = user.getVin();
                                long currentMoney = user.getVinTotal();
                                long rechargeMoney = user.getRechargeMoney();
                                user.setVin(moneyUser += money);
                                user.setVinTotal(currentMoney += money);
                                user.setRechargeMoney(rechargeMoney += money);
                                String description = "M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + response.getCard_amount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), message.getNickname(), "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), message.getNickname(), "RechargeByCard", "N\u00e1\u00ba\u00a1p vin b\u00e1\u00ba\u00b3ng th\u00e1\u00ba\u00bb", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                 userMap.put(message.getNickname(), user);
                            }
                            catch (Exception e) {
                                logger.debug(e);
                                code = 1;
                                message.setError("1037");
                            }
                            finally {
                                 userMap.unlock(message.getNickname());
                            }
                            if (code == 0) {
                                ArrayList<String> nicknames = new ArrayList<String>();
                                nicknames.add(message.getNickname());
                                MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                                String content = "N\u00e1\u00ba\u00a1p Vin th\u00c3\u00a0nh c\u00c3\u00b4ng. B\u00e1\u00ba\u00a1n \u00c4\u2018\u00c3\u00a3 nh\u00e1\u00ba\u00adn \u00c4\u2018\u00c6\u00b0\u00e1\u00bb\u00a3c " + money + " Vin. M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + response.getCard_amount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                                mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00c3\u00b4ng b\u00c3\u00a1o n\u00e1\u00ba\u00a1p Vin th\u00c3\u00a0nh c\u00c3\u00b4ng", content);
                                dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getCard_amount()), String.valueOf(response.getCode()), this.mapVinplayMessage(response.getCode()), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                                dao.updateCard(message.getReferenceId(), (int)response.getCard_amount(), Integer.parseInt(response.getCode()), this.mapVinplayMessage(response.getCode()), code);
                                message.setAmount((int)response.getCard_amount());
                                message.setStatus(Integer.parseInt(response.getCode()));
                                message.setMessage(this.mapVinplayMessage(response.getCode()));
                                message.setCode(code);
                            }
                            break block19;
                        }
                        message.setError("1038");
                        break block19;
                    }
                    ArrayList<String> nicknames = new ArrayList<String>();
                    nicknames.add(message.getNickname());
                    MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                    String content = "N\u00e1\u00ba\u00a1p Vin th\u00e1\u00ba\u00a5t b\u00e1\u00ba\u00a1i. M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + response.getCard_amount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin() + ". L\u00c3\u00bd do: " + this.mapVinplayMessage(response.getCode());
                    mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00c3\u00b4ng b\u00c3\u00a1o n\u00e1\u00ba\u00a1p Vin th\u00e1\u00ba\u00a5t b\u00e1\u00ba\u00a1i", content);
                    dao.updateCard(message.getReferenceId(), (int)response.getCard_amount(), Integer.parseInt(response.getCode()), this.mapVinplayMessage(response.getCode()), code);
                    dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getCard_amount()), String.valueOf(response.getCode()), this.mapVinplayMessage(response.getCode()), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                    message.setAmount((int)response.getCard_amount());
                    message.setStatus(Integer.parseInt(response.getCode()));
                    message.setMessage(this.mapVinplayMessage(response.getCode()));
                    message.setCode(code);
                    break block19;
                }
                switch (response_code = response.getResponse_code()) {
                    case "400": {
                        message.setError("1039");
                        break;
                    }
                    case "404": {
                        message.setError("1040");
                        break;
                    }
                    default: {
                        message.setError("1041");
                    }
                }
                dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), "0", String.valueOf(response.getResponse_code()), this.mapVinplayMessage(response.getResponse_code()), String.valueOf(this.mapVinplayCode(response.getResponse_code())), message.getTimeLog(), "0", actor);
                dao.updateCard(message.getReferenceId(), 0, Integer.parseInt(response.getResponse_code()), this.mapVinplayMessage(response.getResponse_code()), this.mapVinplayCode(response.getResponse_code()));
            }
            catch (Exception e2) {
                e2.printStackTrace();
                logger.debug(e2);
            }
        }
        return message;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargeByCardMessage reCheckRechargeByMegaCard(RechargeByCardMessage message, String actor) {
        block10 : {
            try {
                int code;
                ChargeReponse response = null;
                RechargeDaoImpl dao = new RechargeDaoImpl();
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                String userNameMega = "";
                userNameMega = message.getUserNameMega() == null || message.getUserNameMega().isEmpty() ? GameCommon.getValueStr("MEGA_USER") : message.getUserNameMega();
                EpayMegaCardCharging.updateConfigMega(userNameMega);
                response = EpayMegaCardCharging.getTransactionStatus(EpayMegaCardCharging.login(), message.getReferenceId());
                if (response == null || (code = this.mapErrorCodeEpayMegaCard(response.getStatus())) == 30) break block10;
                long money = 0L;
                if (code == 0) {
                    if (userMap.containsKey(message.getNickname())) {
                        money = Math.round((double)Integer.parseInt(response.getAmount()) * GameCommon.getValueDouble("RATIO_NAP_MEGA_CARD"));
                        try {
                             userMap.lock(message.getNickname());
                            UserCacheModel user = (UserCacheModel)userMap.get(message.getNickname());
                            long moneyUser = user.getVin();
                            long currentMoney = user.getVinTotal();
                            long rechargeMoney = user.getRechargeMoney();
                            user.setVin(moneyUser += money);
                            user.setVinTotal(currentMoney += money);
                            user.setRechargeMoney(rechargeMoney += money);
                            String description = "M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + response.getAmount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), message.getNickname(), "RechargeByMegaCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), message.getNickname(), "RechargeByMegaCard", "N\u00e1\u00ba\u00a1p vin b\u00e1\u00ba\u00b3ng th\u00e1\u00ba\u00bb", currentMoney, money, "vin", description, 0L, false, user.isBot());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                             userMap.put(message.getNickname(), user);
                        }
                        catch (Exception e) {
                            logger.debug(e);
                            code = 1;
                            message.setError("1037");
                        }
                        finally {
                             userMap.unlock(message.getNickname());
                        }
                        if (code == 0) {
                            ArrayList<String> nicknames = new ArrayList<String>();
                            nicknames.add(message.getNickname());
                            MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                            String content = "N\u00e1\u00ba\u00a1p Vin th\u00c3\u00a0nh c\u00c3\u00b4ng. B\u00e1\u00ba\u00a1n \u00c4\u2018\u00c3\u00a3 nh\u00e1\u00ba\u00adn \u00c4\u2018\u00c6\u00b0\u00e1\u00bb\u00a3c " + money + " Vin. M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + response.getAmount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                            mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00c3\u00b4ng b\u00c3\u00a1o n\u00e1\u00ba\u00a1p Vin th\u00c3\u00a0nh c\u00c3\u00b4ng", content);
                            dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), response.getAmount(), response.getStatus(), response.getMessage(), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                            dao.updateCard(message.getReferenceId(), Integer.parseInt(response.getAmount()), Integer.parseInt(response.getStatus()), response.getMessage(), code);
                            message.setAmount(Integer.parseInt(response.getAmount()));
                            message.setStatus(Integer.parseInt(response.getStatus()));
                            message.setMessage(response.getMessage());
                            message.setCode(code);
                        }
                        break block10;
                    }
                    message.setError("1038");
                    break block10;
                }
                ArrayList<String> nicknames = new ArrayList<String>();
                nicknames.add(message.getNickname());
                MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                String content = "N\u00e1\u00ba\u00a1p Vin th\u00e1\u00ba\u00a5t b\u00e1\u00ba\u00a1i. M\u00c3\u00a3 GD: " + message.getReferenceId() + ", Th\u00e1\u00ba\u00bb: " + message.getProvider() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin() + ". L\u00c3\u00bd do: " + response.getMessage();
                mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00c3\u00b4ng b\u00c3\u00a1o n\u00e1\u00ba\u00a1p Vin th\u00e1\u00ba\u00a5t b\u00e1\u00ba\u00a1i", content);
                dao.updateCard(message.getReferenceId(), Integer.parseInt(response.getAmount()), Integer.parseInt(response.getStatus()), response.getMessage(), code);
                dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), response.getAmount(), response.getStatus(), response.getMessage(), String.valueOf(code), message.getTimeLog(), String.valueOf(money), actor);
                message.setAmount(Integer.parseInt(response.getAmount()));
                message.setStatus(Integer.parseInt(response.getStatus()));
                message.setMessage(response.getMessage());
                message.setCode(code);
            }
            catch (Exception e2) {
                e2.printStackTrace();
                logger.debug(e2);
            }
        }
        return message;
    }

    @Override
    public I2BResponse rechargeByBank(String nickname, long money, byte bank, String ip, String platform) throws Exception {
        int code = 1;
        I2BResponse res = new I2BResponse("", code);
        if (GameCommon.getValueInt("IS_RECHARGE_BANK") == 1) {
            return res;
        }
        if (money >= 0L) {
            if (GameCommon.getValueInt("NL_OPEN") != 0) {
                res = NganLuongUtils.setExpressCheckout(nickname, money, bank, ip);
                return res;
            }
            I2BType bankType = I2BType.getBankById((int)bank);
            if (bankType != null) {
                String version = GameCommon.getValueStr("NAPAS_VERSION");
                String command = "pay";
                String accessCode = GameCommon.getValueStr("NAPAS_ACCESS_CODE");
                UserDaoImpl userDao = new UserDaoImpl();
                int userId = userDao.getIdByNickname(nickname);
                String transId = "VP_" + userId + System.currentTimeMillis();
                String merchant = GameCommon.getValueStr("NAPAS_MERCHANT");
                String orderInfo = "N\u00e1\u00ba\u00a1p vin qua ng\u00c3\u00a2n h\u00c3\u00a0ng";
                long amount = money * 100L;
                String urlReturn = GameCommon.getValueStr("NAPAS_URL_RESULT");
                String urlBack = GameCommon.getValueStr("NAPAS_URL_CANCEL");
                String locale = "vn";
                String currencyCode = "VND";
                String paymentGateway = "ATM";
                String cardType = bankType.getName();
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("vpc_Version", version);
                fields.put("vpc_Command", "pay");
                fields.put("vpc_AccessCode", accessCode);
                fields.put("vpc_MerchTxnRef", transId);
                fields.put("vpc_Merchant", merchant);
                fields.put("vpc_OrderInfo", "N\u00e1\u00ba\u00a1p vin qua ng\u00c3\u00a2n h\u00c3\u00a0ng");
                fields.put("vpc_Amount", String.valueOf(amount));
                fields.put("vpc_ReturnURL", urlReturn);
                fields.put("vpc_BackURL ", urlBack);
                fields.put("vpc_Locale", "vn");
                fields.put("vpc_CurrencyCode", "VND");
                fields.put("vpc_TicketNo", ip);
                fields.put("vpc_PaymentGateway", "ATM");
                fields.put("vpc_CardType", bankType.getValue());
                RechargeByBankMessage message = new RechargeByBankMessage(nickname, money, cardType, transId, String.valueOf(amount), "N\u00e1\u00ba\u00a1p vin qua ng\u00c3\u00a2n h\u00c3\u00a0ng", ip, platform);
                RechargeDaoImpl dao = new RechargeDaoImpl();
                if (dao.logRechargeByBank(message)) {
                    String url = NapasUtils.getRedirectUrl(fields);
                    logger.debug(("NAPAS Request: " + url));
                    code = 0;
                    res.setUrl(url);
                }
            } else {
                code = 3;
            }
        } else {
            code = 2;
        }
        res.setCode(code);
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void receiveResultFromBank(Map<String, String[]> request) {
        block22 : {
            try {
                if (GameCommon.getValueInt("NL_OPEN") == 0) {
                    StringBuilder log = new StringBuilder("");
                    HashMap<String, String> fields = new HashMap<String, String>();
                    for (Map.Entry<String, String[]> entry : request.entrySet()) {
                        String fieldName = entry.getKey();
                        String fieldValue = entry.getValue()[0];
                        log.append(fieldName).append(":").append(fieldValue).append(", ");
                        if (fieldValue == null || fieldValue.length() <= 0) continue;
                        fields.put(fieldName, fieldValue);
                    }
                    logger.debug(("NAPAS Response: " + log.toString()));
                    String txnResponseCode = (String)fields.get("vpc_ResponseCode");
                    String merchTxnRef = (String)fields.get("vpc_MerchTxnRef");
                    String amount = (String)fields.get("vpc_Amount");
                    String version = (String)fields.get("vpc_Version");
                    String command = (String)fields.get("vpc_Command");
                    String merchantID = (String)fields.get("vpc_Merchant");
                    String orderInfo = (String)fields.get("vpc_OrderInfo");
                    String currency = (String)fields.get("vpc_CurrencyCode");
                    String locale = (String)fields.get("vpc_Locale");
                    String cardType = (String)fields.get("vpc_CardType");
                    String transactionNo = (String)fields.get("vpc_TransactionNo");
                    String message = (String)fields.get("vpc_Message");
                    if (transactionNo == null) {
                        transactionNo = "";
                    }
                    if (message == null) {
                        message = "";
                    }
                    long money = 0L;
                    String vpcTxnSecureHash = (String)fields.remove("vpc_SecureHash");
                    String secureHash = NapasUtils.hashAllFields(fields);
                    RechargeDaoImpl dao = new RechargeDaoImpl();
                    RechargeByBankMessage mes = null;
                    try {
                        if (merchTxnRef != null) {
                            mes = dao.getRechargeByBank(merchTxnRef);
                        }
                    }
                    catch (Exception e) {
                        logger.debug(e);
                    }
                    String description = "";
                    if (mes == null) {
                        description = "Kh\u00c3\u00b4ng t\u00c3\u00acm th\u00e1\u00ba\u00a5y giao d\u00e1\u00bb\u2039ch";
                    }
                    if (!vpcTxnSecureHash.equalsIgnoreCase(secureHash)) {
                        description = "checksum kh\u00c3\u00b4ng ch\u00c3\u00adnh x\u00c3\u00a1c";
                    }
                    if (txnResponseCode == null) {
                        description = "responseCode is null";
                    }
                    if (description.isEmpty()) {
                        block21 : {
                            description = NapasUtils.getResponseDescription(txnResponseCode);
                            String nickname = mes.getNickname();
                            try {
                                if (!txnResponseCode.equals("0")) break block21;
                                money = Long.parseLong(mes.getAmount()) / 100L;
                                money = Math.round((double)money * GameCommon.getValueDouble("RATIO_RECHARGE_BANK"));
                                HazelcastInstance client = HazelcastClientFactory.getInstance();
                                if (client == null) {
                                    MoneyLogger.log(nickname, "RechargeByBank", money, 0L, "vin", "Nap vin qua ngan hang", "1030", "can not connect hazelcast");
                                    return;
                                }
                                IMap<String, UserModel> userMap = client.getMap("users");
                                if (!userMap.containsKey(nickname)) break block21;
                                try {
                                    userMap.lock(nickname);
                                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                    long moneyUser = user.getVin();
                                    long currentMoney = user.getVinTotal();
                                    long rechargeMoney = user.getRechargeMoney();
                                    user.setVin(moneyUser += money);
                                    user.setVinTotal(currentMoney += money);
                                    user.setRechargeMoney(rechargeMoney += money);
                                    String desc = "M\u00c3\u00a3 GD: " + merchTxnRef + ". Ng\u00c3\u00a2n h\u00c3\u00a0ng: " + mes.getBank();
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByBank", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByBank", "N\u00e1\u00ba\u00a1p Vin qua ng\u00c3\u00a2n h\u00c3\u00a0ng", currentMoney, money, "vin", desc, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                    userMap.put(nickname, user);
                                }
                                catch (Exception e2) {
                                    logger.debug(e2);
                                    MoneyLogger.log(nickname, "RechargeByBank", money, 0L, "vin", "Nap vin qua ngan hang", "1031", "rmq error: " + e2.getMessage());
                                }
                                finally {
                                    userMap.unlock(nickname);
                                }
                            }
                            catch (Exception e3) {
                                logger.debug(e3);
                                MoneyLogger.log(nickname, "RechargeByBank", money, 0L, "vin", "Nap vin qua ngan hang", "1001", e3.getMessage());
                            }
                        }
                        dao.updateRechargeByBank(merchTxnRef, txnResponseCode, description, transactionNo, message, amount);
                        break block22;
                    }
                    dao.insertLogRechargeByBankError(txnResponseCode, version, command, merchTxnRef, merchantID, orderInfo, currency, amount, locale, cardType, transactionNo, message, secureHash, description);
                    break block22;
                }
                NganLuongUtils.receiveResultFromBank(request);
            }
            catch (Exception e4) {
                logger.debug(e4);
                MoneyLogger.log("", "RechargeByBank", 0L, 0L, "vin", "Nap vin qua ngan hang", "1001", e4.getMessage());
            }
        }
    }

    private int mapErrorCodeEpayMegaCard(String status) {
        switch (status) {
            case "1": {
                return 0;
            }
            case "99": {
                return 30;
            }
            case "51": {
                return 36;
            }
            case "-10": {
                return 35;
            }
            case "-2": {
                return 32;
            }
            case "-3": {
                return 34;
            }
            case "50": {
                return 31;
            }
            case "59": {
                return 33;
            }
            case "53": 
            case "-24": 
            case "-11": 
            case "0": 
            case "3": 
            case "4": 
            case "5": 
            case "7": 
            case "8": 
            case "9": 
            case "10": 
            case "11": 
            case "12": 
            case "13": 
            case "52": 
            case "55": 
            case "62": 
            case "57": 
            case "58": 
            case "60": 
            case "61": 
            case "56": 
            case "16": 
            case "63": 
            case "64": 
            case "65": 
            case "66": 
            case "67": 
            case "68": {
                return 40;
            }
        }
        return 30;
    }

    private int getErrorCode(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 1;
            }
            case 12: {
                return 31;
            }
            case 13: {
                return 35;
            }
            case 14: {
                return 32;
            }
            case 15: {
                return 33;
            }
            case 16: {
                return 34;
            }
            case 17: 
            case 18: {
                return 36;
            }
            case 99: {
                return 30;
            }
        }
        return 30;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte checkRechargeIAP(String nickname, int iapPackage) {
        int res;
        block8 : {
            res = 1;
            try {
                IMap userMap;
                HazelcastInstance client;
                if (GameCommon.getValueInt("IS_RECHARGE_IAP") == 1) {
                    return (byte)res;
                }
                IAPModel iapPK = GameCommon.getIAPPackageById(iapPackage);
                if (iapPK == null || !(userMap = (client = HazelcastClientFactory.getInstance()).getMap("users")).containsKey(nickname)) break block8;
                try {
                    userMap.lock(nickname);
                    RechargeDaoImpl dao = new RechargeDaoImpl();
                    Calendar cal = Calendar.getInstance();
                    long iapUserInDay = dao.getTotalRechargeIapInday(nickname, cal);
                    cal = Calendar.getInstance();
                    long iapSystemInDay = dao.getTotalRechargeIapInday("", cal);
                    res = iapUserInDay + (long)iapPK.getValue() <= (long)GameCommon.getValueInt("IAP_MAX") && iapSystemInDay + (long)iapPK.getValue() <= (long)GameCommon.getValueInt("SYSTEM_IAP_MAX") ? 0 : 2;
                }
                catch (Exception e) {
                    logger.debug(e);
                }
                finally {
                    userMap.unlock(nickname);
                }
            }
            catch (Exception e2) {
                logger.debug(e2);
            }
        }
        return (byte)res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RechargeIAPResponse rechargeIAP(String nickname, String signedData, String signature) {
        RechargeIAPResponse res;
        int code;
        block18 : {
            code = 1;
            long currentMoney = 0L;
            res = new RechargeIAPResponse(code, currentMoney, 0);
            try {
                if (signature == null || signedData == null || GameCommon.getValueInt("IS_RECHARGE_IAP") == 1) {
                    return res;
                }
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                if (!userMap.containsKey(nickname)) break block18;
                try {
                    String[] split;
                    userMap.lock(nickname);
                    String[] arr = split = GameCommon.getValueStr("IAP_KEY").split(",,,");
                    for (String base64PublicKey : split) {
                        if (Security.verifyPurchase(base64PublicKey, signedData, signature)) {
                            String itemType = "";
                            RechargeDaoImpl dao = new RechargeDaoImpl();
                            try {
                                Purchase pc = new Purchase("", signedData, signature);
                                if (pc == null) continue;
                                IAPModel iapPK = GameCommon.getIAPPackageByName(pc.getSku());
                                if (iapPK != null) {
                                    res.setProductId(iapPK.getId());
                                    if (!dao.checkOrderId(pc.getOrderId())) {
                                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                        Calendar cal = Calendar.getInstance();
                                        long iapUserInDay = dao.getTotalRechargeIapInday(nickname, cal);
                                        cal = Calendar.getInstance();
                                        long iapSystemInDay = dao.getTotalRechargeIapInday("", cal);
                                        if (iapUserInDay + (long)iapPK.getValue() <= (long)GameCommon.getValueInt("IAP_MAX") && iapSystemInDay + (long)iapPK.getValue() <= (long)GameCommon.getValueInt("SYSTEM_IAP_MAX")) {
                                            int money = iapPK.getValue();
                                            long moneyUser = user.getVin();
                                            currentMoney = user.getVinTotal();
                                            res.setCurrentMoney(currentMoney);
                                            long rechargeMoney = user.getRechargeMoney();
                                            int iapInDay = 0;
                                            if (user.getIapTime() != null && VinPlayUtils.compareDate((Date)new Date(), (Date)user.getIapTime()) == 0) {
                                                iapInDay = user.getIapInDay();
                                            }
                                            user.setVin(moneyUser += (long)money);
                                            user.setVinTotal(currentMoney += (long)money);
                                            user.setRechargeMoney(rechargeMoney += (long)money);
                                            user.setIapInDay(iapInDay += money);
                                            user.setIapTime(new Date());
                                            String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: Th\u00c3\u00a0nh c\u00c3\u00b4ng, G\u00c3\u00b3i: " + money + " vin.";
                                            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByIAP", moneyUser, currentMoney, (long)money, "vin", 0L, 0, 0);
                                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByIAP", "N\u00e1\u00ba\u00a1p Vin qua Google", currentMoney, (long)money, "vin", description, 0L, false, user.isBot());
                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                            dao.saveLogIAP(pc, nickname, money, 0, "Th\u00c3\u00a0nh c\u00c3\u00b4ng");
                                            userMap.put(nickname, user);
                                            res.setCurrentMoney(currentMoney);
                                            code = 0;
                                        }
                                    } else {
                                        code = 2;
                                        logger.debug(("Order id \u00c4\u2018\u00c3\u00a3 t\u00e1\u00bb\u201cn t\u00e1\u00ba\u00a1i: " + pc.getOrderId()));
                                    }
                                } else {
                                    logger.debug(("Product ID kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021: " + pc.getSku()));
                                }
                                break;
                            }
                            catch (Exception e3) {
                                logger.debug(("Parse signedData fail: " + signedData));
                                continue;
                            }
                        }
                        logger.debug(("Purchase verification failed: " + signature));
                    }
                }
                catch (Exception e) {
                    logger.debug(e);
                }
                finally {
                    userMap.unlock(nickname);
                }
            }
            catch (Exception e2) {
                logger.debug(e2);
            }
        }
        res.setCode(code);
        return res;
    }

    @Override
    public String smsPlusCheckMO(Map<String, String[]> request) {
        SMSPlusResponse res = new SMSPlusResponse(0, "", "text");
        String sms = GameCommon.SMSPLUS_ERROR_SYSTEM;
        int code = -1;
        String des = "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
        try {
            if (GameCommon.getValueInt("SMS_PLUS_OPEN") == 0) {
                StringBuilder log = new StringBuilder("");
                HashMap<String, String> fields = new HashMap<String, String>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) continue;
                    fields.put(fieldName, fieldValue);
                }
                logger.debug(("SMS Plus check MO request: " + log.toString()));
                String accessKey = (String)fields.get("access_key");
                int amount = Integer.parseInt((String)fields.get("amount"));
                String commandCode = (String)fields.get("command_code");
                String moMessage = (String)fields.get("mo_message");
                String msisdn = (String)fields.get("msisdn");
                String telco = (String)fields.get("telco");
                String signature = (String)fields.get("signature");
                if (accessKey != null && commandCode != null && moMessage != null && msisdn != null && telco != null && signature != null) {
                    String mobile = DvtUtils.revertMobile84To(msisdn);
                    if (Arrays.asList(DvtUtils.SMS_AMOUNT).contains(amount)) {
                        if (amount >= GameCommon.getValueInt("SMS_PLUS_AMOUNT_MIN")) {
                            if (accessKey.equals(GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"))) {
                                if ((commandCode = commandCode.toUpperCase()).equals(GameCommon.getValueStr("SMS_PLUS_COMMAND_CODE"))) {
                                    String provider = DvtUtils.getProviderSMS(telco);
                                    if (provider.equals("Viettel")) {
                                        String[] arr = moMessage.trim().split(" ");
                                        if (arr.length == 3) {
                                            String gameCode = arr[0].toUpperCase();
                                            String nap = arr[1].toUpperCase();
                                            String nickname = arr[2];
                                            if (gameCode.equals(GameCommon.getValueStr("SMS_PLUS_GAME_CODE"))) {
                                                if (nap.startsWith("NAP")) {
                                                    fields.remove("signature");
                                                    String key = GameCommon.getValueStr("SMS_PLUS_SECRET_KEY");
                                                    String checksum = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(fields), (String)key, (String)"HmacSHA256");
                                                    if (signature.equals(checksum)) {
                                                        HazelcastInstance client = HazelcastClientFactory.getInstance();
                                                        IMap<String, UserModel> userMap = client.getMap("users");
                                                        int checkNN = 1;
                                                        if (userMap.containsKey(nickname)) {
                                                            checkNN = 0;
                                                        } else {
                                                            UserDaoImpl dao = new UserDaoImpl();
                                                            UserModel model = dao.getUserByNickName(nickname);
                                                            if (model != null) {
                                                                nickname = model.getNickname();
                                                                if (userMap.containsKey(nickname)) {
                                                                    checkNN = 0;
                                                                }
                                                            } else {
                                                                checkNN = 2;
                                                            }
                                                        }
                                                        if (checkNN == 0) {
                                                            sms = "";
                                                            res.setStatus(1);
                                                            code = 0;
                                                            des = "Th\u00c3\u00a0nh c\u00c3\u00b4ng";
                                                        } else if (checkNN == 2) {
                                                            sms = String.format(GameCommon.SMSPLUS_ERROR_NICKNAME, nickname);
                                                            logger.debug(("nickname khong ton tai: " + moMessage));
                                                            des = "nickname kh\u00c3\u00b4ng t\u00e1\u00bb\u201cn t\u00e1\u00ba\u00a1i";
                                                        } else {
                                                            sms = GameCommon.SMSPLUS_ERROR_LOGIN;
                                                            logger.debug(("nickname khong co tren cache: " + moMessage));
                                                            des = "nickname kh\u00c3\u00b4ng c\u00c3\u00b3 tr\u00c3\u00aan cache";
                                                        }
                                                    } else {
                                                        logger.debug(("checksum khong hop le: " + signature + " - " + checksum));
                                                        des = "checksum kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                    }
                                                } else {
                                                    sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                                    logger.debug(("maNap khong hop le: " + moMessage));
                                                    des = "maNap kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                }
                                            } else {
                                                sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                                logger.debug(("gameCode khong hop le: " + moMessage));
                                                des = "gameCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                            }
                                        } else {
                                            sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                            logger.debug(("moMessage khong hop le: " + moMessage));
                                            des = "moMessage kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                        }
                                    } else {
                                        logger.debug(("telco khong hop le: " + telco));
                                        des = "telco kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                    }
                                } else {
                                    logger.debug(("commandCode khong hop le: " + commandCode));
                                    des = "commandCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                }
                            } else {
                                logger.debug(("accessKey khong hop le: " + accessKey));
                                des = "accessKey kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                            }
                        } else {
                            sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                            logger.debug(("amount khong hop le: " + amount));
                            des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                        }
                    } else {
                        sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                        logger.debug(("amount khong hop le: " + amount));
                        des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                    }
                    RechargeDaoImpl rcdao = new RechargeDaoImpl();
                    rcdao.saveLogRechargeBySMSPlusCheckMO(mobile, moMessage, amount, "9029", code, des);
                } else {
                    logger.debug("tham so khong hop le");
                }
            } else {
                logger.debug("khoa sms plus");
            }
        }
        catch (Exception e) {
            logger.debug(e);
        }
        res.setSms(sms);
        return res.toJson();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    @Override
    public synchronized String smsPlusRequest(Map<String, String[]> request) {
        SMSPlusResponse res = new SMSPlusResponse(0, "", "text");
        int code = -1;
        String des = "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
        String sms = GameCommon.SMSPLUS_ERROR_SYSTEM;

        try {
            if (GameCommon.getValueInt("SMS_PLUS_OPEN") == 0) {
                StringBuilder log = new StringBuilder("");
                HashMap<String, String> fields = new HashMap<String, String>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) continue;
                    fields.put(fieldName, fieldValue);
                }
                RechargeServiceImpl.logger.debug(("SMS Plus request: " + log.toString()));
                String accessKey = (String)fields.get("access_key");
                int amount = Integer.parseInt((String)fields.get("amount"));
                String commandCode = (String)fields.get("command_code");
                String errorCode = (String)fields.get("error_code");
                String errorMessage = (String)fields.get("error_message");
                String moMessage = (String)fields.get("mo_message");
                String msisdn = (String)fields.get("msisdn");
                String requestId = (String)fields.get("request_id");
                String requestTime = (String)fields.get("request_time");
                String signature = (String)fields.get("signature");
                if (accessKey != null && commandCode != null && errorCode != null && errorMessage != null && moMessage != null && msisdn != null && requestId != null && requestTime != null && signature != null) {
                    String[] arr = moMessage.trim().split(" ");
                    if (arr.length == 3) {
                        String gameCode = arr[0].toUpperCase();
                        String nap = arr[1].toUpperCase();
                        String nickname = arr[2];
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        String timeRequest = VinPlayUtils.getDateTimeStr((Date)format.parse(requestTime));
                        long money = 0L;
                        String mobile = DvtUtils.revertMobile84To(msisdn);
                        RechargeDaoImpl rcdao = new RechargeDaoImpl();
                        lbl167: // 13 sources:
                        if (Arrays.asList(DvtUtils.SMS_AMOUNT).contains(amount)) {
                            if (amount >= GameCommon.getValueInt("SMS_PLUS_AMOUNT_MIN")) {
                                if (accessKey.equals(GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"))) {
                                    if ((commandCode = commandCode.toUpperCase()).equals(GameCommon.getValueStr("SMS_PLUS_COMMAND_CODE"))) {
                                        if (gameCode.equals(GameCommon.getValueStr("SMS_PLUS_GAME_CODE"))) {
                                            if (nap.startsWith("NAP")) {
                                                fields.remove("signature");
                                                String key = GameCommon.getValueStr("SMS_PLUS_SECRET_KEY");
                                                String checksum = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(fields), (String)key, (String)"HmacSHA256");
                                                if (signature.equals(checksum)) {
                                                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                                                    IMap<String, UserModel> userMap = client.getMap("users");
                                                    int checkNN = 1;
                                                    if (userMap.containsKey(nickname)) {
                                                        checkNN = 0;
                                                    } else {
                                                        UserDaoImpl dao = new UserDaoImpl();
                                                        UserModel model = dao.getUserByNickName(nickname);
                                                        if (model != null) {
                                                            nickname = model.getNickname();
                                                            if (userMap.containsKey(nickname)) {
                                                                checkNN = 0;
                                                            }
                                                        } else {
                                                            checkNN = 2;
                                                        }
                                                    }
                                                    if (checkNN == 0) {
                                                        try {
                                                            userMap.lock(nickname);
                                                            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                                            if (!rcdao.checkRequestIdSMSPlus(requestId)) {
                                                                String var35_37 = errorCode;
                                                                int var36_39 = -1;
                                                                switch (var35_37.hashCode()) {
                                                                    case -1585413230: {
                                                                        if (!var35_37.equals("WCG-0000")) break;
                                                                        var36_39 = 0;
                                                                        break;
                                                                    }
                                                                    case -1585413229: {
                                                                        if (!var35_37.equals("WCG-0001")) break;
                                                                        var36_39 = 1;
                                                                        break;
                                                                    }
                                                                    case -1585413228: {
                                                                        if (!var35_37.equals("WCG-0002")) break;
                                                                        var36_39 = 2;
                                                                        break;
                                                                    }
                                                                    case -1585413225: {
                                                                        if (!var35_37.equals("WCG-0005")) break;
                                                                        var36_39 = 3;
                                                                    }
                                                                }
                                                                switch (var36_39) {
                                                                    case 0: {
                                                                        code = 0;
                                                                        des = "Th\u00c3\u00a0nh c\u00c3\u00b4ng";
                                                                        break;
                                                                    }
                                                                    case 1: {
                                                                        code = 1;
                                                                        des = "Thu\u00c3\u00aa bao kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                                        break;
                                                                    }
                                                                    case 2: {
                                                                        code = 2;
                                                                        des = "D\u00e1\u00bb\u00b1 li\u00e1\u00bb\u2021u CP g\u00e1\u00bb\u00adi l\u00c3\u00aan sai";
                                                                        break;
                                                                    }
                                                                    case 3: {
                                                                        code = 5;
                                                                        des = "T\u00c3\u00a0i kho\u00e1\u00ba\u00a3n kh\u00c3\u00b4ng \u00c4\u2018\u00e1\u00bb\u00a7 ti\u00e1\u00bb\ufffdn";
                                                                        break;
                                                                    }
                                                                    default:
                                                                        code = -1;
                                                                        des = "L\u00e1\u00bb\u2014i kh\u00c3\u00b4ng x\u00c3\u00a1c \u00c4\u2018\u00e1\u00bb\u2039nh";
                                                                        break ;
                                                                }
//                                                                code = -1;
//                                                                des = "L\u00e1\u00bb\u2014i kh\u00c3\u00b4ng x\u00c3\u00a1c \u00c4\u2018\u00e1\u00bb\u2039nh";
lbl107: // 5 sources:
                                                                if (code != 0) break lbl167;
                                                                long moneyUser = user.getVin();
                                                                long currentMoney = user.getVinTotal();
                                                                long rechargeMoney = user.getRechargeMoney();
                                                                money = Math.round(GameCommon.getValueDouble("RATIO_RECHARGE_SMS") * (double)amount);
                                                                user.setVin(moneyUser += money);
                                                                user.setVinTotal(currentMoney += money);
                                                                user.setRechargeMoney(rechargeMoney += money);
                                                                String description = "SDT: " + mobile + " .N\u00e1\u00bb\u2122i dung tin nh\u00e1\u00ba\u00afn: " + moMessage + ". \u00c4\ufffd\u00e1\u00ba\u00a7u s\u00e1\u00bb\u2018: 9029";
                                                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeBySMS", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeBySMS", "N\u00e1\u00ba\u00a1p Vin qua SMS", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                                userMap.put(nickname, user);
                                                                sms = String.format(GameCommon.SMSPLUS_SUCCESS, new Object[]{nickname, money});
                                                                res.setStatus(1);
                                                            }
                                                            des = "requestId \u00c4\u2018\u00c3\u00a3 x\u00e1\u00bb\u00ad l\u00c3\u00bd";
                                                            RechargeServiceImpl.logger.debug(("requestId da xu ly: " + requestId));
                                                        }
                                                        catch (Exception e) {
                                                            RechargeServiceImpl.logger.debug(e);
                                                        }
                                                        finally {
                                                            userMap.unlock(nickname);
                                                        }
                                                    } else if (checkNN == 2) {
                                                        sms = String.format(GameCommon.SMSPLUS_ERROR_NICKNAME, new Object[]{nickname});
                                                        des = "nickname kh\u00c3\u00b4ng t\u00e1\u00bb\u201cn t\u00e1\u00ba\u00a1i";
                                                        RechargeServiceImpl.logger.debug(("nickname khong ton tai: " + moMessage));
                                                    } else {
                                                        des = "nickname kh\u00c3\u00b4ng c\u00c3\u00b3 tr\u00c3\u00aan cache";
                                                        sms = GameCommon.SMSPLUS_ERROR_LOGIN;
                                                        RechargeServiceImpl.logger.debug(("nickname khong co tren cache: " + moMessage));
                                                    }
                                                } else {
                                                    des = "checksum kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                    RechargeServiceImpl.logger.debug(("checksum khong hop le: " + signature + " - " + checksum));
                                                }
                                            } else {
                                                des = "m\u00c3\u00a3 n\u00e1\u00ba\u00a1p kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                                RechargeServiceImpl.logger.debug(("maNap khong hop le: " + moMessage));
                                            }
                                        } else {
                                            des = "gameCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                            sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                            RechargeServiceImpl.logger.debug(("gameCode khong hop le: " + moMessage));
                                        }
                                    } else {
                                        des = "commandCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                        RechargeServiceImpl.logger.debug(("commandCode khong hop le: " + commandCode));
                                    }
                                } else {
                                    des = "accessKey kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                    RechargeServiceImpl.logger.debug(("accessKey khong hop le: " + accessKey));
                                }
                            } else {
                                sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                                des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                RechargeServiceImpl.logger.debug(("amount khong hop le: " + amount));
                            }
                        } else {
                            sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                            des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                            RechargeServiceImpl.logger.debug(("amount khong hop le: " + amount));
                        }

                        rcdao.saveLogRechargeBySMSPlus(nickname, mobile, moMessage, amount, "9029", errorCode, errorMessage, requestId, timeRequest, code, des, (int)money);
                    } else {
                        sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                        RechargeServiceImpl.logger.debug(("moMessage khong hop le: " + moMessage));
                    }
                } else {
                    RechargeServiceImpl.logger.debug("tham so khong hop le");
                }
            } else {
                RechargeServiceImpl.logger.debug("khoa sms plus");
            }
        }
        catch (Exception e2) {
            RechargeServiceImpl.logger.debug(e2);
        }
        res.setSms(sms);
        return res.toJson();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    @Override
    public synchronized String sms8xRequest(Map<String, String[]> request) {
        SMSPlusResponse res = new SMSPlusResponse(0, "", "text");
        int code = -1;
        String des = "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
        String sms = GameCommon.SMSPLUS_ERROR_SYSTEM;
        try {
            if (GameCommon.getValueInt("SMS_OPEN") == 0) {
                StringBuilder log = new StringBuilder("");
                HashMap<String, String> fields = new HashMap<String, String>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) continue;
                    fields.put(fieldName, fieldValue);
                }
                RechargeServiceImpl.logger.debug(("SMS 8x request: " + log.toString()));
                String accessKey = (String)fields.get("access_key");
                String command = (String)fields.get("command");
                String moMessage = (String)fields.get("mo_message");
                String msisdn = (String)fields.get("msisdn");
                String requestId = (String)fields.get("request_id");
                String requestTime = (String)fields.get("request_time");
                String shortCode = (String)fields.get("short_code");
                String signature = (String)fields.get("signature");
                if (accessKey != null && command != null && moMessage != null && msisdn != null && requestId != null && requestTime != null && signature != null) {
                    int amount = DvtUtils.getAmountSMSFromShortCode(shortCode);
                    String[] arr = moMessage.trim().split(" ");
                    if (arr.length == 2) {
                        RechargeDaoImpl rcdao = new RechargeDaoImpl();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        String timeRequest = VinPlayUtils.getDateTimeStr((Date)format.parse(requestTime));
                        String mobile = DvtUtils.revertMobile84To(msisdn);
                        long money = 0L;
                        String gameCode = arr[0].toUpperCase();
                        String nickname = arr[1];
                        lbl124: // 12 sources:
                        if (Arrays.asList(DvtUtils.SMS_AMOUNT).contains(amount)) {
                            if (amount >= GameCommon.getValueInt("SMS_PLUS_AMOUNT_MIN")) {
                                if (accessKey.equals(GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"))) {
                                    if ((command = command.toUpperCase()).equals(GameCommon.getValueStr("SMS_COMMAND"))) {
                                        if (gameCode.equals(GameCommon.getValueStr("SMS_COMMAND"))) {
                                            fields.remove("signature");
                                            String key = GameCommon.getValueStr("SMS_PLUS_SECRET_KEY");
                                            String checksum = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(fields), (String)key, (String)"HmacSHA256");
                                            if (signature.equals(checksum)) {
                                                HazelcastInstance client = HazelcastClientFactory.getInstance();
                                                IMap<String, UserModel> userMap = client.getMap("users");
                                                int checkNN = 1;
                                                if (userMap.containsKey(nickname)) {
                                                    checkNN = 0;
                                                } else {
                                                    UserDaoImpl dao = new UserDaoImpl();
                                                    UserModel model = dao.getUserByNickName(nickname);
                                                    if (model != null) {
                                                        nickname = model.getNickname();
                                                        if (userMap.containsKey(nickname)) {
                                                            checkNN = 0;
                                                        }
                                                    } else {
                                                        checkNN = 2;
                                                    }
                                                }
                                                if (checkNN == 0) {
                                                    try {
                                                        userMap.lock(nickname);
                                                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                                        if (!rcdao.checkRequestIdSMS(requestId)) {
                                                            code = 0;
                                                            des = "Th\u00c3\u00a0nh c\u00c3\u00b4ng";
                                                            if (code != 0) break lbl124;
                                                            long moneyUser = user.getVin();
                                                            long currentMoney = user.getVinTotal();
                                                            long rechargeMoney = user.getRechargeMoney();
                                                            money = Math.round(GameCommon.getValueDouble("RATIO_RECHARGE_SMS") * (double)amount);
                                                            user.setVin(moneyUser += money);
                                                            user.setVinTotal(currentMoney += money);
                                                            user.setRechargeMoney(rechargeMoney += money);
                                                            String description = "SDT: " + mobile + " .N\u00e1\u00bb\u2122i dung tin nh\u00e1\u00ba\u00afn: " + moMessage + ". \u00c4\ufffd\u00e1\u00ba\u00a7u s\u00e1\u00bb\u2018: " + shortCode;
                                                            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeBySMS", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeBySMS", "N\u00e1\u00ba\u00a1p Vin qua SMS", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                            userMap.put(nickname, user);
                                                            sms = String.format(GameCommon.SMSPLUS_SUCCESS, new Object[]{nickname, money});
                                                            res.setStatus(1);
                                                        }
                                                        des = "requestId \u00c4\u2018\u00c3\u00a3 x\u00e1\u00bb\u00ad l\u00c3\u00bd";
                                                    }
                                                    catch (Exception e) {
                                                        RechargeServiceImpl.logger.debug(e);
                                                    }
                                                    finally {
                                                        userMap.unlock(nickname);
                                                    }
                                                } else if (checkNN == 2) {
                                                    sms = String.format(GameCommon.SMSPLUS_ERROR_NICKNAME, new Object[]{nickname});
                                                    RechargeServiceImpl.logger.debug(("nickname khong ton tai: " + moMessage));
                                                    des = "nickname kh\u00c3\u00b4ng t\u00e1\u00bb\u201cn t\u00e1\u00ba\u00a1i";
                                                } else {
                                                    sms = GameCommon.SMSPLUS_ERROR_LOGIN;
                                                    RechargeServiceImpl.logger.debug(("nickname khong co tren cache: " + moMessage));
                                                    des = "nickname kh\u00c3\u00b4ng c\u00c3\u00b3 tr\u00c3\u00aan cache";
                                                }
                                            } else {
                                                des = "checksum kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                                RechargeServiceImpl.logger.debug(("checksum khong hop le: " + signature + " - " + checksum));
                                            }
                                        } else {
                                            des = "gameCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                            sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                                            RechargeServiceImpl.logger.debug(("gameCode khong hop le: " + moMessage));
                                        }
                                    } else {
                                        des = "command kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                        RechargeServiceImpl.logger.debug(("command khong hop le: " + command));
                                    }
                                } else {
                                    des = "accessKey kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                    RechargeServiceImpl.logger.debug(("accessKey khong hop le: " + accessKey));
                                }
                            } else {
                                sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                                des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                RechargeServiceImpl.logger.debug(("amount khong hop le: " + amount));
                            }
                        } else {
                            sms = GameCommon.SMSPLUS_ERROR_AMOUNT;
                            des = "shortCode kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                            RechargeServiceImpl.logger.debug(("shortCode khong hop le: " + shortCode));
                        }

                        rcdao.saveLogRechargeBySMS(nickname, mobile, moMessage, amount, shortCode, requestId, timeRequest, code, des, (int)money);
                    } else {
                        sms = GameCommon.SMSPLUS_ERROR_SYNTAX;
                        RechargeServiceImpl.logger.debug(("moMessage khong hop le: " + moMessage));
                    }
                } else {
                    RechargeServiceImpl.logger.debug("tham so khong hop le");
                }
            } else {
                RechargeServiceImpl.logger.debug("khoa sms");
            }
        }
        catch (Exception e2) {
            RechargeServiceImpl.logger.debug(e2);
        }
        res.setSms(sms);
        return res.toJson();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public synchronized RechargeApiOTPResponse sendRequestChargingOTP(String nickname, String mobile, int amount) {
        int code = 1;
        String url = "";
        String requestId = "";
        String transId = "";
        int fail = 0;
        long time = 0L;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                if (GameCommon.getValueInt("API_OTP_OPEN") == 0) {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    fail = user.getApiOTPFail();
                    if (UserValidaton.validateMobileVN((String)mobile)) {
                        if (Arrays.asList(DvtUtils.SMS_AMOUNT).contains(amount) && amount >= GameCommon.getValueInt("SMS_PLUS_AMOUNT_MIN")) {
                            time = DvtUtils.checkApiOtpFail(fail, user.getApiOTPFailTime());
                            if (time <= 0L) {
                                time = DvtUtils.checkApiOtpTimeDelay(user.getApiOTPTime());
                                if (time <= 0L) {
                                    requestId = new StringBuilder(user.getId()).append(System.currentTimeMillis()).toString();
                                    HashMap<String, String> params = new HashMap<String, String>();
                                    params.put("access_key", GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"));
                                    params.put("amount", String.valueOf(amount));
                                    params.put("content", nickname);
                                    params.put("msisdn", DvtUtils.revertMobile0To84(mobile));
                                    params.put("requestId", requestId);
                                    String signature = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(params), (String)GameCommon.getValueStr("SMS_PLUS_SECRET_KEY"), (String)"HmacSHA256");
                                    params.put("signature", signature);
                                    String response = HttpURLClient.sendPOST(GameCommon.getValueStr("API_OTP_URL_REQUEST"), params);
                                    JSONObject obj = new JSONObject(response);
                                    String errorMessage = obj.getString("errorMessage");
                                    String rpRequestId = obj.getString("requestId");
                                    String rpTransId = obj.getString("transId");
                                    String errorCode = obj.getString("errorCode");
                                    String redirectUrl = obj.getString("redirectUrl");
                                    if (errorMessage != null && rpRequestId != null && rpTransId != null && errorCode != null && requestId.equals(rpRequestId)) {
                                        transId = rpTransId;
                                        code = (byte)DvtUtils.getCodeApiOTP(errorCode, false);
                                        if (code == 0) {
                                            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                                                url = redirectUrl;
                                            }
                                            DistCache<String, ApiOtpModel> apiOtpMap = CacheFactory.get("cacheApiOtp", ApiOtpModel.class);
                                            ApiOtpModel apiOtp = new ApiOtpModel(rpRequestId, transId, mobile, nickname, amount);
                                            apiOtpMap.put(requestId, apiOtp, (long)GameCommon.getValueInt("API_OTP_TIMEOUT"), TimeUnit.MINUTES);
                                        }
                                        RechargeDaoImpl rcdao = new RechargeDaoImpl();
                                        rcdao.saveLogRequestApiOTP(nickname, mobile, amount, errorCode, errorMessage, requestId, transId, code, redirectUrl);
                                    }
                                } else {
                                    code = 8;
                                }
                            } else {
                                code = 7;
                            }
                        }
                    } else {
                        code = 2;
                    }
                } else {
                    logger.debug("khoa nap qua api otp");
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        RechargeApiOTPResponse res = new RechargeApiOTPResponse((byte)code, requestId, transId, url, 0L, fail, time);
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public synchronized RechargeApiOTPResponse sendConfirmChargingOTP(String nickname, String requestId, String otp) {
        byte code = 1;
        long currentMoney = 0L;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        DistCache<String, ApiOtpModel> apiOtpMap = CacheFactory.get("cacheApiOtp", ApiOtpModel.class);
        if (userMap.containsKey(nickname)) {
            try {
                if (GameCommon.getValueInt("API_OTP_OPEN") == 0) {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    if (requestId != null && UserValidaton.validate((String)otp, (String)GameCommon.getValueStr("API_OTP_FORMAT")) && apiOtpMap.containsKey(requestId)) {
                        ApiOtpModel apiOtp = (ApiOtpModel)apiOtpMap.get(requestId);
                        apiOtpMap.remove(requestId);
                        String transId = apiOtp.getTransId();
                        int amount = apiOtp.getAmount();
                        String mobile = apiOtp.getMobile();
                        long time = DvtUtils.checkApiOtpFail(user.getApiOTPFail(), user.getApiOTPFailTime());
                        if (time <= 0L && (time = DvtUtils.checkApiOtpTimeDelay(user.getApiOTPTime())) <= 0L) {
                            user.setApiOTPTime(new Date());
                            int money = 0;
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("access_key", GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"));
                            params.put("otp", String.valueOf(otp));
                            params.put("requestId", requestId);
                            params.put("transId", transId);
                            String signature = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(params), (String)GameCommon.getValueStr("SMS_PLUS_SECRET_KEY"), (String)"HmacSHA256");
                            params.put("signature", signature);
                            String response = HttpURLClient.sendPOST(GameCommon.getValueStr("API_OTP_URL_CONFIRM"), params);
                            JSONObject obj = new JSONObject(response);
                            String errorMessage = obj.getString("errorMessage");
                            String rpRequestId = obj.getString("requestId");
                            String rpTransId = obj.getString("transId");
                            String errorCode = obj.getString("errorCode");
                            String des = "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
                            if (errorMessage != null && rpRequestId != null && rpTransId != null && errorCode != null && requestId.equals(rpRequestId) && transId.equals(rpTransId)) {
                                code = (byte)DvtUtils.getCodeApiOTP(errorCode, true);
                                des = DvtUtils.getDesbyCodeApiOTP(code);
                                if (code == 0) {
                                    user.setApiOTPFail(0);
                                    long moneyUser = user.getVin();
                                    currentMoney = user.getVinTotal();
                                    long rechargeMoney = user.getRechargeMoney();
                                    money = (int)Math.round(GameCommon.getValueDouble("RATIO_RECHARGE_SMS") * (double)amount);
                                    user.setVin(moneyUser += (long)money);
                                    user.setVinTotal(currentMoney += (long)money);
                                    user.setRechargeMoney(rechargeMoney += (long)money);
                                    String description = "SDT: " + mobile + " .S\u00e1\u00bb\u2018 ti\u00e1\u00bb\ufffdn: " + amount;
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeBySMS", moneyUser, currentMoney, (long)money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeBySMS", "N\u00e1\u00ba\u00a1p Vin qua SMS", currentMoney, (long)money, "vin", description, 0L, false, user.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                } else if (code != 1) {
                                    user.setApiOTPFail(user.getApiOTPFail() + 1);
                                    user.setApiOTPFailTime(new Date());
                                }
                                userMap.put(nickname, user);
                            }
                            RechargeDaoImpl dao = new RechargeDaoImpl();
                            errorCode = errorCode == null ? "" : errorCode;
                            errorMessage = errorMessage == null ? "" : errorMessage;
                            dao.saveLogConfirmApiOTP(nickname, mobile, amount, otp, errorCode, errorMessage, requestId, transId, code, des, money);
                        }
                    }
                } else {
                    logger.debug("khoa nap qua api otp");
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        RechargeApiOTPResponse res = new RechargeApiOTPResponse(code, requestId, "", "", currentMoney, 0, 0L);
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    @Override
    public synchronized String receiveConfirmChargingOTP(Map<String, String[]> request) {
        SMSPlusResponse res = new SMSPlusResponse(0, "", "text");
        int code = -1;
        String des = "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
        String sms = GameCommon.SMSPLUS_ERROR_SYSTEM;
        try {
            if (GameCommon.getValueInt("API_OTP_OPEN") == 0) {
                StringBuilder log = new StringBuilder("");
                HashMap<String, String> fields = new HashMap<String, String>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) continue;
                    fields.put(fieldName, fieldValue);
                }
                RechargeServiceImpl.logger.debug(("Api OTP confirm: " + log.toString()));
                String accessKey = (String)fields.get("access_key");
                int amount = Integer.parseInt((String)fields.get("amount"));
                String errorCode = (String)fields.get("error_code");
                String errorMessage = (String)fields.get("error_message");
                String msisdn = (String)fields.get("msisdn");
                String requestId = (String)fields.get("request_id");
                String requestTime = (String)fields.get("request_time");
                String transId = (String)fields.get("trans_id");
                String signature = (String)fields.get("signature");
                if (accessKey != null && errorCode != null && errorMessage != null && msisdn != null && requestId != null && requestTime != null && transId != null && signature != null) {
                    long money = 0L;
                    String mobile = DvtUtils.revertMobile84To(msisdn);
                    String nickname = "";
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    DistCache<String, ApiOtpModel> apiOtpMap = CacheFactory.get("cacheApiOtp", ApiOtpModel.class);

                    // SUN-1248 Wave 2 batch H: lock-collapse - atomic remove-returns-prior-value.
                    // Only one of N concurrent callbacks consumes the OTP (winner sees non-null);
                    // any subsequent caller sees null and short-circuits to the "requestId not
                    // found" log path. The OTP is single-use even on validation failure (no
                    // re-put). The original Hazelcast implementation did NOT remove on
                    // mobile/transId/requestId/amount mismatch (break lbl103 skipped the remove
                    // at line 2757), so a misdirected callback could be retried; that pre-port
                    // behavior is intentionally tightened here per the migration plan #7.
                    ApiOtpModel apiOtp = apiOtpMap.remove(requestId);
                    if (apiOtp == null) {
                        des = "requestId kh\u00c3\u00b4ng t\u00e1\u00bb\u201cn t\u00e1\u00ba\u00a1i";
                        RechargeServiceImpl.logger.debug(("requestId khong ton tai: " + requestId));
                    } else if (!mobile.equals(apiOtp.getMobile()) || !transId.equals(apiOtp.getTransId()) || !requestId.equals(apiOtp.getRequestId()) || amount != apiOtp.getAmount()) {
                        // validation failed - fall through to dao log; no re-put (single-use)
                    } else {
                        try {
                            nickname = apiOtp.getNickname();
                            if ((Arrays.asList(DvtUtils.SMS_AMOUNT).contains(amount)) && (amount >= GameCommon.getValueInt("SMS_PLUS_AMOUNT_MIN"))) {
                                if (accessKey.equals(GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"))) {
                                    fields.remove("signature");
                                    String key = GameCommon.getValueStr("SMS_PLUS_SECRET_KEY");
                                    String checksum = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(fields), (String)key, (String)"HmacSHA256");
                                    if (signature.equals(checksum)) {
                                        IMap<String, UserModel>  userMap = client.getMap("users");
                                        if (userMap.containsKey(nickname)) {
                                            try {
                                                userMap.lock(nickname);
                                                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                                code = DvtUtils.getCodeApiOTP(errorCode, true);
                                                des = DvtUtils.getDesbyCodeApiOTP(code);
                                                if (code == 0) {
                                                    user.setApiOTPFail(0);
                                                    long moneyUser = user.getVin();
                                                    long currentMoney = user.getVinTotal();
                                                    long rechargeMoney = user.getRechargeMoney();
                                                    money = Math.round(GameCommon.getValueDouble("RATIO_RECHARGE_SMS") * (double)amount);
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setRechargeMoney(rechargeMoney += money);
                                                    String description = "SDT: " + mobile + " .S\u00e1\u00bb\u2018 ti\u00e1\u00bb\ufffdn: " + amount;
                                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeBySMS", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeBySMS", "N\u00e1\u00ba\u00a1p Vin qua SMS", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                    sms = String.format(GameCommon.SMSPLUS_SUCCESS, new Object[]{nickname, money});
                                                    res.setStatus(1);
                                                } else if (code != 1) {
                                                    user.setApiOTPFail(user.getApiOTPFail() + 1);
                                                    user.setApiOTPFailTime(new Date());
                                                }
                                                userMap.put(nickname, user);
                                            }
                                            catch (Exception e) {
                                                RechargeServiceImpl.logger.debug(e);
                                            }
                                            finally {
                                                userMap.unlock(nickname);
                                            }
                                        }
                                    } else {
                                        des = "checksum kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                        RechargeServiceImpl.logger.debug(("checksum khong hop le: " + signature + " - " + checksum));
                                    }
                                } else {
                                    des = "accessKey kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                    RechargeServiceImpl.logger.debug(("accessKey khong hop le: " + accessKey));
                                }
                            } else {
                                des = "amount kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
                                RechargeServiceImpl.logger.debug(("amount khong hop le: " + amount));
                            }
                        }
                        catch (Exception e2) {
                            RechargeServiceImpl.logger.debug(e2);
                        }
                    }

                    RechargeDaoImpl dao = new RechargeDaoImpl();
                    dao.saveLogConfirmApiOTP(nickname, mobile, amount, "", errorCode, errorMessage, requestId, transId, code, des, (int)money);
                } else {
                    RechargeServiceImpl.logger.debug("tham so khong hop le");
                }
            } else {
                RechargeServiceImpl.logger.debug("khoa sms plus");
            }
        }
        catch (Exception e3) {
            RechargeServiceImpl.logger.debug(e3);
        }
        res.setSms(sms);
        return res.toJson();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RechargeResponse rechargeByVinCard(String nickname, String serial, String pin, String platform) throws Exception {
        HazelcastInstance client;
        logger.debug(("Request rechargeByCard:  nickname: " + nickname + ", serial: " + serial + ", pin: " + pin));
        int code = 1;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_VIN_CARD") == 1) {
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByVinCard", 0L, 0L, "vin", "Nap vin qua vin card", "1030", "can not connect hazelcast");
            return res;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        IMap configMap = client.getMap("cacheConfig");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                configMap.lock("VIN_CARD_SYSTEM_DAILY");
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFail(user.getRechargeVCFail(), user.getRechargeVCFailTime());
                if (time <= 0L) {
                    if (UserValidaton.validateSerialVinCard((String)serial)) {
                        if (UserValidaton.validatePinVinCard((String)pin)) {
                            long rechargeVCToday;
                            boolean rechargedToday = user.getRechargeVCTime() != null && VinPlayUtils.compareDate((Date)new Date(), (Date)user.getRechargeVCTime()) == 0;
                            long l = rechargeVCToday = rechargedToday ? user.getRechargeVCInDay() : 0L;
                            if (rechargeVCToday < GameCommon.getValueLong("VIN_CARD_USER_LIMIT")) {
                                long rechargeVCSystem = GameCommon.getValueLong("VIN_CARD_SYSTEM_DAILY");
                                if (rechargeVCSystem < GameCommon.getValueLong("VIN_CARD_SYSTEM_LIMIT")) {
                                    String id = String.valueOf(System.currentTimeMillis());
                                    String partner = GameCommon.getValueStr("VIN_CARD_PARTNER");
                                    ChargeVCObj obj = null;
                                    try {
                                        obj = VinplayClient.rechargeByVinCard(id, partner, serial, pin, nickname);
                                        if (obj.getSerial() != null) {
                                            serial = obj.getSerial();
                                        }
                                    }
                                    catch (Exception e) {
                                        logger.debug(e);
                                        MoneyLogger.log(nickname, "RechargeByVinCard", 0L, 0L, "vin", "Nap vin qua vin card", "1034", "Loi ket noi dvt: " + e.getMessage());
                                    }
                                    long money = 0L;
                                    if (obj != null) {
                                        money = Math.round((double)obj.getAmount() * GameCommon.getValueDouble("RATIO_RECHARGE_VIN_CARD"));
                                        code = this.getErrorCode(obj.getStatus());
                                        if (code == 0 && (obj.getAmount() == 0 || obj.getAmount() > PhoneCardType._5M.getValue() || obj.getAmount() % PhoneCardType._10K.getValue() != 0)) {
                                            code = 30;
                                        }
                                        message = new RechargeByCardMessage(nickname, id, partner, serial, pin, obj.getAmount(), obj.getStatus(), obj.getMessage(), code, (int)money, (String)null, (String)null, (String)null, platform, (String)null);
                                    } else {
                                        message = new RechargeByCardMessage(nickname, id, partner, serial, pin, 0, -1, "L\u00e1\u00bb\u2014i k\u00e1\u00ba\u00bft n\u00e1\u00bb\u2018i D\u00e1\u00bb\u2039ch v\u00e1\u00bb\u00a5 th\u00e1\u00ba\u00bb", code, 0, (String)null, (String)null, (String)null, platform, (String)null);
                                    }
                                    long moneyUser = user.getVin();
                                    long currentMoney = user.getVinTotal();
                                    long rechargeMoney = user.getRechargeMoney();
                                    if (code == 0) {
                                        configMap.put("VIN_CARD_SYSTEM_DAILY", String.valueOf(rechargeVCSystem += (long)obj.getAmount()));
                                        user.setVin(moneyUser += money);
                                        user.setVinTotal(currentMoney += money);
                                        user.setRechargeMoney(rechargeMoney += money);
                                        user.setRechargeVCFail(0);
                                        user.setRechargeVCInDay(rechargeVCToday += (long)obj.getAmount());
                                        user.setRechargeVCTime(new Date());
                                        String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: Th\u00c3\u00a0nh c\u00c3\u00b4ng, M\u00c3\u00a3 GD: " + id + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                        MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByVinCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByVinCard", "N\u00e1\u00ba\u00a1p qua VinPlay Card", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                        MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)307);
                                        userMap.put(nickname, user);
                                        res.setCurrentMoney(currentMoney);
                                    } else if (code == 30) {
                                        String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: \u00c4\ufffdang x\u00e1\u00bb\u00ad l\u00c3\u00bd, M\u00c3\u00a3 GD: " + id + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + obj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                        LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByVinCard", "N\u00e1\u00ba\u00a1p qua VinPlay Card", currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                        MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)307);
                                    } else {
                                        if (code != 1) {
                                            cardFail = true;
                                        }
                                        MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)307);
                                    }
                                } else {
                                    code = 38;
                                }
                            } else {
                                code = 37;
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }
                if (cardFail) {
                    user.setRechargeVCFail(user.getRechargeVCFail() + 1);
                    user.setRechargeVCFailTime(new Date());
                    userMap.put(nickname, user);
                }
                res.setFail(user.getRechargeVCFail());
            }
            catch (Exception e2) {
                code = 1;
                logger.debug(e2);
                MoneyLogger.log(nickname, "RechargeByVinCard", 0L, 0L, "vin", "Nap vin qua vin card", "1001", e2.getMessage());
            }
            finally {
                userMap.unlock(nickname);
                configMap.unlock("VIN_CARD_SYSTEM_DAILY");
            }
        }
        res.setCode(code);
        logger.debug(("Response rechargeByVinCard: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public RechargeResponse rechargeByMegaCard(String nickname, String serial, String pin, String platform) throws Exception {
        HazelcastInstance client;
        logger.debug(("Request rechargeByMegaCard:  nickname: " + nickname + ", serial: " + serial + ", pin: " + pin + ", platform: " + platform));
        int code = 1;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_NAP_MEGA_CARD") == 1) {
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim().toUpperCase();
        }
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByMegaCard", 0L, 0L, "vin", "Nap vin qua MegaCard", "1030", "can not connect hazelcast");
            return res;
        }
        IMap<String, UserModel> userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                long time = DvtUtils.checkRechargeFail(user.getRechargeMegaCardFail(), user.getRechargeMegaCardFailTime());
                if (time <= 0L) {
                    if (UserValidaton.validateSerialMegaCard((String)serial)) {
                        if (UserValidaton.validatePinMegaCard((String)pin)) {
                            ChargeReponse chargeObj = null;
                            String transID = VinPlayUtils.genTransactionId((int)user.getId());
                            try {
                                EpayMegaCardCharging.init();
                                chargeObj = EpayMegaCardCharging.chargingMegaCard(EpayMegaCardCharging.login(), transID, serial, pin);
                            }
                            catch (Exception e) {
                                logger.debug(e);
                                MoneyLogger.log(nickname, "RechargeByMegaCard", 0L, 0L, "vin", "Nap vin qua MegaCard", "1034", "Loi ket noi Epay MegaCard: " + e.getMessage());
                                ++EpayMegaCardAlert.megaDisconnect;
                            }
                            long money = 0L;
                            String userNameMega = "";
                            if (GameCommon.getValueInt("MEGA_IS_VAT") == 0) {
                                userNameMega = GameCommon.getValueStr("MEGA_USER");
                            } else if (GameCommon.getValueInt("MEGA_IS_VAT") == 1) {
                                userNameMega = GameCommon.getValueStr("MEGA_USER_VAT");
                            }
                            if (chargeObj != null) {
                                code = this.mapErrorCodeEpayMegaCard(chargeObj.getStatus());
                                if (code == 0) {
                                    money = Math.round((double)Integer.parseInt(chargeObj.getAmount()) * GameCommon.getValueDouble("RATIO_NAP_MEGA_CARD"));
                                }
                                message = new RechargeByCardMessage(nickname, transID, "MegaCard", serial, pin, Integer.parseInt(chargeObj.getAmount()), Integer.parseInt(chargeObj.getStatus()), chargeObj.getMessage(), code, (int)money, (String)null, (String)null, "epay", platform, userNameMega);
                                EpayMegaCardAlert.megaDisconnect = 0;
                            } else {
                                message = new RechargeByCardMessage(nickname, transID, "MegaCard", serial, pin, 0, -1, "L\u00e1\u00bb\u2014i k\u00e1\u00ba\u00bft n\u00e1\u00bb\u2018i Epay MegaCard", code, 0, (String)null, (String)null, "epay", platform, userNameMega);
                                ++EpayMegaCardAlert.megaDisconnect;
                            }
                            long moneyUser = user.getVin();
                            long currentMoney = user.getVinTotal();
                            long rechargeMoney = user.getRechargeMoney();
                            if (code == 0) {
                                user.setVin(moneyUser += money);
                                user.setVinTotal(currentMoney += money);
                                user.setRechargeMoney(rechargeMoney += money);
                                user.setRechargeMegaCardFail(0);
                                String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: Th\u00c3\u00a0nh c\u00c3\u00b4ng, M\u00c3\u00a3 GD: " + transID + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + chargeObj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByMegaCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByMegaCard", "N\u00e1\u00ba\u00a1p qua Mega Card", currentMoney, money, "vin", description, 0L, false, user.isBot());
                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                userMap.put(nickname, user);
                                res.setCurrentMoney(currentMoney);
                                EpayMegaCardAlert.megaDisconnect = 0;
                                EpayMegaCardAlert.megaPending = 0;
                            } else if (code == 30) {
                                String description = "K\u00e1\u00ba\u00bft qu\u00e1\u00ba\u00a3: \u00c4\ufffdang x\u00e1\u00bb\u00ad l\u00c3\u00bd, M\u00c3\u00a3 GD: " + transID + ", M\u00e1\u00bb\u2021nh gi\u00c3\u00a1: " + chargeObj.getAmount() + ", Serial: " + serial + ", Pin: " + pin;
                                LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByMegaCard", "N\u00e1\u00ba\u00a1p qua Mega Card", currentMoney, 0L, "vin", description, 0L, false, user.isBot());
                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog2, 601);
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                                EpayMegaCardAlert.megaDisconnect = 0;
                                ++EpayMegaCardAlert.megaPending;
                            } else {
                                if (code != 1) {
                                    cardFail = true;
                                }
                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)301);
                            }
                        } else {
                            code = 35;
                            cardFail = true;
                        }
                    } else {
                        code = 36;
                        cardFail = true;
                    }
                } else {
                    code = 8;
                    res.setTime(time);
                }
                if (cardFail) {
                    user.setRechargeMegaCardFail(user.getRechargeMegaCardFail() + 1);
                    user.setRechargeMegaCardFailTime(new Date());
                    userMap.put(nickname, user);
                }
                res.setFail(user.getRechargeMegaCardFail());
            }
            catch (Exception e2) {
                code = 1;
                logger.debug(e2);
                MoneyLogger.log(nickname, "RechargeByMegaCard", 0L, 0L, "vin", "Nap vin qua MegaCard", "1001", e2.getMessage());
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        res.setCode(code);
        logger.debug(("Response rechargeByMegaCard: " + code));
        if (EpayMegaCardAlert.megaPending == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)EpayMegaCardAlert.alertMegaPendingTime, (int)1)) {
            this.alert(GameCommon.getValueStr("MEGA_CARD_GROUP_NUMBER"), "[CANH BAO] He thong MegaCard tra ve pending qua 5 lan lien tiep!", false);
            EpayMegaCardAlert.alertMegaPendingTime = new Date();
        }
        if (EpayMegaCardAlert.megaDisconnect == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)EpayMegaCardAlert.alertMegaDisconnectTime, (int)1)) {
            this.alert(GameCommon.getValueStr("MEGA_CARD_GROUP_NUMBER"), "SOS! Canh bao he thong MegaCard dang bi mat ket noi!", true);
            EpayMegaCardAlert.alertMegaDisconnectTime = new Date();
        }
        return res;
    }

    public RechargeResponse rechargeByVcoin(String nickname, String serial, String pin, String platform) throws Exception {
        logger.debug(("Start rechargeByVcoin ham cha:  nickname: " + nickname + ", serial: " + serial + ", pin: " + pin));
        RechargeResponse response = null;
        return response;
    }

    private void alert(String number, String content, boolean isCall) {
        AlertServiceImpl alertService = new AlertServiceImpl();
        if (number.contains(",")) {
            String[] arr = number.split(",");
            ArrayList<String> mList = new ArrayList<String>();
            for (String m : arr) {
                m = m.trim();
                mList.add(m);
            }
            alertService.alert2List(mList, content, isCall);
        } else {
            alertService.alert2One(number, content, isCall);
        }
    }

    // xử lý gach thẻ callback
    public String receiveResultFromIwin99BuyCard(Map<String, String[]> request) {
        block22:
        {
            Iwin99CallbackResponse response = new Iwin99CallbackResponse(-1, "");
            try {
                logger.debug("Start receiveResultFromIwin99BuyCard: ");
                StringBuilder log = new StringBuilder();
                HashMap<String, String> fields = new HashMap<>();
                for (Map.Entry<String, String[]> entry : request.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue()[0];
                    log.append(fieldName).append(":").append(fieldValue).append(", ");
                    if (fieldValue == null || fieldValue.length() <= 0) {
                        continue;
                    }
                    fields.put(fieldName, fieldValue);
                }
                String signature = fields.get("signature");
                String md5Hex = DigestUtils.md5Hex(PartnerConfig.GachTheClientId + "|" + PartnerConfig.GachTheSecretKey + "|" + fields.get("transaction_id") + "|" + fields.get("status"));
//                String md5Hex = DigestUtils.md5Hex(encryptString);
                logger.debug("signature = : " + md5Hex);
                boolean isSignature = md5Hex.equals(signature);
                if (!isSignature) {
                    response.setErrorCode(1);
                    response.setErrorDescription("invalid signature");
                    return response.toJson();
                }
                logger.debug("receiveResultFromIwin99BuyCard ==>> Paramester: " + log.toString());
                RechargeByCardMessage message;
                HazelcastInstance client;
                client = HazelcastClientFactory.getInstance();
                // tìm theo id
                RechargeDaoImpl dao = new RechargeDaoImpl();
                Document trans = dao.getRechargeByGachthe(fields.get("transaction_id"));
                if (trans != null && trans.getInteger("code") == 30) {
//                    //Recheck:
//                    TlxGachTheClient tlxGachTheClient = new TlxGachTheClient(PartnerConfig.TlxGachThePrivateKey,
//                        PartnerConfig.TlxGachTheSecretKey, PartnerConfig.TlxGachTheDepositUrl,
//                        PartnerConfig.TlxGachTheRecheckUrl);
//                    TlxRecheckResponse tlxRecheckResponse = tlxGachTheClient.recheck(fields.get("tran_code").toString());
                    String nickname = trans.getString("nick_name");
//                    if(trans.getLong("amount") == Double.parseDouble(fields.get("amount"))){
//                        return "amount not same";
//                    }
                    if (StringUtils.equals(fields.get("status").toLowerCase(), "1")) {
                        IMap<String, UserModel> userMap;
                        //get user
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                        try {
                            long cardAmount = Long.parseLong(fields.get("value"));
                            message = new RechargeByCardMessage(nickname, trans.getString("request_id"), trans.getString("provider"),
                                    trans.getString("serial"), trans.getString("pin"), (int) cardAmount, 1, "Thanh cong", 1, (int) cardAmount,
                                    null, null, "gachthe", trans.getString("platform"), null);
                            String description;
                            long moneyUser = user.getVin();
                            long currentMoney = user.getVinTotal();
                            long rechargeMoney = user.getRechargeMoney();
                            // Thẻ ok
                            long money = Math.round(cardAmount/* * GameCommon.getValueDouble("RATIO_RECHARGE_CARD")*/);
                            user.setVin(moneyUser += money);
                            user.setVinTotal(currentMoney += money);
                            user.setRechargeMoney(rechargeMoney += money);
                            user.setRechargeFail(0);
                            description = "K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: " + trans.getString("request_id") + ", Th\u1ebb: " + trans.getString("provider") + ", M\u1ec7nh gi\u00e1: " + cardAmount + ", Serial: " + trans.getString("serial") + ", Pin: " + trans.getString("pin") + "";
                            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RechargeByCard", "N\u1ea1p Win qua th\u1ebb", currentMoney, money, "vin", description, 0L, false, user.isBot());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                            MessageBusFactory.get("queue_dvt").publish("queue_dvt", message, 301);
                            userMap.put(nickname, user);
                            // update trans success
                            dao.UpdateGachtheTransctions(fields.get("TransID"), 0, "success");
                            logger.debug("receiveResultFromIwin99BuyCard ==>>UpdateGachtheTransctions success ");
                            response.setErrorCode(0);
                            //RMQApi.publishMessage((String) "queue_dvt", (BaseMessage) message, (int) 301);
                        } catch (Exception ex) {
                            logger.debug(ex);
                            MoneyLogger.log("", "RechargeByGachThe", 0L, 0L, "vin", "Nap vin qua the cao", "1001", ex.getMessage());
                            return ex.getMessage();
                        } finally {
                            userMap.unlock(nickname);
                        }
                    } else {
                        // failed
                        message = new RechargeByCardMessage(nickname, trans.getString("request_id"), trans.getString("provider"),
                                trans.getString("serial"), trans.getString("pin"), 0, -1, "The loi", -1, 0,
                                null, null, "gachthe", trans.getString("platform"), null);
                        MessageBusFactory.get("queue_dvt").publish("queue_dvt", message, 301);
                        dao.UpdateGachtheTransctions(fields.get("TransID"), -1, "failed");
                        response.setErrorCode(3);
                    }
                    response.setErrorDescription("valid trans");
                    return response.toJson();
                } else {
                    logger.debug("receiveResultFromIwin99BuyCard ==>> Can't find transaction: ");
                    response.setErrorDescription("Can't find transactionRechargeByGachthe");
                    return response.toJson();
                }
            } catch (Exception e4) {
                logger.debug(e4);
                MoneyLogger.log("", "RechargeByGachThe", 0L, 0L, "vin", "Nap vin qua the cao", "1001", e4.getMessage());
                return response.toJson();
            }
        }
    }


    public RechargeResponse rechargeByGachThe(String nickname, ProviderType provider, String serial, String pin, String sAmount, String platform, int UserId) throws Exception {
        //RechargeServiceImpl.logger.debug(("Start rechargeByCard Gachthe:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin));
        MoneyLogger.logGameThe("Start rechargeByCard Gachthe:  nickname: " + nickname + ", provider: " + provider.getName() + ", serial: " + serial + ", pin: " + pin);
        int code = 1;
        HazelcastInstance client;
        IMap<String, UserModel> userMap;
        String description;
        RechargeResponse res = new RechargeResponse(code, 0L, 0, 0L);
        if (GameCommon.getValueInt("IS_RECHARGE_CARD") == 1 || provider == null) {
            RechargeServiceImpl.logger.debug("rechargeByCard: param fail");
            return res;
        }
        if (pin != null) {
            pin = pin.trim();
        }
        if (serial != null) {
            serial = serial.trim();
        }
        MoneyLogger.logGameThe("rechargeByGachThe  abccc");
        if ((client = HazelcastClientFactory.getInstance()) == null) {
            MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1030", "can not connect hazelcast");
            return res;
        }
        long amount = Long.parseLong(sAmount);
        userMap = client.getMap("users");
        RechargeByCardMessage message = null;
        if (userMap.containsKey(nickname)) {
            try {
                UserCacheModel user = (UserCacheModel) userMap.get(nickname);
                res.setCurrentMoney(user.getVinTotal());
                boolean cardFail = false;
                if (UserValidaton.validateSerialPin(serial)) {
                    if (UserValidaton.validateSerialPin(pin)) {
                        // check exists card pending
                        RechargeDaoImpl dao = new RechargeDaoImpl();
                        Document doc = dao.getRechargeByGachthe(nickname, serial, pin);
                        MoneyLogger.logGameThe("rechargeByGachThe  22222");
                        if (doc != null) {
                            // pending card
                            code = 30;
                        } else {
                            MoneyLogger.logGameThe("rechargeByGachThe  xxxxx");
                            String id = VinPlayUtils.genTransactionId(user.getId());
//                            res.setTid(id);
                            GachTheClient gachTheClient = new GachTheClient();
                            org.json.simple.JSONObject result = gachTheClient.doCharge(provider.getId(), provider.getValue().toUpperCase(), pin, serial, id, amount);
                            MoneyLogger.logGameThe(result.toJSONString());
                            MoneyLogger.log(nickname, "LOGJSON", amount, 0L, "vin", "Nap vin qua the", "1030", result.toJSONString());
                            res.setMessage(result.get("message").toString());
                            if ("0".equals(result.get("status").toString())) {
                                code = 1;
                            } else if ("1".equals(result.get("status").toString())) {
                                try {
                                    code = 30;
                                    // thẻ được chấp nhận
                                    userMap = client.getMap("users");
                                    user = (UserCacheModel) userMap.get(nickname);
                                    long moneyUser = user.getVin();
                                    long currentMoney = user.getVinTotal();
                                    long rechargeMoney = user.getRechargeMoney();

                                    if (code == 30) {
                                        UserServiceImpl userService = new UserServiceImpl();

                                        long money = Math.round((double) amount * GameCommon.getValueDouble("RATIO_RECHARGE_CARD"));
                                        //String nickname, String serial, String pin, int amount, String requestId, String requestTime, int code, String des, int money
                                        dao.saveLogRechargeByGachThe(nickname, serial, pin, amount, id, System.currentTimeMillis() + "", 30, "pending_card", currentMoney, provider.getValue(), platform, currentMoney, money, UserId, user.getUsername(), "gachthe",
                                                "");
                                    }
                                } catch (Exception e2) {
                                    code = 1;
                                    RechargeServiceImpl.logger.debug(e2);
                                    MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e2.getMessage());
                                } finally {

                                }
                            } else {
                                code = 1;
                            }
                        }
                    } else {
                        code = 35;
                        cardFail = true;
                    }
                } else {
                    code = 36;
                    cardFail = true;
                }
                if (cardFail) {
                    try {
                        userMap = client.getMap("users");
                        userMap.lock(nickname);
                        user = (UserCacheModel) userMap.get(nickname);
                        user.setRechargeFail(user.getRechargeFail() + 1);
                        user.setRechargeFailTime(new Date());
                        userMap.put(nickname, user);
                    } catch (Exception e3) {
                        code = 1;
                        RechargeServiceImpl.logger.debug(e3);
                        MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e3.getMessage());
//                        obj = null;
//                        return obj;
                    } finally {
                        userMap.unlock(nickname);
                    }
                }
                res.setFail(user.getRechargeFail());
            } catch (Exception e4) {
                code = 1;
                RechargeServiceImpl.logger.debug(e4);
                MoneyLogger.log(nickname, "RechargeByCard", 0L, 0L, "vin", "Nap vin qua the", "1001", e4.getMessage());
            }
        }
        res.setCode(code);
        return res;
    }

    public String rechargeByBankIwin99(Map<String, String[]> parameterMap) {
        return null;
    }

    public String rechargeByMomoIwin99(Map<String, String[]> parameterMap) {
        return null;
    }
}

