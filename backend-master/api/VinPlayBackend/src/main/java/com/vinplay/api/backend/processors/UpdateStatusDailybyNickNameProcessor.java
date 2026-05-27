/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserForAdminServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.UserForAdminServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateStatusDailybyNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String status = request.getParameter("st");
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        UserForAdminServiceImpl service = new UserForAdminServiceImpl();
        if (nickName != null && status != null) {
            try {
                boolean result = service.updateStatusDailyByNickName(nickName, Integer.parseInt(status));
                if (result) {
                    response.setErrorCode("0");
                    response.setSuccess(true);
                } else {
                    response.setSuccess(false);
                }
            }
            catch (NumberFormatException e) {
                logger.debug((Object)e);
            }
            catch (SQLException e2) {
                logger.debug((Object)e2);
            }
        }
        return response.toJson();
    }
}

