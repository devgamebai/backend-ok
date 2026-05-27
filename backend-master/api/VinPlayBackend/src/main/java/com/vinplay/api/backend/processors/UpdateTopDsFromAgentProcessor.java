/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.entities.agent.TranferMoneyAgent
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.usercore.dao.impl.AgentDaoImpl
 *  com.vinplay.usercore.service.impl.VippointServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.models.cache.TopDSModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.TranferMoneyAgent;
import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.usercore.dao.impl.AgentDaoImpl;
import com.vinplay.usercore.service.impl.VippointServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.models.cache.TopDSModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateTopDsFromAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickNameSend = request.getParameter("nns");
        String nickNameReceive = request.getParameter("nnr");
        String timeLog = request.getParameter("t");
        String topds = request.getParameter("tds");
        boolean result = false;
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (!(nickNameSend == null || nickNameSend.equals("") || nickNameReceive == null || nickNameReceive.equals("") || timeLog == null || timeLog.equals("") || topds == null)) {
            block23 : {
                if (!topds.equals("0") && !topds.equals("1")) {
                    return "MISSING PARAMETTER";
                }
                try {
                    AgentServiceImpl service = new AgentServiceImpl();
                    result = service.updateTopDsFromAgent(nickNameSend, nickNameReceive, timeLog, topds);
                    if (result) {
                        response.setSuccess(true);
                        response.setErrorCode("0");
                        AgentDAOImpl dao = new AgentDAOImpl();
                        TranferMoneyAgent tmAgent = dao.getTransferMoneyAgent(nickNameSend, nickNameReceive, timeLog);
                        if (tmAgent == null) break block23;
                        VippointServiceImpl vpService = new VippointServiceImpl();
                        int cal = topds.equals("1") ? 1 : -1;
                        vpService.updateVippointAgent(tmAgent.getNicknameSend(), tmAgent.getNicknameReceive(), tmAgent.getMoneySend() * (long)cal, tmAgent.getMoneyReceive() * (long)cal, tmAgent.getStatus());
                        String nicknameAgent = null;
                        if (tmAgent.getStatus() == 1 || tmAgent.getStatus() == 2) {
                            nicknameAgent = nickNameReceive;
                        } else if (tmAgent.getStatus() == 3 || tmAgent.getStatus() == 6) {
                            nicknameAgent = nickNameSend;
                        }
                        if (nicknameAgent == null) break block23;
                        try {
                            long money = topds.equals("0") ? -tmAgent.getMoneySend() : tmAgent.getMoneySend();
                            int gd = topds.equals("0") ? -1 : 1;
                            String month = DateTimeUtils.getFormatTime((String)"MM/yyyy", (Date)VinPlayUtils.getDateTime((String)timeLog));
                            HazelcastInstance client = HazelcastClientFactory.getInstance();
                            IMap topMap = client.getMap("cacheTop");
                            TopDSModel topDSModel = (TopDSModel)(topMap == null ? null : topMap.get((Object)("TopDSAgents1_" + month)));
                            if (topDSModel != null && topDSModel.getResults().size() > 0) {
                                Map mapAgentDS = topDSModel.getResults();
                                String daiLyCap1 = "";
                                switch (tmAgent.getStatus()) {
                                    case 1: 
                                    case 2: {
                                        try {
                                            AgentDSModel modelMua = null;
                                            String agentName = nicknameAgent;
                                            if (mapAgentDS.containsKey(agentName)) {
                                                modelMua = (AgentDSModel)mapAgentDS.get(agentName);
                                            } else {
                                                modelMua = new AgentDSModel();
                                                if (tmAgent.getStatus() == 2) {
                                                    AgentDaoImpl agentDao = new AgentDaoImpl();
                                                    daiLyCap1 = agentDao.getAgentLevel1ByNickName(agentName);
                                                    modelMua.setDl1(daiLyCap1);
                                                }
                                            }
                                            modelMua.setDs(modelMua.getDs() + money);
                                            modelMua.setDsMua(modelMua.getDsMua() + money);
                                            modelMua.setGd(modelMua.getGd() + gd);
                                            modelMua.setGdMua(modelMua.getGdMua() + gd);
                                            mapAgentDS.put(agentName, modelMua);
                                        }
                                        catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    }
                                    case 3: 
                                    case 6: {
                                        String agentName2 = nicknameAgent;
                                        AgentDSModel modelBan = null;
                                        if (mapAgentDS.containsKey(agentName2)) {
                                            modelBan = (AgentDSModel)mapAgentDS.get(agentName2);
                                        } else {
                                            modelBan = new AgentDSModel();
                                            if (tmAgent.getStatus() == 6) {
                                                AgentDaoImpl agentDao = new AgentDaoImpl();
                                                daiLyCap1 = agentDao.getAgentLevel1ByNickName(agentName2);
                                                modelBan.setDl1(daiLyCap1);
                                            }
                                        }
                                        modelBan.setDs(modelBan.getDs() + money);
                                        modelBan.setDsBan(modelBan.getDsBan() + money);
                                        modelBan.setGd(modelBan.getGd() + gd);
                                        modelBan.setGdBan(modelBan.getGdBan() + gd);
                                        mapAgentDS.put(agentName2, modelBan);
                                        break;
                                    }
                                }
                                topDSModel.setResults(mapAgentDS);
                                topMap.put((Object)("TopDSAgents1_" + month), (Object)topDSModel);
                            }
                            break block23;
                        }
                        catch (ParseException money) {}
                        break block23;
                    }
                    response.setSuccess(false);
                    response.setErrorCode("1001");
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                    logger.debug((Object)e2);
                }
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

