/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.utils;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;

public class AgentUtils {
    private static final Logger logger = Logger.getLogger(AgentUtils.class);
    private static AgentDAO agentDao = new AgentDAOImpl();

    public static String GenerateRandomNumber(int charLength) {
        return String.valueOf(charLength < 1 ? 0 : new Random().nextInt(9 * (int)Math.pow(10.0, charLength - 1) - 1) + (int)Math.pow(10.0, charLength - 1));
    }

    public static String GenerateRandomNumberString(int charLength) {
        char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charLength; ++i) {
            sb.append(chars[rnd.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    public static String ValidateInfo(UserAgentModel userAgentModel, int action) {
        try {
            if (userAgentModel.getNickname() == null || userAgentModel.getNickname().isEmpty()) {
                return "Nickname can not empty";
            }
            if (userAgentModel.getNameagent() == null || userAgentModel.getNameagent().isEmpty()) {
                return "Name can not empty";
            }
            if (action == 1) {
                if (userAgentModel.getUsername() == null || userAgentModel.getUsername().isEmpty()) {
                    return "Username can not empty";
                }
                if (userAgentModel.getPassword() == null || userAgentModel.getPassword().isEmpty()) {
                    return "Password can not empty";
                }
                if (userAgentModel.getParentid() == null || userAgentModel.getParentid() == 0) {
                    return "Agent parent is invalid";
                }
                if (agentDao.DetailUserAgent(userAgentModel.getParentid()) == null) {
                    return "Agent parent is not exist";
                }
                if (agentDao.DetailUserAgentByUserName(userAgentModel.getUsername()) != null) {
                    return "Username is exist";
                }
                if (agentDao.DetailUserAgentByNickName(userAgentModel.getNickname()) != null) {
                    return "Nickname is exist";
                }
                userAgentModel.setId(0);
                if (agentDao.checkExistAgency(userAgentModel).booleanValue()) {
                    return "Username/Nickname/Name/Phone/Email/Facebook/NumberAccount is exist";
                }
            }
            if (action == 2) {
                if (userAgentModel.getId() == null || userAgentModel.getId() == 0) {
                    return "Id is invalid";
                }
                UserAgentModel old = agentDao.DetailUserAgent(userAgentModel.getId());
                if (!old.getCode().equals(userAgentModel.getCode())) {
                    return "Code is invalid";
                }
                if (!old.getParentid().equals(userAgentModel.getParentid())) {
                    return "ParentId is invalid";
                }
                if (agentDao.checkExistAgency(userAgentModel).booleanValue()) {
                    return "Username/Nickname/Name/Phone/Email/Facebook/NumberAccount is exist";
                }
            }
            return "success";
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    public static BaseResponse<Object> createAgent(UserAgentModel userAgentModel, int codeLength) {
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        String code = null;
        if (codeLength == 0) {
            codeLength = 6;
        }
        List<String> codes = new ArrayList<String>();
        try {
            codes = agentDao.getListCode();
        }
        catch (SQLException sQLException) {
            // empty catch block
        }
        int maxRetries = 10000;
        for (int retryCount = 0; retryCount < maxRetries; ++retryCount) {
            code = AgentUtils.GenerateRandomNumber(codeLength);
            if (codes.size() == 0 || !codes.contains(code)) break;
        }
        userAgentModel.setCode(code);
        String valid = AgentUtils.ValidateInfo(userAgentModel, 1);
        if (!valid.equals("success")) {
            res.setData(null);
            res.setTotalRecords(0L);
            res.setErrorCode("1001");
            res.setMessage(valid);
            res.setSuccess(false);
            return res;
        }
        boolean result = false;
        try {
            result = agentDao.AddNewUserAgent(userAgentModel);
            if (!result) {
                return res;
            }
            UserAgentModel userAgentModelNew = agentDao.DetailUserAgentByCode(code);
            if (userAgentModelNew == null) {
                return res;
            }
            userAgentModelNew.setPassword(userAgentModel.getPassword());
            result = agentDao.AddNewUser(userAgentModelNew);
            if (!result) {
                agentDao.deleteUserAgent(userAgentModelNew.getId());
                return res;
            }
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (SQLException e) {
            logger.error(("[ADD AGENT] Error: " + e.getMessage()));
            e.printStackTrace();
            res.setMessage(e.getMessage());
            return res;
        }
    }

    public static BaseResponse<Object> updateInfo(UserAgentModel userAgentModel) {
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        String valid = AgentUtils.ValidateInfo(userAgentModel, 2);
        if (!valid.equals("success")) {
            res.setData(null);
            res.setTotalRecords(0L);
            res.setErrorCode("1001");
            res.setMessage(valid);
            res.setSuccess(false);
            return res;
        }
        boolean result = false;
        try {
            result = agentDao.UpdateUserAgent(userAgentModel);
            if (!result) {
                return res;
            }
            res.setTotalRecords(0L);
            res.setErrorCode("0");
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (SQLException e) {
            logger.error(("[ADD AGENT] Error: " + e.getMessage()));
            e.printStackTrace();
            res.setMessage(e.getMessage());
            return res;
        }
    }

    public static BaseResponse<Object> getAllChilds(int id, int pageIndex, int limit) {
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        try {
            Map<String, Object> map = agentDao.getAllChilds(null, pageIndex, limit);
            res.setTotalRecords((long)Integer.parseInt(map.get("total").toString()));
            res.setData(map.get("childs"));
            res.setMessage("success");
            res.setErrorCode("0");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[GET CHILD OF AGENT] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> searchChilds(int id, String keyword, int level, int pageIndex, int limit) {
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        try {
            Map<String, Object> map = agentDao.searchChilds(id, keyword, level, pageIndex, limit);
            res.setTotalRecords((long)Integer.parseInt(map.get("total").toString()));
            res.setData(map.get("childs"));
            res.setMessage("success");
            res.setErrorCode("0");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[GET CHILD OF AGENT] Exception: " + e.getMessage()));
            return res;
        }
    }

    public static BaseResponse<Object> getParents(int id) {
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        try {
            List<UserAgentModel> userAgentModels = agentDao.getParents(id);
            res.setTotalRecords((long)userAgentModels.size());
            res.setErrorCode("0");
            res.setData(userAgentModels);
            res.setMessage("success");
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            logger.error(("[GET PARENT OF AGENT] Exception: " + e.getMessage()));
            return res;
        }
    }
}

