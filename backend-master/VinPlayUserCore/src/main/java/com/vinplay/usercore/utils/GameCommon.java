/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.usercore.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.payment.entities.Bank;
import com.vinplay.brandname.dao.impl.BrandnameDaoImpl;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.dao.impl.LuckyDaoImpl;
import com.vinplay.usercore.entities.IAPModel;
import com.vinplay.usercore.utils.LuckyUtils;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameCommon {
    private static final Logger logger = Logger.getLogger("GameCommon");
    public static List<Bank> LIST_BANK_NAME = new ArrayList<Bank>();

    public static TelegramConfigList telegramConfig;

    public static TelegramConfigList getTelegramConfig() { return telegramConfig; }

    public static final String SUCCESS = "1";
    public static final String ERROR = "-1";
    public static final String SERVICE_ID = "8041";
    public static final String VIN = "VIN";
    public static final String VIN_OTP = "OZZ OTP";
    public static final String VIN_APP = "OZZ APP";
    public static final String VIN_ODP = "OZZ ODP";
    public static final String OTP_CMD = "OZZ";
    public static final String CMD_OTP = "OZZ OTP";
    public static final String CMD_APP = "OZZ APP";
    public static final String CMD_ODP = "OZZ ODP";
    public static final String MESSAGE_TYPE = "1";
    public static final String TOTAL_MESSAGE = "1";
    public static final String MESSAGE_INDEX = "1";
    public static final String IS_MORE = "0";
    public static final String CONTENT_TYPE = "0";
    public static final int OTP_SUCCESS = 0;
    public static final int OTP_INVALID = 3;
    public static final int OTP_TIMEOUT = 4;
    public static final int OPEN = 0;
    public static final int CLOSE = 1;
    public static final int ON = 1;
    public static final int OFF = 0;
    public static String OTP_URL_SEND_MT = "";
    public static String OTP_IP_FILTER = "";
    public static String OTP_URL_RECEIVE_MO = "";
    public static int OTP_DELAY_SEND_MT = 5000;
    public static String MESSAGE_OTP_SUCCESS = "";
    public static String MESSAGE_ODP_SUCCESS = "";
    public static String MESSAGE_APP_SUCCESS = "";
    public static String MESSAGE_ERROR_MOBILE = "";
    public static String MESSAGE_ERROR_SYNTAX = "";
    public static String SMSPLUS_SUCCESS = "";
    public static String SMSPLUS_ERROR_NICKNAME = "";
    public static String SMSPLUS_ERROR_SYNTAX = "";
    public static String SMSPLUS_ERROR_SYSTEM = "";
    public static String SMSPLUS_ERROR_LOGIN = "";
    public static String SMSPLUS_ERROR_AMOUNT = "";
    public static String BRANDNAME_SENDER = "";
    public static String BRANDNAME_USER = "";
    public static String BRANDNAME_PASS = "";
    public static String BRANDNAME_URL = "";
    public static int BRANDNAME_CLIENT_ID = 4;
    public static String BRANDNAME_CLIENT_USER = "";
    public static String BRANDNAME_CLIENT_PASS = "";
    public static String BRANDNAME_URL_REPORT_FROM_ST = "";
    public static Map<Integer, IAPModel> iapPackages = new HashMap<Integer, IAPModel>();

    private static String webconf = "";
    private static String iosconf = "";
    private static String adconf = "";

    public static String getConfigVersionStatus(String platform) {
        switch (platform) {
            case "web": {
                return webconf;
            }
            case "ios": {
                return iosconf;
            }
            case "ad": {
                return adconf;
            }
        }
        return "";
    }

    public static void init() throws SQLException, JSONException, ParseException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        BrandnameDaoImpl bndao = new BrandnameDaoImpl();
        long brandNameID = bndao.getLastRequestId();
        map.put("BRAND_NAME_ID", String.valueOf(brandNameID));
        // Ensure VIPPOINT event keys exist (defaults to disabled)
        if (!map.containsKey("VIPPOINT_EVENT_STATUS")) {
            map.put("VIPPOINT_EVENT_STATUS", "0");
        }
        if (!map.containsKey("VIPPOINT_EVENT_X2_STATUS")) {
            map.put("VIPPOINT_EVENT_X2_STATUS", "0");
        }
        LuckyDaoImpl lkdao = new LuckyDaoImpl();
        long lkVipID = lkdao.getLuckyVipLastReferenceId();
        map.put("LUCKY_VIP_ID", String.valueOf(lkVipID));
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        Map<String, String> mapConfig = dao.getGameConfig();

        // "web" section - critical, allowed to throw
        JSONObject cfObj = new JSONObject(mapConfig.get("web"));
        map.put("STATUS_GAME", String.valueOf(cfObj.getInt("status_game")));
        map.put("ADMIN", dao.getGameCommon("admin"));

        // Variables needed across sections
        String hotline = "";
        String email = "";
        String facebook = "";
        String web = "";

        // "bank_sms" section
        try {
            String bank_sms = dao.getGameCommon("bank_sms");
            map.put("BANK_SMS", bank_sms);
            JSONObject bankSmsObj = new JSONObject(bank_sms);
            String bankName = bankSmsObj.getString("bank_name");
            String bankAccount = bankSmsObj.getString("bank_account");
            String bankNumber = bankSmsObj.getString("bank_number");
            String bankIme = bankSmsObj.getString("ime");
            String bankCode = bankSmsObj.getString("code");
            String bankAddress = bankSmsObj.getString("address");
            map.put("BANK_NAME", bankName);
            map.put("BANK_ACCOUNT", bankAccount);
            map.put("BANK_NUMBER", bankNumber);
            map.put("BANK_CODE", bankCode);
            map.put("BANK_IME", bankIme);
            map.put("BANK_ADDRESS", bankAddress);
        } catch (Exception e) {
            logger.warning("Failed to load config section 'bank_sms': " + e.getMessage());
        }

        // "game_common" section
        try {
            String commons = dao.getGameCommon("game_common");
            map.put("COMMONS", commons);
            JSONObject commonObj = new JSONObject(commons);
            hotline = commonObj.getString("hotline");
            email = commonObj.getString("email");
            facebook = commonObj.getString("facebook");
            web = commonObj.getString("web");
            map.put("HOT_LINE", hotline);
            map.put("EMAIL", email);
            map.put("FACEBOOK", facebook);
            map.put("WEB", web);
            map.put("SMS_OTP", commonObj.getString("sms_otp"));
            map.put("BANNER", commonObj.getJSONArray("banner").toString());
            map.put("BANNER_TOUR", commonObj.getJSONArray("banner_tour").toString());
            map.put("PASSWORD_DEFAULT", commonObj.getString("password_default"));
            map.put("IAP_KEY", commonObj.getString("iap_key"));
            map.put("UPDATE_BOT_VIN", String.valueOf(commonObj.getInt("bot_vin")));
            map.put("UPDATE_BOT_XU", String.valueOf(commonObj.getInt("bot_xu")));
            map.put("UPDATE_USER_VIN", String.valueOf(commonObj.getInt("user_vin")));
            map.put("UPDATE_USER_XU", String.valueOf(commonObj.getInt("user_xu")));
            map.put("VIN_PLUS", dao.getGameCommon("vin_plus"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'game_common': " + e.getMessage());
        }

        // "billing" section
        try {
            String billing = dao.getGameCommon("billing");
            map.put("BILLING", billing);
            JSONObject blObj = new JSONObject(billing);
            map.put("IS_NAP_MEGA_CARD", String.valueOf(blObj.get("is_nap_mega_card")));
            map.put("RATIO_NAP_MEGA_CARD", String.valueOf(blObj.get("ratio_nap_mega_card")));
            map.put("IS_RECHARGE_CARD", String.valueOf(blObj.getInt("is_nap_the")));
            map.put("IS_RECHARGE_VIN_CARD", String.valueOf(blObj.getInt("is_nap_vin_card")));
            map.put("IS_RECHARGE_BANK", String.valueOf(blObj.getInt("is_nap_vin_nh")));
            map.put("IS_RECHARGE_IAP", String.valueOf(blObj.getInt("is_nap_vin_iap")));
            map.put("IS_NAP_XU", String.valueOf(blObj.getInt("is_nap_xu")));
            map.put("IS_TRANSFER_MONEY", String.valueOf(blObj.getInt("is_chuyen_vin")));
            map.put("IS_CASHOUT_CARD", String.valueOf(blObj.getInt("is_mua_the")));
            map.put("IS_CASHOUT_TOPUP", String.valueOf(blObj.getInt("is_nap_dt")));
            map.put("IS_CASHOUT_BANK", String.valueOf(blObj.getInt("is_nap_tien_nh")));
            map.put("RATIO_NAP_XU", String.valueOf(blObj.getDouble("ratio_xu")));
            map.put("RATIO_RECHARGE_CARD", String.valueOf(blObj.getDouble("ratio_nap_the")));
            map.put("RATIO_RECHARGE_VIN_CARD", String.valueOf(blObj.getDouble("ratio_nap_vin_card")));
            map.put("RATIO_RECHARGE_BANK", String.valueOf(blObj.getDouble("ratio_nap_vin_nh")));
            map.put("RATIO_RECHARGE_SMS", String.valueOf(blObj.getDouble("ratio_nap_sms")));
            map.put("RATIO_CASHOUT_CARD", String.valueOf(blObj.getDouble("ratio_mua_the")));
            map.put("RATIO_CASHOUT_TOPUP", String.valueOf(blObj.getDouble("ratio_nap_dt")));
            map.put("RATIO_TRANSFER", String.valueOf(blObj.getDouble("ratio_chuyen")));
            map.put("RATIO_CASHOUT_BANK", String.valueOf(blObj.getDouble("ratio_nap_tien_nh")));
            map.put("TRANSFER_MONEY_MIN", String.valueOf(blObj.getInt("chuyen_vin_min")));
            map.put("CASHOUT_LIMIT_USER", String.valueOf(blObj.getLong("cashout_limit_user")));
            map.put("CASHOUT_LIMIT_SYSTEM", String.valueOf(blObj.getLong("cashout_limit_system")));
            map.put("NUM_RECHARGE_FAIL", String.valueOf(blObj.getInt("num_recharge_fail")));
            map.put("NUM_CASHOUT_CARD", String.valueOf(blObj.getInt("num_doi_the")));
            map.put("CASHOUT_TIME_BLOCK", String.valueOf(blObj.getInt("cashout_time_block")));
            map.put("SUPER_ADMIN", blObj.getString("super_admin"));
            map.put("SUPER_AGENT", blObj.getString("super_agent"));
            map.put("CASHOUT_BANK_MAX", String.valueOf(blObj.getInt("cashout_bank_max")));
            map.put("RATIO_REFUND_FEE_1", String.valueOf(blObj.getDouble("ratio_refund_fee_1")));
            map.put("RATIO_REFUND_FEE_2", String.valueOf(blObj.getDouble("ratio_refund_fee_2")));
            map.put("RATIO_REFUND_FEE_2_MORE", String.valueOf(blObj.getDouble("ratio_refund_fee_2_more")));
            map.put("REFUND_FEE_2_MORE", String.valueOf(blObj.getLong("refund_fee_2_more")));
            map.put("RATIO_TRANSFER_DL_1", String.valueOf(blObj.getDouble("ratio_transfer_dl_1")));
            map.put("DL1_TO_SUPER_MIN", String.valueOf(blObj.getLong("dl1_to_super_min")));
            map.put("DL1_TO_SUPER_MAX", String.valueOf(blObj.getLong("dl1_to_super_max")));
            map.put("DL1_TO_SUPER_MIN_X", String.valueOf(blObj.getLong("dl1_to_super_min_x")));
            map.put("IAP_MAX", String.valueOf(blObj.getInt("iap_max")));
            map.put("SYSTEM_IAP_MAX", String.valueOf(blObj.getInt("system_iap_max")));
            map.put("RATIO_TRANSFER_01", String.valueOf(blObj.getDouble("r_tf_01")));
            map.put("RATIO_TRANSFER_02", String.valueOf(blObj.getDouble("r_tf_02")));
            map.put("RATIO_TRANSFER_20", String.valueOf(blObj.getDouble("r_tf_20")));
            map.put("RATIO_TRANSFER_21", String.valueOf(blObj.getDouble("r_tf_21")));
            map.put("RATIO_TRANSFER_22", String.valueOf(blObj.getDouble("r_tf_22")));
            map.put("RATIO_TRANSFER_11", String.valueOf(blObj.getDouble("r_tf_11")));
            map.put("RATIO_TRANSFER_12", String.valueOf(blObj.getDouble("r_tf_12")));
            map.put("SMS_PLUS_OPEN", String.valueOf(blObj.getInt("is_sms_plus")));
            map.put("SMS_OPEN", String.valueOf(blObj.getInt("is_sms")));
            map.put("API_OTP_OPEN", String.valueOf(blObj.getInt("is_api_otp")));
            JSONArray jArrayVP = blObj.getJSONArray("iap_package");
            if (jArrayVP != null) {
                for (int i = 0; i < jArrayVP.length(); ++i) {
                    JSONObject jObj = jArrayVP.getJSONObject(i);
                    Iterator keys = jObj.keys();
                    while (keys.hasNext()) {
                        String key = (String)keys.next();
                        IAPModel model = new IAPModel(i + 1, key, jObj.getInt(key));
                        iapPackages.put(i + 1, model);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load config section 'billing': " + e.getMessage());
        }

        // "otp" section
        try {
            JSONObject otpObj = new JSONObject(dao.getGameCommon("otp"));
            map.put("OTP_DEFAULT", otpObj.getString("otp_default"));
            OTP_URL_SEND_MT = otpObj.getString("otp_url_send_mt");
            OTP_IP_FILTER = otpObj.getString("otp_ip_filter");
            OTP_URL_RECEIVE_MO = otpObj.getString("otp_url_receive_mo");
            OTP_DELAY_SEND_MT = otpObj.getInt("otp_delay_send_mt");
            MESSAGE_OTP_SUCCESS = otpObj.getString("message_otp_success");
            MESSAGE_ODP_SUCCESS = otpObj.getString("message_odp_success");
            MESSAGE_APP_SUCCESS = otpObj.getString("message_app_success");
            MESSAGE_ERROR_MOBILE = otpObj.getString("message_error_mobile");
            MESSAGE_ERROR_SYNTAX = otpObj.getString("message_error_syntax");
        } catch (Exception e) {
            logger.warning("Failed to load config section 'otp': " + e.getMessage());
        }

        // "brandname" section
        try {
            JSONObject bnObj = new JSONObject(dao.getGameCommon("brandname"));
            map.put("BRANDNAME_OPEN", String.valueOf(bnObj.getInt("is_open")));
            BRANDNAME_SENDER = bnObj.getString("brandname_sender");
            BRANDNAME_USER = bnObj.getString("brandname_user");
            BRANDNAME_PASS = bnObj.getString("brandname_pass");
            BRANDNAME_URL = bnObj.getString("brandname_url");
            BRANDNAME_CLIENT_ID = bnObj.getInt("brandname_client_id");
            BRANDNAME_CLIENT_USER = bnObj.getString("brandname_client_user");
            BRANDNAME_CLIENT_PASS = bnObj.getString("brandname_client_pass");
            BRANDNAME_URL_REPORT_FROM_ST = bnObj.getString("brandname_url_report_from_st");
        } catch (Exception e) {
            logger.warning("Failed to load config section 'brandname': " + e.getMessage());
        }

        // "dvt" section
        try {
            JSONObject dvtObj = new JSONObject(dao.getGameCommon("dvt"));
            map.put("DVT_URL", dvtObj.getString("dvt_url"));
            map.put("DVT_PRIVATE_KEY", dvtObj.getString("dvt_private_key"));
            map.put("DVT_SECRET_KEY", dvtObj.getString("dvt_secret_key"));
            map.put("DVT_DATE_RE_CHECK", String.valueOf(dvtObj.getInt("dvt_date_re_check")));
            map.put("DVT_SMS_OPEN", String.valueOf(dvtObj.getInt("sms_open")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'dvt': " + e.getMessage());
        }

        // "email_api" section
        try {
            JSONObject emailApiObj = new JSONObject(dao.getGameCommon("email_api"));
            map.put("EMAIL_API_URL",  emailApiObj.getString("Url"));
            map.put("EMAIL_API_API_KEY",  emailApiObj.getString("ApiKey"));
            map.put("EMAIL_API_SITE_NAME",  emailApiObj.getString("SiteName"));
            map.put("EMAIL_API_FROM_NAME",  emailApiObj.getString("FromName"));
            map.put("EMAIL_API_DOMAIN",  emailApiObj.getString("Domain"));
            map.put("EMAIL_API_MODULE",  emailApiObj.getString("Module"));
            map.put("EMAIL_API_FROM_EMAIL",  emailApiObj.getString("FromEmail"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'email_api': " + e.getMessage());
        }

        // "momo_info" section
        try {
            JSONObject momoInfo = new JSONObject(dao.getGameCommon("momo_info"));
            map.put("MOMO_NAME",  momoInfo.getString("name"));
            map.put("MOMO_PHONE",  momoInfo.getString("phone"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'momo_info': " + e.getMessage());
        }

        // "other" section
        try {
            JSONObject otherObj = new JSONObject(dao.getGameCommon("other"));
            map.put("URL_ACTIVE_EMAIL", otherObj.getString("url_active_email"));
            String sign = otherObj.getString("sign");
            String signEmail = " H\u00c3\u00a3y \u00c4\u2018\u00e1\u00bb\u00abng ng\u00e1\u00ba\u00a7n ng\u00e1\u00ba\u00a1i li\u00c3\u00aan h\u00e1\u00bb\u2021 ngay v\u00e1\u00bb\u203ai ch\u00c3\u00bang t\u00c3\u00b4i khi b\u00e1\u00ba\u00a1n g\u00e1\u00ba\u00b7p s\u00e1\u00bb\u00b1 c\u00e1\u00bb\u2018.<br> Website: " + web + ".<br> Hotline: " + hotline + ".<br> Email: " + email + ".<br> Facebook: " + facebook + ".<br><br> Tr\u00c3\u00a2n tr\u00e1\u00bb\ufffdng! <br> " + sign + ".<br>";
            map.put("SIGN_EMAIL", signEmail);
            map.put("LIST_GAME_BAI", otherObj.getString("list_game_bai"));
            map.put("LIST_PHONE_ALERT", otherObj.getString("list_phone_alert"));
            map.put("HU_GAME_BAI_MAX", String.valueOf(otherObj.getLong("hu_game_bai_max")));
            map.put("SMS_FEE", String.valueOf(otherObj.getInt("sms_fee")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'other': " + e.getMessage());
        }

        // "game_bai" section
        try {
            JSONObject gbObj = new JSONObject(dao.getGameCommon("game_bai"));
            map.put("HU_GAME_BAI", gbObj.toString());
        } catch (Exception e) {
            logger.warning("Failed to load config section 'game_bai': " + e.getMessage());
        }

        // "i2b" section
        try {
            JSONObject npObj = new JSONObject(dao.getGameCommon("i2b"));
            map.put("NAPAS_VERSION", npObj.getString("version"));
            map.put("NAPAS_URL", npObj.getString("napas_url"));
            map.put("NAPAS_MERCHANT", npObj.getString("merchant_id"));
            map.put("NAPAS_ACCESS_CODE", npObj.getString("access_code"));
            map.put("NAPAS_SECRET_KEY", npObj.getString("secret_key"));
            map.put("NAPAS_USER", npObj.getString("user"));
            map.put("NAPAS_PASS", npObj.getString("password"));
            map.put("NAPAS_URL_RESULT", npObj.getString("url_result"));
            map.put("NAPAS_URL_CANCEL", npObj.getString("url_cancel"));
            map.put("NAPAS_AMOUNT_MIN", String.valueOf(npObj.getInt("amount_min")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'i2b': " + e.getMessage());
        }

        // "nganluong" section
        try {
            JSONObject nlObj = new JSONObject(dao.getGameCommon("nganluong"));
            map.put("NL_OPEN", String.valueOf(nlObj.getInt("is_open")));
            map.put("NL_MERCHANT_ID", nlObj.getString("merchant_id"));
            map.put("NL_MERCHANT_PASSWORD", nlObj.getString("merchant_password"));
            map.put("NL_VERSION", nlObj.getString("version"));
            map.put("NL_RECEIVER_EMAIL", nlObj.getString("receiver_email"));
            map.put("NL_RETURN_URL", nlObj.getString("return_url"));
            map.put("NL_CANCEL_URL", nlObj.getString("cancel_url"));
            map.put("NL_TIME_LIMIT", String.valueOf(nlObj.getInt("time_limit")));
            map.put("NL_URL", nlObj.getString("nl_url"));
            map.put("NL_PAYMENT_METHOD", nlObj.getString("payment_method"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'nganluong': " + e.getMessage());
        }

        // "vippoint_event" section
        try {
            JSONObject vpeObj = new JSONObject(dao.getGameCommon("vippoint_event"));
            map.put("EVENT_TIME_START", vpeObj.getString("start"));
            map.put("EVENT_TIME_END", vpeObj.getString("end"));
            map.put("VIPPOINT_EVENT_URL", vpeObj.getString("url_help"));
            map.put("VIPPOINT_EVENT_RATE_SUB", String.valueOf(vpeObj.getInt("rate_sub")));
            map.put("VIPPOINT_EVENT_RATE_ADD", String.valueOf(vpeObj.getInt("rate_add")));
            map.put("VIPPOINT_EVENT_RATE_SUB_BOT", String.valueOf(vpeObj.getInt("rate_sub_bot")));
            map.put("VIPPOINT_EVENT_RATE_ADD_BOT", String.valueOf(vpeObj.getInt("rate_add_bot")));
            map.put("VIPPOINT_INDEX", String.valueOf(vpeObj.getInt("vippoint_index")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'vippoint_event': " + e.getMessage());
        }

        // "lucky" section
        try {
            String luckyVip = dao.getGameCommon("lucky_vip");
            String lucky = dao.getGameCommon("lucky");
            JSONObject luckyObj = new JSONObject(lucky);
            LuckyUtils.init(luckyVip, lucky, luckyObj.getInt("num_type"));
            map.put("LUCKY_RECHARGE_INDEX", String.valueOf(luckyObj.getLong("recharge_index")));
            map.put("LUCKY_SLOT_MAX_WIN", String.valueOf(luckyObj.getInt("slot_max_win")));
            map.put("LUCKY_SLOT_ROOM", String.valueOf(luckyObj.getInt("slot_room")));
            map.put("LUCKY_MAX_IN_DAY", String.valueOf(luckyObj.getInt("max_in_day")));
            map.put("LUCKY_MAX_BY_IP", String.valueOf(luckyObj.getInt("max_by_ip")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'lucky': " + e.getMessage());
        }

        // "sms_plus" section
        try {
            JSONObject smsPlusObj = new JSONObject(dao.getGameCommon("sms_plus"));
            map.put("SMS_PLUS_AMOUNT_MIN", String.valueOf(smsPlusObj.getInt("amount_min")));
            map.put("SMS_PLUS_URL", smsPlusObj.getString("url"));
            map.put("SMS_PLUS_ACCESS_KEY", smsPlusObj.getString("access_key"));
            map.put("SMS_PLUS_SECRET_KEY", smsPlusObj.getString("secret_key"));
            map.put("SMS_PLUS_COMMAND_CODE", smsPlusObj.getString("command_code"));
            map.put("SMS_PLUS_GAME_CODE", smsPlusObj.getString("game_code"));
            map.put("SMS_COMMAND", smsPlusObj.getString("command"));
            map.put("API_OTP_URL_REQUEST", smsPlusObj.getString("url_otp_request"));
            map.put("API_OTP_URL_CONFIRM", smsPlusObj.getString("url_otp_confirm"));
            map.put("API_OTP_FORMAT", smsPlusObj.getString("otp_format"));
            map.put("API_OTP_TIMEOUT", String.valueOf(smsPlusObj.getInt("otp_timeout")));
            map.put("API_OTP_FAIL_DELAY", String.valueOf(smsPlusObj.getInt("otp_fail_delay")));
            map.put("API_OTP_FAIL_NUM_LOCK", String.valueOf(smsPlusObj.getInt("otp_fail_num_lock")));
            SMSPLUS_SUCCESS = smsPlusObj.getString("message_success");
            SMSPLUS_ERROR_NICKNAME = smsPlusObj.getString("message_error_nickname");
            SMSPLUS_ERROR_SYNTAX = smsPlusObj.getString("message_error_syntax");
            SMSPLUS_ERROR_SYSTEM = smsPlusObj.getString("message_error_system");
            SMSPLUS_ERROR_LOGIN = smsPlusObj.getString("message_error_login");
            SMSPLUS_ERROR_AMOUNT = smsPlusObj.getString("message_error_amount");
        } catch (Exception e) {
            logger.warning("Failed to load config section 'sms_plus': " + e.getMessage());
        }

        // "vin_card" section
        try {
            JSONObject vcObj = new JSONObject(dao.getGameCommon("vin_card"));
            map.put("VIN_CARD_URL", vcObj.getString("vc_url"));
            map.put("VIN_CARD_PARTNER", vcObj.getString("vc_partner"));
            map.put("VIN_CARD_USER_LIMIT", String.valueOf(vcObj.getLong("vc_user_limit")));
            map.put("VIN_CARD_SYSTEM_LIMIT", String.valueOf(vcObj.getLong("vc_system_limit")));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'vin_card': " + e.getMessage());
        }

        // "maxpay" section
        try {
            JSONObject maxpayObj = new JSONObject(dao.getGameCommon("maxpay"));
            map.put("MAXPAY_URL", maxpayObj.getString("maxpay_url"));
            map.put("MAXPAY_MERCHANT_ID", maxpayObj.getString("merchant_id"));
            map.put("MAXPAY_SECRET_KEY", maxpayObj.getString("secret_key"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'maxpay': " + e.getMessage());
        }

        // "lucky79" section
        try {
            JSONObject lucky79Obj = new JSONObject(dao.getGameCommon("lucky79"));
            map.put("LUCKY79_URL", lucky79Obj.getString("lucky79_url"));
            map.put("LUCKY79_MERCHANT_ID", lucky79Obj.getString("merchant_id"));
            map.put("LUCKY79_SECRET_KEY", lucky79Obj.getString("secret_key"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'lucky79': " + e.getMessage());
        }

        // "thecao247" section
        try {
            JSONObject thecao247 = new JSONObject(dao.getGameCommon("thecao247"));
            map.put("THE_CAO_URL", thecao247.getString("thecao_url"));
            map.put("THE_CAO_MERCHANT_ID", thecao247.getString("merchant_id"));
            map.put("THE_CAO_SECRET_KEY", thecao247.getString("secret_key"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'thecao247': " + e.getMessage());
        }

        // "priority_partner" section (cashout)
        try {
            JSONObject priorityObj1 = new JSONObject(dao.getGameCommon("priority_partner"));
            map.put("RECHARGE_PARTNER", priorityObj1.getString("recharge"));
            map.put("TOPUP_PARTNER", priorityObj1.getString("topup"));
            JSONObject cashout = new JSONObject(priorityObj1.getString("cashout"));
            JSONObject vtt = new JSONObject(cashout.getString("vtt"));
            map.put("CASHOUT_VTT_PRIMARY", vtt.getString("primary"));
            map.put("CASHOUT_VTT_BACKUP", vtt.getString("backup"));
            JSONObject vms = new JSONObject(cashout.getString("vms"));
            map.put("CASHOUT_VMS_PRIMARY", vms.getString("primary"));
            map.put("CASHOUT_VMS_BACKUP", vms.getString("backup"));
            JSONObject vnp = new JSONObject(cashout.getString("vnp"));
            map.put("CASHOUT_VNP_PRIMARY", vnp.getString("primary"));
            map.put("CASHOUT_VNP_BACKUP", vnp.getString("backup"));
            JSONObject vnm = new JSONObject(cashout.getString("vnm"));
            map.put("CASHOUT_VNM_PRIMARY", vnm.getString("primary"));
            map.put("CASHOUT_VNM_BACKUP", vnm.getString("backup"));
            JSONObject gate = new JSONObject(cashout.getString("gate"));
            map.put("CASHOUT_GATE_PRIMARY", gate.getString("primary"));
            map.put("CASHOUT_GATE_BACKUP", gate.getString("backup"));
            JSONObject zing = new JSONObject(cashout.getString("zing"));
            map.put("CASHOUT_ZING_PRIMARY", zing.getString("primary"));
            map.put("CASHOUT_ZING_BACKUP", zing.getString("backup"));
            JSONObject vcoin = new JSONObject(cashout.getString("vcoin"));
            map.put("CASHOUT_VCOIN_PRIMARY", vcoin.getString("primary"));
            map.put("CASHOUT_VCOIN_BACKUP", vcoin.getString("backup"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'priority_partner': " + e.getMessage());
        }

        // "alert" section
        try {
            JSONObject alertObj = new JSONObject(dao.getGameCommon("alert"));
            map.put("ALERT_URL", alertObj.getString("alert_url"));
            map.put("COUNT_FAIL", alertObj.getString("count_fail"));
            map.put("DISCONNECT_GROUP_NUMBER", alertObj.getString("disconnect_group_number"));
            map.put("PENDING_GROUP_NUMBER", alertObj.getString("pending_group_number"));
            map.put("FREEZE_MONEY_GROUP_NUMBER", alertObj.getString("freeze_money_group_number"));
            map.put("MEGA_CARD_GROUP_NUMBER", alertObj.getString("mega_card_group_number"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'alert': " + e.getMessage());
        }

        // "1pay" section
        try {
            JSONObject _1PayObj = new JSONObject(dao.getGameCommon("1pay"));
            map.put("_1PAY_URL", _1PayObj.getString("1pay_url"));
            map.put("_1PAY_USER", _1PayObj.getString("1pay_user"));
            map.put("_1PAY_USER_API", _1PayObj.getString("1pay_user_api"));
            map.put("_1PAY_PASS", _1PayObj.getString("1pay_pass"));
            map.put("_1PAY_CODE_API", _1PayObj.getString("1pay_code_api"));
            map.put("_1PAY_PRIVATE_KEY", _1PayObj.getString("1pay_private_key"));
        } catch (Exception e) {
            logger.warning("Failed to load config section '1pay': " + e.getMessage());
        }

        // "agent" section
        try {
            JSONObject agentObj = new JSONObject(dao.getGameCommon("agent"));
            map.put("TIME_SEARCH", agentObj.getString("time_search"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'agent': " + e.getMessage());
        }

        // "vtc" section
        try {
            JSONObject vtcObj = new JSONObject(dao.getGameCommon("vtc"));
            map.put("VTC_SERVICE_URL", vtcObj.getString("vtc_url"));
            map.put("VTC_PARTNER_CODE", vtcObj.getString("vtc_code"));
            map.put("VTC_PRIVATE_KEY", vtcObj.getString("vtc_private_key"));
            map.put("VTC_PARTNER_SECRET_KEY", vtcObj.getString("vtc_secret_key"));
            map.put("VTCPAY_PUBLIC_KEY", vtcObj.getString("vtc_pay_public_key"));
            map.put("VTCPAY_PRIVATE_KEY", vtcObj.getString("vtc_pay_private_key"));
            map.put("VTCPAY_PRICE", vtcObj.getString("vtc_pay_price"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'vtc': " + e.getMessage());
        }

        // "epay" section
        try {
            JSONObject ePayObj = new JSONObject(dao.getGameCommon("epay"));
            map.put("CDV_WEBSERVICE_URL", ePayObj.getString("CDV_WEBSERVICE_URL"));
            map.put("CDV_PARTNER_NAME", ePayObj.getString("CDV_PARTNER_NAME"));
            map.put("CDV_PRIVATE_KEY", ePayObj.getString("CDV_PRIVATE_KEY"));
            map.put("CDV_PUBLIC_KEY", ePayObj.getString("CDV_PUBLIC_KEY"));
            map.put("CDV_KEY_SOFTPIN", ePayObj.getString("CDV_KEY_SOFTPIN"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'epay': " + e.getMessage());
        }

        // "time_recheck" section
        try {
            JSONObject timeRecheckObj = new JSONObject(dao.getGameCommon("time_recheck"));
            map.put("TIME_RECHECK_RECHARGE", timeRecheckObj.getString("recharge"));
            map.put("TIME_RECHECK_CASHOUT_BY_CARD", timeRecheckObj.getString("cash_out_by_card"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'time_recheck': " + e.getMessage());
        }

        // "mission_rule" section
        try {
            JSONObject missionRuleObj = new JSONObject(dao.getGameCommon("mission_rule"));
            map.put("MAX_LEVEL_MISSION", missionRuleObj.getString("max_level"));
            map.put("MATCH_MAX_VIN", missionRuleObj.getString("match_max_vin"));
            map.put("MATCH_MAX_XU", missionRuleObj.getString("match_max_xu"));
            map.put("MIN_TAI_XIU_VIN", missionRuleObj.getString("min_tai_xiu_vin"));
            map.put("MIN_TAI_XIU_XU", missionRuleObj.getString("min_tai_xiu_xu"));
            JSONObject bonusVin = new JSONObject(missionRuleObj.getString("bonus_vin"));
            map.put("BONUS_VIN_0", bonusVin.getString("0"));
            map.put("BONUS_VIN_1", bonusVin.getString("1"));
            map.put("BONUS_VIN_2", bonusVin.getString("2"));
            map.put("BONUS_VIN_3", bonusVin.getString("3"));
            map.put("BONUS_VIN_4", bonusVin.getString("4"));
            map.put("BONUS_VIN_5", bonusVin.getString("5"));
            map.put("BONUS_VIN_6", bonusVin.getString("6"));
            map.put("BONUS_VIN_7", bonusVin.getString("7"));
            map.put("BONUS_VIN_8", bonusVin.getString("8"));
            map.put("BONUS_VIN_9", bonusVin.getString("9"));
            map.put("BONUS_VIN_10", bonusVin.getString("10"));
            map.put("BONUS_VIN_11", bonusVin.getString("11"));
            map.put("BONUS_VIN_12", bonusVin.getString("12"));
            map.put("BONUS_VIN_13", bonusVin.getString("13"));
            map.put("BONUS_VIN_14", bonusVin.getString("14"));
            map.put("BONUS_VIN_15", bonusVin.getString("15"));
            map.put("BONUS_VIN_16", bonusVin.getString("16"));
            map.put("BONUS_VIN_17", bonusVin.getString("17"));
            map.put("BONUS_VIN_18", bonusVin.getString("18"));
            map.put("BONUS_VIN_19", bonusVin.getString("19"));
            map.put("BONUS_VIN_20", bonusVin.getString("20"));
            JSONObject bonusXu = new JSONObject(missionRuleObj.getString("bonus_xu"));
            map.put("BONUS_XU_0", bonusXu.getString("0"));
            map.put("BONUS_XU_1", bonusXu.getString("1"));
            map.put("BONUS_XU_2", bonusXu.getString("2"));
            map.put("BONUS_XU_3", bonusXu.getString("3"));
            map.put("BONUS_XU_4", bonusXu.getString("4"));
            map.put("BONUS_XU_5", bonusXu.getString("5"));
            map.put("BONUS_XU_6", bonusXu.getString("6"));
            map.put("BONUS_XU_7", bonusXu.getString("7"));
            map.put("BONUS_XU_8", bonusXu.getString("8"));
            map.put("BONUS_XU_9", bonusXu.getString("9"));
            map.put("BONUS_XU_10", bonusXu.getString("10"));
            map.put("BONUS_XU_11", bonusXu.getString("11"));
            map.put("BONUS_XU_12", bonusXu.getString("12"));
            map.put("BONUS_XU_13", bonusXu.getString("13"));
            map.put("BONUS_XU_14", bonusXu.getString("14"));
            map.put("BONUS_XU_15", bonusXu.getString("15"));
            map.put("BONUS_XU_16", bonusXu.getString("16"));
            map.put("BONUS_XU_17", bonusXu.getString("17"));
            map.put("BONUS_XU_18", bonusXu.getString("18"));
            map.put("BONUS_XU_19", bonusXu.getString("19"));
            map.put("BONUS_XU_20", bonusXu.getString("20"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'mission_rule': " + e.getMessage());
        }

        // "epay_megacard" section
        try {
            JSONObject megaObj = new JSONObject(dao.getGameCommon("epay_megacard"));
            map.put("MEGA_IS_VAT", megaObj.getString("is_vat"));
            map.put("MEGA_URL", megaObj.getString("mega_url"));
            map.put("MEGA_PARTNER_CODE", megaObj.getString("partner_code"));
            map.put("MEGA_PARTNER_ID", megaObj.getString("partner_id"));
            map.put("MEGA_MPIN", megaObj.getString("mpin"));
            map.put("MEGA_USER", megaObj.getString("user"));
            map.put("MEGA_PASS", megaObj.getString("pass"));
            map.put("MEGA_PUBLIC_KEY", megaObj.getString("public_key"));
            map.put("MEGA_PRIVATE_KEY", megaObj.getString("private_key"));
            map.put("MEGA_URL_VAT", megaObj.getString("mega_url_vat"));
            map.put("MEGA_PARTNER_CODE_VAT", megaObj.getString("partner_code_vat"));
            map.put("MEGA_PARTNER_ID_VAT", megaObj.getString("partner_id_vat"));
            map.put("MEGA_MPIN_VAT", megaObj.getString("mpin_vat"));
            map.put("MEGA_USER_VAT", megaObj.getString("user_vat"));
            map.put("MEGA_PASS_VAT", megaObj.getString("pass_vat"));
            map.put("MEGA_PUBLIC_KEY_VAT", megaObj.getString("public_key_vat"));
            map.put("MEGA_PRIVATE_KEY_VAT", megaObj.getString("private_key_vat"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'epay_megacard': " + e.getMessage());
        }

        // "partner_id" section
        try {
            JSONObject partnerIdObj = new JSONObject(dao.getGameCommon("partner_id"));
            map.put("VTCPAY_PARTNER_ID", partnerIdObj.getString("vtc_pay"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'partner_id': " + e.getMessage());
        }

        // "priority_partner" recharge section
        try {
            JSONObject priorityObj2 = new JSONObject(dao.getGameCommon("priority_partner"));
            JSONObject recharge = new JSONObject(priorityObj2.getString("recharge"));
            JSONObject vttRecharge = new JSONObject(recharge.getString("vtt"));
            map.put("RECHARGE_VTT_PRIMARY", vttRecharge.getString("primary"));
            map.put("RECHARGE_VTT_BACKUP", vttRecharge.getString("backup"));
            JSONObject vmsRecharge = new JSONObject(recharge.getString("vms"));
            map.put("RECHARGE_VMS_PRIMARY", vmsRecharge.getString("primary"));
            map.put("RECHARGE_VMS_BACKUP", vmsRecharge.getString("backup"));
            JSONObject vnpRecharge = new JSONObject(recharge.getString("vnp"));
            map.put("RECHARGE_VNP_PRIMARY", vnpRecharge.getString("primary"));
            map.put("RECHARGE_VNP_BACKUP", vnpRecharge.getString("backup"));
            JSONObject gateRecharge = new JSONObject(recharge.getString("gate"));
            map.put("RECHARGE_GATE_PRIMARY", gateRecharge.getString("primary"));
            map.put("RECHARGE_GATE_BACKUP", gateRecharge.getString("backup"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'priority_partner' (recharge): " + e.getMessage());
        }

        // "merchant" section
        try {
            JSONObject mcObj = new JSONObject(dao.getGameCommon("merchant"));
            JSONArray jArray = mcObj.getJSONArray("mc_info");
            if (jArray != null) {
                for (int j = 0; j < jArray.length(); ++j) {
                    JSONObject jObj2 = jArray.getJSONObject(j);
                    Iterator keys2 = jObj2.keys();
                    while (keys2.hasNext()) {
                        String key2 = (String)keys2.next();
                        JSONArray a = jObj2.getJSONArray(key2);
                        map.put((key2 + "CASHOUT_LIMIT_SYSTEM"), String.valueOf(a.getLong(6)));
                        map.put((key2 + "CASHOUT_LIMIT_USER"), String.valueOf(a.getLong(7)));
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load config section 'merchant': " + e.getMessage());
        }

        // "vtc_vcoin" section
        try {
            JSONObject VTCVcoinObj = new JSONObject(dao.getGameCommon("vtc_vcoin"));
            map.put("vcoin_url", VTCVcoinObj.getString("vcoin_url"));
            map.put("vcoin_partner_id", VTCVcoinObj.getString("vcoin_partner_id"));
            map.put("vcoin_partner_key", VTCVcoinObj.getString("vcoin_partner_key"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'vtc_vcoin': " + e.getMessage());
        }

        // "revenue_config" section
        try {
            JSONObject revenueObj = new JSONObject(dao.getGameCommon("revenue_config"));
            map.put("REVENUE_TAIXIU",  revenueObj.getString("taixiu"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'revenue_config': " + e.getMessage());
        }

        // "mini_prizes" section
        try {
            JSONObject miniPrizes = new JSONObject(dao.getGameCommon("mini_prizes"));
            JSONObject thanhDuPrizes = new JSONObject(miniPrizes.getString("thanh_du"));
            map.put("THANH_DU_WIN_PRIZES_DAILY", thanhDuPrizes.getString("winPrizesDaily"));
            map.put("THANH_DU_LOSS_PRIZES_DAILY", thanhDuPrizes.getString("lossPrizesDaily"));
            map.put("THANH_DU_WIN_PRIZES_MONTHLY", thanhDuPrizes.getString("winPrizesMonthly"));
            map.put("THANH_DU_LOSS_PRIZES_MONTHLY", thanhDuPrizes.getString("lossPrizesMonthly"));

            JSONObject tpsPrizes = new JSONObject(miniPrizes.getString("thung_pha_sanh"));
            map.put("TPS_WIN_PRIZES_DAILY", tpsPrizes.getString("winPrizesDaily"));
            map.put("TPS_WIN_PRIZES_MONTHLY", tpsPrizes.getString("winPrizesMonthly"));

            JSONObject tctPrizes = new JSONObject(miniPrizes.getString("toi_chon_tom"));
            map.put("TCT_WIN_PRIZES_DAILY", tctPrizes.getString("winPrizesDaily"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'mini_prizes': " + e.getMessage());
        }

        // "esms" section
        try {
            JSONObject esms = new JSONObject(dao.getGameCommon("esms"));
            map.put("ESMS_API_KEY", esms.getString("ApiKey"));
            map.put("ESMS_SECRET_KEY", esms.getString("SecretKey"));
            map.put("ESMS_IS_UNICODE", esms.getString("IsUnicode"));
            map.put("ESMS_SMS_TYPE", esms.getString("SmsType"));
            map.put("ESMS_BRAND_NAME", esms.getString("brandname"));
        } catch (Exception e) {
            logger.warning("Failed to load config section 'esms': " + e.getMessage());
        }

        // SUN-1026: promote silent per-section WARNING drops into visible
        // startup errors for critical features. Opt into CONFIG_STRICT=1
        // to make this fail startup.
        ConfigSanityCheck.auditInitState(map);
    }

    public static String getValueStr(String key) throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return (String)map.get(key);
        }
        throw new KeyNotFoundException(key);
    }

    public static int getValueInt(String key) throws KeyNotFoundException, NumberFormatException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return Integer.parseInt((String)map.get(key));
        }
        throw new KeyNotFoundException(key);
    }

    public static double getValueDouble(String key) throws KeyNotFoundException, NumberFormatException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return Double.parseDouble((String)map.get(key));
        }
        throw new KeyNotFoundException(key);
    }

    public static long getValueLong(String key) throws KeyNotFoundException, NumberFormatException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return Long.parseLong((String)map.get(key));
        }
        throw new KeyNotFoundException(key);
    }

    public static String getHuVangGameBai() throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey("HU_GAME_BAI")) {
            return (String)map.get("HU_GAME_BAI");
        }
        throw new KeyNotFoundException("HU_GAME_BAI");
    }

    public static List<String> getPhoneAlert() throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey("LIST_PHONE_ALERT")) {
            String[] split;
            ArrayList<String> res = new ArrayList<String>();
            String[] arr = split = ((String)map.get("LIST_PHONE_ALERT")).split(",");
            for (String m : split) {
                if (m.isEmpty()) continue;
                res.add(m);
            }
            return res;
        }
        throw new KeyNotFoundException("LIST_PHONE_ALERT");
    }

    public static IAPModel getIAPPackageById(int id) {
        return iapPackages.get(id);
    }

    public static IAPModel getIAPPackageByName(String name) {
        IAPModel model = null;
        for (Map.Entry<Integer, IAPModel> entry : iapPackages.entrySet()) {
            if (!entry.getValue().getName().equals(name)) continue;
            model = entry.getValue();
            break;
        }
        return model;
    }
}
