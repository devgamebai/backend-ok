/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.cp.NoCommandRegistered
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.pay.server;

import com.vinplay.pay.server.JettyServer;
import com.vinplay.utils.RequestUtil;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class APIPaymentServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(JettyServer.class);
    private static final long serialVersionUID = 1L;
    private final BaseController<HttpServletRequest, String> controller;

    public APIPaymentServlet(BaseController<HttpServletRequest, String> controller) {
        this.controller = controller;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        Map requestMap = request.getParameterMap();
        String remoteAddr = RequestUtil.getIpAddress(request);
        logger.info((Object)String.format("remoteAddr: %s - %s - %s ", remoteAddr, request.getRemoteAddr(), request.getQueryString()));
        if (requestMap.containsKey("c")) {
            String command = request.getParameter("c");
            if (command == null || command.trim().isEmpty()) {
                logger.debug((Object)"COMMAND NOT FOUND");
                response.getWriter().println("COMMAND NOT FOUND");
                return;
            }
            Param param = new Param();
            param.set((Object)request);
            try {
                String responseString = (String)this.controller.processCommand(Integer.valueOf(Integer.parseInt(command)), param);
                response.getWriter().println(responseString);
            }
            catch (NoCommandRegistered e2) {
                logger.debug((Object)"COMMAND NOT FOUND");
                response.getWriter().println("COMMAND NOT FOUND");
                return;
            }
            catch (Exception e1) {
                e1.printStackTrace();
                logger.error((Object)e1);
                if (this.controller == null) {
                    logger.info((Object)"controller null");
                    response.getWriter().println("EXCEPTION: controller - " + e1.getMessage() + " | " + request.getQueryString());
                    return;
                }
                response.getWriter().println("EXCEPTION: " + e1.getMessage() + " | " + request.getQueryString());
                return;
            }
        } else {
            response.getWriter().println("NO COMMANDS PARAMETERS");
            return;
        }
    }
}

