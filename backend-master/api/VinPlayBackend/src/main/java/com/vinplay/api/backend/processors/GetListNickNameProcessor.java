/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.UserInfoServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.GetUserInfoResponse
 *  com.vinplay.vbee.common.response.ResultGetUserInfoResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.UserInfoServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.GetUserInfoResponse;
import com.vinplay.vbee.common.response.ResultGetUserInfoResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetListNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultGetUserInfoResponse response = new ResultGetUserInfoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        UserInfoServiceImpl service = new UserInfoServiceImpl();
        ArrayList<GetUserInfoResponse> results = new ArrayList<GetUserInfoResponse>();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("users");
        String lst = "";
        try {
            String[] parts;
            for (String name : parts = nickName.split(",")) {
                GetUserInfoResponse trans;
                if (userMap.containsKey((Object)name)) {
                    trans = new GetUserInfoResponse();
                    UserCacheModel users = (UserCacheModel)userMap.get((Object)name);
                    trans.nick_name = users.getNickname();
                    trans.user_name = users.getUsername();
                    trans.phone = users.getMobile();
                    trans.ip = users.getIp();
                    trans.time_log = users.getCreateTime().toString();
                    results.add(trans);
                    continue;
                }
                trans = new GetUserInfoResponse();
                trans = service.listGetNickName(name);
                if (trans.user_name != null) {
                    results.add(trans);
                    continue;
                }
                lst = String.valueOf(lst) + trans.nick_name + ",";
            }
            response.setLstNickName(lst);
            response.setTransactions(results);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}

