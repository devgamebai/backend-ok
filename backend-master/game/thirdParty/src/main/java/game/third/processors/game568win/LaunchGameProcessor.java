/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package game.third.processors.game568win;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import game.third.processors.common.AuthProcessor;
import game.third.processors.response.LaunchGameResponse;
import game.third.usecase.game568win.entities.UserGame568Win;
import game.third.usecase.game568win.request.Login;
import game.third.usecase.game568win.response.LoginResult;
import game.third.usecase.game568win.service.APIGame568winService;
import game.third.usecase.game568win.service.impl.APIGame568winServiceImpl;
import game.third.usecase.game568win.service.impl.UserGame568WinServiceImpl;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LaunchGameProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private final APIGame568winService apiGame568winService = new APIGame568winServiceImpl();
    private final UserGame568WinServiceImpl userGame568WinService = new UserGame568WinServiceImpl();
    private final String Casino = "casino";
    private final String ThirdPartySportsBook = "ThirdPartySportsBook";
    private final String SeamlessGame = "SeamlessGame";

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        LaunchGameResponse baseResponse = new LaunchGameResponse(false, "1");
        UserModel userService = this.getUser(param);
        if (userService == null) {
            return notAuth;
        }
        try {
            String nickname = userService.getNickname();
            String portfolio = request.getParameter("po");
            if (portfolio == null || portfolio.isEmpty()) {
                baseResponse.setMessage("Portfolio is required");
                return baseResponse.toJson();
            }
            String platform = request.getParameter("pf");
            if (platform.contains("web")) {
                platform = "d";
            }
            if (platform.contains("mobile")) {
                platform = "m";
            }
            if (platform.isEmpty()) {
                platform = "d";
            }
            Login login = new Login();
            login.setUsername(nickname);
            login.setPortfolio(portfolio);
            UserGame568Win user = this.userGame568WinService.checkAndCreateUser(nickname);
            if (user == null) {
                logger.error((Object)("Not create user " + nickname));
                baseResponse.setMessage("Server have error");
                return baseResponse.toJson();
            }
            LoginResult res = this.apiGame568winService.Login(login);
            if (res != null && res.getError().getId() == 0) {
                baseResponse.setSuccess(true);
                baseResponse.setErrorCode("0");
                if ("casino".contains(portfolio.toLowerCase())) {
                    String productID = request.getParameter("pi");
                    if (productID.isEmpty()) {
                        productID = "0";
                    }
                    baseResponse.setUrl(res.getUrl() + "&locale=en&device=" + platform + "&productId=" + productID);
                } else if ("ThirdPartySportsBook".toLowerCase().contains(portfolio.toLowerCase())) {
                    String productID = request.getParameter("pi");
                    if (productID.isEmpty()) {
                        productID = "44";
                    }
                    baseResponse.setUrl(res.getUrl() + "&device=" + platform + "&gpId=" + productID);
                } else if ("SeamlessGame".toLowerCase().contains(portfolio.toLowerCase())) {
                    String productID = request.getParameter("pi");
                    if (productID.isEmpty()) {
                        productID = "44";
                    }
                    baseResponse.setUrl(res.getUrl() + "&device=" + platform + "&gpId=" + productID);
                } else {
                    baseResponse.setUrl(res.getUrl() + "&device=" + platform);
                }
            }
            if (res != null) {
                baseResponse.setMessage(res.getError().getMsg());
            } else {
                baseResponse.setMessage("Server have error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
            baseResponse.setMessage("Server have error: " + e.getMessage());
        }
        return baseResponse.toJson();
    }
}

