package bitzero.server;

import bitzero.engine.config.ControllerConfig;
import bitzero.engine.config.EngineConfiguration;
import bitzero.engine.config.SocketConfig;
import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.data.BindableSocket;
import bitzero.engine.data.BufferType;
import bitzero.engine.data.TransportType;
import bitzero.engine.events.IEvent;
import bitzero.engine.events.IEventListener;
import bitzero.engine.service.IService;
import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.engine.websocket.WebSocketConfig;
import bitzero.server.api.APIManager;
import bitzero.server.config.BZConfigurator;
import bitzero.server.config.CoreSettings;
import bitzero.server.config.DefaultConstants;
import bitzero.server.config.IConfigurator;
import bitzero.server.config.ServerSettings;
import bitzero.server.core.BZEvent;
import bitzero.server.core.BZEventManager;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.BZShutdownHook;
import bitzero.server.core.IBZEventManager;
import bitzero.server.core.ServerState;
import bitzero.server.entities.ExtensionManager;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.BZBannedUserManager;
import bitzero.server.entities.managers.BZStatsManager;
import bitzero.server.entities.managers.BZUserManager;
import bitzero.server.entities.managers.BZZoneManager;
import bitzero.server.entities.managers.IBannedUserManager;
import bitzero.server.entities.managers.IExtensionManager;
import bitzero.server.entities.managers.IStatsManager;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.entities.managers.IZoneManager;
import bitzero.server.exceptions.BZException;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.exceptions.ExceptionMessageComposer;
import bitzero.server.util.BZRestart;
import bitzero.server.util.ClientDisconnectionReason;
import bitzero.server.util.DebugConsole;
import bitzero.server.util.FlashMasterSocketPolicyLoader;
import bitzero.server.util.IDisconnectionReason;
import bitzero.server.util.PacketCount;
import bitzero.server.util.ServerUptime;
import bitzero.server.util.StringHelper;
import bitzero.server.util.TaskScheduler;
import bitzero.server.util.deadlock.DefaultDeadlockListener;
import bitzero.server.util.deadlock.ThreadDeadlockDetector;
import bitzero.server.util.executor.SmartExecutorConfig;
import bitzero.server.util.executor.SmartThreadPoolExecutor;
import bitzero.server.util.monitor.GhostUserHunter;
import bitzero.server.util.monitor.IGhostUserHunter;
import bitzero.util.config.bean.ConstantMercury;
import bitzero.util.datacontroller.business.DataController;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.vinplay.vbee.common.config.VBeePath;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BitZeroServer {
     private final String version = "alpha";
     private static BitZeroServer _instance = null;
     private static AtomicInteger restartCounter = new AtomicInteger(0);
     private final BitZeroEngine bitZeroEngine = BitZeroEngine.getInstance();
     private final Logger log = LoggerFactory.getLogger(this.getClass());
     private APIManager apiManager;
     private volatile ServerState state;
     private volatile boolean initialized;
     private volatile boolean started;
     private volatile long serverStartTime;
     private volatile boolean isRebooting;
     private volatile boolean isHalting;
     private final IConfigurator bzConfigurator = new BZConfigurator();
     private IEventListener networkEventListener;
     private TaskScheduler taskScheduler;
     private IService adminToolService;
     private boolean clustered;
     private final IBZEventManager eventManager = new BZEventManager();
     private IGhostUserHunter ghostUserHunter;
     private IStatsManager statsManager;
     private IExtensionManager extensionManager;
     private IBannedUserManager bannedUserManger;
     private IZoneManager zoneManager;
     private IUserManager userManager;
     private DebugConsole debugConsole;
     private BZException zoneInitError;
     private PacketCount packetCount = new PacketCount();
     private ThreadPoolExecutor sysmtemWorkerPool;
     private static String basePath = VBeePath.basePath;

     public static BitZeroServer getInstance() {
          if (_instance == null) {
               _instance = new BitZeroServer();
          }

          return _instance;
     }

     private BitZeroServer() {
          this.state = ServerState.STARTING;
          this.initialized = false;
          this.started = false;
          this.isRebooting = false;
          this.isHalting = false;
          this.clustered = false;
          this.networkEventListener = new BitZeroEventListener((BitZeroEventListener)null);
          this.zoneManager = new BZZoneManager();
          if (this.userManager == null) {
               this.userManager = new BZUserManager();
          }

          this.statsManager = new BZStatsManager();
          this.taskScheduler = new TaskScheduler(1);
          this.bannedUserManger = new BZBannedUserManager();
          this.extensionManager = new ExtensionManager();
     }

     public static boolean isDebug() {
          return _instance.bzConfigurator.getServerSettings().useDebugMode;
     }

     public String getVersion() {
          return "alpha";
     }

     public void start() {
          if (!this.initialized) {
               this.initialize();
          }

          ExceptionMessageComposer msg;
          try {
               this.bzConfigurator.loadConfiguration();
               this.initSystemWorkers();
               this.configureServer();
               this.configureBitZero();
               this.extensionManager.init();

               try {
                    this.zoneManager.init((Object)null);
                    this.zoneManager.initializeZones();
               } catch (BZException var3) {
                    this.zoneInitError = var3;
               }

               this.bitZeroEngine.start("BitZeroServer alpha");
               this.log.debug("START ENGINE SERVER ***   ");
               this.registerShutdown();
               this.deadLockDetector();
          } catch (FileNotFoundException var4) {
               msg = new ExceptionMessageComposer(var4);
               msg.setDescription("There has been a problem loading the server configuration. The server cannot start.");
               msg.setPossibleCauses("Make sure that core.xml and server.xml files exist in your config/ folder.");
               this.log.error(msg.toString());
          } catch (BZException var5) {
               msg = new ExceptionMessageComposer(var5);
               msg.setDescription("An error occurred during the Server boot, preventing it to start.");
               this.log.error(msg.toString());
          } catch (Exception var6) {
               msg = new ExceptionMessageComposer(var6);
               msg.setDescription("Unexpected error during Server boot. The server cannot start.");
               msg.addInfo("Solution: Please email us the content of this error message, including the stack trace to toannobita@gmail..com");
               this.log.error(msg.toString());
          }

     }

     public boolean isClustered() {
          return this.clustered;
     }

     public void setClustered(boolean value) {
          if (this.initialized) {
               throw new IllegalStateException("Server already initialized, cannot change the cluster mode!");
          } else {
               this.clustered = value;
          }
     }

     public void registerShutdown() {
          Runtime.getRuntime().addShutdownHook(new Thread() {
               public void run() {
                    try {
                         List listBind = BitZeroServer.this.bitZeroEngine.getEngineAcceptor().getBoundSockets();
                         Iterator var2 = listBind.iterator();

                         while(var2.hasNext()) {
                              BindableSocket bindS = (BindableSocket)var2.next();
                              DataController.getController().delete("cache_binded_port__" + bindS.getAddress() + "_" + bindS.getPort());
                         }
                    } catch (Exception var4) {
                    }

               }
          });
     }

     public void deadLockDetector() {
          if (ConstantMercury.ENABLE_DEADLOCK_DETECTOR) {
               DefaultDeadlockListener listener = new DefaultDeadlockListener();
               ThreadDeadlockDetector detector = new ThreadDeadlockDetector(ConstantMercury.DEADLOCK_TIME);
               detector.addListener(listener);
          }

     }

     public int getRestartCount() {
          return restartCounter.get();
     }

     public synchronized void restart() {
          if (!this.isRebooting) {
               this.isRebooting = true;
               this.log.warn("*** SERVER RESTARTING ***");

               try {
                    this.bitZeroEngine.shutDownSequence();
                    this.started = false;
                    Thread restarter = new BZRestart();
                    restarter.start();
               } catch (Exception var2) {
                    this.log.error("Restart Failure: " + var2);
               }

          }
     }

     public void halt() {
          if (!this.isHalting) {
               this.isHalting = true;
               this.log.warn("*** SERVER HALTING ***");

               try {
                    Thread stopper = new Thread(new Runnable() {
                         int countDown = 3;

                         public void run() {
                              while(this.countDown > 0) {
                                   BitZeroServer.this.log.warn("Server Halt in " + this.countDown-- + " seconds...");

                                   try {
                                        Thread.sleep(1000L);
                                   } catch (InterruptedException var2) {
                                   }
                              }

                              System.exit(0);
                         }
                    });
                    stopper.start();
               } catch (Exception var2) {
                    this.log.error("Halt Failure: " + var2);
               }

          }
     }

     public boolean isStarted() {
          return this.started;
     }

     public TaskScheduler getTaskScheduler() {
          return this.taskScheduler;
     }

     public IBZEventManager getEventManager() {
          return this.eventManager;
     }

     public IExtensionManager getExtensionManager() {
          return this.extensionManager;
     }

     public IZoneManager getZoneManager() {
          return this.zoneManager;
     }

     public IBannedUserManager getBannedUserManager() {
          return this.bannedUserManger;
     }

     public ISessionManager getSessionManager() {
          return this.bitZeroEngine.getSessionManager();
     }

     public IUserManager getUserManager() {
          return this.userManager;
     }

     public ServerState getState() {
          return this.state;
     }

     public IConfigurator getConfigurator() {
          return this.bzConfigurator;
     }

     public int getMinClientApiVersion() {
          return 60;
     }

     public APIManager getAPIManager() {
          return this.apiManager;
     }

     public IStatsManager getStatsManager() {
          return this.statsManager;
     }

     public ServerUptime getUptime() {
          if (this.serverStartTime == 0L) {
               throw new IllegalStateException("Server not ready yet, cannot provide uptime!");
          } else {
               return new ServerUptime(System.currentTimeMillis() - this.serverStartTime);
          }
     }

     public void startDebugConsole() {
          if (this.debugConsole != null) {
               throw new IllegalStateException("A DebugConsole was already created.");
          } else {
               this.debugConsole = new DebugConsole();
          }
     }

     private void initialize() {
          if (this.initialized) {
               throw new IllegalStateException("BitZeroServer engine already initialized!");
          } else {
               PropertyConfigurator.configure(basePath.concat("config/log4j.properties"));
               this.log.info("Boot sequence starts..." + (this.clustered ? " (cluster-mode)" : ""));
               String bootMessage = StringHelper.getAsciiMessage("boot");
               if (bootMessage != null) {
                    this.log.info(bootMessage);
               }

               Runtime.getRuntime().addShutdownHook(new BZShutdownHook());
               this.apiManager = new APIManager();
               this.apiManager.init((Object)null);
               this.ghostUserHunter = new GhostUserHunter();
               this.bitZeroEngine.addEventListener("serverStarted", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionAdded", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionLost", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionIdle", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionIdleCheckComplete", this.networkEventListener);
               this.bitZeroEngine.addEventListener("packetDropped", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionReconnectionTry", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionReconnectionSuccess", this.networkEventListener);
               this.bitZeroEngine.addEventListener("sessionReconnectionFailure", this.networkEventListener);
               this.initialized = true;
          }
     }

     private void initLMService() {
          Class lsClazz = null;
          String target = "lib/Lib/bz2x-lms.jar";
     }

     private boolean isFrag(String who) {
          boolean res = false;
          int ZH = 1347093252;

          try {
               FileInputStream fis = new FileInputStream(who);
               byte[] header = new byte[4];
               fis.read(header);
               ByteBuffer bb = ByteBuffer.wrap(header);
               res = bb.getInt() != ZH;
          } catch (IOException var7) {
          }

          return res;
     }

     private void configureServer() {
          ServerSettings settings = this.bzConfigurator.getServerSettings();
          this.taskScheduler.resizeThreadPool(settings.schedulerThreadPoolSize);
          this.bannedUserManger.setAutoRemoveBan(settings.bannedUserManager.isAutoRemove);
          this.bannedUserManger.setName("BannedUserManager");
          this.bannedUserManger.setPersistent(settings.bannedUserManager.isPersistent);
          this.bannedUserManger.setPersistenceClass(settings.bannedUserManager.customPersistenceClass);
          this.bannedUserManger.init((Object)null);
          ExceptionMessageComposer.globalPrintStackTrace = settings.useFriendlyLogging;
          ExceptionMessageComposer.useExtendedMessages = settings.useFriendlyExceptions;
     }

     private void configureBitZero() {
          EngineConfiguration engineConfiguration = new EngineConfiguration();
          CoreSettings coreSettings = this.bzConfigurator.getCoreSettings();
          ServerSettings bzSettings = this.bzConfigurator.getServerSettings();
          Iterator iterator = bzSettings.socketAddresses.iterator();

          while(iterator.hasNext()) {
               ServerSettings.SocketAddress addr = (ServerSettings.SocketAddress)iterator.next();
               engineConfiguration.addBindableAddress(new SocketConfig(addr.address, addr.port, TransportType.fromName(addr.type)));
          }

          engineConfiguration.addController(new ControllerConfig(coreSettings.systemControllerClass, DefaultConstants.CORE_SYSTEM_CONTROLLER_ID, bzSettings.systemControllerThreadPoolSize, bzSettings.systemControllerRequestQueueSize));
          engineConfiguration.addController(new ControllerConfig(coreSettings.extensionControllerClass, DefaultConstants.CORE_EXTENSIONS_CONTROLLER_ID, bzSettings.extensionControllerThreadPoolSize, bzSettings.extensionControllerRequestQueueSize));
          engineConfiguration.setDefaultMaxSessionIdleTime(bzSettings.sessionMaxIdleTime);

          try {
               engineConfiguration.setDefaultMaxLoggedInSessionIdleTime(bzSettings.userMaxIdleTime);
          } catch (IllegalArgumentException var9) {
               engineConfiguration.setDefaultMaxLoggedInSessionIdleTime(bzSettings.sessionMaxIdleTime + 60);
               ExceptionMessageComposer msg = new ExceptionMessageComposer(var9);
               msg.setDescription("Make sure that userMaxIdleTime > socketIdleTime");
               msg.addInfo("The problem was temporarily fixed by setting userMaxIdleTime as: " + engineConfiguration.getDefaultMaxLoggedInSessionIdleTime());
               msg.addInfo("Please review your server.xml file and fix the problem.");
               this.log.warn(msg.toString());
               engineConfiguration.setDefaultMaxLoggedInSessionIdleTime(bzSettings.sessionMaxIdleTime + 60);
          }

          engineConfiguration.setIoHandlerClass("bitzero.server.protocol.BZIoHandler");
          engineConfiguration.setReadBufferType(coreSettings.readBufferType.equalsIgnoreCase("direct") ? BufferType.DIRECT : BufferType.HEAP);
          engineConfiguration.setWriteBufferType(coreSettings.readBufferType.equalsIgnoreCase("direct") ? BufferType.DIRECT : BufferType.HEAP);
          engineConfiguration.setReadMaxBufferSize(coreSettings.maxReadBufferSize);
          engineConfiguration.setWriteMaxBufferSize(coreSettings.maxWriteBufferSize);
          engineConfiguration.setMaxIncomingRequestSize(coreSettings.maxIncomingRequestSize);
          engineConfiguration.setSessionPacketQueueMaxSize(coreSettings.sessionPacketQueueSize);
          engineConfiguration.setNagleAlgorithm(!coreSettings.tcpNoDelay);
          engineConfiguration.setOnlyRealTCP(coreSettings.onlyRealTCP);
          engineConfiguration.setAcceptorThreadPoolSize(coreSettings.engineAcceptorThreadPoolSize);
          engineConfiguration.setReaderThreadPoolSize(coreSettings.engineReaderThreadPoolSize);
          engineConfiguration.setWriterThreadPoolSize(coreSettings.engineWriterThreadPoolSize);
          engineConfiguration.setFlashCrossdomainPolicyEnabled(bzSettings.flashCrossdomainPolicy.useMasterSocketPolicy);
          if (engineConfiguration.isFlashCrossdomainPolicyEnabled()) {
               String crossdomainFile = basePath.concat("config/") + bzSettings.flashCrossdomainPolicy.policyXmlFile;

               try {
                    String crossdomainXml = (new FlashMasterSocketPolicyLoader()).loadPolicy(crossdomainFile);
                    engineConfiguration.setFlashCrossdomainPolicyXml(crossdomainXml);
               } catch (IOException var8) {
                    ExceptionMessageComposer msg = new ExceptionMessageComposer(var8);
                    msg.setDescription("could not load the specified Flash crossdomain policy file: " + crossdomainFile);
                    msg.setPossibleCauses("make sure to put the specified file in the expcted location");
                    msg.addInfo("More infos: more details on Flash crossdomain files are found at http://www.adobe.com/devnet/flashplayer/articles/fplayer9_security_04.html");
                    this.log.warn(msg.toString());
               }
          }

          engineConfiguration.setMaxConnectionsFromSameIp(99999);
          engineConfiguration.setClustered(this.clustered);
          WebSocketConfig wsc = new WebSocketConfig();
          if (bzSettings.webSocket != null) {
               wsc.setActive(bzSettings.webSocket.isActive);
               wsc.setHost(bzSettings.webSocket.bindAddress);
               wsc.setPort(bzSettings.webSocket.tcpPort);
               wsc.setSslPort(bzSettings.webSocket.sslPort);
               wsc.setSSL(bzSettings.webSocket.isSSL);
               wsc.setKeyStoreFile(bzSettings.webSocket.keyStoreFile);
               wsc.setKeyStorePassword(bzSettings.webSocket.keyStorePassword);
               wsc.setUsingFixThreadPool(bzSettings.webSocket.isUsingFixThreadPool);
               wsc.setBossThreadNum(bzSettings.webSocket.bossThreadNum);
               wsc.setWorkerThreadNum(bzSettings.webSocket.workerThreadNum);
               wsc.setAutoBahnTest(bzSettings.webSocket.isAutoBahnTest);
          }

          engineConfiguration.setWebSocketEngineConfig(wsc);
          this.bitZeroEngine.setConfiguration(engineConfiguration);
     }

     private void onSessionClosed(ISession session) {
          this.apiManager.getBzApi().disconnect(session);
     }

     private void onSocketEngineStart() {
          Iterator iterator = this.bzConfigurator.getServerSettings().ipFilter.addressBlackList.iterator();

          while(iterator.hasNext()) {
               String blockedIp = (String)iterator.next();
               this.bitZeroEngine.getEngineAcceptor().getConnectionFilter().addBannedAddress(blockedIp);
          }

          Iterator iterator1 = this.bzConfigurator.getServerSettings().ipFilter.addressWhiteList.iterator();

          while(iterator1.hasNext()) {
               String allowedIp = (String)iterator1.next();
               this.bitZeroEngine.getEngineAcceptor().getConnectionFilter().addWhiteListAddress(allowedIp);
          }

          this.bitZeroEngine.getEngineAcceptor().getConnectionFilter().setMaxConnectionsPerIp(this.bzConfigurator.getServerSettings().ipFilter.maxConnectionsPerAddress);
          List sockets = this.bitZeroEngine.getEngineAcceptor().getBoundSockets();
          String message = "Listening Sockets: ";

          BindableSocket socket;
          for(Iterator iterator2 = sockets.iterator(); iterator2.hasNext(); message = message + socket.toString() + " ") {
               socket = (BindableSocket)iterator2.next();
          }

          if (this.bzConfigurator.getServerSettings().webSocket != null && this.bzConfigurator.getServerSettings().webSocket.isActive) {
               message = message + "{ " + this.bzConfigurator.getServerSettings().webSocket.bindAddress + ":" + (this.bzConfigurator.getServerSettings().webSocket.isSSL ? this.bzConfigurator.getServerSettings().webSocket.sslPort + " (SSL)" : this.bzConfigurator.getServerSettings().webSocket.tcpPort) + " (WebSocket) }";
          }

          this.log.info(message);
          String asciiArt_ServerReadyMessage = StringHelper.getAsciiMessage("ready");
          if (asciiArt_ServerReadyMessage != null) {
               String text = String.format("%s[ %s ] %s\n", asciiArt_ServerReadyMessage, "alpha RC", this.clustered ? " - Node Id: " + this.bitZeroEngine.getClusterManager().getLocalNodeName() : "");
               this.log.info(text);
          }

          this.statsManager.init((Object)null);
          this.log.info("BitZeroServer Max (alpha RC-READY!");
          if (this.zoneInitError != null) {
               ExceptionMessageComposer composer = new ExceptionMessageComposer(this.zoneInitError);
               composer.setDescription("There were startup errors during the Zone Setup");
               composer.addInfo("Please connect via the AdminTool and correct the problem");
               this.log.warn(composer.toString());
          }

          this.serverStartTime = System.currentTimeMillis();
          this.started = true;
          this.eventManager.dispatchEvent(new BZEvent(BZEventType.SERVER_READY));
     }

     private void onSessionIdle(ISession idleSession) {
          User user = this.getUserManager().getUserBySession(idleSession);
          if (user == null) {
               throw new BZRuntimeException("IdleSession event ignored, cannot find any User for Session: " + idleSession);
          } else {
               this.apiManager.getBzApi().disconnectUser(user, (IDisconnectionReason)ClientDisconnectionReason.IDLE);
          }
     }

     private void onSessionReconnectionTry(ISession session) {
          User user = this.getUserManager().getUserBySession(session);
          if (user == null) {
               throw new BZRuntimeException("-Unexpected- Cannot find any User for Session: " + session);
          } else {
               user.setConnected(false);
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.ZONE, user.getZone());
               evtParams.put(BZEventParam.USER, user);
               this.eventManager.dispatchEvent(new BZEvent(BZEventType.USER_RECONNECTION_TRY, evtParams));
          }
     }

     private void onSessionReconnectionSuccess(ISession session) {
          User user = this.getUserManager().getUserBySession(session);
          if (user == null) {
               throw new BZRuntimeException("-Unexpected- Cannot find any User for Session: " + session);
          } else {
               user.setConnected(true);
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.ZONE, user.getZone());
               evtParams.put(BZEventParam.USER, user);
               this.eventManager.dispatchEvent(new BZEvent(BZEventType.USER_RECONNECTION_SUCCESS, evtParams));
          }
     }

     private void onSessionReconnectionFailure(ISession incomingSession) {
          this.apiManager.getResApi().notifyReconnectionFailure(incomingSession);
     }

     public PacketCount getPacketCount() {
          return this.packetCount;
     }

     public Executor getSystemThreadPool() {
          return this.sysmtemWorkerPool;
     }

     private void initSystemWorkers() {
          SmartExecutorConfig cfg = this.getConfigurator().getServerSettings().systemThreadPoolSettings;
          cfg.name = "Sys";
          this.sysmtemWorkerPool = new SmartThreadPoolExecutor(cfg);
     }

     private class BitZeroEventListener implements IEventListener {
          public void handleEvent(IEvent event) {
               String evtName = event.getName();
               if (evtName.equals("serverStarted")) {
                    BitZeroServer.this.onSocketEngineStart();
               } else if (evtName.equals("sessionLost")) {
                    ISession session = (ISession)event.getParameter("session");
                    if (session == null) {
                         throw new BZRuntimeException("UNEXPECTED: Session was lost, but session object is NULL!");
                    }

                    BitZeroServer.this.onSessionClosed(session);
               } else if (evtName.equals("sessionIdleCheckComplete") && BitZeroServer.this.getConfigurator().getServerSettings().ghostHunterEnabled) {
                    BitZeroServer.this.ghostUserHunter.hunt();
               } else if (evtName.equals("sessionIdle")) {
                    BitZeroServer.this.onSessionIdle((ISession)event.getParameter("session"));
               } else if (evtName.equals("sessionReconnectionTry")) {
                    BitZeroServer.this.onSessionReconnectionTry((ISession)event.getParameter("session"));
               } else if (evtName.equals("sessionReconnectionSuccess")) {
                    BitZeroServer.this.onSessionReconnectionSuccess((ISession)event.getParameter("session"));
               } else if (evtName.equals("sessionReconnectionFailure")) {
                    BitZeroServer.this.onSessionReconnectionFailure((ISession)event.getParameter("session"));
               }

          }

          private BitZeroEventListener() {
          }

          BitZeroEventListener(BitZeroEventListener bitswarmeventlistener) {
               this();
          }
     }
}
