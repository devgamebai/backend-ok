/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.VippointDaoImpl
 *  com.vinplay.usercore.service.impl.UserExtraServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vippoint.entiies.EventVPTopStrongModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.vippoint;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.vippoint.TopVippoint;
import com.vinplay.api.processors.vippoint.response.EventVPTopStrongResponse;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.service.impl.UserExtraServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vippoint.entiies.EventVPTopStrongModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetEventVPTopStrongProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        EventVPTopStrongResponse res = new EventVPTopStrongResponse();
        if (nickname != null && !nickname.isEmpty()) {
            try {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = client.getMap("users");
                if (userMap.containsKey(nickname)) {
                    EventVPTopStrongModel model;
                    VippointDaoImpl dao = new VippointDaoImpl();
                    UserVPEventModel eventModel = dao.getUserVPByNickName(nickname);
                    int stt = 0;
                    if (eventModel.getPlaceMax() > 0) {
                        stt = dao.getEventStrongIndex(eventModel.getVpSub(), eventModel.getNumSub(), eventModel.getPlaceMax());
                    }
                    res.strong = model = new EventVPTopStrongModel(stt, nickname, eventModel.getVpSub(), eventModel.getNumSub(), eventModel.getPlaceMax(), "", "");
                    UserExtraServiceImpl ser = new UserExtraServiceImpl();
                    String platform = ser.getPlatformFromToken(request.getParameter("at"));
                    res.strongs = TopVippoint.getStrong(platform);
                    res.total = 20;
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return res.toJson();
    }
}

