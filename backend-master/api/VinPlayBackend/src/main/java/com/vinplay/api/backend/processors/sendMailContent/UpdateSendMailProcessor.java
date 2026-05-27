package com.vinplay.api.backend.processors.sendMailContent;

import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.entities.SendMail;
import com.vinplay.usercore.service.SendMailService;
import com.vinplay.usercore.service.impl.SendMailServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

public class UpdateSendMailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    SendMailService service = new SendMailServiceImpl();

    @Override
    public String execute(Param<HttpServletRequest> param) {

        BaseResponseModel response = new BaseResponseModel(false, "1001");

        if (!"POST".equalsIgnoreCase(param.get().getMethod())) {
            response.setMessage( "METHOD_NOT_ENABLE");
            return response.toJson();
        }
        String jsonString = "";
        try {
            jsonString = IOUtils.toString(param.get().getReader());
            JSONObject obj = new JSONObject(jsonString);

            SendMail sendMail = new SendMail();

            sendMail.setId(obj.getInt("id"));
            sendMail.setTitle(obj.getString("title"));
            if (Strings.isBlank(sendMail.getTitle())) {
                return BaseResponse.error(Constant.ERROR_PARAM, "title is null or empty");
            }

            sendMail.setMessage(obj.getString("message"));
            if (Strings.isBlank(sendMail.getMessage())) {
                return BaseResponse.error(Constant.ERROR_PARAM, "message is null or empty");
            }

            sendMail.setExtra_data(obj.getString("extra_data"));
            sendMail.setStatus(obj.getInt("status"));
            sendMail.setType(obj.getInt("type"));
            if (sendMail.getType() == 0) {
                return BaseResponse.error(Constant.ERROR_PARAM, "type is null or empty");
            }

            boolean ok =  service.updateMail(sendMail);
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
