package com.vinplay.api.backend.processors.report;

import javax.servlet.http.HttpServletRequest;
import com.vinplay.report.service.CountReportService;
import com.vinplay.report.service.impl.CountReportServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

public class LogCountUserApi implements BaseProcessor<HttpServletRequest, String> {

	@Override
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String nickName = request.getParameter("nickname");
		String startDate = request.getParameter("startdate");
		String endDate = request.getParameter("enddate");
		int page = 0;
		try {
			page = Integer.parseInt(request.getParameter("page"));
		} catch (Exception e) {
			page =1;
		}
		int totalrecord = 0;
		try {
			totalrecord = Integer.parseInt(request.getParameter("totalrecord"));
		} catch (Exception e) {
			totalrecord = 10;
		}
		if(page <=0) page = 1;
		if(totalrecord <=10 || totalrecord>100) totalrecord = 10;
		CountReportService service = new CountReportServiceImpl();
		return service.getLogReportModelSQL(nickName, startDate, endDate, page, totalrecord).toJson();
	}
}
