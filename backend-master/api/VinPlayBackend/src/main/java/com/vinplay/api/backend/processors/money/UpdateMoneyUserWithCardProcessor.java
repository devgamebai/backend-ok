package com.vinplay.api.backend.processors.money;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.api.backend.response.TransferMoneyBankSmsResponse;

import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.service.impl.RechargeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.response.UpdatePendingCardResponse;
import org.apache.log4j.Logger;


import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

public class UpdateMoneyUserWithCardProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        int code = 1;
        UpdatePendingCardResponse response = new UpdatePendingCardResponse(false, "1001");
        try {
            String status = request.getParameter("status");
            String message = request.getParameter("message");
            String request_id = request.getParameter("request_id");
            String declared_value = request.getParameter("declared_value");
            String amount = request.getParameter("amount");
            String code_cart = request.getParameter("code");
            String serial = request.getParameter("serial");
            String telco = request.getParameter("telco");
            String trans_id = request.getParameter("trans_id");
            String callback_sign = request.getParameter("callback_sign");


            logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  status: " + status + "ime wrong: " + message + "," + request_id+ "," + trans_id+ "," + amount+ "," + declared_value+ "," + message+ "," + telco+ "," + code_cart+ "," + serial+ "," + callback_sign + "," + request.getParameterNames().toString()));
            if(request_id.isEmpty() || serial.isEmpty() || code_cart.isEmpty() || trans_id.isEmpty() || telco.isEmpty() || status.isEmpty()){
                logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  status: " + status + "ime wrong: " + message + "," + request_id+ "," + trans_id+ "," + amount+ "," + declared_value+ "," + message+ "," + telco+ "," + code_cart+ "," + serial+ "," + callback_sign));
            }

            RechargeDaoImpl dao = new RechargeDaoImpl();
            String actor = "bankingttbv";
            RechargeByCardMessage pendingCard = dao.getPendingCardByReferenceId(request_id);
            if (pendingCard != null) {
//                if(!pendingCard.getTranId().equals(trans_id)){
//                    logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  id: " + request_id + "," + trans_id + "," + pendingCard.getTranId()));
//                    return response.toJson();
//                }
                if(!pendingCard.getSerial().equals(serial)){
                    logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  id: " + request_id + "," + serial + "," + pendingCard.getSerial()));
                    return response.toJson();
                }
                if(!pendingCard.getPin().equals(code_cart)){
                    logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  id: " + request_id + "," + code + "," + pendingCard.getPin()));
                    return response.toJson();
                }
                RechargeByCardMessage rechargeByCardMessage = new RechargeByCardMessage();
                RechargeServiceImpl service = new RechargeServiceImpl();
                if(status.equals("1") && pendingCard.getStatus() == 30){
                    try {
                        rechargeByCardMessage = service.updatePendingCardStatus(request_id, actor);

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        return response.toJson();
                    }
                    if (rechargeByCardMessage == null) {
                        response.setRechargeByCardMessage(rechargeByCardMessage);
                        response.setSuccess(false);
                        response.setErrorCode("1036");
                    } else if (rechargeByCardMessage.getError() != null) {
                        response.setRechargeByCardMessage(rechargeByCardMessage);
                        response.setSuccess(false);
                        response.setErrorCode(rechargeByCardMessage.getError());
                    } else {
                        ArrayList<String> listUser = new ArrayList<>();
                        listUser.add(pendingCard.getNickname());
                        listUser.add(actor);
                        BackendUtils.sendUpdateUserMoneyInfo(listUser);
                        response.setRechargeByCardMessage(rechargeByCardMessage);
                        response.setSuccess(true);
                        response.setErrorCode("0");
                    }
                    return response.toJson();
                }else{

                    if(pendingCard.getStatus() == 30){
                        dao.updateRechargeByCard(request_id, status, message, amount);
                    }

                    logger.debug((Object) ("Request UpdateMoneyUserWithCardProcessor:  id: " + request_id + "," + status + "," + pendingCard.getStatus()));
                    return response.toJson();
                }
            }

        }catch (Exception e) {
            System.out.format("'%s'\n", e);
            logger.debug((Object) e);
        }
        logger.debug((Object) ("Response UpdateMoneyUser: " + response.toJson()));
        return response.toJson();
    }
}
