/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.hooks.game568win.BaseProcess;
import game.third.hooks.game568win.request.GetBetStatusRequest;
import game.third.hooks.game568win.response.GetBetStatusResponse;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.hook.Param;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.service.Game568winService;
import game.third.utils.Request;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class GetBetStatusProcess
extends BaseProcess {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        GetBetStatusRequest getBetStatusRequest;
        HttpServletRequest request = param.get();
        GetBetStatusResponse response = new GetBetStatusResponse();
        if (!Request.isPost(request)) {
            response.setErrorMessage("MethodNotAllowed");
            response.setErrorCode(7);
            return response.toJson();
        }
        try {
            String line;
            StringBuilder jsonBuilder = new StringBuilder();
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            System.out.println("568Win -> GetBetStatus execute -> " + jsonBuilder.toString());
            getBetStatusRequest = GetBetStatusRequest.fromJson(jsonBuilder.toString());
        }
        catch (IOException e) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (getBetStatusRequest == null) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (getBetStatusRequest.getUsername() == null || getBetStatusRequest.getUsername().isEmpty()) {
            response.setErrorMessage("USERNAME EMPTY");
            response.setErrorCode(3);
            return response.toJson();
        }
        Game568winConfig game568winConfig = ThirdPartyLoad.getGame568winConfig();
        String companyKey = getBetStatusRequest.getCompanyKey();
        if (!GetBetStatusRequest.validate(getBetStatusRequest)) {
            response.setErrorMessage("Not Validate");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (!companyKey.equals(game568winConfig.getCompanyKey())) {
            response.setErrorMessage("companyKey not match");
            response.setErrorCode(4);
            return response.toJson();
        }
        Game568winService game568winService = this.getByProduct(getBetStatusRequest.getProductType());
        try {
            TransactionGame568Win result = game568winService.GetBetStatus(getBetStatusRequest.getTransferCode(), getBetStatusRequest.getTransactionId());
            if (result == null) {
                response.setErrorMessage("GetBetStatus error");
                response.setErrorCode(6);
                return response.toJson();
            }
            switch (result.getStatus()) {
                case Settled: {
                    response.setStatus(Status.Settled.name());
                    break;
                }
                case Running: {
                    response.setStatus(Status.Running.name());
                    break;
                }
                default: {
                    response.setStatus(Status.Void.name());
                }
            }
            response.setErrorMessage("GetBetStatus successfully");
            response.setAccountName(getBetStatusRequest.getUsername());
            response.setStake(result.getCurrentStake());
            response.setTransactionId(result.getTransactionId());
            response.setTransferCode(result.getTransferCode());
            if (result.getReturnAmount() > 0.0) {
                response.setWinLoss(result.getReturnAmount());
            } else {
                response.setWinLoss(result.getAmount());
            }
            response.setErrorCode(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setErrorMessage("GetBetStatus error");
            response.setErrorCode(7);
            return response.toJson();
        }
        return response.toJson();
    }
}

