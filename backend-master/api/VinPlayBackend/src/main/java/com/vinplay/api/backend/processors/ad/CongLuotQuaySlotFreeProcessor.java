/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.ad;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class CongLuotQuaySlotFreeProcessor
implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        String[] arrSoLuot;
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String listNickname = ((HttpServletRequest)param.get()).getParameter("nn");
        String otp = ((HttpServletRequest)param.get()).getParameter("otp");
        String type = ((HttpServletRequest)param.get()).getParameter("type");
        String gameName = ((HttpServletRequest)param.get()).getParameter("gn");
        String listSoLuot = ((HttpServletRequest)param.get()).getParameter("va");
        String ad = ((HttpServletRequest)param.get()).getParameter("ad");
        String[] arrNickName = listNickname.split(",");
        try {
            int code = BackendUtils.checkOTPSuperAdmin(otp, type, ad);
            if (code == 0) {
                if (arrNickName.length == (arrSoLuot = listSoLuot.split(",")).length) {
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap slotMap = client.getMap("cacheSlotFree");
                    for (int i = 0; i < arrNickName.length; ++i) {
                        SlotFreeDaily slotModel;
                        String nickname = arrNickName[i];
                        String key = String.valueOf(nickname) + "-" + gameName + "-" + 100;
                        int slotFree = Integer.parseInt(arrSoLuot[i]);
                        if (slotMap.containsKey((Object) key)) {
                            slotModel = (SlotFreeDaily) slotMap.get((Object) key);
                            slotModel.setRotateFree(slotFree);
                            slotMap.put((Object) key, (Object) slotModel);
                            continue;
                        }
                        slotModel = new SlotFreeDaily(slotFree, 2000L);
                        slotMap.put((Object) key, (Object) slotModel);
                    }
                    response.setSuccess(true);
                    response.setErrorCode("0");
                }
            }
            if (code == 3) {
                response.setErrorCode("1008");
            } else if (code == 4) {
                response.setErrorCode("1021");
            }
        }  catch (Exception e2) {
            logger.debug((Object)e2);
        }
        return response.toJson();
    }
}

