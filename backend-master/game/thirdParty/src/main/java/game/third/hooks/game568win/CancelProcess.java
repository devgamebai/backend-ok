/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.hooks.game568win.BaseProcess;
import game.third.hooks.game568win.request.CancelRequest;
import game.third.hooks.game568win.response.BalanceResponse;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.hook.Param;
import game.third.usecase.game568win.service.Game568winService;
import game.third.usecase.service.impl.UserMoneyServiceImpl;
import game.third.utils.Request;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class CancelProcess
extends BaseProcess {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        CancelRequest cancelRequest;
        HttpServletRequest request = param.get();
        BalanceResponse response = new BalanceResponse();
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
            System.out.println("568Win -> Cancel execute -> " + jsonBuilder.toString());
            cancelRequest = CancelRequest.fromJson(jsonBuilder.toString());
        }
        catch (IOException e) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (cancelRequest == null) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        Game568winConfig game568winConfig = ThirdPartyLoad.getGame568winConfig();
        String companyKey = cancelRequest.getCompanyKey();
        if (!CancelRequest.validate(cancelRequest)) {
            response.setErrorMessage("Not Validate");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (!companyKey.equals(game568winConfig.getCompanyKey())) {
            response.setErrorMessage("companyKey not match");
            response.setErrorCode(4);
            return response.toJson();
        }
        Game568winService game568winService = this.getByProduct(cancelRequest.getProductType());
        UserMoneyServiceImpl userMoneyService = new UserMoneyServiceImpl();
        try {
            int result = game568winService.Cancel(cancelRequest.toCancelData());
            if (result == -1) {
                response.setErrorMessage("Not found transaction");
                response.setErrorCode(6);
                return response.toJson();
            }
            if (result == -2) {
                response.setErrorMessage("BET ALREADY CANCELED");
                response.setErrorCode(2002);
                return response.toJson();
            }
            if (result == 0) {
                response.setErrorMessage("Cancel error");
                response.setErrorCode(7);
                return response.toJson();
            }
            long balance = userMoneyService.getBalance(cancelRequest.getUsername());
            response.setBalance(balance);
            response.setErrorMessage("Cancel successfully");
            response.setAccountName(cancelRequest.getUsername());
            response.setErrorCode(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setErrorMessage("Cancel error");
            response.setErrorCode(7);
            return response.toJson();
        }
        return response.toJson();
    }
}

