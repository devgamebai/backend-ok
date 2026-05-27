/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.service.impl.LogMoneyUserServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.processors.response.LogMoneyResponseNew;
import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class SearchLogMoneyUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LogMoneyResponseNew response = new LogMoneyResponseNew(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String token = request.getParameter("t");
        String nickName = request.getParameter("nn");
        String userName = request.getParameter("un");
        String timestart = request.getParameter("ts");
        String timeend = request.getParameter("te");
        String moneyType = request.getParameter("mt");
        String actionName = request.getParameter("ag");
        String serviceName = request.getParameter("sn");
        try {
            UserModel userModel;
            UserServiceImpl userService = new UserServiceImpl();
            if (userName != null && !"".equals(userName)) {
                userModel = userService.getUserByUserName(userName);
            } else if (nickName != null && !"".equals(nickName)) {
                userModel = userService.getUserByNickName(nickName);
            } else {
                return response.toJson();
            }
            if (userModel == null) {
                return response.toJson();
            }
            IMap userMap = HazelcastClientFactory.getInstance().getMap("users");
            if (!userMap.containsKey(userModel.getNickname())) {
                return response.toJson();
            }
            UserCacheModel userCache = (UserCacheModel)userMap.get(userModel.getNickname());
            if (userCache != null) {
                if (!userCache.getAccessToken().equals(token)) {
                    response.setErrorCode("400");
                    return response.toJson();
                }
            } else {
                return response.toJson();
            }
            int page = 1;
            int like = 0;
            int totalrecord = 50;
            try {
                if (request.getParameter("p") != null) {
                    page = Integer.parseInt(request.getParameter("p"));
                }
                if (request.getParameter("lk") != null) {
                    like = Integer.parseInt(request.getParameter("lk"));
                }
                if (request.getParameter("tr") != null) {
                    totalrecord = Integer.parseInt(request.getParameter("tr"));
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
            try {
                List trans = service.searchLogMoneyUser(userModel.getNickname(), null, moneyType, serviceName, actionName, timestart, timeend, page, like, totalrecord);
                if (trans != null && trans.size() > 0) {
                    for (Object _t : trans) {
                        com.vinplay.vbee.common.response.LogUserMoneyResponse tran = (com.vinplay.vbee.common.response.LogUserMoneyResponse) _t;
                        try {
                            if (tran.description.toLowerCase().contains("chuy\u1ec3n")) {
                                tran.sender_nick_name = tran.nickName;
                                String name = tran.description.replace("Chuy\u1ec3n t\u1edbi", "").trim();
                                tran.receiver_nick_name = name = name.substring(11, name.indexOf(":"));
                                tran.action = "TRANSFER";
                            } else {
                                tran.receiver_nick_name = tran.nickName;
                                String name = tran.description.replace("Nh\u1eadn t\u1eeb", "").trim();
                                tran.sender_nick_name = name = name.substring(8, name.indexOf(":"));
                                tran.action = "RECEIVE";
                            }
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                    }
                }
                int totalPages = service.countsearchLogMoneyUser(userModel.getNickname(), moneyType, serviceName, actionName, timestart, timeend, like);
                response.setTotalPages(totalPages);
                response.setTransactions(trans);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug(e);
            }
            return response.toJson();
        }
        catch (Exception ex) {
            logger.debug(ex);
            return response.toJson();
        }
    }
}

