/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.hooks.game568win.BaseProcess;
import game.third.hooks.game568win.request.DeductRequest;
import game.third.hooks.game568win.response.DeductResponse;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.hook.Param;
import game.third.usecase.game568win.response.DeductResult;
import game.third.usecase.game568win.service.Game568winService;
import game.third.utils.Request;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class DeductProcess
extends BaseProcess {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        DeductRequest deductRequest;
        HttpServletRequest request = param.get();
        DeductResponse response = new DeductResponse();
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
            System.out.println("568Win -> Deduct execute -> " + jsonBuilder.toString());
            deductRequest = DeductRequest.fromJson(jsonBuilder.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (deductRequest == null) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        Game568winConfig game568winConfig = ThirdPartyLoad.getGame568winConfig();
        String companyKey = deductRequest.getCompanyKey();
        if (!DeductRequest.validate(deductRequest)) {
            response.setErrorMessage("Not Validate");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (!companyKey.equals(game568winConfig.getCompanyKey())) {
            response.setErrorMessage("companyKey not match");
            response.setErrorCode(4);
            return response.toJson();
        }
        Game568winService game568winService = this.getByProduct(deductRequest.getProductType());
        try {
            DeductResult result = game568winService.Deduct(deductRequest.getTransaction());
            response.setBetAmount((long)deductRequest.getAmount());
            response.setAccountName(deductRequest.getUsername());
            if (!result.isSuccess()) {
                if (result.getResult() == -1) {
                    response.setErrorCode(5003);
                    response.setErrorMessage("Deduct have exits");
                }
                if (result.getResult() == -2) {
                    response.setErrorCode(5);
                    response.setErrorMessage("NOT ENOUGH BALANCE");
                }
                if (result.getResult() == -3) {
                    response.setErrorCode(7);
                    response.setErrorMessage("INTERNAL ERROR BET");
                }
            } else {
                response.setBalance(result.getBalance());
                response.setErrorMessage("Deduct successfully");
                response.setErrorCode(0);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setErrorMessage("Deduct error");
            response.setErrorCode(7);
            return response.toJson();
        }
        return response.toJson();
    }
}

