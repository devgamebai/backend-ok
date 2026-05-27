/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.hooks.game568win.BaseProcess;
import game.third.hooks.game568win.request.GetBalanceRequest;
import game.third.hooks.game568win.response.BalanceResponse;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.hook.Param;
import game.third.usecase.service.impl.UserMoneyServiceImpl;
import game.third.utils.Request;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class GetBalanceProcess
extends BaseProcess {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        GetBalanceRequest balanceRequest;
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
            System.out.println("568Win -> BalanceProcess execute -> " + jsonBuilder.toString());
            balanceRequest = GetBalanceRequest.fromJson(jsonBuilder.toString());
        }
        catch (IOException e) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (balanceRequest == null) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        Game568winConfig game568winConfig = ThirdPartyLoad.getGame568winConfig();
        UserMoneyServiceImpl userMoneyService = new UserMoneyServiceImpl();
        String companyKey = balanceRequest.getCompanyKey();
        String username = balanceRequest.getUsername();
        if (!GetBalanceRequest.validate(balanceRequest)) {
            response.setErrorMessage("Not Validate");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (!companyKey.equals(game568winConfig.getCompanyKey())) {
            response.setErrorMessage("companyKey not match");
            response.setErrorCode(4);
            return response.toJson();
        }
        boolean haveUser = userMoneyService.isUser(username);
        if (!haveUser) {
            response.setErrorMessage("User not found");
            response.setErrorCode(1);
            return response.toJson();
        }
        long balance = userMoneyService.getBalance(username);
        System.out.println("568Win -> GetBalanceProcess ->  " + username + " => " + balance);
        response.setErrorMessage("Balance retrieved successfully");
        response.setBalance(balance);
        response.setAccountName(username);
        response.setErrorCode(0);
        return response.toJson();
    }
}

