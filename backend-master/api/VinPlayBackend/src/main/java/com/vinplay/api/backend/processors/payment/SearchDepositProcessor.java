package com.vinplay.api.backend.processors.payment;

import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.payment.dao.RechargePaygateDao;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class SearchDepositProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String nickName = request.getParameter("nn");
		String providerName = request.getParameter("pn");
		String orderId = request.getParameter("oi");
		String transactionId = request.getParameter("ti");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");
		String bankName = request.getParameter("bc");
		Double fromAmount = null, toAmount = null;
		try {
			fromAmount = Double.parseDouble(request.getParameter("fa"));
			toAmount = Double.parseDouble(request.getParameter("ta"));
		} catch (Exception e) {
			fromAmount =null;
			toAmount = null;
		}
		int status = -1, page = 1, maxItem = 10;
		try {
			status = Integer.parseInt(request.getParameter("st"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			page = Integer.parseInt(request.getParameter("pg"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			maxItem = Integer.parseInt(request.getParameter("mi"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
//		if (!GameCommon.LIST_BANK_NAME.contains(bankName)) {
//			return BaseResponse.error("8", "Hệ thống không hỗ trợ BankCode này ,bank_code= " + bankName);
//		}
		logger.info("Request InsertOrUpdateBankProcessor nickName= " + nickName + ", providerName: " + providerName
				+ ", orderId: " + orderId + ", " + "transactionId: " + transactionId + ", " + "fromTime: " + fromTime
				+ "endTime: " + endTime + "status: " + status + "page: " + page + "maxItem: " + maxItem);

		RechargePaygateDao rechargeDAO = new RechargePaygateDaoImpl();
//		UserService userService = new UserServiceImpl();
		try {
			// TODO: Check role
			DepositPaygateReponse depositPaygateReponse = rechargeDAO.Find(nickName, status, page, maxItem, fromTime,
					endTime, providerName, orderId, transactionId ,bankName , fromAmount,toAmount);
			if (depositPaygateReponse != null) {
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json = ow.writeValueAsString(depositPaygateReponse);
				return new BaseResponse<String>().success(json);
			} else {
				return BaseResponse.error(Constant.ERROR_SYSTEM, "null");
			}

		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}

	}
}
