/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.cp.NoCommandRegistered
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  javax.servlet.DispatcherType
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.servlet.FilterHolder
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.servlet.ServletHolder
 */
package com.vinplay.api.backend.server;

import com.vinplay.api.backend.auth.AdminAuthHelper;
import com.vinplay.api.backend.auth.AdminPermissionRegistry;
import com.vinplay.api.backend.agent.utils.AgentUtils;
import com.vinplay.api.backend.gamebai.utils.GameBaiUtils;
import com.vinplay.api.backend.processors.commission.UpdateCommissionUtils;
import com.vinplay.api.backend.processors.rechargeByCardPending.CardOzzePendingUtils;
import com.vinplay.api.backend.processors.userMission.ResetUserMissionUtils;
import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.api.backend.report.utils.ReportMoneyUtils;
import com.vinplay.dal.rtp.RtpBackgroundJobs;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
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
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VinPlayBackendMain {
    private static final Logger logger = Logger.getLogger((String)"backend");
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static String API_PORT = "28056";
    private static BaseController<HttpServletRequest, String> controller;
    public static int TYPE;
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
            basePath = VBeePath.initBasePath(VinPlayBackendMain.class);
            VinPlayBackendMain.initializeLogger();
            logger.debug((Object)"STARTING BACKEND API SERVER .... !!!!");
            System.out.println("[BACKEND] Starting VinPlayBackend...");
            VinPlayBackendMain.loadCommands();
            System.out.println("[BACKEND] Commands loaded. Starting Hazelcast...");
            HazelcastLoader.start();
            System.out.println("[BACKEND] Hazelcast started. Initializing MongoDB...");
            MongoDBConnectionFactory.init();
            System.out.println("[BACKEND] MongoDB initialized. Starting RMQ...");
            RMQApi.start((String)"config/rmq.properties");
            System.out.println("[BACKEND] RMQ config loaded. Running GameCommon.init()...");
            try { GameCommon.init(); System.out.println("[BACKEND] GameCommon.init() OK"); } catch (Exception e) { System.err.println("[BACKEND] GameCommon.init() FAILED: " + e); e.printStackTrace(); }
            System.out.println("[BACKEND] Running AgentUtils.init()...");
            try { AgentUtils.init(); System.out.println("[BACKEND] AgentUtils.init() OK"); } catch (Exception e) { System.err.println("[BACKEND] AgentUtils.init() FAILED: " + e); e.printStackTrace(); }
            System.out.println("[BACKEND] Running GameBaiUtils.init()...");
            try { GameBaiUtils.init(); System.out.println("[BACKEND] GameBaiUtils.init() OK"); } catch (Exception e) { System.err.println("[BACKEND] GameBaiUtils.init() FAILED: " + e); e.printStackTrace(); }
            try {
                if (BackendUtils.apiRunTask()) {
                    ReportMoneyUtils.init();
                    CardOzzePendingUtils.init();
                    ResetUserMissionUtils.init();
                    UpdateCommissionUtils.init();
                    RtpBackgroundJobs.start();
                    // SUN-1108 Tier 3: daily rollup of rebate_logs into
                    // rebate_daily_rollup. Idempotent + cluster-safe via
                    // ON DUPLICATE KEY UPDATE — multiple backend-api
                    // instances coalesce safely. Disable via env
                    // REBATE_ROLLUP_ENABLED=false at any time.
                    com.vinplay.api.backend.perf.RebateRollupScheduler.start();
                    System.out.println("[BACKEND] RebateRollupScheduler started");

                    // SUN-1204: scan stuck GSC live-casino wagers (Dream,
                    // Evolution, Pragmatic, AI Live...) every 5 min and
                    // reconcile against vendor's authoritative wager
                    // status when our deposit/settle event was lost.
                    // Idempotent via MoneyGateway (tx_id, source) dedup.
                    // Disable via env GSC_RECONCILER_ENABLED=false.
                    com.vinplay.api.backend.perf.GscWagerReconcilerScheduler.start();
                    System.out.println("[BACKEND] GscWagerReconcilerScheduler started");

                    // Hourly cross-check against GSC's 3.2 Wager List.
                    // Complementary to the 5-min reconciler above:
                    // walks GSC's authoritative ledger and finds bets
                    // we never recorded. Auto-fix is safe by
                    // construction — every wallet movement uses
                    // (tx_id=wager_code, source=...) UNIQUE so the
                    // realtime path's eventual arrival or another
                    // recon instance can never double-pay.
                    // Disable via env GSC_HOURLY_RECON_ENABLED=false.
                    com.vinplay.api.backend.perf.GscHourlyReconScheduler.start();
                    System.out.println("[BACKEND] GscHourlyReconScheduler started");

                    // Phase 5 prep gate 5p4 — sample mysqlpoolname every 30s,
                    // alert ops on Telegram when utilization stays >80% for 2
                    // consecutive samples. Catches slow exhaustion before
                    // users see connection-timeout errors. After 5p1 bumped
                    // maxpool 10→30, this is the active monitor that proves
                    // the bump is sufficient under the 5p5 load test.
                    // Disable via env POOL_MONITOR_ENABLED=false.
                    com.vinplay.api.backend.perf.ConnectionPoolMonitorScheduler.start();
                    System.out.println("[BACKEND] ConnectionPoolMonitorScheduler started");

                    // Phase 5 prep gate 5p3 — per-handler p99 latency monitor
                    // for SeamlessWalletAggregator. Polls AggregatorMetrics
                    // every 60s, fires Telegram alert when any handler's
                    // rolling 1-minute p99 > 100ms. Surfaces Mongo backoff
                    // / pool pressure regressions in seconds. Disable via
                    // env AGGREGATOR_P99_SCHEDULER_ENABLED=false.
                    com.vinplay.api.backend.perf.AggregatorP99Scheduler.start();
                    System.out.println("[BACKEND] AggregatorP99Scheduler started");
                }
            } catch (Exception e) { System.err.println("[BACKEND] Task init FAILED: " + e); e.printStackTrace(); }
            // SUN-764 / SUN-750 / SUN-751: rebate + cashback are now fully realtime
            // (per-bet cascade in RealTimeCommission.calculate()). No daily/period cron.
            // ── RTP Config: đồng bộ cache từ DB khi server khởi động ─────────────
            // Đảm bảo Hazelcast đã start trước khi gọi (đã start ở trên)
            try {
                new com.vinplay.dal.rtp.RtpConfigService().reloadAllToCache();
                System.out.println("[BACKEND] RtpConfigService.reloadAllToCache() OK");
            } catch (Exception e) {
                System.err.println("[BACKEND] RtpConfigService.reloadAllToCache() FAILED (non-fatal): " + e);
            }
            // ────────────────────────────────────────────────────────────────────
            System.out.println("[BACKEND] Starting Jetty on port " + API_PORT + "...");
            Server server = new Server(Integer.parseInt(API_PORT));
            ServletHandler handler = new ServletHandler();
            handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServletWithMapping(JeetyServlet.class, "/api_backend");
            handler.addServletWithMapping(JeetyServlet.class, "/api_agent");
            server.setHandler((Handler)handler);
            server.start();
            System.out.println("[BACKEND] Jetty started on port " + API_PORT);
            logger.info((Object)"BACKEND API SERVER Started ...!!!");
            server.join();
        }
        catch (Exception e) {
            System.err.println("[BACKEND] FATAL startup error: " + e);
            e.printStackTrace();
            logger.error("BACKEND API SERVER Start error: ", e);
        }
    }

    private static void loadCommands() throws Exception {
        File file = new File(basePath.concat("config/api_backend.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("backend");
        Element el = (Element)nodeList.item(0);
        API_PORT = el.getElementsByTagName("port").item(0).getTextContent();
        try {
            TYPE = Integer.parseInt(el.getElementsByTagName("type").item(0).getTextContent());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Element cmds = (Element)el.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmds.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        int skipped = 0;
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent();
            try {
                Class.forName(path);
                System.out.println(id + " <-> " + path);
                commandsMap.put(id, path);
            } catch (ClassNotFoundException e) {
                skipped++;
                System.err.println("[BACKEND] SKIP command " + id + ": class not found: " + path);
            } catch (Throwable t) {
                skipped++;
                System.err.println("[BACKEND] SKIP command " + id + ": load error for " + path + " -> " + t);
            }
        }
        if (skipped > 0) {
            System.out.println("[BACKEND] WARNING: Skipped " + skipped + " commands with missing classes");
        }
        controller = new BaseController();
        controller.initCommands(commandsMap);

        // Commission is now calculated real-time in LogMoneyUserExtraProcessor (vbee consumer).
        // Cron job removed — c=9767 still available for manual trigger if needed.
    }

    static {
        TYPE = 0;
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
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            Map requestMap = request.getParameterMap();
            if (requestMap.containsKey("c")) {
                String command = request.getParameter("c");
                int commandId = Integer.parseInt(command);

                // Admin auth + RBAC for /api_backend commands.
                // Keep /api_agent compatibility unchanged.
                String requestUri = request.getRequestURI();
                boolean isBackendEndpoint = requestUri != null && requestUri.contains("/api_backend");
                if (isBackendEndpoint && AdminAuthHelper.requiresAuth(commandId)) {
                    String adminToken = request.getParameter("aat");
                    if (adminToken == null || adminToken.isEmpty()) {
                        adminToken = request.getParameter("at");
                    }
                    String sidCheck = request.getParameter("sid");
                    String aidCheck = request.getParameter("aid");
                    boolean hasAgencySession = sidCheck != null && !sidCheck.isEmpty()
                            && aidCheck != null && !aidCheck.isEmpty();

                    if (adminToken == null || adminToken.isEmpty()) {
                        if (!hasAgencySession) {
                            response.getWriter().println(AdminAuthHelper.errorResponse("9000", "Admin token required"));
                            return;
                        }
                        // Agency session present — skip admin token; validated in the session block below
                    } else {
                        if (!AdminAuthHelper.verifyToken(adminToken)) {
                            response.getWriter().println(AdminAuthHelper.errorResponse("9001", "Admin token expired or invalid"));
                            return;
                        }
                        String adminNickname = AdminAuthHelper.getAdminUsernameByToken(adminToken);
                        if (AdminPermissionRegistry.isPermissionControlled(commandId)
                                && !AdminPermissionRegistry.canAccessCommand(adminNickname, commandId)) {
                            String requiredPerms = AdminPermissionRegistry.describeRequiredPermissions(commandId);
                            response.getWriter().println(
                                    "{\"success\":false,\"errorCode\":\"4003\",\"message\":\"Permission denied for command "
                                            + commandId + "\",\"required\":\"" + requiredPerms + "\"}");
                            return;
                        }
                    }
                }

                // SUN-767: agency session check. sunkr-nextagency sends `sid`
                // (UUID issued by LoginAgentProcessor c=9428) and `aid`
                // (agent id) on every call. If both are present we validate
                // against Hazelcast `agencySessions[agentId].sessionId`. A fresh
                // login overwrites the entry, so any request from the old
                // device with the old sid fails here. If neither is present
                // we pass through (admin CMS / legacy calls).
                String sidParam = request.getParameter("sid");
                String aidParam = request.getParameter("aid");
                if (sidParam != null && !sidParam.isEmpty() && aidParam != null && !aidParam.isEmpty()) {
                    try {
                        int aidVal = Integer.parseInt(aidParam);
                        com.vinplay.vbee.common.cache.DistCache<Integer, com.vinplay.vbee.common.models.AgencySession> sessMap =
                                com.vinplay.vbee.common.cache.CacheFactory.get("agencySessions",
                                        com.vinplay.vbee.common.models.AgencySession.class);
                        com.vinplay.vbee.common.models.AgencySession current = sessMap.get(aidVal);
                        if (current == null || !sidParam.equals(current.getSessionId())) {
                            response.getWriter().println(
                                    "{\"success\":false,\"errorCode\":\"9004\"," +
                                    "\"message\":\"Agency session superseded — please log in again\"}");
                            return;
                        }
                    } catch (NumberFormatException nfe) {
                        response.getWriter().println(
                                "{\"success\":false,\"errorCode\":\"9005\",\"message\":\"Invalid aid\"}");
                        return;
                    } catch (Exception sessErr) {
                        logger.warn((Object)("Agency session check error: " + sessErr.getMessage()));
                        // On error, fail open for now to avoid breaking the dashboard
                        // while Hazelcast is degraded. Flip to fail-closed after v1.
                    }
                }

                Param param = new Param();
                param.set((Object)request);
                logger.debug((Object)("command: " + command));
                try {
                    response.getWriter().println((String)controller.processCommand(Integer.valueOf(commandId), param));
                }
                catch (NoCommandRegistered e2) {
                    logger.debug((Object)("COMMAND NOT FOUND: " + command));
                    response.getWriter().println("{\"success\":false,\"errorCode\":\"9002\",\"message\":\"Command not found: " + command + "\"}");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    logger.error((Object)("Command " + command + " exception: " + e1.getMessage()));
                    response.getWriter().println("{\"success\":false,\"errorCode\":\"9003\",\"message\":\"Internal server error\"}");
                }
            } else {
                response.getWriter().println("{\"success\":false,\"errorCode\":\"9000\",\"message\":\"Admin token required\"}");
            }
        }
    }

}
