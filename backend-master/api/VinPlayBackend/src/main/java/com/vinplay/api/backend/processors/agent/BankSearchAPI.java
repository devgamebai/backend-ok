package com.vinplay.api.backend.processors.agent;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vinplay.payment.entities.Bank;
import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.dao.BankDao;
import com.vinplay.usercore.dao.impl.BankDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class BankSearchAPI implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");// Logger.getLogger(BankSearchProcessor.class);
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }
        
		String bankName = request.getParameter("bn");
		String bankCode = request.getParameter("bc");
		String pageNumberStr= request.getParameter("pn");
		String limitStr= request.getParameter("l");
		int pageNumber =0;
		int limit = 0;
		try {
			pageNumber = Integer.parseInt(pageNumberStr);
			limit = Integer.parseInt(limitStr);
		} catch (NumberFormatException e) {
			return BaseResponse.error(Constant.ERROR_PARAM, "pageNumber or limit format");
		}
		if (pageNumber <= 0)
			pageNumber = 1;
		if (limit < 0)
			limit = 15;
		if (StringUtils.isBlank(bankName)) {
			bankName ="";
		}
		if (StringUtils.isBlank(bankCode)) {
			bankCode ="";
		}
		BankDao bankDao = new BankDaoImpl();
		List<Bank> res =null;
		try {
			res = bankDao.search(bankName, bankCode, pageNumber, limit);
			return new BaseResponse<List<Bank>>().success(res);
		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}

	}
}
