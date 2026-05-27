/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.vinplay.api.processors;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetAppConfigProcesscor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    public static Map<String, String> configs = new HashMap<String, String>();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String clientVersion = request.getParameter("v");
        String platform = request.getParameter("pf");
        String deviceId = request.getParameter("did");
        String versionNative = request.getParameter("vnt");
        String IOS = "ios";
        String ANDROID = "ad";
        String WINDOW_PHONE = "wp";
        String WEB = "web";
        logger.debug(("Request Get app config: \n - clientVersion: " + clientVersion + "\n - platform: " + platform + "\n - deviceId: " + deviceId));
        if (clientVersion == null || platform == null || deviceId == null || !platform.equals("ios") && !platform.equals("ad") && !platform.equals("wp") && !platform.equals("web")) {
            return "MISSING PARAMETTER";
        }
        logger.debug( "Run get app config");
        if (configs.containsKey(platform)) {
            String config = configs.get(platform);
            try {
                JSONObject cfObj = new JSONObject(config);
                String version = cfObj.getString("version");
                int update = cfObj.getInt("update");
                if (clientVersion.equals(version) && update != 0) {
                    cfObj.put("update", 0);
                }
                if (!platform.equals("web") && versionNative != null && versionNative.equals("1")) {
                    cfObj.put("recharge", cfObj.get("recharge_store"));
                    cfObj.put("cashout", cfObj.get("cashout_store"));
                    cfObj.put("is_chuyen_vin", cfObj.get("is_chuyen_vin_store"));
                }
                cfObj.put("banner", new JSONArray(GameCommon.getValueStr((String)"BANNER")));
                cfObj.put("banner_tour", new JSONArray(GameCommon.getValueStr((String)"BANNER_TOUR")));
                cfObj.put("email", GameCommon.getValueStr((String)"EMAIL"));
                cfObj.put("hotline", GameCommon.getValueStr((String)"HOT_LINE"));
                cfObj.put("bank_account", GameCommon.getValueStr((String)"BANK_ACCOUNT"));
                cfObj.put("bank_name", GameCommon.getValueStr((String)"BANK_NAME"));
                cfObj.put("bank_number", GameCommon.getValueStr((String)"BANK_NUMBER"));
                cfObj.put("bank_code", GameCommon.getValueStr((String)"BANK_CODE"));
                cfObj.put("momo_name", GameCommon.getValueStr((String)"MOMO_NAME"));
                cfObj.put("momo_phone", GameCommon.getValueStr((String)"MOMO_PHONE"));
                config = cfObj.toString();
            }
            catch (Exception e) {
                logger.debug(e);
            }
            return config;
        }
        return "PLATFORM NOT SUPPORT";
    }
}

