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
package game.third.server;

import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import game.third.utils.Request;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class GameServlet
extends HttpServlet {
    private final BaseController<HttpServletRequest, String> controller;
    private static final Logger logger = Logger.getLogger((String)"thirdPart");
    private static final long serialVersionUID = 1L;

    public GameServlet(BaseController<HttpServletRequest, String> controller) {
        this.controller = controller;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        block6: {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            Map requestMap = request.getParameterMap();
            String remoteAddr = Request.getIpAddress(request);
            logger.info((Object)String.format("remoteAddr: %s - %s - %s ", remoteAddr, request.getRemoteAddr(), request.getQueryString()));
            if (requestMap.containsKey("c")) {
                String command = request.getParameter("c");
                Param param = new Param();
                param.set((Object)request);
                try {
                    String responseString = (String)this.controller.processCommand(Integer.valueOf(command), param);
                    response.getWriter().println(responseString);
                }
                catch (NoCommandRegistered e2) {
                    logger.debug((Object)"COMMAND NOT FOUND");
                    response.getWriter().println("COMMAND NOT FOUND");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    logger.error((Object)e1);
                    if (this.controller == null) {
                        logger.info((Object)"controller null");
                        response.getWriter().println("EXCEPTION: controller - " + e1.getMessage() + " | " + request.getQueryString());
                        break block6;
                    }
                    response.getWriter().println("EXCEPTION: " + e1.getMessage() + " | " + request.getQueryString());
                }
            } else {
                response.getWriter().println("NO COMMANDS PARAMETERS");
            }
        }
    }
}

