/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.hooks.game568win.BaseProcess;
import game.third.hooks.game568win.request.BonusRequest;
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

public class BonusProcess
extends BaseProcess {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        BonusRequest bonusRequest;
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
            System.out.println("568Win -> Bonus execute -> " + jsonBuilder.toString());
            bonusRequest = BonusRequest.fromJson(jsonBuilder.toString());
        }
        catch (IOException e) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (bonusRequest == null) {
            response.setErrorMessage("Invalid JSON format");
            response.setErrorCode(7);
            return response.toJson();
        }
        Game568winConfig game568winConfig = ThirdPartyLoad.getGame568winConfig();
        String companyKey = bonusRequest.getCompanyKey();
        if (!BonusRequest.validate(bonusRequest)) {
            response.setErrorMessage("Not Validate");
            response.setErrorCode(7);
            return response.toJson();
        }
        if (!companyKey.equals(game568winConfig.getCompanyKey())) {
            response.setErrorMessage("companyKey not match");
            response.setErrorCode(4);
            return response.toJson();
        }
        Game568winService game568winService = this.getByProduct(bonusRequest.getProductType());
        UserMoneyServiceImpl userMoneyService = new UserMoneyServiceImpl();
        boolean haveUser = userMoneyService.isUser(bonusRequest.getUsername());
        if (!haveUser) {
            response.setErrorMessage("User not found");
            response.setErrorCode(1);
            return response.toJson();
        }
        try {
            int result = game568winService.Bonus(bonusRequest.getBonus());
            if (result == -1) {
                response.setErrorMessage("BET WITH SAME REFNO EXISTS");
                response.setErrorCode(5003);
                return response.toJson();
            }
            if (result == 0) {
                response.setErrorMessage("Bonus error");
                response.setErrorCode(7);
                return response.toJson();
            }
            long balance = userMoneyService.getBalance(bonusRequest.getUsername());
            response.setBalance(balance);
            response.setErrorMessage("Bonus successfully");
            response.setAccountName(bonusRequest.getUsername());
            response.setErrorCode(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setErrorMessage("Bonus error");
            response.setErrorCode(7);
            return response.toJson();
        }
        return response.toJson();
    }
}

