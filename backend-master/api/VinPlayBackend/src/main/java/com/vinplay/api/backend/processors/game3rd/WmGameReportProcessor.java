package com.vinplay.api.backend.processors.game3rd;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.common.game3rd.ThirdPartyResponse;
import com.vinplay.common.game3rd.WMGameRecordItem;
import com.vinplay.payment.utils.Constant;
import com.vinplay.report.service.ThirdPartyGameReport;
import com.vinplay.report.service.impl.ThirdPartyGameReportImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class WmGameReportProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger(WmGameReportProcessor.class);

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();

		String nick_name = request.getParameter("nn");
		String user = request.getParameter("us");
		String ip= request.getParameter("ip");

		String gameName= request.getParameter("gn");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");

		int page = 1, maxItem = 10, flagTime = 0;
		Long  betid = null;
		try {
			betid = Long.parseLong(request.getParameter("bid"));
		} catch (Exception e) {
			betid = null;
		}

		try {
			flagTime = Integer.parseInt(request.getParameter("fgt"));
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

		logger.info("Request AG report nickName= " + nick_name + ", betid: " + betid);
		ThirdPartyGameReport service = new ThirdPartyGameReportImpl();
		try {
			// TODO: Check role
			ThirdPartyResponse<List<WMGameRecordItem>> res = service.filterWM(nick_name, user, ip, gameName, betid,
					flagTime, fromTime, endTime, page, maxItem);
					if (res != null) {
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json = ow.writeValueAsString(res);
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
