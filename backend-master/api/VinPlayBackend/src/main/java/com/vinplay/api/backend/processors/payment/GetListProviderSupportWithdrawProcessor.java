package com.vinplay.api.backend.processors.payment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.payment.dao.WithDrawPaygateDao;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.service.PaymentConfigService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.payment.utils.PaymentConstant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class GetListProviderSupportWithdrawProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger = Logger.getLogger(GetListProviderSupportWithdrawProcessor.class);

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String approvedName = request.getParameter("nns");
		String orderId = request.getParameter("oid");
		
		logger.info("Request GetListProviderSupportWithdrawProcessor approvedName= " + approvedName + ", orderId: " + orderId);
		
		if (StringUtils.isBlank(approvedName)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "nickName of staff is null or empty");
		}
		if (StringUtils.isBlank(orderId)) {
			return BaseResponse.error(Constant.ERROR_PARAM, "orderId is null or empty");
		}
		
		WithDrawPaygateDao withdrawDAO = new WithDrawPaygateDaoImpl();
		WithDrawPaygateModel withdrawModel = withdrawDAO.GetByOrderId(orderId);
		if(withdrawModel == null) {
			return BaseResponse.error(Constant.ERROR_SYSTEM, "orderId not exist");
		}
		PaymentConfigService paymentConfigService = new PaymentConfigServiceImpl();
		try {
			
			List<PaymentConfig> lstPConfigs = paymentConfigService.getConfig();
			Set<String> uniProvider = new HashSet<String>();
			
			for (PaymentConfig pm : lstPConfigs) {
				if(!PaymentConstant.PROVIDER.PAYWELL.equals(pm.getName())&& !PaymentConstant.PROVIDER.MANUAL_BANK.equals(pm.getName())) {
					List<BankConfig> lstBankCf = pm.getConfig().getBanks();
					if (lstBankCf != null) {
						for (BankConfig bc : lstBankCf) {
							//compare name in config with 
							if ((bc.getName().equalsIgnoreCase(withdrawModel.BankCode) || bc.getKey().equalsIgnoreCase(withdrawModel.BankCode))
									&& bc.getIsWithdraw() == 1) {
								uniProvider.add(pm.getName());
								break;
							}
						}
					}
				}
			}
			uniProvider.add("manualbank");
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = ow.writeValueAsString(uniProvider);
			return new BaseResponse<String>().success(json);
		} catch (Exception e) {
			logger.error(e);
			return BaseResponse.error(Constant.ERROR_SYSTEM, e.getMessage());
		}

	}
}
