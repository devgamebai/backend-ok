/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  com.google.gson.Gson
 *  com.vinplay.vbee.common.statics.Consts
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.eclipse.jetty.server.Request
 *  org.eclipse.jetty.server.handler.AbstractHandler
 */
package game.Jetty.handle;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import com.google.gson.Gson;
import com.vinplay.vbee.common.statics.Consts;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.Jetty.model.UserOnline;
import game.Jetty.model.UserOnlineResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ListUserOnlineHandler
extends AbstractHandler {
    private String getIpAddress(HttpServletRequest request) {
        String[] arrayIp;
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        String clientIp = null;
        if (ipAddress != null && !"".equals(ipAddress) && (arrayIp = ipAddress.split(",")).length > 0) {
            clientIp = arrayIp[0].trim();
        }
        return clientIp;
    }

    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        String ip = this.getIpAddress((HttpServletRequest)request);
        if (!Consts.IP_SERVER.contains(ip)) {
            return;
        }
        int page = 1;
        int maxItem = 10;
        int count = BitZeroServer.getInstance().getUserManager().getUserCount();
        String nickname = request.getParameter("nickname");
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        int start = (page - 1) * maxItem;
        int end = page * maxItem;
        if (end > count) {
            end = count;
        }
        List<User> user1 = BitZeroServer.getInstance().getUserManager().getAllUsers().subList(start, end);
        Gson gson = new Gson();
        ArrayList<UserOnline> users = new ArrayList<UserOnline>();
        for (User u : user1) {
            boolean isMobile = false;
            boolean isWeb = false;
            if (u.getSession() != null) {
                isMobile = u.getSession().isMobile();
                isWeb = u.getSession().isWebsocket();
            }
            UserOnline userOnline = new UserOnline(u.getId(), u.getName(), u.getIpAddress(), u.getJoinedRoom() == null ? null : u.getJoinedRoom().getName(), isMobile, isWeb, u.getPrivilegeId(), u.getLastLoginTime(), u.getPlayerId(), u.getBadWordsWarnings(), u.getFloodWarnings(), u.isBeingKicked(), u.isConnected(), u.isJoining());
            if (nickname == null || nickname.isEmpty()) {
                users.add(userOnline);
                continue;
            }
            if (userOnline.getName() != nickname) continue;
            users.add(userOnline);
        }
        UserOnlineResponse response = new UserOnlineResponse(users, count);
        JettyUtils.send(request, httpServletResponse, new JettyResponse(0, gson.toJson(response)));
    }
}

