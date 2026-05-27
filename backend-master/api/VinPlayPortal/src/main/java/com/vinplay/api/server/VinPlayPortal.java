/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.LogPortalService
 *  com.vinplay.dal.service.impl.LogPortalServiceImpl
 *  com.vinplay.dal.utils.PotUtils
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.cp.NoCommandRegistered
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  javax.servlet.DispatcherType
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 *  org.eclipse.jetty.server.ConnectionFactory
 *  org.eclipse.jetty.server.Connector
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.HttpConfiguration
 *  org.eclipse.jetty.server.HttpConfiguration$Customizer
 *  org.eclipse.jetty.server.HttpConnectionFactory
 *  org.eclipse.jetty.server.SecureRequestCustomizer
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.server.ServerConnector
 *  org.eclipse.jetty.server.SslConnectionFactory
 *  org.eclipse.jetty.server.handler.HandlerCollection
 *  org.eclipse.jetty.servlet.FilterHolder
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.servlet.ServletHolder
 *  org.eclipse.jetty.util.ssl.SslContextFactory
 */
package com.vinplay.api.server;

import com.vinplay.api.processors.vippoint.TopVippoint;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.api.ws.BalanceWebSocketServlet;
import com.vinplay.api.ws.PortalBalanceConsumer;
import com.vinplay.dal.service.LogPortalService;
import com.vinplay.dal.service.impl.LogPortalServiceImpl;
import com.vinplay.dal.utils.PotUtils;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.UserValidaton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VinPlayPortal {
    private static final Logger logger = Logger.getLogger(VinPlayPortal.class);
    private static final Logger blackListIpLogger = Logger.getLogger((String)"BlackListIpLogger");
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static LogPortalService service = new LogPortalServiceImpl();
    private static String API_PORT = "8081";
    private static String SSL_PORT = "8443";
    private static BaseController<HttpServletRequest, String> controller;
    private static String basePath;

    private static void initializeLogger() {
        Properties logProperties = new Properties();
        try {
            File file = new File(basePath.concat(LOG_PROPERTIES_FILE));
            logProperties.load(new FileInputStream(file));
            PropertyConfigurator.configure((Properties)logProperties);
            logger.info("Logging initialized.");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load logging property config/log4j.properties");
        }
    }

    public static void main(String[] args) {
        try {
            basePath = VBeePath.initBasePath(VinPlayPortal.class);
            VinPlayPortal.initializeLogger();
            logger.debug("STARTING PORTAL API SERVER .... !!!!");
            VinPlayPortal.loadCommands();
            RMQApi.start("config/rmq.properties");
            HazelcastLoader.start();
            // SUN-832: kick /ws/balance sockets whose accessToken gets revoked.
            // Uses the shared VbeeCommon SessionKickListener which ALSO handles
            // Hazelcast client-reconnect re-registration. Hazelcast 3.12 may
            // issue a new client UUID after a heartbeat timeout; server-side
            // listener registrations are keyed by UUID and go silent without
            // the re-register logic in SessionKickListener.
            try {
                com.vinplay.vbee.common.session.SessionKickListener.register(
                    new com.vinplay.vbee.common.session.SessionKickListener.KickHandler() {
                        @Override
                        public void kick(String accessToken, String nickname, String reason) {
                            try {
                                com.vinplay.api.ws.BalanceWebSocketServlet.kickByToken(
                                        accessToken, reason);
                            } catch (Throwable t) {
                                logger.warn("portal WS kickByToken failed: " + t.getMessage());
                            }
                        }
                    });
                logger.info("PORTAL API: cacheToken kick listener registered for /ws/balance");
            } catch (Exception sxe) {
                logger.warn("PORTAL API: ws-balance kick listener setup failed: " + sxe.getMessage());
            }
            MongoDBConnectionFactory.init();
            try { UserValidaton.init(); } catch (Exception e) { logger.warn("UserValidaton.init() failed (non-fatal): " + e.getMessage()); }
            try { PortalUtils.loadGameConfig(); } catch (Exception e) { logger.warn("loadGameConfig() failed (non-fatal): " + e.getMessage()); }
            try { PotUtils.init(); } catch (Exception e) { logger.warn("PotUtils.init() failed (non-fatal): " + e.getMessage()); }
            try { TopVippoint.init(); } catch (Exception e) { logger.warn("TopVippoint.init() failed (non-fatal): " + e.getMessage()); }
            int port = Integer.parseInt(API_PORT);
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            connector.setIdleTimeout(30000L);

            // Use ServletContextHandler (required for WebSocket upgrade support)
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addFilter(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            context.addServlet(JeetyServlet.class, "/api");

            // Register WebSocket balance endpoint (requires NativeWebSocketServletContainerInitializer)
            try {
                NativeWebSocketServletContainerInitializer.configure(context, null);
                context.addServlet(new ServletHolder("ws-balance", BalanceWebSocketServlet.class), "/ws/balance");
                logger.info("PORTAL API: WebSocket balance endpoint registered at /ws/balance");
            } catch (Exception wsEx) {
                logger.warn("PORTAL API: WebSocket setup failed (non-fatal): " + wsEx.getMessage());
            }

            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.setHandlers(new Handler[]{context});
            server.setHandler((Handler)handlerCollection);
            // Try SSL, fall back to HTTP-only if SSL setup fails
            try {
                int sslPort = Integer.parseInt(SSL_PORT);
                HttpConfiguration https = new HttpConfiguration();
                https.addCustomizer((HttpConfiguration.Customizer)new SecureRequestCustomizer());
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(basePath.concat("config/vinplay.jks"));
                String ksPassword = System.getenv().getOrDefault("KEYSTORE_PASSWORD", "cardgame@123!");
                sslContextFactory.setKeyStorePassword(ksPassword);
                sslContextFactory.setKeyManagerPassword(ksPassword);
                ServerConnector sslConnector = new ServerConnector(server, new ConnectionFactory[]{new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https)});
                sslConnector.setPort(sslPort);
                server.setConnectors(new Connector[]{connector, sslConnector});
                logger.info(("PORTAL API: SSL enabled on port " + sslPort));
            } catch (Exception sslEx) {
                logger.warn("PORTAL API: SSL setup failed, running HTTP-only: " + sslEx.getMessage());
                server.setConnectors(new Connector[]{connector});
            }
            server.start();
            logger.info(("PORTAL API SERVER Started on port " + port + " ...!!!"));

            // Start RMQ consumer for balance push (after server is up)
            try {
                PortalBalanceConsumer balanceConsumer = new PortalBalanceConsumer();
                balanceConsumer.start();
                logger.info("PORTAL API: PortalBalanceConsumer started");
            } catch (Exception rmqEx) {
                logger.warn("PORTAL API: PortalBalanceConsumer start failed (non-fatal): " + rmqEx.getMessage());
            }

            server.join();
        }
        catch (Exception e) {
            logger.error("PORTAL API SERVER Start Error:", e);
        }
    }

    private static void loadCommands() throws Exception {
        File file = new File(basePath.concat("config/api_portal.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("portal");
        Element el = (Element)nodeList.item(0);
        API_PORT = el.getElementsByTagName("port").item(0).getTextContent();
        SSL_PORT = el.getElementsByTagName("ssl_port").item(0).getTextContent();
        Element cmds = (Element)el.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmds.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent();
            logger.debug((id + " <-> " + path));
            System.out.println(id + " <-> " + path);
            commandsMap.put(id, path);
        }
        controller = new BaseController();
        controller.initCommands(commandsMap);
    }

    public static class JeetyServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

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
            String remoteAddr = request.getRemoteAddr();
            logger.info(("IP:" + remoteAddr));
            if (requestMap.containsKey("c")) {
                String command = request.getParameter("c");
                if (command == null || command.equalsIgnoreCase("")) {
                    blackListIpLogger.debug(remoteAddr);
                    return;
                }
                Param param = new Param();
                param.set(request);
                logger.debug(("command: " + command));
                try {
                    response.getWriter().println((String)controller.processCommand(Integer.valueOf(Integer.parseInt(command)), param));
                    service.log(command);
                }
                catch (NoCommandRegistered e2) {
                    logger.debug("COMMAND NOT FOUND");
                    response.getWriter().println("COMMAND NOT FOUND");
                    service.log("CMD_404");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println(e1);
                    response.getWriter().println("EXCEPTION: " + e1.getMessage());
                }
            } else {
                blackListIpLogger.debug(remoteAddr);
                response.getWriter().println("NO COMMANDS PARAMETERS");
                service.log("NO_CMD");
            }
        }
    }

}

