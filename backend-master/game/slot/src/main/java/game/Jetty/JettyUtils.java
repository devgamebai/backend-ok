package game.Jetty;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.Jetty.handle.AdminCPHandler;
import game.Jetty.handle.ChangeConfigCPHandler;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class JettyUtils {

//    private static JettyUtils __instance;
//
//    private JettyUtils() {
//
//    }
//
//    public static JettyUtils getInstance() {
//        if (__instance == null) {
//            __instance = new JettyUtils();
//        }
//        return __instance;
//    }

    public static void jettyInit() {
        try {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMaxThreads(10);
            // Server
            Server server = new Server(threadPool);
            server.addBean(new ScheduledExecutorScheduler());

            HttpConfiguration http_config = new HttpConfiguration();
//            http_config.setSecureScheme("https");
//            http_config.setSecurePort(8443);
            http_config.setOutputBufferSize(32768);
            http_config.setRequestHeaderSize(8192);
            http_config.setResponseHeaderSize(8192);
            http_config.setSendServerVersion(true);
            http_config.setSendDateHeader(false);
            ServerConnector http = new ServerConnector(server);
            http.setHost(ConfigHandle.instance().get("jetty-ip"));
            http.setIdleTimeout(30000);
            http.setPort(Integer.parseInt(ConfigHandle.instance().get("jetty-port")));
//            http.setPort(16070);
            server.addConnector(http);

            ContextHandler adminContext = new ContextHandler();
            adminContext.setContextPath("/admin-handle");
            adminContext.setAllowNullPathInfo(true);
            adminContext.setHandler(new AdminCPHandler());

            ContextHandler changeConfigContext = new ContextHandler();
            changeConfigContext.setContextPath("/change-config");
            changeConfigContext.setAllowNullPathInfo(true);
            changeConfigContext.setHandler(new ChangeConfigCPHandler());


            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{adminContext, changeConfigContext});

            server.setHandler(handlers);

            server.start();
            server.dumpStdErr();
            server.join();
//
//            Server server = new Server(8080);
//            server.setHandler(new HelloHandler());
//
//            server.start();
//            server.join();
        } catch (Exception ex) {
            Debug.trace("Jetty crash");
            Debug.trace(ex);
        }

    }

    public static void send(Request baseRequest, HttpServletResponse response, JettyResponse jettyResponse) {
        try {
            response.setContentType("application/json; charset=utf-8");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "origin, x-requested-with, content-type");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.println(GameConfig.gson.toJson(jettyResponse));
            baseRequest.setHandled(true);
        } catch (IOException ex) {
            Debug.trace(ex);
        }

    }

    public static long getLong(String num) {
        if (NumberUtils.isNumber(num)) {
            return Long.valueOf(num);
        }
        return -1;
    }

    public static int getInt(String num) {
        if (NumberUtils.isNumber(num)) {
            return Integer.valueOf(num);
        }
        return -1;
    }

    public static short getShort(String num) {
        if (NumberUtils.isNumber(num)) {
            return Short.valueOf(num);
        }
        return -1;
    }

    public static byte getByte(String num) {
        if (NumberUtils.isNumber(num)) {
            return Byte.valueOf(num);
        }
        return -1;
    }

    public static double getDouble(String num) {
        if (NumberUtils.isNumber(num)) {
            return Double.valueOf(num);
        }
        return -1;
    }


}
