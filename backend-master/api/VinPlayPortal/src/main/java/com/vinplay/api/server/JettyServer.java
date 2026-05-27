/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.LogPortalService
 *  com.vinplay.dal.service.impl.LogPortalServiceImpl
 *  com.vinplay.dal.utils.PotUtils
 *  com.vinplay.otp.sender.MangerSender
 *  com.vinplay.vbee.common.config.VBeePath
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.cp.NoCommandRegistered
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  javax.servlet.DispatcherType
 *  javax.servlet.ServletException
 *  javax.servlet.ServletOutputStream
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 *  org.eclipse.jetty.server.Connector
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.server.ServerConnector
 *  org.eclipse.jetty.server.handler.HandlerCollection
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.util.thread.QueuedThreadPool
 *  org.eclipse.jetty.util.thread.ThreadPool
 */
package com.vinplay.api.server;

import com.vinplay.api.server.CorsFilter;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.api.ws.BalanceWebSocketServlet;
import com.vinplay.api.ws.PortalBalanceConsumer;
import com.vinplay.dal.service.LogPortalService;
import com.vinplay.dal.service.impl.LogPortalServiceImpl;
import com.vinplay.dal.utils.PotUtils;
import com.vinplay.otp.sender.MangerSender;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.UserValidaton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JettyServer {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static final Logger blackListIpLogger = Logger.getLogger((String)"BlackListIpLogger");
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static LogPortalService service = new LogPortalServiceImpl();
    private static String API_PORT = "8081";
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
            basePath = VBeePath.initBasePath(JettyServer.class);
            JettyServer.initializeLogger();
            logger.debug("STARTING PORTAL API SERVER .... !!!!");
            JettyServer.loadCommands();
            RMQApi.start((String)"./config/rmq.properties");
            HazelcastLoader.start();
            MongoDBConnectionFactory.init();
            UserValidaton.init();
            PortalUtils.loadGameConfig();
            PotUtils.init();
            MangerSender.init();
            try {
                com.vinplay.dal.service.seamless.gsc.GscStuckRowReconciler.getInstance().start();
            } catch (Exception e) {
                logger.warn("GscStuckRowReconciler.start() failed (non-fatal): " + e.getMessage());
            }
            // SUN-1387 follow-up (MR !434 review CRITICAL): close the
            // PAID-before-credit gap in SELF cashback claims. The
            // in-flight retry inside ClaimCashbackProcessor catches the
            // routine transient case; this reconciler covers the residual
            // (process kill, JVM pause, all retries exhausted) by
            // anti-joining rebate_logs(status='PAID') against
            // money_gateway_log and crediting via SOURCE_REBATE_RECOVERY.
            try {
                com.vinplay.dal.service.cashback.RebateCashbackReconciler.getInstance().start();
            } catch (Exception e) {
                logger.warn("RebateCashbackReconciler.start() failed (non-fatal): " + e.getMessage());
            }
            // SUN-1340 follow-up: AWC seamless callbacks live in this JVM
            // (portal-api), so the AggregatorP99Scheduler must run here to
            // observe AwcGetBalance / AwcBet / AwcSettle p99. Backend-api's
            // existing start() call sees no AWC traffic. Telegram alert
            // fires when any handler's rolling 1-min p99 exceeds
            // GSC_P99_THRESHOLD_MS (the env var name is GSC-prefixed for
            // legacy reasons; it gates every aggregator's threshold).
            //
            // Invoked via reflection — the class lives in VinPlayBackend
            // which VinPlayPortal does not depend on at compile-time, but
            // the runtime classpath has it (libs/runtime/VinPlayBackend.jar).
            try {
                Class<?> cls = Class.forName("com.vinplay.api.backend.perf.AggregatorP99Scheduler");
                cls.getMethod("start").invoke(null);
                logger.info("PORTAL API: AggregatorP99Scheduler started");
            } catch (ClassNotFoundException nf) {
                logger.warn("AggregatorP99Scheduler not on classpath — skipping");
            } catch (Throwable t) {
                logger.warn("AggregatorP99Scheduler bootstrap failed (non-fatal): " + t.getMessage());
            }
            int port = Integer.parseInt(API_PORT);
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMinThreads(20);
            threadPool.setMaxThreads(200);
            threadPool.setIdleTimeout(60000);
            threadPool.setName("portal-pool");
            Server server = new Server((ThreadPool)threadPool);
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            connector.setIdleTimeout(30000L);
            // Use ServletContextHandler (required for WebSocket upgrade support)
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addFilter(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            context.addServlet(JeetyServlet.class, "/api");
            context.addServlet(DownloadServlet.class, "/download");

            // Register WebSocket balance endpoint (SUN-739)
            try {
                NativeWebSocketServletContainerInitializer.configure(context, null);
                context.addServlet(new ServletHolder("ws-balance", BalanceWebSocketServlet.class), "/ws/balance");
                logger.info("PORTAL API: WebSocket balance endpoint registered at /ws/balance");
            } catch (Exception wsEx) {
                logger.warn("PORTAL API: WebSocket setup failed (non-fatal): " + wsEx.getMessage());
            }

            // SUN-832 (rewired): kick /ws/balance sockets whose accessToken
            // gets revoked. Previously only registered in VinPlayPortal.java,
            // but the actual main class is JettyServer — so the listener was
            // never registered in production/staging → no force_logout
            // reached browser 2 when browser 1 re-logged in.
            try {
                com.vinplay.vbee.common.session.SessionKickListener.register(
                    new com.vinplay.vbee.common.session.SessionKickListener.KickHandler() {
                        @Override
                        public void kick(String accessToken, String nickname, String reason) {
                            try {
                                BalanceWebSocketServlet.kickByToken(accessToken, reason);
                            } catch (Throwable t) {
                                logger.warn("portal WS kickByToken failed: " + t.getMessage());
                            }
                        }
                    });
                logger.info("PORTAL API: cacheToken kick listener registered for /ws/balance");
            } catch (Exception sxe) {
                logger.warn("PORTAL API: ws-balance kick listener setup failed: " + sxe.getMessage());
            }

            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.setHandlers(new Handler[]{context});
            server.setHandler((Handler)handlerCollection);
            server.addConnector((Connector)connector);
            server.start();
            logger.info("PORTAL API SERVER Started ...!!!");

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
            logger.info(("PORTAL API SERVER Start error: " + e.getMessage()));
            e.printStackTrace();
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

    public static class DownloadServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            int i;
            String localDirConfig = "";
            try (BufferedReader br = Files.newBufferedReader(Paths.get("./config/foler_upload.properties", new String[0]));){
                Properties prop = new Properties();
                prop.load(br);
                localDirConfig = prop.getProperty("download");
            }
            catch (Exception e) {
                localDirConfig = "./config/cdn";
            }
            File folder = new File(localDirConfig);
            File file = null;
            for (File f : folder.listFiles()) {
                if (!f.isFile()) continue;
                file = f;
                break;
            }
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            response.setContentType("APPLICATION/OCTET-STREAM");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            FileInputStream fileInputStream = new FileInputStream(file.getAbsoluteFile());
            while ((i = fileInputStream.read()) != -1) {
                out.write(i);
            }
            fileInputStream.close();
            out.close();
        }
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
            block7: {
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
                        if (!"8000".equals(command)) {
                            response.getWriter().println((String)controller.processCommand(Integer.valueOf(Integer.parseInt(command)), param));
                            service.log(command);
                            break block7;
                        }
                        response.setContentType("text/csv");
                        response.setHeader("Content-Disposition", "attachment; filename=\"export.csv\"");
                        ServletOutputStream outputStream = response.getOutputStream();
                        String outputResult = (String)controller.processCommand(Integer.valueOf(Integer.parseInt(command)), param);
                        outputStream.write(outputResult.getBytes());
                        outputStream.flush();
                        outputStream.close();
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
}

