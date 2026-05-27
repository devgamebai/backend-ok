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
 *  com.vinplay.vbee.common.utils.MapUtils
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
import com.vinplay.vbee.common.utils.MapUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class ListTotalDoanhSoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultTotalDoanhSoResponse response = new ResultTotalDoanhSoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String month = request.getParameter("month");
        AgentServiceImpl service = new AgentServiceImpl();
        ArrayList<TotalDoanhSoResponse> trans = null;
        try {
            trans = new ArrayList<TotalDoanhSoResponse>();
            AgentDAOImpl dao = new AgentDAOImpl();
            List<AgentResponse> agent = service.listUserAgent(nickName);
            HashMap<String, Long> hmapTopDS = new HashMap<String, Long>();
            HashMap<String, Long> hmapTopDSNotEnough = new HashMap();
            HashMap<String, Long> mapDS1 = new HashMap<String, Long>();
            HashMap<String, AgentDSModel> mapAgentDS1 = new HashMap<String, AgentDSModel>();
            Map<String, AgentDSModel> mapAgentDS2 = new HashMap();
            HashMap<String, Integer> percentMap = new HashMap<String, Integer>();
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topDSMap = client.getMap("cacheTop");
            TopDSModel topDSModel = (TopDSModel)(topDSMap == null ? null : topDSMap.get((Object)("TopDSAgents1_" + month)));
            if (topDSModel != null && topDSModel.getResults().size() > 0) {
                mapAgentDS2 = topDSModel.getResults();
                for (Map.Entry entry : mapAgentDS2.entrySet()) {
                    AgentDSModel mdA = (AgentDSModel)entry.getValue();
                    String nicknameDL1 = (String)entry.getKey();
                    if (mdA.getDl1() != null && !mdA.getDl1().isEmpty()) {
                        nicknameDL1 = mdA.getDl1();
                    } else {
                        mapDS1.put(nicknameDL1, mdA.getDs());
                    }
                    if (hmapTopDS.containsKey(nicknameDL1)) {
                        hmapTopDS.put(nicknameDL1, mdA.getDs() + hmapTopDS.get(nicknameDL1));
                    } else {
                        hmapTopDS.put(nicknameDL1, mdA.getDs());
                    }
                    if (mapAgentDS1.containsKey(nicknameDL1)) {
                        AgentDSModel mdA2 = (AgentDSModel)mapAgentDS1.get(nicknameDL1);
                        mdA2.setDs(mdA2.getDs() + mdA.getDs());
                        mdA2.setDsMua(mdA2.getDsMua() + mdA.getDsMua());
                        mdA2.setDsBan(mdA2.getDsBan() + mdA.getDsBan());
                        mdA2.setGd(mdA2.getGd() + mdA.getGd());
                        mdA2.setGdMua(mdA2.getGdMua() + mdA.getGdMua());
                        mdA2.setGdBan(mdA2.getGdBan() + mdA.getGdBan());
                        mapAgentDS1.put(nicknameDL1, mdA2);
                        continue;
                    }
                    mapAgentDS1.put(nicknameDL1, mdA);
                }
                for (AgentResponse name : agent) {
                    percentMap.put(name.nickName, name.percent);
                }
            } else {
                for (AgentResponse name : agent) {
                    if (name.parentid != -1 || name.show != 1 || name.active != 1) continue;
                    AgentDSModel ds1 = dao.getDS(name.nickName, timeStart, timeEnd, true);
                    mapAgentDS2.put(name.nickName, new AgentDSModel(ds1.getDsMua(), ds1.getDsBan(), ds1.getDs(), ds1.getGdMua(), ds1.getGdBan(), ds1.getGd()));
                    mapDS1.put(name.nickName, ds1.getDs());
                    List agent2 = dao.listUserAgentByParentID(name.id);
                    Iterator mdA2 = agent2.iterator();
                    while (mdA2.hasNext()) {
                        AgentResponse nameAgent = (AgentResponse)mdA2.next();
                        AgentDSModel ds2 = dao.getDS(nameAgent.nickName, timeStart, timeEnd, false);
                        ds1.setDs(ds1.getDs() + ds2.getDs());
                        ds1.setDsMua(ds1.getDsMua() + ds2.getDsMua());
                        ds1.setDsBan(ds1.getDsBan() + ds2.getDsBan());
                        ds1.setGd(ds1.getGd() + ds2.getGd());
                        ds1.setGdMua(ds1.getGdMua() + ds2.getGdMua());
                        ds1.setGdBan(ds1.getGdBan() + ds2.getGdBan());
                        ds2.setDl1(name.nickName);
                        mapAgentDS2.put(nameAgent.nickName, ds2);
                    }
                    hmapTopDS.put(name.nickName, ds1.getDs());
                    mapAgentDS1.put(name.nickName, ds1);
                    percentMap.put(name.nickName, name.percent);
                }
                topDSModel = new TopDSModel();
                topDSModel.setResults(mapAgentDS2);
                topDSMap.put((Object)("TopDSAgents1_" + month), (Object)topDSModel);
            }
            for (Map.Entry entry2 : hmapTopDS.entrySet()) {
                if ((Long)mapDS1.get(entry2.getKey()) >= AgentUtils.dsMin1) continue;
                hmapTopDSNotEnough.put((String)entry2.getKey(), (Long)entry2.getValue());
            }
            for (Map.Entry entry2 : hmapTopDSNotEnough.entrySet()) {
                hmapTopDS.remove(entry2.getKey());
            }
            Map<String, Long> mapTopDS = AgentUtils.sortMapDS(hmapTopDS, mapAgentDS1);
            Map<String, Long>  mapTopDSNotEnough = MapUtils.sortMapByValue2(hmapTopDSNotEnough);
            int i = 0;
            for (Map.Entry<String, Long> entry3 : mapTopDS.entrySet()) {
                String agent3 = entry3.getKey();
                long ds3 = ((AgentDSModel)mapAgentDS1.get(agent3)).getDs();
                long moneyBonusFix = 0L;
                long moneyBonusMore = 0L;
                long money = 0L;
                if (ds3 >= AgentUtils.dsMin) {
                    if (i < AgentUtils.listBonusFix.size()) {
                        moneyBonusFix = AgentUtils.listBonusFix.get(i);
                    }
                    for (Map.Entry<Long, Double> et : AgentUtils.mapBonusMore.entrySet()) {
                        if (ds3 < et.getKey()) continue;
                        moneyBonusMore = Math.round((double)ds3 * et.getValue());
                        break;
                    }
                    money = moneyBonusFix + moneyBonusMore;
                }
                int percent = 0;
                try {
                    percent = (Integer)percentMap.get(agent3);
                }
                catch (Exception et) {
                    // empty catch block
                }
                long moneyBonusVinplayCardTemp = Math.round(money * (long)percent / 100L);
                long moneyBonusVinplayCard = ListTotalDoanhSoProcessor.roundVinCard(money, moneyBonusVinplayCardTemp);
                long moneyBonusVinCash = money - moneyBonusVinplayCard;
                TotalDoanhSoResponse doanhso = new TotalDoanhSoResponse();
                doanhso.setNickName(agent3);
                doanhso.setBonusFix(moneyBonusFix);
                doanhso.setBonusMore(moneyBonusMore);
                doanhso.setBonusTotal(money);
                doanhso.setTop(i + 1);
                doanhso.setTotal(ds3);
                doanhso.setBonusByVinplayCard(moneyBonusVinplayCard);
                doanhso.setBonusByVinCash(moneyBonusVinCash);
                doanhso.setPercent(percent);
                for (AgentResponse name2 : agent) {
                    if (!name2.nickName.equals(agent3)) continue;
                    doanhso.setAgentName(name2.fullName);
                    break;
                }
                trans.add(doanhso);
                ++i;
            }
            for (Map.Entry entry4 : mapTopDSNotEnough.entrySet()) {
                TotalDoanhSoResponse doanhso2 = new TotalDoanhSoResponse();
                doanhso2.setNickName((String)entry4.getKey());
                doanhso2.setTop(i + 1);
                doanhso2.setTotal((Long)entry4.getValue());
                doanhso2.setPercent(((Integer)percentMap.get(entry4.getKey())).intValue());
                for (AgentResponse name3 : agent) {
                    if (!name3.nickName.equals(entry4.getKey())) continue;
                    doanhso2.setAgentName(name3.fullName);
                    break;
                }
                trans.add(doanhso2);
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

    private static long roundVinCard(long moneyTotal, long moneyVinCard) {
        if (moneyVinCard < 10500L) {
            return 0L;
        }
        long division = moneyVinCard / 10500L;
        if ((division + 1L) * 10500L < moneyTotal) {
            return (division + 1L) * 10500L;
        }
        return division * 10500L;
    }
}

