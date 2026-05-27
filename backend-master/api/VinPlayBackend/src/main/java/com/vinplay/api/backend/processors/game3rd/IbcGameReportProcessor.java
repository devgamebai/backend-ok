package com.vinplay.api.backend.processors.game3rd;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.common.game3rd.IbcGameRecordItem;
import com.vinplay.common.game3rd.ThirdPartyResponse;
import com.vinplay.payment.utils.Constant;
import com.vinplay.report.service.ThirdPartyGameReport;
import com.vinplay.report.service.impl.ThirdPartyGameReportImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class IbcGameReportProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();

		String nick_name = request.getParameter("nn");
		String playername = request.getParameter("pn");
		String ticketstatus = request.getParameter("tt");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");

		int page = 1, maxItem = 10 ,ticketStatus=0,flagTime=0;
		Integer matchid = null,homeid = null;
		Long stake = null;
		Long transid = null;
		Long winloseamount = null,refund_amount = null;
		Date winlostdatetime = null;

		try {
			flagTime = Integer.parseInt(request.getParameter("fgt"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			transid = Long.parseLong(request.getParameter("trid"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			matchid = Integer.parseInt(request.getParameter("mid"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			homeid = Integer.parseInt(request.getParameter("hid"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}
		try {
			stake = Long.parseLong(request.getParameter("sk"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}

		try {
			winloseamount = Long.parseLong(request.getParameter("wa"));
		} catch (NumberFormatException e) {
			// TODO: handle exception
		}

//		try {
//			winlostdatetime = new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("wdt").toString());
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}

		try {
			refund_amount = Long.parseLong(request.getParameter("ra"));
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

		logger.info("Request IBC report nickName= " + nick_name + ", transId: " + transid);
		ThirdPartyGameReport service = new ThirdPartyGameReportImpl();
		try {
			// TODO: Check role
			ThirdPartyResponse<List<IbcGameRecordItem>> res = service.filterIBC(transid, playername, null
					, matchid, null, null, null, null, homeid, null,
					null, null, null, null, null, null,
					null, null, null, null, null, null,
					null, null, null, stake, null ,winloseamount ,
					winlostdatetime, null, null, ticketstatus, null, null,
					refund_amount, nick_name,flagTime, fromTime, endTime, page, maxItem);
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
