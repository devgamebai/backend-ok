package com.vinplay.api.backend.processors.sendMailContent;

import com.vinplay.usercore.service.SendMailService;
import com.vinplay.usercore.service.impl.SendMailServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class DeleteSendMailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    SendMailService service = new SendMailServiceImpl();

    @Override
    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        HttpServletRequest request = param.get();
        String idStr = request.getParameter("id");

        try {

            int id = Integer.parseInt(idStr);
            boolean ok =  service.deleteMail(id);
            if (ok) {
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            response.setMessage(e.getMessage());
        }

        return response.toJson();
    }
}
