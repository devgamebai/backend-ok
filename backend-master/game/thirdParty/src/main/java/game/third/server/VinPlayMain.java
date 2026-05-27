/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.usercore.utils.GameThirdPartyInit
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
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.servlet.ServletHolder
 */
package game.third.server;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import game.third.migrations.DatabaseTable;
import game.third.schedule.ScheduleMain;
import game.third.server.CorsFilter;
import game.third.server.GameServlet;
import game.third.server.HookServlet;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.hook.Context;
import game.third.usecase.core.hook.HookController;
import game.third.usecase.core.hook.ProcessorInfo;
import game.third.usecase.game568win.service.impl.APIGame568winServiceImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VinPlayMain {
    private static final Logger logger = Logger.getLogger((String)"thirdPart");
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static int API_PORT = 9590;
    private static int HOOK_PORT = 9591;
    public static int TYPE;
    private static BaseController<HttpServletRequest, String> controller;
    private static HookController<HttpServletRequest, String> hookController;
    private static final Map<String, List<String>> firewall;
    private static String basePath;

    private static void initializeLogger() {
        Properties logProperties = new Properties();
        try {
            File file = new File(basePath.concat(LOG_PROPERTIES_FILE));
            logProperties.load(new FileInputStream(file));
            PropertyConfigurator.configure((Properties)logProperties);
            logger.info((Object)"Logging initialized.");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load logging property config/log4j.properties");
        }
    }

    public static void main(String[] args) {
        try {
            basePath = VBeePath.initBasePath(VinPlayMain.class);
            VinPlayMain.initializeLogger();
            logger.debug((Object)"STARTING Third Part Game .... !!!!");
            HazelcastLoader.start();
            MongoDBConnectionFactory.init();
            RMQApi.start((String)"./config/rmq.properties");
            GameCommon.init();
            GameThirdPartyInit.init();
            ScheduleMain.run();
            // SUN-XXXX: GSC stuck-RECEIVED row reconciler (wallet debited
            // but no response back to GSC). thirdParty is the only entry
            // path for /seamless/withdraw, so this is the only container
            // that needs the recovery sweep. Feature-flagged off by default
            // via env GSC_STUCK_RECONCILER_ENABLED — start() is a no-op
            // until ops opts in.
            try {
                com.vinplay.dal.service.seamless.gsc.GscStuckRowReconciler
                        .getInstance().start();
            } catch (Throwable t) {
                logger.error((Object)("GscStuckRowReconciler bootstrap failed: " + t.getMessage()), (Throwable)t);
            }
            // AggregatorP99Scheduler runs in whichever JVM serves /withdraw,
            // /deposit, /balance — i.e. game-thirdparty, since
            // SeamlessWalletAggregator.AggregatorMetrics records in-memory
            // samples on the request thread. Backend-api's existing start()
            // call sees zero GSC traffic and never fires the p99 alert.
            // Telegram alert when any GSC handler's rolling 1-min p99 > 100ms.
            // Invoked via reflection: the class lives in VinPlayBackend
            // which thirdParty does not depend on at compile-time, but the
            // fat-jar at runtime has it on the classpath.
            try {
                Class<?> cls = Class.forName("com.vinplay.api.backend.perf.AggregatorP99Scheduler");
                cls.getMethod("start").invoke(null);
            } catch (ClassNotFoundException nf) {
                logger.warn((Object)"AggregatorP99Scheduler not on classpath — skipping");
            } catch (Throwable t) {
                logger.error((Object)("AggregatorP99Scheduler bootstrap failed: " + t.getMessage()), (Throwable)t);
            }
            Server server = VinPlayMain.startServerGame();
            Server serverHook = VinPlayMain.startServerHook();
            DatabaseTable.migration();
            APIGame568winServiceImpl apiGame568winService = new APIGame568winServiceImpl();
            apiGame568winService.checkAndCreateAgent();
            server.join();
            serverHook.join();
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.info((Object)("Third Part Game Start error: " + e.getMessage()));
        }
    }

    private static Server startServerGame() throws Exception {
        VinPlayMain.loadCommands();
        Server server = new Server(API_PORT);
        ServletHandler handler = new ServletHandler();
        handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        handler.addServletWithMapping(new ServletHolder((Servlet)new GameServlet(controller)), "/api_game");
        server.setHandler((Handler)handler);
        server.start();
        logger.info((Object)"Third Part Game Started ...!!!");
        return server;
    }

    private static Server startServerHook() throws Exception {
        Context context = new Context();
        context.set("gscConfig", ThirdPartyLoad.getGscConfig());
        VinPlayMain.loadHook(context);
        Server server = new Server(HOOK_PORT);
        ServletHandler handler = new ServletHandler();
        handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        handler.addServletWithMapping(new ServletHolder((Servlet)new HookServlet(hookController)), "/*");
        server.setHandler((Handler)handler);
        server.start();
        logger.info((Object)"Third Part Hook Started ...!!!");
        return server;
    }

    private static void loadCommands() throws Exception {
        File file = new File(basePath.concat("config/api_game.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("server");
        Element el = (Element)nodeList.item(0);
        try {
            API_PORT = Integer.parseInt(el.getElementsByTagName("port").item(0).getTextContent());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
        }
        try {
            TYPE = Integer.parseInt(el.getElementsByTagName("type").item(0).getTextContent());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e);
        }
        Element cmd = (Element)el.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmd.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        System.out.println("------- Commands controller mapping ---------");
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent();
            System.out.println(id + " <-> " + path);
            commandsMap.put(id, path);
        }
        if (controller == null) {
            controller = new BaseController();
            controller.initCommands(commandsMap);
        }
    }

    private static void loadFirewall(Document document) {
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

    static {
        firewall = new HashMap<String, List<String>>();
    }
}

