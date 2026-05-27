/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.vinplay.api.backend.processors.money;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.models.UpdateMoneyModel;
import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.api.backend.response.UpdateMoneyResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class UpdateMoneyListUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        UpdateMoneyResponse response = new UpdateMoneyResponse(false, "1001");
        try {
            String actionName = request.getParameter("ac");
            String data = request.getParameter("data");
            String moneyType = request.getParameter("mt");
            String reason = request.getParameter("rs");
            String otp = request.getParameter("otp");
            String type = request.getParameter("type");
            String ad = request.getParameter("ad");
            logger.debug((Object)("Request UpdateMoneyListUserProcessor: data: " + data + ", moneyType: " + moneyType + ", reason: " + reason + ", otp: " + otp + ", otpType: " + type));
            if (data != null && reason != null && !reason.isEmpty() && moneyType != null && (moneyType.equals("vin") || moneyType.equals("xu"))) {
                int code = BackendUtils.checkOTPSuperAdmin(otp, type, ad);
                if (code == 0) {
                    if (actionName == null) {
                        actionName = "Admin";
                    }
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap userMap = client.getMap("users");
                    UserServiceImpl service = new UserServiceImpl();
                    ArrayList<UpdateMoneyModel> listUpdate = new ArrayList<UpdateMoneyModel>();
                    ArrayList<UpdateMoneyModel> listResponse = new ArrayList<UpdateMoneyModel>();
                    JSONArray jArr = new JSONArray(data);
                    if (jArr != null) {
                        for (int i = 0; i < jArr.length(); ++i) {
                            JSONObject jObj = jArr.getJSONObject(i);
                            Iterator keys = jObj.keys();
                            while (keys.hasNext()) {
                                UpdateMoneyModel model;
                                String key = (String)keys.next();
                                long money = jObj.getLong(key);
                                UserModel userModel = service.getNicknameExactly(key, userMap);
                                if (userModel == null) {
                                    model = new UpdateMoneyModel(key, money, false, "2001");
                                    listResponse.add(model);
                                    continue;
                                }
                                if (userModel.isBot()) {
                                    model = new UpdateMoneyModel(key, money, true, "1001");
                                    listResponse.add(model);
                                    continue;
                                }
                                model = new UpdateMoneyModel(userModel.getNickname(), money, false, "1001");
                                listUpdate.add(model);
                            }
                        }
                    }
                    for (UpdateMoneyModel md : listUpdate) {
                        BaseResponseModel mnres = service.updateMoneyFromAdmin(md.nickname, md.money, moneyType, actionName, "Admin", reason);
                        md.errorCode = mnres.getErrorCode();
                        listResponse.add(md);
                    }
                    response.setListResponse(listResponse);
                    response.setSuccess(true);
                    response.setErrorCode("0");
                } else if (code == 3) {
                    response.setErrorCode("1008");
                } else if (code == 4) {
                    response.setErrorCode("1021");
                }
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        logger.debug((Object)("Response UpdateMoneyUser: " + response.toJson()));
        return response.toJson();
    }
}

