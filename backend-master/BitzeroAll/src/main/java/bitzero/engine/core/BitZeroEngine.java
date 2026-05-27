package bitzero.engine.core;

import bitzero.engine.boot.SystemPropertiesEnumerator;
import bitzero.engine.clustering.IClusterManager;
import bitzero.engine.config.ControllerConfig;
import bitzero.engine.config.EngineConfiguration;
import bitzero.engine.config.SocketConfig;
import bitzero.engine.controllers.DefaultControllerManager;
import bitzero.engine.controllers.IController;
import bitzero.engine.controllers.IControllerManager;
import bitzero.engine.core.security.DefaultSecurityManager;
import bitzero.engine.core.security.ISecurityManager;
import bitzero.engine.events.Event;
import bitzero.engine.events.IEvent;
import bitzero.engine.events.IEventListener;
import bitzero.engine.exceptions.BootSequenceException;
import bitzero.engine.io.IOHandler;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.service.BaseCoreService;
import bitzero.engine.service.IService;
import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.engine.util.Logging;
import bitzero.engine.util.scheduling.ITaskHandler;
import bitzero.engine.util.scheduling.Scheduler;
import bitzero.engine.websocket.WebSocketService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BitZeroEngine extends BaseCoreService {
     public static final String version = "0.alpha";
     private static BitZeroEngine __engine__;
     private IEngineAcceptor engineAcceptor;
     private IEngineReader engineReader;
     private IEngineWriter engineWriter;
     private WebSocketService webSocketService;
     private Scheduler scheduler;
     private Logger logger;
     private Logger bootLogger;
     private EngineConfiguration configuration;
     private ISessionManager sessionManager;
     private IControllerManager controllerManager;
     private ISecurityManager securityManager;
     private IClusterManager clusterManager;
     private volatile boolean running = false;
     private volatile boolean restarting = false;
     private volatile boolean inited = false;
     private Map coreServicesByName;
     private IEventListener eventHandler;
     private EngineDelayedTaskHandler engineDelayedTaskHandler;
     private volatile int restartCount = 0;

     public static BitZeroEngine getInstance() {
          if (__engine__ == null) {
               __engine__ = new BitZeroEngine();
          }

          return __engine__;
     }

     private BitZeroEngine() {
          this.setName("BitZeroEngine 0.alpha");
     }

     private void initializeServerEngine() {
          this.bootLogger = LoggerFactory.getLogger("bootLogger");
          this.logger = LoggerFactory.getLogger(this.getClass());
          this.inited = true;
     }

     public void start() throws Exception {
          this.start((String)null);
     }

     public void start(String extraLogMessage) throws Exception {
          if (!this.inited) {
               this.initializeServerEngine();
          }

          if (extraLogMessage != null) {
               this.bootLogger.info(extraLogMessage);
          }

          this.eventHandler = new IEventListener() {
               public void handleEvent(IEvent event) {
                    BitZeroEngine.this.dispatchEvent(event);
               }
          };
          this.engineDelayedTaskHandler = new EngineDelayedTaskHandler();
          this.coreServicesByName = new ConcurrentHashMap();
          this.bootSequence();
          ((BaseCoreService)this.engineAcceptor).addEventListener("sessionAdded", this.eventHandler);
          ((BaseCoreService)this.engineReader).addEventListener("sessionLost", this.eventHandler);
          ((BaseCoreService)this.engineWriter).addEventListener("packetDropped", this.eventHandler);
          Event engineStartedEvent = new Event("serverStarted");
          this.dispatchEvent(engineStartedEvent);
          this.running = true;
          if (this.configuration.isClustered()) {
               this.bootLogger.info("Cluster Node Id: " + this.clusterManager.getLocalNodeName());
          }

          this.logger.debug("START IN BITREZEO ENGINE ** ");
     }

     public void restart() {
          Thread runningThread = Thread.currentThread();
          if (!this.securityManager.isEngineThread(runningThread)) {
               this.logger.error(String.format("This thread is not allowed to perform a restart: %s (%s) ", runningThread.getName(), runningThread.getThreadGroup().getName()));
               Logging.logStackTrace(this.logger, runningThread.getStackTrace());
          } else {
               ++this.restartCount;
               if (!this.restarting && this.running) {
                    this.bootLogger.info("Restart Sequence inited...");
                    this.restarting = true;
                    this.running = false;
                    Thread restarter = new Thread(new Runnable() {
                         public void run() {
                              BitZeroEngine.this.halt();
                              BitZeroEngine.this.bootLogger.info("Restart Sequence complete!");
                         }
                    }, "--== Restarter ==--");
                    restarter.start();
               }
          }
     }

     public void halt() {
          try {
               boolean needRestart = this.restarting;
               this.bootLogger.info("Halting Server Engine...");
               ((BaseCoreService)this.engineAcceptor).removeEventListener("sessionAdded", this.eventHandler);
               ((BaseCoreService)this.engineReader).removeEventListener("sessionLost", this.eventHandler);
               this.shutDownSequence();
               this.engineAcceptor = null;
               this.engineReader = null;
               this.engineWriter = null;
               this.eventHandler = null;
               this.engineDelayedTaskHandler = null;
               this.coreServicesByName = null;
               this.restarting = false;
               this.running = false;
               this.bootLogger.info("ShutDown Sequence Complete... ");
               if (needRestart) {
                    System.gc();
                    Thread.sleep(4000L);
                    this.start("Restarting Server Engine...");
               }
          } catch (Throwable var2) {
               this.bootLogger.warn("Error while shutting down the server: " + var2.getMessage());
               Logging.logStackTrace(this.bootLogger, var2);
          }

     }

     public int getRestartCount() {
          return this.restartCount;
     }

     public void bootSequence() throws BootSequenceException, Exception {
          this.bootLogger.info("BitZeroEngine version: alpha{ " + Thread.currentThread().getName() + " }");
          (new SystemPropertiesEnumerator()).logProperties(this.bootLogger);
          this.startCoreServices();
          this.bindSockets(this.configuration.getBindableSockets());
          Iterator iterator = this.coreServicesByName.values().iterator();

          while(iterator.hasNext()) {
               IService service = (IService)iterator.next();
               if (service != null) {
                    service.init((Object)null);
               }
          }

          this.startClusteringServices();
          this.bootLogger.info("[[[ ===--- Boot sequence complete ---=== ]]]");
     }

     public void shutDownSequence() throws Exception {
          this.stopClusteringServices();
          this.stopCoreServices();
     }

     private void startCoreServices() throws Exception {
          this.securityManager = new DefaultSecurityManager();
          this.scheduler = new Scheduler(this.bootLogger);
          Class sessionManagerClass = Class.forName(this.configuration.getSessionManagerClass());
          Method getInstanceMethod = sessionManagerClass.getDeclaredMethod("getInstance");
          if (getInstanceMethod != null) {
               this.sessionManager = (ISessionManager)getInstanceMethod.invoke(sessionManagerClass);
          } else {
               this.sessionManager = (ISessionManager)sessionManagerClass.newInstance();
          }

          this.bootLogger.info("Session manager ready: " + this.sessionManager);
          this.controllerManager = new DefaultControllerManager();
          this.configureControllers();
          this.engineReader = new EngineReader(this.configuration.getReaderThreadPoolSize());
          Class ioHandlerClass = Class.forName(this.configuration.getIoHandlerClass());
          IOHandler ioHandler = (IOHandler)ioHandlerClass.newInstance();
          this.engineReader.setIoHandler(ioHandler);
          this.engineAcceptor = new EngineAcceptor(this.configuration.getAcceptorThreadPoolSize());
          this.engineAcceptor.getConnectionFilter().setMaxConnectionsPerIp(this.configuration.getMaxConnectionsFromSameIp());
          this.engineWriter = new EngineWriter(this.configuration.getWriterThreadPoolSize());
          this.engineWriter.setIOHandler(ioHandler);
          this.webSocketService = new WebSocketService();
          this.coreServicesByName.put("webSocketEngine", this.webSocketService);
          this.securityManager.setName("securityManager");
          this.scheduler.setName("scheduler");
          this.sessionManager.setName("sessionManager");
          this.controllerManager.setName("controllerManager");
          ((BaseCoreService)this.engineAcceptor).setName("engineAcceptor");
          ((BaseCoreService)this.engineReader).setName("engineReader");
          ((BaseCoreService)this.engineWriter).setName("engineWriter");
          this.coreServicesByName.put("scheduler", this.scheduler);
          this.coreServicesByName.put("sessionManager", this.sessionManager);
          this.coreServicesByName.put("controllerManager", this.controllerManager);
          this.coreServicesByName.put("engineAcceptor", (IService)this.engineAcceptor);
          this.coreServicesByName.put("engineReader", (IService)this.engineReader);
          this.coreServicesByName.put("engineWriter", (IService)this.engineWriter);
          this.coreServicesByName.put("securityManager", this.securityManager);
     }

     private void stopCoreServices() throws Exception {
          this.scheduler.destroy((Object)null);
          ((IService)this.engineWriter).destroy((Object)null);
          ((IService)this.engineReader).destroy((Object)null);
          Thread.sleep(2000L);
          this.controllerManager.destroy((Object)null);
          this.sessionManager.destroy((Object)null);
          this.securityManager.destroy((Object)null);
          ((IService)this.engineAcceptor).destroy((Object)null);
     }

     private void startClusteringServices() throws Exception {
          if (this.configuration.isClustered()) {
          }

     }

     private void stopClusteringServices() {
          if (this.configuration.isClustered()) {
               this.clusterManager.destroy((Object)null);
          }

     }

     private void configureControllers() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
          List cfgs = this.configuration.getControllerConfigs();
          Iterator iterator = cfgs.iterator();

          while(iterator.hasNext()) {
               ControllerConfig controllerConfig = (ControllerConfig)iterator.next();
               Class controllerClass = Class.forName(controllerConfig.getClassName());
               IController controller = (IController)controllerClass.newInstance();
               controller.setId(controllerConfig.getId());
               controller.setThreadPoolSize(controllerConfig.getThreadPoolSize());
               controller.setMaxQueueSize(controllerConfig.getMaxRequestQueueSize());
               this.controllerManager.addController(controller.getId(), controller);
          }

     }

     private void bindSockets(List bindableSockets) {
          Iterator iterator = bindableSockets.iterator();

          while(iterator.hasNext()) {
               SocketConfig socketCfg = (SocketConfig)iterator.next();

               try {
                    this.engineAcceptor.bindSocket(socketCfg);
               } catch (IOException var5) {
                    this.bootLogger.warn("Was not able to bind socket: " + socketCfg);
               }
          }

     }

     public IService getServiceByName(String serviceName) {
          return (IService)this.coreServicesByName.get(serviceName);
     }

     public IEngineAcceptor getEngineAcceptor() {
          return this.engineAcceptor;
     }

     public IEngineReader getEngineReader() {
          return this.engineReader;
     }

     public IEngineWriter getEngineWriter() {
          return this.engineWriter;
     }

     public Logger getLogger() {
          return this.logger;
     }

     public void setLogger(Logger logger) {
          this.logger = logger;
     }

     public EngineConfiguration getConfiguration() {
          return this.configuration;
     }

     public void setConfiguration(EngineConfiguration configuration) {
          this.configuration = configuration;
     }

     public ITaskHandler getEngineDelayedTaskHandler() {
          return this.engineDelayedTaskHandler;
     }

     public IControllerManager getControllerManager() {
          return this.controllerManager;
     }

     public ISessionManager getSessionManager() {
          return this.sessionManager;
     }

     public IClusterManager getClusterManager() {
          return this.clusterManager;
     }

     public void init(Object o) {
          throw new UnsupportedOperationException("This call is not supported in this class!");
     }

     public void destroy(Object o) {
          throw new UnsupportedOperationException("This call is not supported in this class!");
     }

     private String threadDump() {
          String dump = "{{{ Thread Dump }}}\n";
          Thread[] threads = new Thread[Thread.activeCount()];
          Thread.enumerate(threads);
          Thread[] athread = threads;
          int j = threads.length;

          for(int i = 0; i < j; ++i) {
               Thread t = athread[i];
               dump = dump + "\t" + t.getName() + ", " + t.getPriority() + ", " + t.getState() + ", daemon: " + t.isDaemon() + "\n";
          }

          return dump;
     }

     public void write(IResponse response) {
          if (this.configuration.getWebSocketEngineConfig().isActive()) {
               List webSocketRecipients = new ArrayList();
               List socketRecipients = new ArrayList();
               Iterator it = response.getRecipients().iterator();

               while(it.hasNext()) {
                    Object _tmp = it.next(); ISession session = (_tmp instanceof bitzero.engine.sessions.ISession) ? (bitzero.engine.sessions.ISession)_tmp : ((bitzero.server.entities.User)_tmp).getSession();
                    if (session == null) continue; // skip bots with no session
                    if (session.isWebsocket()) {
                         webSocketRecipients.add(session);
                    } else {
                         socketRecipients.add(session);
                    }
               }

               this.bootLogger.debug("Web size: " + webSocketRecipients.size());
               this.bootLogger.debug("Socket size: " + socketRecipients.size());
               if (webSocketRecipients.size() > 0) {
                    response.setRecipients((Collection)socketRecipients);
                    IResponse webSocketResponse = Response.clone(response);
                    webSocketResponse.setRecipients((Collection)webSocketRecipients);
                    this.writeToWebSocket(webSocketResponse);
               }
          }

          this.writeToSocket(response);
     }

     private void writeToSocket(IResponse res) {
          this.bootLogger.debug("Write to socket");
          this.engineWriter.getIOHandler().getCodec().onPacketWrite(res);
     }

     private void writeToWebSocket(IResponse res) {
          this.bootLogger.debug("Write to websocket");
          this.webSocketService.getProtocolCodec().onPacketWrite(res);
     }
}
