package com.vinplay.api.backend.processors.payment;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.payment.dao.WithDrawPaygateDao;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.WithDrawPaygateReponse;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class SearchWithdrawProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger(SearchWithdrawProcessor.class);

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String nickName = request.getParameter("nn");
		String providerName = request.getParameter("pn");
		String orderId = request.getParameter("oi");
		String transactionId = request.getParameter("ti");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");
		String bankCode = request.getParameter("bc");
		String bankAccountName = request.getParameter("ba");
		String bankAccountNumber = request.getParameter("bn");

		int status = -1, page = 1, maxItem = 10;
		Double fromAmount = null, toAmount = null;
		try {
			status = Integer.parseInt(request.getParameter("st"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			fromAmount = Double.parseDouble(request.getParameter("fa"));
			toAmount = Double.parseDouble(request.getParameter("ta"));
		} catch (Exception e) {
			fromAmount =null;
			toAmount = null;
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

		logger.info("Request InsertOrUpdateBankProcessor nickName= " + nickName + ", providerName: " + providerName
				+ ", orderId: " + orderId + ", " + "transactionId: " + transactionId + ", " + "fromTime: " + fromTime
				+ "endTime: " + endTime + "status: " + status + "page: " + page + "maxItem: " + maxItem
				+ "bankCode: " + bankCode + "bankAccountName: " + bankAccountName + "bankAccountNumber: " + bankAccountNumber+" ,fromAmount="+fromAmount+",toamount="+fromAmount);

		WithDrawPaygateDao withdrawDAO = new WithDrawPaygateDaoImpl();
//		UserService userService = new UserServiceImpl();
		try {
			// TODO: Check role
			WithDrawPaygateReponse withdrawPaygateReponse = withdrawDAO.Find(nickName, status, page, maxItem, fromTime,
					endTime, providerName, orderId, transactionId, bankCode, bankAccountNumber, bankAccountName,fromAmount,toAmount);
			if (withdrawPaygateReponse != null) {
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json = ow.writeValueAsString(withdrawPaygateReponse);
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
