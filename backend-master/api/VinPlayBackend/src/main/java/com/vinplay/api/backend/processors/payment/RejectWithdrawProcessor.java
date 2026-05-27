package com.vinplay.api.backend.processors.payment;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import com.vinplay.payment.service.WithDrawOneClickPayService;
import com.vinplay.payment.service.impl.WithDrawOneClickPayServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import com.vinplay.vbee.common.response.BaseResponse;

public class RejectWithdrawProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger = Logger.getLogger(RejectWithdrawProcessor.class);

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String approvedName = request.getParameter("nns");
		String orderId = request.getParameter("oid");
		String reason = request.getParameter("reason");
		logger.info("Request RejectWithdrawProcessor approvedName= " + approvedName + ", orderId: " + orderId + ", reason: " + reason);
		if (StringUtils.isBlank(approvedName)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "nickName of staff is null or empty");
		}
		if (StringUtils.isBlank(orderId)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "orderId is null or empty");
		}
		
		try {
			WithDrawOneClickPayService withdrawOneClickPayService = new WithDrawOneClickPayServiceImpl();
			if(withdrawOneClickPayService.reject(orderId, approvedName, reason))
				return new BaseResponse<String>().success("success");
			else 
				return BaseResponse.error(Constant.ERROR_SYSTEM, "Failed");
		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}

	}
}
