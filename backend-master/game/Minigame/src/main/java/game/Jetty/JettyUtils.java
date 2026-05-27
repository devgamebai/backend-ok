/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.config.ConfigHandle
 *  bitzero.util.common.business.Debug
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.commons.lang3.math.NumberUtils
 *  org.eclipse.jetty.server.Connector
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.HttpConfiguration
 *  org.eclipse.jetty.server.Request
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.server.ServerConnector
 *  org.eclipse.jetty.server.handler.ContextHandler
 *  org.eclipse.jetty.server.handler.HandlerList
 *  org.eclipse.jetty.util.thread.QueuedThreadPool
 *  org.eclipse.jetty.util.thread.ScheduledExecutorScheduler
 *  org.eclipse.jetty.util.thread.ThreadPool
 */
package game.Jetty;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.Jetty.JettyResponse;
import game.Jetty.handle.AdminCPHandler;
import game.Jetty.handle.ChangeConfigCPHandler;
import game.Jetty.handle.CountUserOnlineHandler;
import game.Jetty.handle.ListUserOnlineHandler;
import game.Jetty.handle.LogoutNowHandler;
import game.Jetty.handle.UpdateMoneyNowHandler;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

public class JettyUtils {
    public static void jettyInit() {
        try {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMinThreads(20);
            threadPool.setMaxThreads(200);
            threadPool.setIdleTimeout(60000);
            threadPool.setName("minigame-pool");
            Server server = new Server((ThreadPool)threadPool);
            server.addBean(new ScheduledExecutorScheduler());
            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setOutputBufferSize(32768);
            http_config.setRequestHeaderSize(8192);
            http_config.setResponseHeaderSize(8192);
            http_config.setSendServerVersion(true);
            http_config.setSendDateHeader(false);
            ServerConnector http = new ServerConnector(server);
            http.setHost(ConfigHandle.instance().get("jetty-ip"));
            http.setIdleTimeout(30000L);
            http.setPort(Integer.parseInt(ConfigHandle.instance().get("jetty-port")));
            server.addConnector((Connector)http);
            ContextHandler adminContext = new ContextHandler();
            adminContext.setContextPath("/admin-handle");
            adminContext.setAllowNullPathInfo(true);
            adminContext.setHandler((Handler)new AdminCPHandler());
            ContextHandler changeConfigContext = new ContextHandler();
            changeConfigContext.setContextPath("/change-config");
            changeConfigContext.setAllowNullPathInfo(true);
            changeConfigContext.setHandler((Handler)new ChangeConfigCPHandler());
            ContextHandler countUserOnlineContext = new ContextHandler();
            countUserOnlineContext.setContextPath("/count-useronline");
            countUserOnlineContext.setAllowNullPathInfo(true);
            countUserOnlineContext.setHandler((Handler)new CountUserOnlineHandler());
            ContextHandler listUserOnlineContext = new ContextHandler();
            listUserOnlineContext.setContextPath("/list-useronline");
            listUserOnlineContext.setAllowNullPathInfo(true);
            listUserOnlineContext.setHandler((Handler)new ListUserOnlineHandler());
            ContextHandler userMoneyNow = new ContextHandler();
            userMoneyNow.setContextPath("/user-money-now");
            userMoneyNow.setAllowNullPathInfo(true);
            userMoneyNow.setHandler((Handler)new UpdateMoneyNowHandler());
            ContextHandler logout = new ContextHandler();
            logout.setContextPath("/log-out-now");
            logout.setAllowNullPathInfo(true);
            logout.setHandler((Handler)new LogoutNowHandler());
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{adminContext, changeConfigContext, countUserOnlineContext, listUserOnlineContext, logout, userMoneyNow, logout});
            server.setHandler((Handler)handlers);
            server.start();
            server.dumpStdErr();
            server.join();
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"Jetty crash"});
            Debug.trace((Object[])new Object[]{ex});
        }
    }

    public static void send(Request baseRequest, HttpServletResponse response, JettyResponse jettyResponse) {
        try {
            response.setContentType("application/json; charset=utf-8");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "origin, x-requested-with, content-type");
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.println(GameConfig.gson.toJson(jettyResponse));
            baseRequest.setHandled(true);
        }
        catch (IOException ex) {
            Debug.trace((Object[])new Object[]{ex});
        }
    }

    public static long getLong(String num) {
        if (NumberUtils.isNumber((String)num)) {
            return Long.valueOf(num);
        }
        return -1L;
    }

    public static int getInt(String num) {
        if (NumberUtils.isNumber((String)num)) {
            return Integer.valueOf(num);
        }
        return -1;
    }

    public static short getShort(String num) {
        if (NumberUtils.isNumber((String)num)) {
            return Short.valueOf(num);
        }
        return -1;
    }

    public static byte getByte(String num) {
        if (NumberUtils.isNumber((String)num)) {
            return Byte.valueOf(num);
        }
        return -1;
    }

    public static double getDouble(String num) {
        if (NumberUtils.isNumber((String)num)) {
            return Double.valueOf(num);
        }
        return -1.0;
    }
}

