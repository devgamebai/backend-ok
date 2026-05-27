/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.VippointDaoImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.usercore.utils.VippointUtils
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  com.vinplay.vippoint.entiies.EventVPMapModel
 *  com.vinplay.vippoint.entiies.EventVPTopVipModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.vippoint;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.vippoint.TopVippoint;
import com.vinplay.api.processors.vippoint.response.EventVPMapResponse;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vippoint.entiies.EventVPMapModel;
import com.vinplay.vippoint.entiies.EventVPTopVipModel;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetEventVPMapProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        EventVPMapResponse res = new EventVPMapResponse();
        if (nickname != null && !nickname.isEmpty()) {
            try {
                try {
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap userMap = client.getMap("users");
                    if (userMap.containsKey(nickname)) {
                        EventVPTopVipModel myVip;
                        EventVPMapModel myMap;
                        VippointDaoImpl dao = new VippointDaoImpl();
                        UserVPEventModel eventModel = dao.getUserVPByNickName(nickname);
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        int minPlace = 0;
                        if (eventModel.getPlace() >= 1) {
                            minPlace = (Integer)VippointUtils.PLACES.get(eventModel.getPlace() - 1);
                        }
                        res.place = myMap = new EventVPMapModel(nickname, user.getAvatar(), eventModel.getVpEvent(), eventModel.getVpSub(), eventModel.getPlace(), minPlace);
                        int stt = dao.getEventVipsIndex(eventModel.getVpEvent());
                        res.vip = myVip = new EventVPTopVipModel(stt, nickname, eventModel.getVpEvent());
                        res.vips = TopVippoint.vips;
                        res.places = TopVippoint.maps;
                    }
                }
                catch (Exception e) {
                    logger.debug(e);
                }
                Date now = new Date();
                int status = 0;
                String des = "";
                String start = GameCommon.getValueStr((String)"EVENT_TIME_START");
                String end = GameCommon.getValueStr((String)"EVENT_TIME_END");
                long startTime = VinPlayUtils.getDateTime((String)start).getTime();
                long endTime = VinPlayUtils.getDateTime((String)end).getTime();
                if (now.getTime() < startTime) {
                    status = 0;
                    des = "S\u1ef1 ki\u1ec7n s\u1ebd b\u1eaft \u0111\u1ea7u v\u00e0o l\u00fac \n" + start.substring(11, 13) + "h" + start.substring(14, 16) + "'" + start.substring(17, 19) + "'' ng\u00e0y " + start.substring(8, 10) + "/" + start.substring(5, 7) + "/" + start.substring(0, 4);
                } else if (now.getTime() <= endTime) {
                    status = 1;
                } else {
                    status = 2;
                    des = "S\u1ef1 ki\u1ec7n \u0111\u00e3 k\u1ebft th\u00fac !!!";
                }
                res.des = des;
                res.status = status;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res.toJson();
    }
}

