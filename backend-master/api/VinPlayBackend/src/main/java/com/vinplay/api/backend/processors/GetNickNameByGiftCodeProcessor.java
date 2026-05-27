/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.response.GiftCodeByNickNameResponse
 *  com.vinplay.vbee.common.response.ResultNickNameByGiftCodeResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.response.GiftCodeByNickNameResponse;
import com.vinplay.vbee.common.response.ResultNickNameByGiftCodeResponse;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetNickNameByGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String giftCode = request.getParameter("gc");
        int totalRecord = Integer.parseInt(request.getParameter("tr"));
        int page = Integer.parseInt(request.getParameter("p"));
        ResultNickNameByGiftCodeResponse response = new ResultNickNameByGiftCodeResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("users");

        ArrayList<GiftCodeByNickNameResponse> results = new ArrayList<GiftCodeByNickNameResponse>();
        GiftCodeByNickNameResponse rs = new GiftCodeByNickNameResponse();
        GiftCodeServiceImpl sevice = new GiftCodeServiceImpl();
        long total = 0L;
        long totalPages = 0L;
        if (page < 0) {
            return response.toJson();
        }
        String notExist = "";
        String notUse = "";
        if (giftCode != null && !giftCode.equals("")) {
            String[] parts;
            for (String giftcode : parts = giftCode.split(",")) {
                if (giftcode.equals("")) {
                    response.setErrorCode("10002");
                    response.setSuccess(false);
                    return response.toJson();
                }
                rs = sevice.getUserInfoByGiftCode(giftcode, userMap, page, totalRecord);
                if (rs.giftcode == null) {
                    notExist = String.valueOf(notExist) + giftcode + ",";
                    continue;
                }
                if (rs.nickName == null) {
                    notUse = String.valueOf(notUse) + giftcode + ",";
                    continue;
                }
                results.add(rs);
            }
            total = results.size();
            totalPages = total % (long)totalRecord == 0L ? total / (long)totalRecord : total / (long)totalRecord + 1L;
            response.setGiftCodeNotUse(notUse);
            response.setGiftCodeNotExits(notExist);
            response.setTransactions(results);
            response.setTotal(totalPages);
            response.setSuccess(true);
            response.setErrorCode("0");
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

