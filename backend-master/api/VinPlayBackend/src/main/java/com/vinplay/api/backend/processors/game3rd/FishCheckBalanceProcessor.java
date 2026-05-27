package com.vinplay.api.backend.processors.game3rd;

import javax.servlet.http.HttpServletRequest;
import com.vinplay.payment.utils.Constant;
import com.vinplay.utils.ShotFishUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

public class FishCheckBalanceProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
    	HttpServletRequest request = (HttpServletRequest) param.get();
		String nickname = request.getParameter("nn");
		if(nickname == null || nickname.trim().isEmpty())
			return BaseResponse.error(Constant.ERROR_PARAM, "Nickname is not empty");
		
		return ShotFishUtils.CheckUserBalance(nickname).toJson();
    }
}