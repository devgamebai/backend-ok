/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.http.util.TextUtils
 *  org.eclipse.jetty.server.Request
 *  org.eclipse.jetty.server.handler.AbstractHandler
 */
package game.Jetty.handle;

import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import game.Jetty.FunctionUtils;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.modules.lobby.cmd.send.GetInfoMsg;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.util.TextUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class UpdateMoneyNowHandler
extends AbstractHandler {
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        String input = request.getQueryString();
        Map<String, String> params = FunctionUtils.splitQuery(input);
        String data = params.get("data");
        if (TextUtils.isEmpty((CharSequence)data)) {
            JettyUtils.send(request, httpServletResponse, new JettyResponse(98, "Kh\u00f4ng c\u00f3 nickName"));
            return;
        }
        List users = ExtensionUtility.getExtension().getApi().getUserByName(data);
        if (users != null) {
            ExtensionUtility.getExtension().sendUsers((BaseMsg)this.getInfo(data), users);
            JettyUtils.send(request, httpServletResponse, new JettyResponse(0, "G\u1eedi th\u00e0nh c\u00f4ng"));
        } else {
            JettyUtils.send(request, httpServletResponse, new JettyResponse(98, "Kh\u00f4ng t\u00ecm \u0111\u01b0\u1ee3c nickName"));
        }
    }

    private GetInfoMsg getInfo(String nickName) {
        GetInfoMsg msg;
        block5: {
            msg = new GetInfoMsg();
            UserServiceImpl userService = new UserServiceImpl();
            UserCacheModel userCache = userService.getUser(nickName);
            try {
                if (userCache != null) {
                    msg.Error = 0;
                    msg.isVerifyPhone = userCache.isVerifyMobile();
                    msg.username = userCache.getUsername();
                    msg.cmt = userCache.getIdentification() != null ? userCache.getIdentification() : "";
                    msg.mobile = userCache.getMobile() != null ? userCache.getMobile() : "";
                    msg.email = userCache.getEmail() != null ? userCache.getEmail() : "";
                    msg.mobileSecure = userCache.isHasMobileSecurity() ? (byte)1 : 0;
                    msg.emailSecure = userCache.isHasEmailSecurity() ? (byte)1 : 0;
                    msg.appSecure = userCache.isHasAppSecurity() ? (byte)1 : 0;
                    msg.loginSecure = userCache.isHasLoginSecurity() ? (byte)1 : 0;
                    msg.moneyLoginotp = userCache.getLoginOtp();
                    msg.usertype = userCache.getUsertype();
                    msg.configGame = "";
                    msg.referralCode = userCache.getReferralCode() == null ? "" : userCache.getReferralCode();
                    msg.birthday = userCache.getBirthday() == null ? "" : userCache.getBirthday();
                    msg.address = userCache.getAddress() == null ? "" : userCache.getAddress();
                    try {
                        msg.gender = userCache.isGender();
                    }
                    catch (Exception e) {
                        msg.gender = false;
                    }
                    msg.safe = userCache.getSafe();
                    msg.moneyUse = userCache.getVin();
                    msg.userId = userCache.getId();
                    break block5;
                }
                msg.Error = 1;
            }
            catch (Exception e) {
                Debug.trace((Object[])new Object[]{"LobbyModule get info error: " + e});
                msg.Error = 1;
            }
        }
        return msg;
    }
}

