/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 */
package game.third.server;

import game.third.usecase.core.hook.HookController;
import game.third.usecase.core.hook.NoHookRegistered;
import game.third.usecase.core.hook.NoWhitelistRegistered;
import game.third.usecase.core.hook.Param;
import game.third.utils.Request;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class HookServlet
extends HttpServlet {
    private final HookController<HttpServletRequest, String> hookController;
    private static final Logger logger = Logger.getLogger((String)"thirdPart");
    private static final long serialVersionUID = 1L;

    public HookServlet(HookController<HttpServletRequest, String> hookController) {
        this.hookController = hookController;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        String remoteAddr = Request.getIpAddress(request);
        logger.info((Object)String.format("remoteAddr: %s - %s - %s ", remoteAddr, request.getRemoteAddr(), request.getQueryString()));
        Param<HttpServletRequest> param = new Param<HttpServletRequest>();
        param.set(request);
        param.setStatus(200);
        param.setContentType("application/json");
        try {
            String responseString = this.hookController.processHook(request.getPathInfo(), remoteAddr, param);
            response.setContentType(param.getContentType());
            response.setStatus(param.getStatus());
            response.getWriter().println(responseString);
        }
        catch (NoHookRegistered e1) {
            logger.debug((Object)"HOOK NOT FOUND");
            response.getWriter().println(e1.getMessage());
        }
        catch (NoWhitelistRegistered e2) {
            logger.debug((Object)"Whitelist NOT FOUND");
            response.getWriter().println(e2.getMessage());
        }
        catch (Exception e3) {
            e3.printStackTrace();
            logger.error((Object)e3);
            response.getWriter().println("EXCEPTION: " + e3.getMessage());
        }
    }
}

