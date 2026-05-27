/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.GiftCodeDAOImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.UserInfoModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.gamebase.dao.impl.GiftCodeDAOImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.UserInfoModel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetSpecialGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        try {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String type = request.getParameter("t");
            String mobile = request.getParameter("m");
            UserServiceImpl userService = new UserServiceImpl();
            List users = userService.checkPhoneByUser(mobile);
            if (users != null && users.size() == 1) {
                UserModel userModel = userService.getUserByNickName(((UserInfoModel)users.get((int)0)).nickName);
                if (userModel != null && userModel.getMobile().equals(mobile)) {
                    GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
                    return dao.GetGiftCodeByTypeNN(Integer.parseInt(type), ((UserInfoModel)users.get((int)0)).nickName);
                }
                return "invalid mobile";
            }
            return "invalid mobile";
        }
        catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();
            return ex.getMessage() + "\n" + sStackTrace;
        }
    }
}

