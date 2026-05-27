package com.vinplay.api.backend.processors.payment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vinplay.payment.dao.WithDrawPaygateDao;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class UpdateWithdrawalStatusProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger("backend");

	private static Map<String, Long> mapCache = new ConcurrentHashMap<String, Long>();

	private static boolean validateRequest(String orderID) {
		if (mapCache.isEmpty()) {
			long t1 = new java.util.Date().getTime();
			mapCache.put(orderID, t1);
		} else {
			if (mapCache.containsKey(orderID)) {

				long t1 = mapCache.get(orderID);
				long t2 = new java.util.Date().getTime();
				if ((t2 - t1) > 1000 * 20) {
					mapCache.put(orderID, t2);
					return true;
				} else {
					return false;
				}

			} else {
				long t1 = new java.util.Date().getTime();
				mapCache.put(orderID, t1);
			}
		}
		return true;
	}
	private static final List<Integer> LST_STATUS = Arrays.asList(new Integer[] {0,1,2,3,5,11,12});
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String approvedName = request.getParameter("nns");
		String orderId = request.getParameter("oid");
		String statusStr = request.getParameter("st");
		String reason = request.getParameter("rs");
		String ip = request.getParameter("ip");
		logger.info("Request update status Withdraw orderId: " + orderId + ", approvedName: " + approvedName + "ip: " + ip);

		if (StringUtils.isBlank(approvedName)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "nickName of staff is null or empty");
		}
		if (StringUtils.isBlank(orderId)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "orderId is null or empty");
		}
		if (StringUtils.isBlank(ip)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "ip is null or empty");
		}
		Integer status = 0;
		try {
			status = Integer.parseInt(statusStr);
		} catch (Exception e) {
			return BaseResponse.error(Constant.ERROR_PARAM, e.getMessage());
		}
		if (!validateRequest(orderId)) {
			return BaseResponse.error(Constant.ERROR_DUPLICATE,
					"Trong 20s chỉ được thao tác order này 1 lần , orderId=" + orderId);
		}
		
		if(!LST_STATUS.contains(status)) {
			return BaseResponse.error(Constant.ERROR_SYSTEM, "Không thể update về trạng thái này");
		}
		WithDrawPaygateDao wdPaygate = new WithDrawPaygateDaoImpl();
		try {
			wdPaygate.UpdateStatus(orderId, status, approvedName, reason);
		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}
		return ip;

	}
	public static void main(String[] args) {
		System.out.println(PayCommon.PAYSTATUS.COMPLETED.getId());
	}
}
