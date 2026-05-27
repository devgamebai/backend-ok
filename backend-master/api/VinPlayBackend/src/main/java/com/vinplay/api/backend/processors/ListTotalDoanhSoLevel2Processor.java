/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.ResultTotalDoanhSoResponse
 *  com.vinplay.vbee.common.response.TotalDoanhSoResponse
 *  com.vinplay.vbee.common.utils.MapUtils
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.ResultTotalDoanhSoResponse;
import com.vinplay.vbee.common.response.TotalDoanhSoResponse;
import com.vinplay.vbee.common.utils.MapUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class ListTotalDoanhSoLevel2Processor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultTotalDoanhSoResponse response = new ResultTotalDoanhSoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        AgentServiceImpl service = new AgentServiceImpl();
        ArrayList<TotalDoanhSoResponse> trans = null;
        try {
            int i;
            //Object value2;
            trans = new ArrayList<>();
            AgentDAOImpl dao = new AgentDAOImpl();
            List<AgentResponse> agent = service.listUserAgentActive(nickName);
            HashMap<String, Long> hmapTopDS = new HashMap<String, Long>();
            Map<String, Long> mapTopDS = new HashMap();
            HashMap<String, AgentDSModel> mapAgentDS = new HashMap<String, AgentDSModel>();
            for (AgentResponse name : agent) {
                List<AgentResponse> agent2 = dao.listUserAgentLevel2ByParentID(((AgentResponse)name).id);
                if (agent2 == null) continue;
                for (AgentResponse nameAgent : agent2) {
                    AgentDSModel ds2 = dao.getDS(nameAgent.nickName, timeStart, timeEnd, false);
                    hmapTopDS.put(nameAgent.nickName, ds2.getDs());
                    mapAgentDS.put(nameAgent.nickName, ds2);
                }
            }
            for (i = 0; i <= hmapTopDS.size(); ++i) {
                for (Map.Entry entry : hmapTopDS.entrySet()) {
                    for (Map.Entry entry2 : hmapTopDS.entrySet()) {
                        String key1 = (String)entry.getKey();
                        String key2 = (String)entry2.getKey();
                        long value1 = (Long)entry.getValue();
                        long value2 = (Long)entry2.getValue();
                        if (key1.equals(key2) || value1 != value2) continue;
                        AgentDSModel ds3 = (AgentDSModel)mapAgentDS.get(entry.getKey());
                        AgentDSModel ds4 = (AgentDSModel)mapAgentDS.get(entry2.getKey());
                        if (ds3.getDsBan() > ds4.getDsBan()) {
                            hmapTopDS.put((String)entry.getKey(), (Long)entry.getValue() + 1L);
                            continue;
                        }
                        if (ds3.getDsBan() < ds4.getDsBan()) {
                            hmapTopDS.put((String)entry2.getKey(), (Long)entry2.getValue() + 1L);
                            continue;
                        }
                        if (ds3.getGd() > ds4.getGd()) {
                            hmapTopDS.put((String)entry.getKey(), (Long)entry.getValue() + 1L);
                            continue;
                        }
                        if (ds3.getGd() < ds4.getGd()) {
                            hmapTopDS.put((String)entry2.getKey(), (Long)entry2.getValue() + 1L);
                            continue;
                        }
                        if (ds3.getGdBan() > ds4.getGdBan()) {
                            hmapTopDS.put((String)entry.getKey(), (Long)entry.getValue() + 1L);
                            continue;
                        }
                        if (ds3.getGdBan() >= ds4.getGdBan()) continue;
                        hmapTopDS.put((String)entry2.getKey(), (Long)entry2.getValue() + 1L);
                    }
                }
            }
            mapTopDS = MapUtils.sortMapByValue2(hmapTopDS);
            i = 0;
            for (Map.Entry entry : mapTopDS.entrySet()) {
                String agent3 = (String)entry.getKey();
                long ds5 = ((AgentDSModel)mapAgentDS.get(agent3)).getDs();
                int top = i + 1;
                TotalDoanhSoResponse doanhso = new TotalDoanhSoResponse();
                doanhso.nickName = agent3;
                doanhso.top = top;
                doanhso.total = ds5;
                List<AgentResponse> agentnick = service.listUserAgent(agent3);
                Iterator value2 = agentnick.iterator();
                while (value2.hasNext()) {
                    AgentResponse name2 = (AgentResponse)value2.next();
                    List<AgentResponse> agentlv2 = dao.listUserAgentLevel2ByID(name2.parentid);
                    for (AgentResponse nameAgent2 : agentlv2) {
                        doanhso.agentName = String.valueOf(nameAgent2.nickName) + "( " + nameAgent2.fullName + ")";
                    }
                }
                trans.add(doanhso);
                ++i;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        response.setTransactions(trans);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}

