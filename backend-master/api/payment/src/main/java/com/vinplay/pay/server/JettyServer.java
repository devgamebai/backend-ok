/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.core.hook.Context
 *  com.payment.core.hook.HookController
 *  com.payment.core.hook.ProcessorInfo
 *  com.payment.migrations.DatabaseTable
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.usercore.utils.PartnerConfig
 *  com.vinplay.vbee.common.config.VBeePath
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  javax.servlet.DispatcherType
 *  javax.servlet.Servlet
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 *  org.eclipse.jetty.server.Connector
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.server.ServerConnector
 *  org.eclipse.jetty.server.handler.HandlerCollection
 *  org.eclipse.jetty.server.handler.IPAccessHandler
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.servlet.ServletHolder
 *  org.eclipse.jetty.util.thread.QueuedThreadPool
 *  org.eclipse.jetty.util.thread.ThreadPool
 */
package com.vinplay.pay.server;

import com.payment.core.hook.Context;
import com.payment.core.hook.HookController;
import com.payment.core.hook.ProcessorInfo;
import com.payment.migrations.DatabaseTable;
import com.vinplay.api.BankOutServlet;
import com.vinplay.api.BankServlet;
import com.vinplay.api.CardServlet;
import com.vinplay.api.GetListProviderSupportWithdrawServlet;
import com.vinplay.api.ListBankServlet;
import com.vinplay.api.ListCardServlet;
import com.vinplay.pay.server.APIPaymentServlet;
import com.vinplay.pay.server.CorsFilter;
import com.vinplay.pay.server.HookServlet;
import com.vinplay.pay.server.ServerIPFilter;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.schedule.ScheduleMain;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.PartnerConfig;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JettyServer {
    private static final Logger logger = Logger.getLogger(JettyServer.class);
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static int HTTP_PORT = 18081;
    private static int HOOK_PORT = 18082;
    private static long IDLE_TIMEOUT = 30000L;
    private static BaseController<HttpServletRequest, String> controller;
    private static HookController<HttpServletRequest, String> hookController;
    private static String basePath;

    private static void initializeLogger() {
        Properties logProperties = new Properties();
        try {
            File file = new File(basePath.concat(LOG_PROPERTIES_FILE));
            logProperties.load(Files.newInputStream(file.toPath(), new OpenOption[0]));
            PropertyConfigurator.configure((Properties)logProperties);
            logger.info((Object)"Logging initialized.");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load logging property config/log4j.properties");
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
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

    public static void main(String[] args) {
        try {
            System.out.println("Start Payment Service");
            basePath = VBeePath.initBasePath(JettyServer.class);
            JettyServer.initializeLogger();
            logger.info((Object)"Starting Payment API .... !!!!");
            RMQApi.start((String)"./config/rmq.properties");
            HazelcastLoader.start();
            PartnerConfig.ReadConfig();
            MongoDBConnectionFactory.init();
            GameCommon.init();
            PayUtils.init();
            DatabaseTable.migration();
            ScheduleMain.run();
            Server serverHook = JettyServer.startServerHook();
            Server server = JettyServer.startServer();
            server.join();
            serverHook.join();
        }
        catch (Exception e) {
            logger.error((Object)("Payment API Start error: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private static Server startServer() throws Exception {
        JettyServer.loadCommands();
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(5);
        threadPool.setMaxThreads(200);
        threadPool.setIdleTimeout(60000);
        threadPool.setName("payment-pool");
        Server server = new Server((ThreadPool)threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(HTTP_PORT);
        connector.setIdleTimeout(IDLE_TIMEOUT);
        ServletHandler handler = new ServletHandler();
        handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        handler.addServletWithMapping(BankServlet.class, "/api_payment/bank");
        handler.addServletWithMapping(CardServlet.class, "/api_payment/card");
        handler.addServletWithMapping(ListBankServlet.class, "/api_payment/list_bank");
        handler.addServletWithMapping(ListCardServlet.class, "/api_payment/list_card");
        handler.addServletWithMapping(new ServletHolder((Servlet)new APIPaymentServlet(controller)), "/api_payment");
        IPAccessHandler ipAccessHandler = new IPAccessHandler();
        ipAccessHandler.setHandler((Handler)handler);
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[]{ipAccessHandler});
        server.setHandler((Handler)handlerCollection);
        server.addConnector((Connector)connector);
        server.start();
        logger.info((Object)"Payment API Started ...!!!");
        return server;
    }

    private static void loadCommands() throws Exception {
        File file = new File(basePath.concat("config/api_payment.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("portal");
        Element el = (Element)nodeList.item(0);
        HTTP_PORT = Integer.parseInt(el.getElementsByTagName("http_port").item(0).getTextContent());
        IDLE_TIMEOUT = Integer.parseInt(el.getElementsByTagName("idle_timeout").item(0).getTextContent());
        Element cmds = (Element)el.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmds.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent();
            logger.debug((Object)(id + " <-> " + path));
            System.out.println(id + " <-> " + path);
            commandsMap.put(id, path);
        }
        controller = new BaseController();
        controller.initCommands(commandsMap);
    }

    private static Server startServerHook() throws Exception {
        Context context = new Context();
        JettyServer.loadHook(context);
        Server server = new Server(HOOK_PORT);
        ServletHandler handler = new ServletHandler();
        handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        handler.addFilterWithMapping(ServerIPFilter.class, "/api/bankOut", EnumSet.of(DispatcherType.REQUEST));
        handler.addServletWithMapping(BankOutServlet.class, "/api/bankOut");
        handler.addServletWithMapping(GetListProviderSupportWithdrawServlet.class, "/api/listProvider");
        handler.addServletWithMapping(new ServletHolder((Servlet)new HookServlet(hookController)), "/*");
        server.setHandler((Handler)handler);
        server.start();
        logger.info((Object)"Payment Hook Started ...!!!");
        return server;
    }

    private static void loadHook(Context context) throws Exception {
        File file = new File(basePath.concat("config/api_hook.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("hook");
        Element el = (Element)nodeList.item(0);
        try {
            HOOK_PORT = Integer.parseInt(el.getElementsByTagName("port").item(0).getTextContent());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
        }
        Element gameEl = (Element)el.getElementsByTagName("games").item(0);
        NodeList gameListEl = gameEl.getElementsByTagName("game");
        HashMap<String, ProcessorInfo> hooksMap = new HashMap<String, ProcessorInfo>();
        System.out.println("------- Hooks controller mapping ---------");
        for (int i = 0; i < gameListEl.getLength(); ++i) {
            Element gameRoot = (Element)gameListEl.item(i);
            String name = gameRoot.getElementsByTagName("name").item(0).getTextContent();
            System.out.println("Hook -> " + name);
            String pathRoot = gameRoot.getElementsByTagName("path_root").item(0).getTextContent();
            HashSet<String> whiteList = new HashSet<String>();
            Element whitelists = (Element)gameRoot.getElementsByTagName("whitelist").item(0);
            NodeList ipList = whitelists.getElementsByTagName("ip");
            for (int j = 0; j < ipList.getLength(); ++j) {
                Element ipEl = (Element)ipList.item(j);
                String ip = ipEl.getTextContent();
                whiteList.add(ip);
            }
            Element paths = (Element)gameRoot.getElementsByTagName("paths").item(0);
            NodeList pathList = paths.getElementsByTagName("path");
            for (int j = 0; j < pathList.getLength(); ++j) {
                Element pathEl = (Element)pathList.item(j);
                String pattern = pathEl.getElementsByTagName("pattern").item(0).getTextContent();
                String processPath = pathEl.getElementsByTagName("process").item(0).getTextContent();
                String fullPath = pathRoot + pattern;
                System.out.println(fullPath + " <-> " + processPath);
                ProcessorInfo processorInfo = new ProcessorInfo(processPath, whiteList);
                hooksMap.put(fullPath, processorInfo);
            }
        }
        if (hookController == null) {
            hookController = new HookController();
            hookController.initPaths(hooksMap, context);
        }
    }
}

