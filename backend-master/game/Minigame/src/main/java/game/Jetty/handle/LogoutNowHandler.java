/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import game.Jetty.FunctionUtils;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.modules.lobby.cmd.send.LogoutMsg;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.util.TextUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class LogoutNowHandler
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
            ExtensionUtility.getExtension().sendUsers((BaseMsg)this.logout(data), users);
            JettyUtils.send(request, httpServletResponse, new JettyResponse(0, "G\u1eedi th\u00e0nh c\u00f4ng"));
            try {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = client.getMap("users");
                userMap.remove(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            JettyUtils.send(request, httpServletResponse, new JettyResponse(98, "Kh\u00f4ng t\u00ecm \u0111\u01b0\u1ee3c nickName"));
        }
    }

    private LogoutMsg logout(String nickName) {
        LogoutMsg msg = new LogoutMsg();
        try {
            msg.Error = 0;
            msg.username = nickName;
        }
        catch (Exception e) {
            msg.Error = 1;
        }
        return msg;
    }
}

