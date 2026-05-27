/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.models.cache.TopDSModel
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.ResultTotalDoanhSoResponse
 *  com.vinplay.vbee.common.response.TotalDoanhSoResponse
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.agent.utils.AgentUtils;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.models.cache.TopDSModel;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.ResultTotalDoanhSoResponse;
import com.vinplay.vbee.common.response.TotalDoanhSoResponse;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class ListDoanhSoTotalLevel2Processor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultTotalDoanhSoResponse response = new ResultTotalDoanhSoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        AgentServiceImpl service = new AgentServiceImpl();
        ArrayList<TotalDoanhSoResponse> trans = null;
        try {
            trans = new ArrayList<TotalDoanhSoResponse>();
            AgentDAOImpl dao = new AgentDAOImpl();
            List<AgentResponse> agent = service.listUserAgentLevel1Active();
            HashMap<String, Long> mapListLevel2 = new HashMap<String, Long>();
            HashMap<String, AgentDSModel> mapAgentDS1 = new HashMap<String, AgentDSModel>();
            Map mapAgentDS2 = null;
            if (timeStart != null && !timeStart.isEmpty()) {
                try {
                    String month = DateTimeUtils.getFormatTime((String)"MM/yyyy", (Date)VinPlayUtils.getDateTime((String)timeStart));
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap topDSMap = client.getMap("cacheTop");
                    TopDSModel topDSModel = (TopDSModel)(topDSMap == null ? null : topDSMap.get((Object)("TopDSAgents1_" + month)));
                    if (topDSModel != null && topDSModel.getResults().size() > 0) {
                        mapAgentDS2 = topDSModel.getResults();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (AgentResponse name : agent) {
                long totalDS = 0L;
                List<AgentResponse> agent2 = dao.listUserAgentLevel2ByParentID(name.id);
                if (agent2.size() <= 0) continue;
                AgentDSModel ds1 = new AgentDSModel();
                for (AgentResponse nameAgent : agent2) {
                    AgentDSModel ds2 = null;
                    ds2 = mapAgentDS2 != null && mapAgentDS2.containsKey(nameAgent.nickName) ? (AgentDSModel)mapAgentDS2.get(nameAgent.nickName) : dao.getDS(nameAgent.nickName, timeStart, timeEnd, false);
                    totalDS += ds2.getDs();
                    ds1.setDs(ds1.getDs() + ds2.getDs());
                    ds1.setDsMua(ds1.getDsMua() + ds2.getDsMua());
                    ds1.setDsBan(ds1.getDsBan() + ds2.getDsBan());
                    ds1.setGd(ds1.getGd() + ds2.getGd());
                    ds1.setGdMua(ds1.getGdMua() + ds2.getGdMua());
                    ds1.setGdBan(ds1.getGdBan() + ds2.getGdBan());
                }
                mapListLevel2.put(String.valueOf(name.nickName) + "(" + name.fullName + ")", totalDS);
                mapAgentDS1.put(String.valueOf(name.nickName) + "(" + name.fullName + ")", ds1);
            }
            Map<String, Long> mapTopDS = AgentUtils.sortMapDS(mapListLevel2, mapAgentDS1);
            int i = 0;
            for (Map.Entry<String, Long> entry : mapTopDS.entrySet()) {
                TotalDoanhSoResponse doanhso = new TotalDoanhSoResponse();
                String agent3 = entry.getKey();
                long ds3 = ((AgentDSModel)mapAgentDS1.get(agent3)).getDs();
                int top = i + 1;
                long moneyBonusFix = 0L;
                long moneyBonusMore = 0L;
                long money = 0L;
                if (ds3 >= AgentUtils.dsMinDL2) {
                    if (i < AgentUtils.listBonusFixDL2.size()) {
                        moneyBonusFix = AgentUtils.listBonusFixDL2.get(i);
                    }
                    for (Map.Entry<Long, Double> et : AgentUtils.mapBonusMoreDL2.entrySet()) {
                        if (ds3 < et.getKey()) continue;
                        moneyBonusMore = Math.round((double)ds3 * et.getValue());
                        break;
                    }
                    money = moneyBonusFix + moneyBonusMore;
                }
                doanhso.nickName = agent3;
                doanhso.bonusFix = moneyBonusFix;
                doanhso.bonusMore = moneyBonusMore;
                doanhso.bonusTotal = money;
                TotalDoanhSoResponse totalDoanhSoResponse = doanhso;
                totalDoanhSoResponse.total += ds3;
                doanhso.top = top;
                trans.add(doanhso);
                ++i;
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}

