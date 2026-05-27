/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.BankType
 *  com.vinplay.vbee.common.enums.PhoneCardType
 *  com.vinplay.vbee.common.enums.ProviderType
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage
 *  com.vinplay.vbee.common.models.Softpin
 *  com.vinplay.vbee.common.models.SoftpinJson
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.NumberUtils
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.dichvuthe.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay._1pay._1PayAlert;
import com.vinplay._1pay._1PayClient;
import com.vinplay.dichvuthe.client.DvtAlert;
import com.vinplay.dichvuthe.client.VinplayClient;
import com.vinplay.dichvuthe.dao.impl.CashoutDaoImpl;
import com.vinplay.dichvuthe.encode.AES;
import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.dichvuthe.entities.BankAccountInfo;
import com.vinplay.dichvuthe.entities.BankcoObj;
import com.vinplay.dichvuthe.entities.SoftpinObj;
import com.vinplay.dichvuthe.entities.TopupObj;
import com.vinplay.dichvuthe.response.CashoutResponse;
import com.vinplay.dichvuthe.response.CashoutTransResponse;
import com.vinplay.dichvuthe.response.SoftpinJsonResponse;
import com.vinplay.dichvuthe.response.SoftpinResponse;
import com.vinplay.dichvuthe.service.CashOutService;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.dichvuthe.utils.DvtUtils;
import com.vinplay.epay.EpayAlert;
import com.vinplay.epay.EpayClient;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.BankType;
import com.vinplay.vbee.common.enums.PhoneCardType;
import com.vinplay.vbee.common.enums.ProviderType;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.Softpin;
import com.vinplay.vbee.common.models.SoftpinJson;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.NumberUtils;
import com.vinplay.vbee.common.utils.UserValidaton;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vtc.VTCAlert;
import com.vinplay.vtc.VTCClient;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class CashOutServiceImpl
implements CashOutService {
    private static final Logger logger = Logger.getLogger((String)"cashout");

    @Override
    public SoftpinResponse cashOutByCard(String nickname, ProviderType provider, PhoneCardType phoneCardType, int quantity, boolean check) throws Exception {
        String name;
        logger.debug(("Start cashOutByCard ham cha:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
        SoftpinResponse response = null;
        block9 : switch (name = provider.getName()) {
            case "Viettel": {
                String valueStr2;
                String valueStr;
                switch (valueStr = GameCommon.getValueStr("CASHOUT_VTT_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr2 = GameCommon.getValueStr("CASHOUT_VTT_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "Mobifone": {
                String valueStr4;
                String valueStr3;
                switch (valueStr3 = GameCommon.getValueStr("CASHOUT_VMS_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr4 = GameCommon.getValueStr("CASHOUT_VMS_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "Vinaphone": {
                String valueStr6;
                String valueStr5;
                switch (valueStr5 = GameCommon.getValueStr("CASHOUT_VNP_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr6 = GameCommon.getValueStr("CASHOUT_VNP_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "VietNamMobile": {
                String valueStr7;
                String valueStr8;
                switch (valueStr7 = GameCommon.getValueStr("CASHOUT_VNM_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr8 = GameCommon.getValueStr("CASHOUT_VNM_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "Gate": {
                String valueStr10;
                String valueStr9;
                switch (valueStr9 = GameCommon.getValueStr("CASHOUT_GATE_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr10 = GameCommon.getValueStr("CASHOUT_GATE_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "Zing": {
                String valueStr12;
                String valueStr11;
                switch (valueStr11 = GameCommon.getValueStr("CASHOUT_ZING_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr12 = GameCommon.getValueStr("CASHOUT_ZING_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
            case "Vcoin": {
                String valueStr14;
                String valueStr13;
                switch (valueStr13 = GameCommon.getValueStr("CASHOUT_VCOIN_PRIMARY")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                if (response != null) break;
                switch (valueStr14 = GameCommon.getValueStr("CASHOUT_VCOIN_BACKUP")) {
                    case "1pay": {
                        response = this.cashOutByCard1Pay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "vtc": {
                        response = this.cashOutByCardVTC(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "epay": {
                        response = this.cashOutByCardEpay(nickname, provider, phoneCardType, quantity, check);
                        break block9;
                    }
                    case "dvt": {
                        response = this.cashOutByCardDvt(nickname, provider, phoneCardType, quantity, check);
                    }
                }
                break;
            }
        }
        if (response == null) {
            response = new SoftpinResponse("", 0, 0L, null);
            CashoutByCardMessage message = new CashoutByCardMessage(nickname, "", provider.getName(), 0, quantity, -1, "M\u1ea5t k\u1ebft n\u1ed1i \u0111\u1ebfn t\u1ea5t c\u1ea3 c\u00e1c partner", "", "", 1, "", "");
            MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
        }
        logger.debug(("Finish cashOutByCard ham cha:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private SoftpinResponse cashOutByCardEpay(String nickname, ProviderType provider, PhoneCardType phoneCardType, int quantity, boolean check) throws Exception {
        int code;
        SoftpinResponse res;
        block49 : {
            logger.debug(("Start cashOutByCardEpay:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
            code = 1;
            String errorMessage = "";
            res = new SoftpinResponse("", code, 0L, null);
            if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1 || provider == null || phoneCardType == null || quantity > GameCommon.getValueInt("NUM_CASHOUT_CARD") || quantity <= 0) {
                logger.debug("cashOutByCard: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)(amount * quantity) * GameCommon.getValueDouble("RATIO_CASHOUT_CARD"));
            long moneyCashoutReal = amount * quantity;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByCard: money limit cashout");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1030", "can not connect hazelcast");
                return res;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            CashoutDaoImpl cashDao = new CashoutDaoImpl();
            if (userMap.containsKey(nickname)) {
                try {
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    res.setCurrentMoney(currentMoney);
                    if (!user.isBanCashOut()) {
                        if (user.getMobile() != null && user.isHasMobileSecurity()) {
                            if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                if (moneyUser - money >= 0L) {
                                    int moneyCashout = 0;
                                    if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                        moneyCashout = user.getCashout();
                                    }
                                    if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                        long systemCashout = cashDao.getSystemCashout();
                                        if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                            if (check) {
                                                code = 0;
                                                break block49;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                if (moneyUser - money < 0L) {
                                                    code = 3;
                                                    res.setCode(code);
                                                    SoftpinResponse softpinResponse = res;
                                                    return softpinResponse;
                                                }
                                                user.setVin(moneyUser -= money);
                                                user.setVinTotal(currentMoney -= money);
                                                user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                user.setCashoutTime(new Date());
                                                userMap.put(nickname, user);
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                                logger.debug(e);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e.getMessage());
                                                code = 1;
                                                errorMessage = e.getMessage();
                                                SoftpinResponse softpinResponse = null;
                                                return softpinResponse;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                                            SoftpinObj softpinObj = null;
                                            try {
                                                softpinObj = EpayClient.downloadSoftpin(id, provider.getValue(), amount, quantity);
                                            }
                                            catch (SocketTimeoutException timeout) {
                                                timeout.printStackTrace();
                                                logger.debug(timeout);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi ePay time out: " + timeout.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                if (++EpayAlert.timeoutCashout == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertTimeoutCashoutTime, (int)1)) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den ePay bi timeout qua 5 lan lien tiep!", false);
                                                    EpayAlert.alertTimeoutCashoutTime = new Date();
                                                }
                                                code = 30;
                                                errorMessage = timeout.getMessage();
                                            }
                                            catch (Exception e2) {
                                                e2.printStackTrace();
                                                logger.debug(e2);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi ePay" + e2.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                ++EpayAlert.disconnectCashout;
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    logger.debug(ex);
                                                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex.getMessage());
                                                    code = 1;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                CashoutByCardMessage message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "Loi ket noi ePay" + e2.getMessage(), "", "", code, "epay", "");
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                if (EpayAlert.disconnectCashout != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                                if (!VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertDisconnectCashoutTime, (int)1)) return null;
                                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den ePay bi timeout qua 5 lan lien tiep!", false);
                                                EpayAlert.alertDisconnectCashoutTime = new Date();
                                                return null;
                                            }
                                            String sign = "";
                                            CashoutByCardMessage message = null;
                                            if (softpinObj != null) {
                                                sign = softpinObj.getSign();
                                                code = this.mapErrorCodeEpay(softpinObj.getStatus());
                                                if (code == 999) {
                                                    if (VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertOutOfMoneyTime, (int)1)) {
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "[CANH BAO] So du tai khoan Vinplay tai 1Pay dang het tien!", false);
                                                        EpayAlert.alertOutOfMoneyTime = new Date();
                                                    }
                                                    try {
                                                        userMap = client.getMap("users");
                                                         userMap.lock(nickname);
                                                        user = (UserCacheModel)userMap.get(nickname);
                                                        moneyUser = user.getVin();
                                                        currentMoney = user.getVinTotal();
                                                        user.setVin(moneyUser += money);
                                                        user.setVinTotal(currentMoney += money);
                                                        user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                        userMap.put(nickname, user);
                                                        return null;
                                                    }
                                                    catch (Exception ex2) {
                                                        ex2.printStackTrace();
                                                        logger.debug(ex2);
                                                        MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex2.getMessage());
                                                        code = 1;
                                                        return null;
                                                    }
                                                    finally {
                                                         userMap.unlock(nickname);
                                                    }
                                                }
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, softpinObj.getStatus(), softpinObj.getMessage(), "", sign, code, "epay", softpinObj.getPartnerTransId());
                                                EpayAlert.disconnectCashout = 0;
                                                EpayAlert.timeoutCashout = 0;
                                            } else {
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "L\u1ed7i k\u1ebft n\u1ed1i ePay: " + errorMessage, "", sign, code, "epay", "");
                                            }
                                            if (code == 0 || code == 30) {
                                                String description = "";
                                                if (code == 0) {
                                                    StringBuilder builder = new StringBuilder("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: ").append(id).append(", Lo\u1ea1i th\u1ebb: ").append(provider.getName()).append(", M\u1ec7nh gi\u00e1: ").append(amount).append(", S\u1ed1 l\u01b0\u1ee3ng: ").append(quantity).append(", Danh s\u00e1ch th\u1ebb:,");
                                                    ArrayList<SoftpinJsonResponse> resSP = new ArrayList<SoftpinJsonResponse>();
                                                    StringBuilder des = new StringBuilder("");
                                                    for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                                                        String pin = sp.getPin();
                                                        pin = pin.substring(0, 4) + "-" + pin.substring(4, pin.length() - 4) + "-" + pin.substring(pin.length() - 4);
                                                        String expire = sp.getExpire();
                                                        if (expire.length() > 10) {
                                                            expire = expire.substring(expire.length() - 10);
                                                        }
                                                        builder.append("Serial: ").append(sp.getSerial()).append(". M\u00e3 th\u1ebb: ").append(pin).append(". HSD: ").append(expire).append(",");
                                                        SoftpinJsonResponse spj = new SoftpinJsonResponse(id, sp.getSerial(), pin, expire);
                                                        resSP.add(spj);
                                                        des.append("serial: ").append(sp.getSerial()).append(", pin: ").append(pin).append(", expire: ").append(expire).append("|");
                                                    }
                                                    builder.delete(builder.length() - 1, builder.length());
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    res.setSoftpin(mapper.writeValueAsString(resSP));
                                                    des.delete(des.length() - 1, des.length());
                                                    message.setSoftpin(des.toString());
                                                    description = builder.toString();
                                                } else {
                                                    description = "K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd, M\u00e3 GD: " + id + ", Lo\u1ea1i th\u1ebb: " + provider.getName() + ", M\u1ec7nh gi\u00e1: " + amount + ", S\u1ed1 l\u01b0\u1ee3ng: " + quantity;
                                                }
                                                res.setCurrentMoney(currentMoney);
                                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByCard", this.getServiceName(provider), currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                cashDao.updateSystemCashout(moneyCashoutReal);
                                                break block49;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                user.setVin(moneyUser += money);
                                                user.setVinTotal(currentMoney += money);
                                                user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                userMap.put(nickname, user);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                break block49;
                                            }
                                            catch (Exception e3) {
                                                e3.printStackTrace();
                                                logger.debug(e3);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e3.getMessage());
                                                code = 1;
                                                break block49;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                        }
                                        code = 21;
                                        break block49;
                                    }
                                    code = 20;
                                    break block49;
                                }
                                code = 3;
                                break block49;
                            }
                            code = 10;
                            break block49;
                        }
                        code = 9;
                        break block49;
                    }
                    code = 2;
                }
                catch (Exception e4) {
                    e4.printStackTrace();
                    logger.debug(e4);
                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e4.getMessage());
                    code = 1;
                }
            }
        }
        res.setCode(code);
        logger.debug(("Finish cashOutByCardEpay, Response: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private SoftpinResponse cashOutByCard1Pay(String nickname, ProviderType provider, PhoneCardType phoneCardType, int quantity, boolean check) throws Exception {
        int code;
        SoftpinResponse res;
        block48 : {
            logger.debug(("Start cashOutByCard1Pay:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
            code = 1;
            String errorMessage = "";
            res = new SoftpinResponse("", code, 0L, null);
            if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1 || provider == null || phoneCardType == null || quantity > GameCommon.getValueInt("NUM_CASHOUT_CARD") || quantity <= 0) {
                logger.debug("cashOutByCard: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)(amount * quantity) * GameCommon.getValueDouble("RATIO_CASHOUT_CARD"));
            long moneyCashoutReal = amount * quantity;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByCard: money limit cashout");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1030", "can not connect hazelcast");
                return res;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            CashoutDaoImpl cashDao = new CashoutDaoImpl();
            if (userMap.containsKey(nickname)) {
                try {
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    res.setCurrentMoney(currentMoney);
                    if (!user.isBanCashOut()) {
                        if (user.getMobile() != null && user.isHasMobileSecurity()) {
                            if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                if (moneyUser - money >= 0L) {
                                    int moneyCashout = 0;
                                    if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                        moneyCashout = user.getCashout();
                                    }
                                    if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                        long systemCashout = cashDao.getSystemCashout();
                                        if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                            if (check) {
                                                code = 0;
                                                break block48;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                if (moneyUser - money < 0L) {
                                                    code = 3;
                                                    res.setCode(code);
                                                    SoftpinResponse softpinResponse = res;
                                                    return softpinResponse;
                                                }
                                                user.setVin(moneyUser -= money);
                                                user.setVinTotal(currentMoney -= money);
                                                user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                user.setCashoutTime(new Date());
                                                userMap.put(nickname, user);
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                                logger.debug(e);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e.getMessage());
                                                code = 1;
                                                errorMessage = e.getMessage();
                                                SoftpinResponse softpinResponse = null;
                                                return softpinResponse;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                                            SoftpinObj softpinObj = null;
                                            try {
                                                softpinObj = _1PayClient.buyCard(id, provider.getValue(), String.valueOf(amount), quantity);
                                            }
                                            catch (SocketTimeoutException timeout) {
                                                timeout.printStackTrace();
                                                logger.debug(timeout);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi 1Pay time out: " + timeout.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                if (++_1PayAlert._1PayTimeout == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)_1PayAlert.alert1PayTimeoutTime, (int)1)) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den 1Pay bi timeout qua 5 lan lien tiep!", false);
                                                    _1PayAlert.alert1PayTimeoutTime = new Date();
                                                }
                                                code = 30;
                                                errorMessage = timeout.getMessage();
                                            }
                                            catch (Exception e2) {
                                                e2.printStackTrace();
                                                logger.debug(e2);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi 1Pay" + e2.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                ++_1PayAlert._1PayDisconnect;
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    logger.debug(ex);
                                                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex.getMessage());
                                                    code = 1;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                CashoutByCardMessage message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "Loi ket noi 1Pay" + e2.getMessage(), "", "", code, "1pay", "");
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                if (_1PayAlert._1PayDisconnect != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                                if (!VinPlayUtils.isAlertTimeout((Date)_1PayAlert.alert1PayDisconnectTime, (int)1)) return null;
                                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong 1Pay dang bi mat ket noi!", true);
                                                _1PayAlert.alert1PayDisconnectTime = new Date();
                                                return null;
                                            }
                                            String sign = "";
                                            CashoutByCardMessage message = null;
                                            if (softpinObj != null) {
                                                sign = softpinObj.getSign();
                                                code = this.mapErrorCode1Pay(softpinObj.getStatus());
                                                if (code == 999) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "[CANH BAO] So du tai khoan Vinplay tai 1Pay dang het tien!", false);
                                                    try {
                                                        userMap = client.getMap("users");
                                                         userMap.lock(nickname);
                                                        user = (UserCacheModel)userMap.get(nickname);
                                                        moneyUser = user.getVin();
                                                        currentMoney = user.getVinTotal();
                                                        user.setVin(moneyUser += money);
                                                        user.setVinTotal(currentMoney += money);
                                                        user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                        userMap.put(nickname, user);
                                                        return null;
                                                    }
                                                    catch (Exception ex2) {
                                                        ex2.printStackTrace();
                                                        logger.debug(ex2);
                                                        MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex2.getMessage());
                                                        code = 1;
                                                        return null;
                                                    }
                                                    finally {
                                                         userMap.unlock(nickname);
                                                    }
                                                }
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, softpinObj.getStatus(), this.mapMessage1Pay(softpinObj.getStatus()), "", sign, code, "1pay", softpinObj.getPartnerTransId());
                                                _1PayAlert._1PayDisconnect = 0;
                                                _1PayAlert._1PayTimeout = 0;
                                            } else {
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "L\u1ed7i k\u1ebft n\u1ed1i 1Pay: " + errorMessage, "", sign, code, "1pay", "");
                                            }
                                            if (code == 0 || code == 30) {
                                                String description = "";
                                                if (code == 0) {
                                                    StringBuilder builder = new StringBuilder("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: ").append(id).append(", Lo\u1ea1i th\u1ebb: ").append(provider.getName()).append(", M\u1ec7nh gi\u00e1: ").append(amount).append(", S\u1ed1 l\u01b0\u1ee3ng: ").append(quantity).append(", Danh s\u00e1ch th\u1ebb:,");
                                                    ArrayList<SoftpinJsonResponse> resSP = new ArrayList<SoftpinJsonResponse>();
                                                    StringBuilder des = new StringBuilder("");
                                                    for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                                                        String pin = sp.getPin();
                                                        pin = pin.substring(0, 4) + "-" + pin.substring(4, pin.length() - 4) + "-" + pin.substring(pin.length() - 4);
                                                        String expire = sp.getExpire();
                                                        if (expire.length() > 10) {
                                                            expire = expire.substring(expire.length() - 10);
                                                        }
                                                        builder.append("Serial: ").append(sp.getSerial()).append(". M\u00e3 th\u1ebb: ").append(pin).append(". HSD: ").append(expire).append(",");
                                                        SoftpinJsonResponse spj = new SoftpinJsonResponse(id, sp.getSerial(), pin, expire);
                                                        resSP.add(spj);
                                                        des.append("serial: ").append(sp.getSerial()).append(", pin: ").append(pin).append(", expire: ").append(expire).append("|");
                                                    }
                                                    builder.delete(builder.length() - 1, builder.length());
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    res.setSoftpin(mapper.writeValueAsString(resSP));
                                                    des.delete(des.length() - 1, des.length());
                                                    message.setSoftpin(des.toString());
                                                    description = builder.toString();
                                                } else {
                                                    description = "K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd, M\u00e3 GD: " + id + ", Lo\u1ea1i th\u1ebb: " + provider.getName() + ", M\u1ec7nh gi\u00e1: " + amount + ", S\u1ed1 l\u01b0\u1ee3ng: " + quantity;
                                                }
                                                res.setCurrentMoney(currentMoney);
                                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByCard", this.getServiceName(provider), currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                cashDao.updateSystemCashout(moneyCashoutReal);
                                                break block48;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                user.setVin(moneyUser += money);
                                                user.setVinTotal(currentMoney += money);
                                                user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                userMap.put(nickname, user);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                break block48;
                                            }
                                            catch (Exception e3) {
                                                e3.printStackTrace();
                                                logger.debug(e3);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e3.getMessage());
                                                code = 1;
                                                break block48;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                        }
                                        code = 21;
                                        break block48;
                                    }
                                    code = 20;
                                    break block48;
                                }
                                code = 3;
                                break block48;
                            }
                            code = 10;
                            break block48;
                        }
                        code = 9;
                        break block48;
                    }
                    code = 2;
                }
                catch (Exception e4) {
                    e4.printStackTrace();
                    logger.debug(e4);
                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e4.getMessage());
                    code = 1;
                }
            }
        }
        res.setCode(code);
        logger.debug(("Finish cashOutByCard1Pay, Response: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private SoftpinResponse cashOutByCardVTC(String nickname, ProviderType provider, PhoneCardType phoneCardType, int quantity, boolean check) throws Exception {
        int code;
        SoftpinResponse res;
        block48 : {
            logger.debug(("Start cashOutByCardVTC:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
            String errorMessage = "";
            code = 1;
            res = new SoftpinResponse("", code, 0L, null);
            if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1 || provider == null || phoneCardType == null || quantity > GameCommon.getValueInt("NUM_CASHOUT_CARD") || quantity <= 0) {
                logger.debug("cashOutByCard: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)(amount * quantity) * GameCommon.getValueDouble("RATIO_CASHOUT_CARD"));
            long moneyCashoutReal = amount * quantity;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByCard: money limit cashout");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1030", "can not connect hazelcast");
                return res;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            CashoutDaoImpl cashDao = new CashoutDaoImpl();
            if (userMap.containsKey(nickname)) {
                try {
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    res.setCurrentMoney(currentMoney);
                    if (!user.isBanCashOut()) {
                        if (user.getMobile() != null && user.isHasMobileSecurity()) {
                            if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                if (moneyUser - money >= 0L) {
                                    int moneyCashout = 0;
                                    if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                        moneyCashout = user.getCashout();
                                    }
                                    if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                        long systemCashout = cashDao.getSystemCashout();
                                        if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                            if (check) {
                                                code = 0;
                                                break block48;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                if (moneyUser - money < 0L) {
                                                    code = 3;
                                                    res.setCode(code);
                                                    SoftpinResponse softpinResponse = res;
                                                    return softpinResponse;
                                                }
                                                user.setVin(moneyUser -= money);
                                                user.setVinTotal(currentMoney -= money);
                                                user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                user.setCashoutTime(new Date());
                                                userMap.put(nickname, user);
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                                logger.debug(e);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e.getMessage());
                                                code = 1;
                                                errorMessage = e.getMessage();
                                                SoftpinResponse softpinResponse = null;
                                                return softpinResponse;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                                            SoftpinObj softpinObj = null;
                                            try {
                                                softpinObj = VTCClient.buyCard(id, provider.getValue(), String.valueOf(amount), quantity);
                                            }
                                            catch (SocketTimeoutException timeout) {
                                                timeout.printStackTrace();
                                                logger.debug(timeout);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi VTC timeout: " + timeout.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                if (++VTCAlert.timeoutCashout == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertTimeoutCashoutTime, (int)1)) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den VTC bi timeout qua 5 lan lien tiep!", false);
                                                    VTCAlert.alertTimeoutCashoutTime = new Date();
                                                }
                                                code = 30;
                                                errorMessage = timeout.getMessage();
                                            }
                                            catch (Exception e2) {
                                                e2.printStackTrace();
                                                logger.debug(e2);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi VTC" + e2.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                ++VTCAlert.disconnectCashout;
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    logger.debug(ex);
                                                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex.getMessage());
                                                    code = 1;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                CashoutByCardMessage message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "Loi ket noi VTC" + e2.getMessage(), "", "", code, "vtc", "");
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                if (VTCAlert.disconnectCashout != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                                if (!VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertDisconnectCashoutTime, (int)1)) return null;
                                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong VTC dang bi mat ket noi!", true);
                                                VTCAlert.alertDisconnectCashoutTime = new Date();
                                                return null;
                                            }
                                            String sign = "";
                                            CashoutByCardMessage message = null;
                                            if (softpinObj != null) {
                                                sign = softpinObj.getSign();
                                                code = this.mapErrorCodeVTC(softpinObj.getStatus());
                                                if (code == 999) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "[CANH BAO] So du tai khoan Vinplay tai VTC dang het tien!", false);
                                                    try {
                                                        userMap = client.getMap("users");
                                                         userMap.lock(nickname);
                                                        user = (UserCacheModel)userMap.get(nickname);
                                                        moneyUser = user.getVin();
                                                        currentMoney = user.getVinTotal();
                                                        user.setVin(moneyUser += money);
                                                        user.setVinTotal(currentMoney += money);
                                                        user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                        userMap.put(nickname, user);
                                                        return null;
                                                    }
                                                    catch (Exception ex2) {
                                                        ex2.printStackTrace();
                                                        logger.debug(ex2);
                                                        MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex2.getMessage());
                                                        code = 1;
                                                        return null;
                                                    }
                                                    finally {
                                                         userMap.unlock(nickname);
                                                    }
                                                }
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, softpinObj.getStatus(), softpinObj.getMessage(), "", sign, code, "vtc", softpinObj.getPartnerTransId());
                                                VTCAlert.disconnectCashout = 0;
                                                VTCAlert.timeoutCashout = 0;
                                            } else {
                                                message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "L\u1ed7i k\u1ebft n\u1ed1i VTC: " + errorMessage, "", sign, code, "vtc", "");
                                            }
                                            if (code == 0 || code == 30) {
                                                String description = "";
                                                if (code == 0) {
                                                    StringBuilder builder = new StringBuilder("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: ").append(id).append(", Lo\u1ea1i th\u1ebb: ").append(provider.getName()).append(", M\u1ec7nh gi\u00e1: ").append(amount).append(", S\u1ed1 l\u01b0\u1ee3ng: ").append(quantity).append(", Danh s\u00e1ch th\u1ebb:,");
                                                    ArrayList<SoftpinJsonResponse> resSP = new ArrayList<SoftpinJsonResponse>();
                                                    StringBuilder des = new StringBuilder("");
                                                    for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                                                        String pin = sp.getPin();
                                                        pin = pin.substring(0, 4) + "-" + pin.substring(4, pin.length() - 4) + "-" + pin.substring(pin.length() - 4);
                                                        String expire = sp.getExpire();
                                                        if (expire.length() > 10) {
                                                            expire = expire.substring(expire.length() - 10);
                                                        }
                                                        builder.append("Serial: ").append(sp.getSerial()).append(". M\u00e3 th\u1ebb: ").append(pin).append(". HSD: ").append(expire).append(",");
                                                        SoftpinJsonResponse spj = new SoftpinJsonResponse(id, sp.getSerial(), pin, expire);
                                                        resSP.add(spj);
                                                        des.append("serial: ").append(sp.getSerial()).append(", pin: ").append(pin).append(", expire: ").append(expire).append("|");
                                                    }
                                                    builder.delete(builder.length() - 1, builder.length());
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    res.setSoftpin(mapper.writeValueAsString(resSP));
                                                    des.delete(des.length() - 1, des.length());
                                                    message.setSoftpin(des.toString());
                                                    description = builder.toString();
                                                } else {
                                                    description = "K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd, M\u00e3 GD: " + id + ", Lo\u1ea1i th\u1ebb: " + provider.getName() + ", M\u1ec7nh gi\u00e1: " + amount + ", S\u1ed1 l\u01b0\u1ee3ng: " + quantity;
                                                }
                                                res.setCurrentMoney(currentMoney);
                                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByCard", this.getServiceName(provider), currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                cashDao.updateSystemCashout(moneyCashoutReal);
                                                break block48;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                user.setVin(moneyUser += money);
                                                user.setVinTotal(currentMoney += money);
                                                user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                userMap.put(nickname, user);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                break block48;
                                            }
                                            catch (Exception e3) {
                                                e3.printStackTrace();
                                                logger.debug(e3);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e3.getMessage());
                                                code = 1;
                                                break block48;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                        }
                                        code = 21;
                                        break block48;
                                    }
                                    code = 20;
                                    break block48;
                                }
                                code = 3;
                                break block48;
                            }
                            code = 10;
                            break block48;
                        }
                        code = 9;
                        break block48;
                    }
                    code = 2;
                }
                catch (Exception e4) {
                    e4.printStackTrace();
                    logger.debug(e4);
                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e4.getMessage());
                    code = 1;
                }
            }
        }
        res.setCode(code);
        logger.debug(("Finish cashOutByCardVTC, Response: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private SoftpinResponse cashOutByCardDvt(String nickname, ProviderType provider, PhoneCardType phoneCardType, int quantity, boolean check) throws Exception {
        int code;
        SoftpinResponse res;
        block42 : {
            logger.debug(("Start Request cashOutByCardDvt:  nickname: " + nickname + ", provider: " + provider.getName() + ", phoneCardType: " + phoneCardType.getValue() + ", quantity: " + quantity + ", check: " + check));
            String errorMessage = "";
            code = 1;
            res = new SoftpinResponse("", code, 0L, null);
            if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1 || provider == null || phoneCardType == null || quantity > GameCommon.getValueInt("NUM_CASHOUT_CARD") || quantity <= 0) {
                logger.debug("cashOutByCard: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)(amount * quantity) * GameCommon.getValueDouble("RATIO_CASHOUT_CARD"));
            long moneyCashoutReal = amount * quantity;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByCard: money limit cashout");
                return res;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1030", "can not connect hazelcast");
                return res;
            }
            IMap<String, UserModel> userMap = client.getMap("users");
            CashoutDaoImpl cashDao = new CashoutDaoImpl();
            if (userMap.containsKey(nickname)) {
                try {
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    res.setCurrentMoney(currentMoney);
                    if (!user.isBanCashOut()) {
                        if (user.getMobile() != null && user.isHasMobileSecurity()) {
                            if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                if (moneyUser - money >= 0L) {
                                    int moneyCashout = 0;
                                    if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                        moneyCashout = user.getCashout();
                                    }
                                    if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                        long systemCashout = cashDao.getSystemCashout();
                                        if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                            if (check) {
                                                code = 0;
                                                break block42;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                if (moneyUser - money < 0L) {
                                                    code = 3;
                                                    res.setCode(code);
                                                    SoftpinResponse softpinResponse = res;
                                                    return softpinResponse;
                                                }
                                                user.setVin(moneyUser -= money);
                                                user.setVinTotal(currentMoney -= money);
                                                user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                user.setCashoutTime(new Date());
                                                userMap.put(nickname, user);
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                                logger.debug(e);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e.getMessage());
                                                code = 1;
                                                errorMessage = e.getMessage();
                                                SoftpinResponse softpinResponse = null;
                                                return softpinResponse;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                            String id = VinPlayUtils.genTransactionId((int)user.getId());
                                            String sign = RSA.sign(id + provider.getValue() + amount + quantity, GameCommon.getValueStr("DVT_PRIVATE_KEY"));
                                            SoftpinObj softpinObj = null;
                                            try {
                                                softpinObj = VinplayClient.cashOutByCard(id, provider.getValue(), amount, quantity, sign);
                                            }
                                            catch (SocketTimeoutException timeout) {
                                                timeout.printStackTrace();
                                                logger.debug(timeout);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi dvt timeout: " + timeout.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                if (++DvtAlert.timeoutCashout == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertTimeoutCashoutTime, (int)1)) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den dichvuthe bi timeout qua 5 lan lien tiep!", false);
                                                    DvtAlert.alertTimeoutCashoutTime = new Date();
                                                }
                                                code = 30;
                                                errorMessage = timeout.getMessage();
                                            }
                                            catch (Exception e2) {
                                                e2.printStackTrace();
                                                logger.debug(e2);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1034", "Loi ket noi dvt" + e2.getMessage());
                                                DvtUtils.errorDvt(client, "CashOutByCard");
                                                ++DvtAlert.disconnectCashout;
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    logger.debug(ex);
                                                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", ex.getMessage());
                                                    code = 1;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                CashoutByCardMessage message = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "Loi ket noi Dvt" + e2.getMessage(), "", "", code, "dvt", "");
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)303);
                                                if (DvtAlert.disconnectCashout != GameCommon.getValueInt("COUNT_FAIL")) return null;
                                                if (!VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertDisconnectCashoutTime, (int)1)) return null;
                                                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong dichvuthe dang bi mat ket noi!", true);
                                                DvtAlert.alertDisconnectCashoutTime = new Date();
                                                return null;
                                            }
                                            CashoutByCardMessage message2 = null;
                                            if (softpinObj != null) {
                                                code = this.getErrorCodeCard(softpinObj.getStatus());
                                                message2 = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, softpinObj.getStatus(), softpinObj.getMessage(), "", sign, code, softpinObj.getPartner(), softpinObj.getPartnerTransId());
                                                DvtAlert.disconnectCashout = 0;
                                                DvtAlert.timeoutCashout = 0;
                                            } else {
                                                message2 = new CashoutByCardMessage(nickname, id, provider.getName(), amount, quantity, -1, "L\u1ed7i k\u1ebft n\u1ed1i d\u1ecbch v\u1ee5 th\u1ebb: " + errorMessage, "", sign, code, "dvt", "");
                                            }
                                            if (code == 0 || code == 30) {
                                                String description = "";
                                                if (code == 0) {
                                                    StringBuilder builder = new StringBuilder("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: ").append(id).append(", Lo\u1ea1i th\u1ebb: ").append(provider.getName()).append(", M\u1ec7nh gi\u00e1: ").append(amount).append(", S\u1ed1 l\u01b0\u1ee3ng: ").append(quantity).append(", Danh s\u00e1ch th\u1ebb:,");
                                                    ArrayList<SoftpinJsonResponse> resSP = new ArrayList<SoftpinJsonResponse>();
                                                    StringBuilder des = new StringBuilder("");
                                                    for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                                                        String pin = AES.decrypt(sp.getPin(), GameCommon.getValueStr("DVT_SECRET_KEY"));
                                                        pin = pin.substring(0, 4) + "-" + pin.substring(4, pin.length() - 4) + "-" + pin.substring(pin.length() - 4);
                                                        String expire = sp.getExpire();
                                                        if (expire.length() > 10) {
                                                            expire = expire.substring(expire.length() - 10);
                                                        }
                                                        builder.append("Serial: ").append(sp.getSerial()).append(". M\u00e3 th\u1ebb: ").append(pin).append(". HSD: ").append(expire).append(",");
                                                        SoftpinJsonResponse spj = new SoftpinJsonResponse(id, sp.getSerial(), pin, expire);
                                                        resSP.add(spj);
                                                        des.append("serial: ").append(sp.getSerial()).append(", pin: ").append(pin).append(", expire: ").append(expire).append("|");
                                                    }
                                                    builder.delete(builder.length() - 1, builder.length());
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    res.setSoftpin(mapper.writeValueAsString(resSP));
                                                    des.delete(des.length() - 1, des.length());
                                                    message2.setSoftpin(des.toString());
                                                    description = builder.toString();
                                                } else {
                                                    description = "K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd, M\u00e3 GD: " + id + ", Lo\u1ea1i th\u1ebb: " + provider.getName() + ", M\u1ec7nh gi\u00e1: " + amount + ", S\u1ed1 l\u01b0\u1ee3ng: " + quantity;
                                                }
                                                res.setCurrentMoney(currentMoney);
                                                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByCard", this.getServiceName(provider), currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message2, (int)303);
                                                cashDao.updateSystemCashout(moneyCashoutReal);
                                                break block42;
                                            }
                                            try {
                                                userMap = client.getMap("users");
                                                 userMap.lock(nickname);
                                                user = (UserCacheModel)userMap.get(nickname);
                                                moneyUser = user.getVin();
                                                currentMoney = user.getVinTotal();
                                                user.setVin(moneyUser += money);
                                                user.setVinTotal(currentMoney += money);
                                                user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                userMap.put(nickname, user);
                                                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message2, (int)303);
                                                break block42;
                                            }
                                            catch (Exception e3) {
                                                e3.printStackTrace();
                                                logger.debug(e3);
                                                MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e3.getMessage());
                                                code = 1;
                                                break block42;
                                            }
                                            finally {
                                                 userMap.unlock(nickname);
                                            }
                                        }
                                        code = 21;
                                        break block42;
                                    }
                                    code = 20;
                                    break block42;
                                }
                                code = 3;
                                break block42;
                            }
                            code = 10;
                            break block42;
                        }
                        code = 9;
                        break block42;
                    }
                    code = 2;
                }
                catch (Exception e4) {
                    e4.printStackTrace();
                    logger.debug(e4);
                    MoneyLogger.log(nickname, "CashOutByCard", money, 0L, "vin", "Doi thuong qua the", "1001", e4.getMessage());
                    code = 1;
                }
            }
        }
        res.setCode(code);
        logger.debug(("Finish cashOutByCardDvt, Response : " + code));
        return res;
    }

    @Override
    public Map<String, Long> reCheckCashOutByCard() throws Exception {
        HashMap<String, Long> mapRes = new HashMap<String, Long>();
        if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1) {
            return mapRes;
        }
        CashoutDaoImpl dao = new CashoutDaoImpl();
        List<CashoutTransResponse> listPending = dao.getListCashoutByCardPending();
        for (CashoutTransResponse pendingCard : listPending) {
            SoftpinResponse softpinResponse = null;
            if ("dvt".equals(pendingCard.getPartner())) {
                softpinResponse = this.reCheckCashOutByCardDVT(pendingCard.getReferenceId());
            }
            if ("1pay".equals(pendingCard.getPartner())) {
                softpinResponse = this.reCheckCashOutByCard1Pay(pendingCard.getPartnerTransId(), pendingCard.getProvider(), String.valueOf(pendingCard.getAmount()), pendingCard.getQuantity());
            }
            if ("vtc".equals(pendingCard.getPartner())) {
                softpinResponse = this.reCheckCashOutByCardVTC(pendingCard.getPartnerTransId(), String.valueOf(pendingCard.getAmount()), pendingCard.getQuantity(), pendingCard.getPartnerTransId());
            }
            if ("epay".equals(pendingCard.getPartner())) {
                softpinResponse = this.reCheckCashOutByCardEpay(pendingCard.getPartnerTransId(), pendingCard.getProvider(), pendingCard.getAmount(), pendingCard.getQuantity());
            }
            if (softpinResponse == null) continue;
            SoftpinObj softpinObj = softpinResponse.getSoftpinObj();
            if (pendingCard.getIsScanned() == 0) {
                if (softpinResponse.getCode() == 0) {
                    for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                        dao.insertCardIntoDB(pendingCard.getProvider(), pendingCard.getAmount(), sp.getSerial(), sp.getPin(), sp.getExpire());
                    }
                    dao.updateCashOutByCard(pendingCard.getReferenceId(), softpinResponse.getSoftpin(), softpinObj.getStatus(), softpinObj.getMessage(), softpinResponse.getCode(), 0);
                } else if (softpinResponse.getCode() == 1) {
                    dao.updateCashOutByCard(pendingCard.getReferenceId(), pendingCard.getSoftpin(), softpinObj.getStatus(), softpinObj.getMessage(), softpinResponse.getCode(), 0);
                }
            }
            if (pendingCard.getIsScanned() != 1) continue;
            if (softpinResponse.getCode() == 0) {
                dao.updateCashOutByCard(pendingCard.getReferenceId(), softpinResponse.getSoftpin(), softpinObj.getStatus(), softpinObj.getMessage(), softpinResponse.getCode(), 0);
            }
            if (softpinResponse.getCode() == 1) {
                dao.updateCashOutByCard(pendingCard.getReferenceId(), softpinResponse.getSoftpin(), softpinObj.getStatus(), softpinObj.getMessage(), softpinResponse.getCode(), 0);
                continue;
            }
            dao.updateCashOutByCard(pendingCard.getReferenceId(), softpinResponse.getSoftpin(), pendingCard.getStatus(), pendingCard.getMessage(), softpinResponse.getCode(), 0);
        }
        return mapRes;
    }

    private SoftpinResponse reCheckCashOutByCard1Pay(String partnerTransId, String provider, String price, int quantity) throws Exception {
        SoftpinResponse res = null;
        if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1) {
            return res;
        }
        try {
            String requestId = VinPlayUtils.genMessageId();
            SoftpinObj softpinObj = _1PayClient.reCheckBuyCard(requestId, partnerTransId, provider, price, quantity);
            int code = this.mapErrorCode1Pay(softpinObj.getStatus());
            ArrayList<Softpin> softpinList = new ArrayList<Softpin>();
            if (code == 0) {
                for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                    softpinList.add(new Softpin(sp.getSerial(), sp.getPin(), sp.getExpire()));
                }
            }
            res = new SoftpinResponse((softpinList).toString(), code, 0L, softpinObj);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return res;
    }

    private SoftpinResponse reCheckCashOutByCardVTC(String provider, String price, int quantity, String partnerTransId) throws Exception {
        SoftpinResponse res = null;
        if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1) {
            return res;
        }
        try {
            SoftpinObj softpinObj = VTCClient.getCards(provider, price, quantity, partnerTransId);
            int code = this.mapErrorCodeVTC(softpinObj.getStatus());
            ArrayList<Softpin> softpinList = new ArrayList<Softpin>();
            if (code == 0) {
                for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                    softpinList.add(new Softpin(sp.getSerial(), sp.getPin(), sp.getExpire()));
                }
            }
            res = new SoftpinResponse((softpinList).toString(), code, 0L, softpinObj);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return res;
    }

    private SoftpinResponse reCheckCashOutByCardDVT(String id) throws Exception {
        SoftpinResponse res = null;
        if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1) {
            return res;
        }
        try {
            String sign = RSA.sign(id, GameCommon.getValueStr("DVT_PRIVATE_KEY"));
            SoftpinObj softpinObj = VinplayClient.reCheckCashOutByCard(id, sign);
            int code = this.getErrorCodeCard(softpinObj.getStatus());
            ArrayList<Softpin> softpinList = new ArrayList<Softpin>();
            if (code == 0) {
                for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                    softpinList.add(new Softpin(sp.getSerial(), sp.getPin(), sp.getExpire()));
                }
            }
            res = new SoftpinResponse((softpinList).toString(), code, 0L, softpinObj);
        }
        catch (Exception e) {
            logger.debug(e);
            e.printStackTrace();
        }
        return res;
    }

    private SoftpinResponse reCheckCashOutByCardEpay(String partnerTransId, String provider, int amount, int quantity) throws Exception {
        SoftpinResponse res = null;
        if (GameCommon.getValueInt("IS_CASHOUT_CARD") == 1) {
            return res;
        }
        try {
            SoftpinObj softpinObj = EpayClient.reDownloadSoftpin(partnerTransId, provider, amount, quantity);
            int code = this.mapErrorCodeEpay(softpinObj.getStatus());
            ArrayList<Softpin> softpinList = new ArrayList<Softpin>();
            if (code == 0) {
                for (SoftpinJson sp : softpinObj.getSoftpinList()) {
                    softpinList.add(new Softpin(sp.getSerial(), sp.getPin(), sp.getExpire()));
                }
            }
            res = new SoftpinResponse((softpinList).toString(), code, 0L, softpinObj);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return res;
    }

    @Override
    public CashoutResponse cashOutByTopUp(String nickname, String target, PhoneCardType phoneCardType, byte type, boolean check) throws Exception {
        String s;
        logger.debug(("Start cashOutByTopUp ham cha:  nickname: " + nickname + ", target: " + target + ", phoneCardType: " + phoneCardType.getValue() + ", type: " + type + ", check: " + check));
        CashoutResponse response = null;
        String[] priorityPartner = GameCommon.getValueStr("TOPUP_PARTNER").split(",");
        block5 : switch (s = priorityPartner[0]) {
            case "vtc": {
                String s2;
                response = this.cashOutByTopUpVTC(nickname, target, phoneCardType, type, check);
                if (response != null) break;
                switch (s2 = priorityPartner[1]) {
                    case "epay": {
                        response = this.cashOutByTopUpEpay(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpDvt(nickname, target, phoneCardType, type, check);
                        break block5;
                    }
                    case "dvt": {
                        response = this.cashOutByTopUpDvt(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpEpay(nickname, target, phoneCardType, type, check);
                    }
                }
                break;
            }
            case "epay": {
                String s3;
                response = this.cashOutByTopUpEpay(nickname, target, phoneCardType, type, check);
                if (response != null) break;
                switch (s3 = priorityPartner[1]) {
                    case "vtc": {
                        response = this.cashOutByTopUpVTC(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpDvt(nickname, target, phoneCardType, type, check);
                        break block5;
                    }
                    case "dvt": {
                        response = this.cashOutByTopUpDvt(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpVTC(nickname, target, phoneCardType, type, check);
                    }
                }
                break;
            }
            case "dvt": {
                String s4;
                response = this.cashOutByTopUpDvt(nickname, target, phoneCardType, type, check);
                if (response != null) break;
                switch (s4 = priorityPartner[1]) {
                    case "vtc": {
                        response = this.cashOutByTopUpVTC(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpEpay(nickname, target, phoneCardType, type, check);
                        break block5;
                    }
                    case "epay": {
                        response = this.cashOutByTopUpEpay(nickname, target, phoneCardType, type, check);
                        if (response != null) break;
                        response = this.cashOutByTopUpVTC(nickname, target, phoneCardType, type, check);
                    }
                }
                break;
            }
        }
        if (response == null) {
            response = new CashoutResponse(1, 0L);
            CashoutByTopUpMessage message = new CashoutByTopUpMessage(nickname, "", target, phoneCardType.getValue(), -1, "M\u1ea5t k\u1ebft n\u1ed1i \u0111\u1ebfn t\u1ea5t c\u1ea3 c\u00e1c partner", "", 1, "", "", "", -1);
            MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
            if ((VTCAlert.disconnectTopup == GameCommon.getValueInt("COUNT_FAIL") || EpayAlert.disconnectTopup == GameCommon.getValueInt("COUNT_FAIL") || DvtAlert.disconnectTopup == GameCommon.getValueInt("COUNT_FAIL")) && (VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertDisconnectTopupTime, (int)1) || VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertDisconnectTopupTime, (int)1) || VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertDisconnectTopupTime, (int)1))) {
                this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao tat ca cac partner topup dang bi mat ket noi!", true);
                VTCAlert.alertDisconnectTopupTime = new Date();
                EpayAlert.alertDisconnectTopupTime = new Date();
                DvtAlert.alertDisconnectTopupTime = new Date();
            }
        }
        logger.debug(("Start cashOutByTopUp ham cha:  nickname: " + nickname + ", target: " + target + ", phoneCardType: " + phoneCardType.getValue() + ", type: " + type + ", check: " + check));
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private CashoutResponse cashOutByTopUpEpay(String nickname, String target, PhoneCardType phoneCardType, byte type, boolean check) throws Exception {
        CashoutResponse res;
        int code;
        block37 : {
            logger.debug(("Start Request cashOutByTopUpEpay:  nickname: " + nickname + ", target: " + target + ", phoneCardType: " + phoneCardType.getValue() + ", type: " + type + ", check: " + check));
            String errorMessage = "";
            code = 1;
            res = new CashoutResponse(code, 0L);
            if (GameCommon.getValueInt("IS_CASHOUT_TOPUP") == 1 || target == null || target.isEmpty() || phoneCardType == null || type != 1 && type != 2) {
                logger.debug("cashOutByTopUp: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)amount * GameCommon.getValueDouble("RATIO_CASHOUT_TOPUP"));
            long moneyCashoutReal = amount;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByTopUp: money limit cashout");
                return res;
            }
            if (UserValidaton.validateMobileVN((String)target)) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1030", "can not connect hazelcast");
                    return res;
                }
                IMap<String, UserModel> userMap = client.getMap("users");
                CashoutDaoImpl cashDao = new CashoutDaoImpl();
                if (userMap.containsKey(nickname)) {
                    try {
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();
                        if (!user.isBanCashOut()) {
                            if (user.getMobile() != null && user.isHasMobileSecurity()) {
                                if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                    if (moneyUser - money >= 0L) {
                                        int moneyCashout = 0;
                                        if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                            moneyCashout = user.getCashout();
                                        }
                                        if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                            long systemCashout = cashDao.getSystemCashout();
                                            if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                                TopupObj topupObj;
                                                String id;
                                                String sign;
                                                block39 : {
                                                    if (check) {
                                                        code = 0;
                                                        break block37;
                                                    }
                                                    try {
                                                        userMap = client.getMap("users");
                                                         userMap.lock(nickname);
                                                        user = (UserCacheModel)userMap.get(nickname);
                                                        moneyUser = user.getVin();
                                                        currentMoney = user.getVinTotal();
                                                        if (moneyUser - money < 0L) {
                                                            code = 3;
                                                            res.setCode(code);
                                                            CashoutResponse cashoutResponse = res;
                                                            return cashoutResponse;
                                                        }
                                                        user.setVin(moneyUser -= money);
                                                        user.setVinTotal(currentMoney -= money);
                                                        user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                        user.setCashoutTime(new Date());
                                                        userMap.put(nickname, user);
                                                    }
                                                    catch (Exception e) {
                                                        e.printStackTrace();
                                                        logger.debug(e);
                                                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e.getMessage());
                                                        code = 1;
                                                        errorMessage = e.getMessage();
                                                        CashoutResponse cashoutResponse = null;
                                                        return cashoutResponse;
                                                    }
                                                    finally {
                                                         userMap.unlock(nickname);
                                                    }
                                                    id = VinPlayUtils.genTransactionId((int)user.getId());
                                                    sign = "";
                                                    topupObj = null;
                                                    try {
                                                        topupObj = EpayClient.topupTelco(id, target, type, amount);
                                                    }
                                                    catch (SocketTimeoutException timeout) {
                                                        timeout.printStackTrace();
                                                        logger.debug(timeout);
                                                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi ePay time out: " + timeout.getMessage());
                                                        DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                        if (++EpayAlert.timeoutTopup == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertTimeoutTopupTime, (int)1)) {
                                                            this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den ePay bi timeout qua 5 lan lien tiep!", false);
                                                            EpayAlert.alertTimeoutTopupTime = new Date();
                                                        }
                                                        code = 30;
                                                        errorMessage = timeout.getMessage();
                                                    }
                                                    catch (Exception e2) {
                                                        e2.printStackTrace();
                                                        logger.debug(e2);
                                                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi ePay: " + e2.getMessage());
                                                        DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                        errorMessage = e2.getMessage();
                                                        if (++EpayAlert.disconnectTopup != GameCommon.getValueInt("COUNT_FAIL") || !VinPlayUtils.isAlertTimeout((Date)EpayAlert.alertDisconnectTopupTime, (int)1)) break block39;
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong ePay dang bi mat ket noi!", true);
                                                        EpayAlert.alertDisconnectTopupTime = new Date();
                                                    }
                                                }
                                                CashoutByTopUpMessage message = null;
                                                if (topupObj != null) {
                                                    sign = topupObj.getSign();
                                                    code = this.mapErrorCodeEpay(topupObj.getStatus());
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, topupObj.getStatus(), topupObj.getMessage(), sign, code, "epay", topupObj.getPartnerTransId(), topupObj.getProvider(), (int)type);
                                                    EpayAlert.disconnectTopup = 0;
                                                    EpayAlert.timeoutTopup = 0;
                                                } else {
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, -1, "L\u1ed7i k\u1ebft n\u1ed1i ePay: " + errorMessage, sign, code, "epay", "", "", (int)type);
                                                }
                                                if (code == 0 || code == 30) {
                                                    StringBuilder builder = new StringBuilder("");
                                                    if (code == 0) {
                                                        builder.append("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng");
                                                    } else {
                                                        builder.append("K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd");
                                                    }
                                                    builder.append(", M\u00e3 GD: ").append(id).append(", S\u1ed1 \u0111i\u1ec7n tho\u1ea1i: ").append(target).append(", S\u1ed1 ti\u1ec1n: ").append(amount);
                                                    String description = builder.toString();
                                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByTopUp", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByTopUp", "N\u1ea1p ti\u1ec1n \u0111i\u1ec7n tho\u1ea1i", currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    cashDao.updateSystemCashout(moneyCashoutReal);
                                                    res.setCurrentMoney(currentMoney);
                                                    break block37;
                                                }
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    break block37;
                                                }
                                                catch (Exception e3) {
                                                    e3.printStackTrace();
                                                    logger.debug(e3);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e3.getMessage());
                                                    code = 1;
                                                    break block37;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                            }
                                            code = 21;
                                            break block37;
                                        }
                                        code = 20;
                                        break block37;
                                    }
                                    code = 3;
                                    break block37;
                                }
                                code = 10;
                                break block37;
                            }
                            code = 9;
                            break block37;
                        }
                        code = 2;
                    }
                    catch (Exception e4) {
                        e4.printStackTrace();
                        logger.debug(e4);
                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e4.getMessage());
                        code = 1;
                    }
                }
            } else {
                code = 23;
            }
        }
        res.setCode(code);
        logger.debug(("Finish Response cashOutByTopUpEpay: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private CashoutResponse cashOutByTopUpVTC(String nickname, String target, PhoneCardType phoneCardType, byte type, boolean check) throws Exception {
        CashoutResponse res;
        int code;
        block39 : {
            logger.debug(("Start Request cashOutByTopUpVTC:  nickname: " + nickname + ", target: " + target + ", phoneCardType: " + phoneCardType.getValue() + ", type: " + type + ", check: " + check));
            String errorMessage = "";
            code = 1;
            res = new CashoutResponse(code, 0L);
            if (GameCommon.getValueInt("IS_CASHOUT_TOPUP") == 1 || target == null || target.isEmpty() || phoneCardType == null || type != 1 && type != 2) {
                logger.debug("cashOutByTopUp: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)amount * GameCommon.getValueDouble("RATIO_CASHOUT_TOPUP"));
            long moneyCashoutReal = amount;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByTopUp: money limit cashout");
                return res;
            }
            if (UserValidaton.validateMobileVN((String)target)) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1030", "can not connect hazelcast");
                    return res;
                }
                IMap<String, UserModel> userMap = client.getMap("users");
                CashoutDaoImpl cashDao = new CashoutDaoImpl();
                if (userMap.containsKey(nickname)) {
                    try {
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();
                        if (!user.isBanCashOut()) {
                            if (user.getMobile() != null && user.isHasMobileSecurity()) {
                                if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                    if (moneyUser - money >= 0L) {
                                        int moneyCashout = 0;
                                        if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                            moneyCashout = user.getCashout();
                                        }
                                        if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                            long systemCashout = cashDao.getSystemCashout();
                                            if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                                if (check) {
                                                    code = 0;
                                                    break block39;
                                                }
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    if (moneyUser - money < 0L) {
                                                        code = 3;
                                                        res.setCode(code);
                                                        CashoutResponse cashoutResponse = res;
                                                        return cashoutResponse;
                                                    }
                                                    user.setVin(moneyUser -= money);
                                                    user.setVinTotal(currentMoney -= money);
                                                    user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                    user.setCashoutTime(new Date());
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception e) {
                                                    e.printStackTrace();
                                                    logger.debug(e);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e.getMessage());
                                                    code = 1;
                                                    errorMessage = e.getMessage();
                                                    CashoutResponse cashoutResponse = null;
                                                    return cashoutResponse;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                String id = VinPlayUtils.genTransactionId((int)user.getId());
                                                String sign = "";
                                                TopupObj topupObj = null;
                                                try {
                                                    topupObj = VTCClient.topupTelco(id, target, type, amount);
                                                }
                                                catch (SocketTimeoutException timeout) {
                                                    timeout.printStackTrace();
                                                    logger.debug(timeout);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi VTC time out: " + timeout.getMessage());
                                                    DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                    if (++VTCAlert.timeoutTopup == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertTimeoutTopupTime, (int)1)) {
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den VTC bi timeout qua 5 lan lien tiep!", false);
                                                        VTCAlert.alertTimeoutTopupTime = new Date();
                                                    }
                                                    code = 30;
                                                    errorMessage = timeout.getMessage();
                                                }
                                                catch (Exception e2) {
                                                    e2.printStackTrace();
                                                    logger.debug(e2);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi VTC: " + e2.getMessage());
                                                    DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                    if (++VTCAlert.disconnectTopup == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)VTCAlert.alertDisconnectTopupTime, (int)1)) {
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong VTC dang bi mat ket noi!", true);
                                                        VTCAlert.alertDisconnectTopupTime = new Date();
                                                    }
                                                    errorMessage = e2.getMessage();
                                                }
                                                CashoutByTopUpMessage message = null;
                                                if (topupObj != null) {
                                                    sign = topupObj.getSign();
                                                    code = this.mapErrorCodeVTC(topupObj.getStatus());
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, topupObj.getStatus(), topupObj.getMessage(), sign, code, "vtc", topupObj.getPartnerTransId(), topupObj.getProvider(), (int)type);
                                                    VTCAlert.disconnectTopup = 0;
                                                    VTCAlert.timeoutTopup = 0;
                                                } else {
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, -1, "L\u1ed7i k\u1ebft n\u1ed1i VTC: " + errorMessage, sign, code, "vtc", "", "", (int)type);
                                                }
                                                if (code == 0 || code == 30) {
                                                    StringBuilder builder = new StringBuilder("");
                                                    if (code == 0) {
                                                        builder.append("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng");
                                                    } else {
                                                        builder.append("K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd");
                                                    }
                                                    builder.append(", M\u00e3 GD: ").append(id).append(", S\u1ed1 \u0111i\u1ec7n tho\u1ea1i: ").append(target).append(", S\u1ed1 ti\u1ec1n: ").append(amount);
                                                    String description = builder.toString();
                                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByTopUp", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByTopUp", "N\u1ea1p ti\u1ec1n \u0111i\u1ec7n tho\u1ea1i", currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    cashDao.updateSystemCashout(moneyCashoutReal);
                                                    res.setCurrentMoney(currentMoney);
                                                    break block39;
                                                }
                                                if (code == 999) {
                                                    this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "[CANH BAO] So du tai khoan Vinplay tai VTC dang het tien!", false);
                                                }
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    break block39;
                                                }
                                                catch (Exception e3) {
                                                    e3.printStackTrace();
                                                    logger.debug(e3);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e3.getMessage());
                                                    code = 1;
                                                    break block39;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                            }
                                            code = 21;
                                            break block39;
                                        }
                                        code = 20;
                                        break block39;
                                    }
                                    code = 3;
                                    break block39;
                                }
                                code = 10;
                                break block39;
                            }
                            code = 9;
                            break block39;
                        }
                        code = 2;
                    }
                    catch (Exception e4) {
                        e4.printStackTrace();
                        logger.debug(e4);
                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e4.getMessage());
                        code = 1;
                    }
                }
            } else {
                code = 23;
            }
        }
        res.setCode(code);
        logger.debug(("Finish Response cashOutByTopUpVTC: " + code));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private CashoutResponse cashOutByTopUpDvt(String nickname, String target, PhoneCardType phoneCardType, byte type, boolean check) throws Exception {
        CashoutResponse res;
        int code;
        block38 : {
            logger.debug(("Start Request cashOutByTopUpDvt:  nickname: " + nickname + ", target: " + target + ", phoneCardType: " + phoneCardType.getValue() + ", type: " + type + ", check: " + check));
            String errorMessage = "";
            code = 1;
            res = new CashoutResponse(code, 0L);
            if (GameCommon.getValueInt("IS_CASHOUT_TOPUP") == 1 || target == null || target.isEmpty() || phoneCardType == null || type != 1 && type != 2) {
                logger.debug("cashOutByTopUp: param fail");
                return res;
            }
            int amount = phoneCardType.getValue();
            long money = Math.round((double)amount * GameCommon.getValueDouble("RATIO_CASHOUT_TOPUP"));
            long moneyCashoutReal = amount;
            if (moneyCashoutReal > GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                logger.debug("cashOutByTopUp: money limit cashout");
                return res;
            }
            if (UserValidaton.validateMobileVN((String)target)) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                if (client == null) {
                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1030", "can not connect hazelcast");
                    return res;
                }
                IMap<String, UserModel> userMap = client.getMap("users");
                CashoutDaoImpl cashDao = new CashoutDaoImpl();
                if (userMap.containsKey(nickname)) {
                    try {
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();
                        if (!user.isBanCashOut()) {
                            if (user.getMobile() != null && user.isHasMobileSecurity()) {
                                if (user.getSecurityTime() != null && VinPlayUtils.cashoutBlockTimeout((Date)user.getSecurityTime(), (int)GameCommon.getValueInt("CASHOUT_TIME_BLOCK"))) {
                                    if (moneyUser - money >= 0L) {
                                        int moneyCashout = 0;
                                        if (user.getCashoutTime() != null && VinPlayUtils.compareDate((Date)user.getCashoutTime(), (Date)new Date()) == 0) {
                                            moneyCashout = user.getCashout();
                                        }
                                        if ((long)moneyCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_USER")) {
                                            long systemCashout = cashDao.getSystemCashout();
                                            if (systemCashout + moneyCashoutReal <= GameCommon.getValueLong("CASHOUT_LIMIT_SYSTEM")) {
                                                if (check) {
                                                    code = 0;
                                                    break block38;
                                                }
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    if (moneyUser - money < 0L) {
                                                        code = 3;
                                                        res.setCode(code);
                                                        CashoutResponse cashoutResponse = res;
                                                        return cashoutResponse;
                                                    }
                                                    user.setVin(moneyUser -= money);
                                                    user.setVinTotal(currentMoney -= money);
                                                    user.setCashout(moneyCashout += (int)moneyCashoutReal);
                                                    user.setCashoutTime(new Date());
                                                    userMap.put(nickname, user);
                                                }
                                                catch (Exception e) {
                                                    e.printStackTrace();
                                                    logger.debug(e);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e.getMessage());
                                                    code = 1;
                                                    errorMessage = e.getMessage();
                                                    CashoutResponse cashoutResponse = null;
                                                    return cashoutResponse;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                                String id = VinPlayUtils.genTransactionId((int)user.getId());
                                                TopupObj topupObj = null;
                                                String sign = RSA.sign(id + target + type + amount, GameCommon.getValueStr("DVT_PRIVATE_KEY"));
                                                try {
                                                    topupObj = VinplayClient.cashOutByTopUp(id, target, type, amount, sign);
                                                }
                                                catch (SocketTimeoutException timeout) {
                                                    timeout.printStackTrace();
                                                    logger.debug(timeout);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi DVT time out: " + timeout.getMessage());
                                                    DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                    if (++DvtAlert.timeoutTopup == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertTimeoutTopupTime, (int)1)) {
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao ket noi den dichvuthe bi timeout qua 5 lan lien tiep!", false);
                                                        DvtAlert.alertTimeoutTopupTime = new Date();
                                                    }
                                                    code = 30;
                                                    errorMessage = timeout.getMessage();
                                                }
                                                catch (Exception e2) {
                                                    e2.printStackTrace();
                                                    logger.debug(e2);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dien thoai", "1034", "Loi ket noi Dvt: " + e2.getMessage());
                                                    DvtUtils.errorDvt(client, "CashOutByTopUp");
                                                    if (++DvtAlert.disconnectTopup == GameCommon.getValueInt("COUNT_FAIL") && VinPlayUtils.isAlertTimeout((Date)DvtAlert.alertDisconnectTopupTime, (int)1)) {
                                                        this.alert(GameCommon.getValueStr("DISCONNECT_GROUP_NUMBER"), "SOS! Canh bao he thong dichvuthe dang bi mat ket noi!", true);
                                                        DvtAlert.alertDisconnectTopupTime = new Date();
                                                    }
                                                    errorMessage = e2.getMessage();
                                                }
                                                CashoutByTopUpMessage message = null;
                                                if (topupObj != null) {
                                                    code = this.getErrorCodeTopUp(topupObj.getStatus());
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, topupObj.getStatus(), topupObj.getMessage(), sign, code, topupObj.getPartner(), topupObj.getPartnerTransId(), topupObj.getProvider(), (int)type);
                                                    DvtAlert.disconnectTopup = 0;
                                                    DvtAlert.timeoutTopup = 0;
                                                } else {
                                                    message = new CashoutByTopUpMessage(nickname, id, target, amount, -1, "L\u1ed7i k\u1ebft n\u1ed1i Dvt: " + errorMessage, sign, code, "dvt", "", "", (int)type);
                                                }
                                                if (code == 0 || code == 30) {
                                                    StringBuilder builder = new StringBuilder("");
                                                    if (code == 0) {
                                                        builder.append("K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng");
                                                    } else {
                                                        builder.append("K\u1ebft qu\u1ea3: \u0110ang x\u1eed l\u00fd");
                                                    }
                                                    builder.append(", M\u00e3 GD: ").append(id).append(", S\u1ed1 \u0111i\u1ec7n tho\u1ea1i: ").append(target).append(", S\u1ed1 ti\u1ec1n: ").append(amount);
                                                    String description = builder.toString();
                                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByTopUp", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByTopUp", "N\u1ea1p ti\u1ec1n \u0111i\u1ec7n tho\u1ea1i", currentMoney, -money, "vin", description, 0L, false, user.isBot());
                                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    cashDao.updateSystemCashout(moneyCashoutReal);
                                                    res.setCurrentMoney(currentMoney);
                                                    break block38;
                                                }
                                                try {
                                                    userMap = client.getMap("users");
                                                     userMap.lock(nickname);
                                                    user = (UserCacheModel)userMap.get(nickname);
                                                    moneyUser = user.getVin();
                                                    currentMoney = user.getVinTotal();
                                                    user.setVin(moneyUser += money);
                                                    user.setVinTotal(currentMoney += money);
                                                    user.setCashout(moneyCashout -= (int)moneyCashoutReal);
                                                    userMap.put(nickname, user);
                                                    MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)message, (int)304);
                                                    break block38;
                                                }
                                                catch (Exception e3) {
                                                    e3.printStackTrace();
                                                    logger.debug(e3);
                                                    MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e3.getMessage());
                                                    code = 1;
                                                    break block38;
                                                }
                                                finally {
                                                     userMap.unlock(nickname);
                                                }
                                            }
                                            code = 21;
                                            break block38;
                                        }
                                        code = 20;
                                        break block38;
                                    }
                                    code = 3;
                                    break block38;
                                }
                                code = 10;
                                break block38;
                            }
                            code = 9;
                            break block38;
                        }
                        code = 2;
                    }
                    catch (Exception e4) {
                        e4.printStackTrace();
                        logger.debug(e4);
                        MoneyLogger.log(nickname, "CashOutByTopUp", money, 0L, "vin", "Doi thuong qua nap dt", "1001", e4.getMessage());
                        code = 1;
                    }
                }
            } else {
                code = 23;
            }
        }
        res.setCode(code);
        logger.debug(("Finish Response cashOutByTopUpVTC: " + code));
        return res;
    }

    @Override
    public void cashOutByBank(String nickname, int amount) {
        String desc;
        BankcoObj bankcoObj;
        String sign;
        String account;
        HazelcastInstance client;
        CashoutDaoImpl dao;
        String error;
        String bankname;
        String id;
        StringBuilder content;
        String name;
        int code;
        block28 : {
            logger.debug(("Request cashOutByBank:  nickname: " + nickname + ", amount: " + amount));
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
            String time = format.format(new Date());
            content = new StringBuilder("Dai ly ").append(nickname).append(" da chuyen ").append(NumberUtils.formatNumber((String)String.valueOf(amount))).append(" vin cho ban luc ").append(time).append(". Dang thuc hien chuyen khoan ngan hang");
            error = "";
            dao = new CashoutDaoImpl();
            bankname = "";
            account = "";
            name = "";
            code = 0;
            desc = "";
            BankType bank = null;
            BankAccountInfo info = null;
            try {
                amount = (int)Math.round((double)amount * GameCommon.getValueDouble("RATIO_CASHOUT_BANK"));
                content.append(".So tien: ").append(NumberUtils.formatNumber((String)String.valueOf(amount)));
                info = dao.getBankAccountInfo(nickname);
            }
            catch (Exception e) {
                code = 1;
                desc = "Kh\u00f4ng t\u00ecm th\u1ea5y th\u00f4ng tin t\u00e0i kho\u1ea3n: " + e.getMessage();
                error = "He thong gian doan";
            }
            if (info == null) {
                code = 1;
                desc = "Kh\u00f4ng t\u00ecm th\u1ea5y th\u00f4ng tin t\u00e0i kho\u1ea3n";
                error = "Khong tim thay thong tin tai khoan ngan hang cua dai ly.";
            } else {
                bankname = info.getBank();
                account = info.getAccount();
                name = info.getName();
            }
            if (code == 0) {
                try {
                    bank = BankType.getBankByName((String)bankname);
                    if (GameCommon.getValueInt("IS_CASHOUT_BANK") == 1) {
                        code = 2;
                        desc = "\u0110ang kh\u00f3a chuy\u1ec3n kho\u1ea3n ng\u00e2n h\u00e0ng";
                        error = "Dang khoa chuyen khoan ngan hang.";
                    } else if (bank == null) {
                        desc = "Kh\u00f4ng t\u00ecm th\u1ea5y ng\u00e2n h\u00e0ng: " + bankname;
                        error = "Khong tim thay thong tin tai khoan ngan hang cua dai ly.";
                        code = 3;
                    } else if (account == null || account.isEmpty()) {
                        desc = "Kh\u00f4ng t\u00ecm th\u1ea5y s\u1ed1 t\u00e0i kho\u1ea3n: " + account;
                        error = "Khong tim thay thong tin tai khoan ngan hang cua dai ly.";
                        code = 4;
                    } else if (name == null || name.isEmpty()) {
                        desc = "Kh\u00f4ng t\u00ecm th\u1ea5y t\u00ean t\u00e0i kho\u1ea3n: " + name;
                        error = "Khong tim thay thong tin tai khoan ngan hang cua dai ly.";
                        code = 5;
                    } else if (amount > GameCommon.getValueInt("CASHOUT_BANK_MAX")) {
                        desc = "S\u1ed1 ti\u1ec1n qu\u00e1 l\u1edbn: " + amount;
                        error = "So tien qua lon.";
                        code = 6;
                    }
                }
                catch (Exception e) {
                    desc = "L\u1ed7i l\u1ea5y config t\u1eeb cache: ";
                    error = "He thong gian doan.";
                    code = 7;
                }
            }
            bankcoObj = null;
            id = String.valueOf(System.currentTimeMillis());
            sign = null;
            if (code == 0) {
                try {
                    sign = RSA.sign(id + bank.getValue() + account + name + amount, GameCommon.getValueStr("DVT_PRIVATE_KEY"));
                }
                catch (Exception e2) {
                    code = 8;
                    desc = "L\u1ed7i t\u1ea1o ch\u1eef k\u00fd s\u1ed1: " + e2.getMessage();
                    error = "He thong gian doan.";
                }
            }
            client = HazelcastClientFactory.getInstance();
            if (code == 0) {
                try {
                    bankcoObj = VinplayClient.cashOutByBank(id, bank.getValue(), account, name, amount, sign);
                }
                catch (Exception e3) {
                    code = 9;
                    desc = "L\u1ed7i k\u1ebft n\u1ed1i d\u1ecbch v\u1ee5 th\u1ebb: " + e3.getMessage();
                    error = "He thong gian doan.";
                    if (client == null) break block28;
                    DvtUtils.errorDvt(client, "CashOutByBank");
                }
            }
        }
        int statusDvt = -1;
        String messageDvt = "";
        if (bankcoObj != null) {
            statusDvt = bankcoObj.getStatus();
            messageDvt = bankcoObj.getMessage();
            code = this.getErrorCodeBank(statusDvt);
            error = this.getDescErrorCodeBank(statusDvt);
        }
        try {
            UserCacheModel superUser;
            IMap<String, UserModel> userMap = client.getMap("users");
            String superAgent = GameCommon.getValueStr("SUPER_AGENT");
            if (userMap.containsKey(superAgent) && (superUser = (UserCacheModel)userMap.get(superAgent)).getMobile() != null && superUser.isHasMobileSecurity()) {
                AlertServiceImpl alertService = new AlertServiceImpl();
                alertService.sendSMS2One(superUser.getMobile(), content.append(". Ket qua: ").append(error).toString(), false);
            }
        }
        catch (Exception e4) {
            logger.debug(e4);
            MoneyLogger.log(nickname, "CashOutByBank", amount, 0L, "vin", "Chuyen tien ngan hang", "1034", "error semd alert: " + e4.getMessage());
        }
        try {
            CashoutByBankMessage message = new CashoutByBankMessage(nickname, id, bankname, account, name, amount, statusDvt, messageDvt, sign, code, desc);
            dao.logCashoutByBank(message);
        }
        catch (Exception e4) {
            logger.debug(e4);
            MoneyLogger.log(nickname, "CashOutByBank", amount, 0L, "vin", "Chuyen tien ngan hang", "1033", "error mongodb: " + e4.getMessage());
        }
        logger.debug(("Response cashOutByBank:  code: " + code + ", desc: " + desc));
    }

    private int mapErrorCodeEpay(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 23: 
            case 99: {
                return 30;
            }
            case 30: {
                return 999;
            }
            case 10: 
            case 11: 
            case 12: 
            case 13: 
            case 14: 
            case 15: 
            case 17: 
            case 21: 
            case 22: 
            case 31: 
            case 32: 
            case 33: 
            case 35: 
            case 52: 
            case 101: 
            case 102: 
            case 103: 
            case 104: 
            case 105: 
            case 106: 
            case 107: 
            case 108: 
            case 109: 
            case 110: 
            case 111: 
            case 112: 
            case 113: {
                return 1;
            }
        }
        return 30;
    }

    private int mapErrorCodeVTC(int status) {
        switch (status) {
            case 1: {
                return 0;
            }
            case -55: {
                return 999;
            }
            case -600: 
            case -509: 
            case -503: 
            case -502: 
            case -501: 
            case -500: 
            case -350: 
            case -348: 
            case -320: 
            case -318: 
            case -317: 
            case -316: 
            case -315: 
            case -311: 
            case -310: 
            case -309: 
            case -308: 
            case -307: 
            case -306: 
            case -305: 
            case -304: 
            case -302: {
                return 1;
            }
            case -505: 
            case -504: 
            case -290: 
            case -99: 
            case -1: 
            case 0: {
                return 30;
            }
        }
        return 30;
    }

    private int mapErrorCode1Pay(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 800: {
                return 999;
            }
            case 1: 
            case 2: 
            case 901: 
            case 903: 
            case 1001: 
            case 2000: 
            case 2001: 
            case 2002: 
            case 2003: 
            case 2005: 
            case 9999: {
                return 1;
            }
        }
        return 30;
    }

    private String mapMessage1Pay(int status) {
        switch (status) {
            case 0: {
                return "Th\u00e0nh c\u00f4ng";
            }
            case 1: 
            case 9999: {
                return "Giao d\u1ecbch th\u1ea5t b\u1ea1i";
            }
            case 2: {
                return "Th\u00f4ng tin x\u00e1c th\u1ef1c kh\u00f4ng ch\u00ednh x\u00e1c";
            }
            case 800: {
                return "S\u1ed1 d\u01b0 kh\u00f4ng \u0111\u1ee7";
            }
            case 901: {
                return "Th\u00f4ng tin \u0111\u0103ng nh\u1eadp kh\u00f4ng \u0111\u00fang";
            }
            case 903: {
                return "Th\u00f4ng tin \u0111\u0103ng nh\u1eadp kh\u00f4ng \u0111\u00fang";
            }
            case 1001: {
                return "Nh\u00e0 m\u1ea1ng ng\u1eebng ho\u1ea1t \u0111\u1ed9ng ho\u1eb7c \u0111ang b\u1ea3o tr\u00ec";
            }
            case 2000: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o kh\u00f4ng \u0111\u00fang";
            }
            case 2001: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o Topup kh\u00f4ng \u0111\u00fang";
            }
            case 2002: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o Mua th\u1ebb kh\u00f4ng \u0111\u00fang";
            }
            case 2003: {
                return "S\u1ed1 \u0111i\u1ec7n tho\u1ea1i cung c\u1ea5p kh\u00f4ng \u0111\u00fang";
            }
            case 2005: {
                return "Giao d\u1ecbch b\u1ecb l\u1eb7p";
            }
        }
        return "\u0110ang x\u1eed l\u00fd";
    }

    private int getErrorCodeCard(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 1;
            }
            case 20: {
                return 22;
            }
            case 99: {
                return 30;
            }
        }
        return 30;
    }

    private int getErrorCodeTopUp(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 1;
            }
            case 11: {
                return 23;
            }
            case 99: {
                return 30;
            }
        }
        return 30;
    }

    private int getErrorCodeBank(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 30;
            }
            case 30: {
                return 31;
            }
            case 31: {
                return 32;
            }
            case 32: {
                return 33;
            }
            case 98: {
                return 34;
            }
            case 99: {
                return 34;
            }
        }
        return 34;
    }

    private String getDescErrorCodeBank(int status) {
        switch (status) {
            case 0: {
                return "Thanh cong.";
            }
            case 10: {
                return "That bai.";
            }
            case 30: {
                return "Sai ngan hang.";
            }
            case 31: {
                return "Sai ten tai khoan.";
            }
            case 32: {
                return "Sai so tai khoan.";
            }
            case 98: {
                return "Dang xu ly.";
            }
            case 99: {
                return "Dang xu ly.";
            }
        }
        return "Dang xu ly.";
    }

    private String getServiceName(ProviderType provider) {
        if (provider.getType() == 0) {
            return "Mua th\u1ebb \u0111i\u1ec7n tho\u1ea1i";
        }
        return "Mua th\u1ebb game";
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
}

