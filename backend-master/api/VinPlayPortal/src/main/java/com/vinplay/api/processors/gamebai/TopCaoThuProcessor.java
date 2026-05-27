/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.TopCaoThuResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class TopCaoThuProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        TopCaoThuResponse response = new TopCaoThuResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String date = request.getParameter("date");
        String moneyType = request.getParameter("mt");
        String numStr = request.getParameter("n");
        if (date != null && moneyType != null && (moneyType.equals("vin") || moneyType.equals("xu")) && numStr != null) {
            try {
                ArrayList<TopCaoThu> userList = new ArrayList<TopCaoThu>();
                String today = VinPlayUtils.getCurrentDate();
                today.equals(date);
                response.setDate(date);
                response.setUserList(userList);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return response.toJson();
    }
}

