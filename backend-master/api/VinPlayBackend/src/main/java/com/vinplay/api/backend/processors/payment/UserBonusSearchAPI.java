package com.vinplay.api.backend.processors.payment;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.UserBonusService;
import com.vinplay.usercore.service.impl.UserBonusServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserBonusModel;
import com.vinplay.vbee.common.response.BaseResponse;

public class UserBonusSearchAPI implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger("backend");// Logger.getLogger(BankSearchProcessor.class);

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String nick_name = request.getParameter("nn");
		String ip = request.getParameter("ip");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");

		int page = 1, maxItem = 10;
		Integer bonusType;

		try {
			bonusType = Integer.parseInt(request.getParameter("bt"));
		} catch (NumberFormatException e) {
			bonusType = null;
		}
		try {
			page = Integer.parseInt(request.getParameter("pg"));
		} catch (NumberFormatException e) {
			page = 1;
		}
		try {
			maxItem = Integer.parseInt(request.getParameter("mi"));
		} catch (NumberFormatException e) {
			maxItem = 10;
		}


		UserBonusService bankDao = new UserBonusServiceImpl();
		try {
			Long total = bankDao.count(nick_name, ip, bonusType, fromTime, endTime);
			List<UserBonusModel> data = bankDao.search(nick_name, ip, bonusType, fromTime, endTime, page, maxItem);
			double sumAmount = bankDao.sumAmount(nick_name, ip, bonusType, fromTime, endTime);
			Map<String, Object> mapData = new HashMap<>();
			mapData.put("listData", data);
			mapData.put("totalAmount", sumAmount);
			return BaseResponse.success(mapData, total);
		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}

	}
}
