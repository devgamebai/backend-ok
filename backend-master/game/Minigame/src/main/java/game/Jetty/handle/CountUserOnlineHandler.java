/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.eclipse.jetty.server.Request
 *  org.eclipse.jetty.server.handler.AbstractHandler
 */
package game.Jetty.handle;

import bitzero.server.BitZeroServer;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class CountUserOnlineHandler
extends AbstractHandler {
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        int ccu = BitZeroServer.getInstance().getUserManager().getUserCount();
        JettyUtils.send(request, httpServletResponse, new JettyResponse(0, ccu + ""));
    }
}

