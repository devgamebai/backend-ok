package bitzero.engine.sessions;

import bitzero.engine.config.EngineConfiguration;
import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.events.Event;
import bitzero.engine.exceptions.BitZeroEngineRuntimeException;
import bitzero.engine.exceptions.SessionReconnectionException;
import bitzero.engine.service.IService;
import bitzero.engine.util.Logging;
import bitzero.engine.util.scheduling.ITaskHandler;
import bitzero.engine.util.scheduling.Scheduler;
import bitzero.engine.util.scheduling.Task;
import bitzero.engine.websocket.IWebSocketChannel;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultSessionManager implements ISessionManager {
     private static final String SESSION_CLEANING_TASK_ID = "SessionCleanerTask";
     private static final int SESSION_CLEANING_INTERVAL_SECONDS = 10;
     private static ISessionManager __instance__;
     private final Logger bootLogger = LoggerFactory.getLogger("bootLogger");
     private Logger logger;
     private ConcurrentMap sessionsByNode;
     private ConcurrentMap sessionsById;
     private BitZeroEngine engine = null;
     private EngineConfiguration config = null;
     private final List localSessions = new ArrayList();
     private final ConcurrentMap localSessionsById = new ConcurrentHashMap();
     private final ConcurrentMap localSessionsByConnection = new ConcurrentHashMap();
     private final ConcurrentMap localSessionsByToken = new ConcurrentHashMap();
     private String serviceName = "DefaultSessionManager";
     private Task sessionCleanTask;
     private Scheduler systemScheduler;
     IReconnectionManager reconnectionManager;
     private IPacketQueuePolicy packetQueuePolicy;
     private int highestCCS = 0;
     private volatile int reconnectTimes = 0;
     private volatile int reconnectSuccessTimes = 0;
     private volatile int ghostReconnectTimes = 0;

     public static ISessionManager getInstance() {
          if (__instance__ == null) {
               __instance__ = new DefaultSessionManager();
          }

          return __instance__;
     }

     private DefaultSessionManager() {
          if (this.sessionsByNode == null) {
               this.sessionsByNode = new ConcurrentHashMap();
          }

          if (this.sessionsById == null) {
               this.sessionsById = new ConcurrentHashMap();
          }

          this.reconnectionManager = new DefaultReconnectionManager(this);
     }

     public int[] getReconnectStatus() {
          return new int[]{this.reconnectTimes, this.reconnectSuccessTimes, this.ghostReconnectTimes};
     }

     public void init(Object o) {
          this.engine = BitZeroEngine.getInstance();
          this.config = this.engine.getConfiguration();
          this.logger = LoggerFactory.getLogger(DefaultSessionManager.class);
          this.systemScheduler = (Scheduler)this.engine.getServiceByName("scheduler");
          this.sessionCleanTask = new Task("SessionCleanerTask");
          this.systemScheduler.addScheduledTask(this.sessionCleanTask, 10, true, new SessionCleaner((SessionCleaner)null));
          ((IService)this.reconnectionManager).init(this.systemScheduler);

          try {
               Class packetPolicyClass = Class.forName(this.config.getPacketQueuePolicyClass());
               this.packetQueuePolicy = (IPacketQueuePolicy)packetPolicyClass.newInstance();
          } catch (Exception var3) {
               this.bootLogger.warn("SessionManager could not load a valid PacketQueuePolicy. Reason: " + var3);
               Logging.logStackTrace(this.bootLogger, (Throwable)var3);
          }

     }

     public void destroy(Object o) {
          this.sessionCleanTask.setActive(false);
          ((IService)this.reconnectionManager).destroy((Object)null);
          this.shutDownLocalSessions();
          this.localSessionsById.clear();
          this.localSessionsByConnection.clear();
     }

     public void publishLocalNode(String nodeId) {
          if (this.sessionsByNode.get(nodeId) != null) {
               throw new IllegalStateException("NodeID already exists in the cluster: " + nodeId);
          } else {
               this.sessionsByNode.put(nodeId, this.localSessions);
          }
     }

     public int getHighestCCS() {
          return this.highestCCS;
     }

     public void addSession(ISession session) {
          synchronized(this.localSessions) {
               this.localSessions.add(session);
          }

          this.localSessionsById.put(session.getId(), session);
          if (session.getType() == SessionType.DEFAULT) {
               this.localSessionsByConnection.put(session.getConnection(), session);
          }

          if (this.localSessions.size() > this.highestCCS) {
               this.highestCCS = this.localSessions.size();
          }

     }

     public void addSessionToken(ISession session) {
          this.localSessionsByToken.put(session.getHashId(), session);
     }

     public ISession getSessionbyToken(String key) {
          ISession reSession = null;

          try {
               reSession = (ISession)this.localSessionsByToken.get(key);
               if (reSession != null) {
                    ++this.ghostReconnectTimes;
               }
          } catch (Exception var4) {
               reSession = null;
          }

          return reSession;
     }

     public boolean containsSession(ISession session) {
          return this.localSessionsById.containsValue(session);
     }

     public void removeSession(ISession session) {
          if (session != null) {
               synchronized(this.localSessions) {
                    this.localSessions.remove(session);
               }

               SocketChannel connection = session.getConnection();
               int id = session.getId();
               this.localSessionsById.remove(id);
               this.localSessionsByToken.remove(session.getHashId());
               if (connection != null) {
                    this.localSessionsByConnection.remove(connection);
               }

               if (session.getType() == SessionType.DEFAULT || session.getType() == SessionType.WEBSOCKET) {
                    this.engine.getEngineAcceptor().getConnectionFilter().removeAddress(session.getAddress());
               }

               if (this.config.isClustered()) {
                    this.sessionsById.remove(id);
               }

               this.logger.info("Session removed: " + session);
          }
     }

     public ISession removeSession(int id) {
          ISession session = (ISession)this.localSessionsById.get(id);
          if (session != null) {
               this.removeSession(session);
          }

          return session;
     }

     public ISession removeSession(String hash) {
          throw new UnsupportedOperationException("Not implemented yet!");
     }

     public ISession removeSession(SocketChannel connection) {
          ISession session = this.getLocalSessionByConnection(connection);
          if (session != null) {
               this.removeSession(session);
          }

          return session;
     }

     public void removeChannel(SocketChannel connection) {
          this.localSessionsByConnection.remove(connection);
     }

     public void onSocketDisconnected(SocketChannel connection) throws IOException {
          ISession session = (ISession)this.localSessionsByConnection.get(connection);
          if (session != null) {
               this.localSessionsByConnection.remove(connection);
               session.setConnected(false);
               this.onSocketDisconnected(session);
          }
     }

     public void onSocketDisconnected(ISession session) throws IOException {
          if (session.getReconnectionSeconds() > 0) {
               this.reconnectionManager.onSessionLost(session);
               this.dispatchSessionReconnectionTryEvent(session);
          } else {
               this.removeSession(session);
               this.dispatchLostSessionEvent(session);
          }

     }

     public ISession reconnectSession(ISession tempSession, String sessionToken) throws SessionReconnectionException, IOException {
          ISession resumedSession = null;

          try {
               ++this.reconnectTimes;
               resumedSession = this.reconnectionManager.reconnectSession(tempSession, sessionToken);
          } catch (SessionReconnectionException var5) {
               throw var5;
          }

          ++this.reconnectSuccessTimes;
          this.localSessionsByConnection.put(tempSession.getConnection(), resumedSession);
          tempSession.setConnection((SocketChannel)null);
          this.logger.info("Session was resurrected: " + resumedSession + ", using temp Session: " + tempSession + ", " + resumedSession.getReconnectionSeconds());
          return resumedSession;
     }

     public List getAllLocalSessions() {
          List allSessions = null;
          synchronized(this.localSessions) {
               allSessions = new ArrayList(this.localSessions);
               return allSessions;
          }
     }

     public List getAllSessions() {
          List sessions = null;
          sessions = this.config.isClustered() ? new LinkedList(this.sessionsById.values()) : this.getAllLocalSessions();
          return (List)sessions;
     }

     public List getAllSessionsAtNode(String nodeName) {
          List allSessions = null;
          List theSessions = (List)this.sessionsByNode.get(nodeName);
          if (theSessions != null) {
               allSessions = new ArrayList(theSessions);
          }

          return allSessions;
     }

     public ISession getLocalSessionByHash(String hash) {
          throw new UnsupportedOperationException("Not implemented yet!");
     }

     public ISession getLocalSessionById(int id) {
          return (ISession)this.localSessionsById.get(id);
     }

     public ISession getSessionByHash(String hash) {
          throw new UnsupportedOperationException("Not implemented yet!");
     }

     public ISession getLocalSessionByConnection(SocketChannel connection) {
          return (ISession)this.localSessionsByConnection.get(connection);
     }

     public ISession getSessionById(int id) {
          return (ISession)this.sessionsById.get(id);
     }

     public void shutDownLocalSessions() {
          synchronized(this.localSessions) {
               Iterator it = this.localSessions.iterator();

               while(it.hasNext()) {
                    ISession session = (ISession)it.next();
                    it.remove();

                    try {
                         session.close();
                    } catch (IOException var6) {
                         this.bootLogger.warn("I/O Error while closing session: " + session);
                    }
               }

          }
     }

     public void onNodeLost(String nodeId) {
          List nodeSessions = (List)this.sessionsByNode.remove(nodeId);
          if (nodeSessions == null) {
               throw new IllegalStateException("Unable to remove node sessions from cluster. Lost Node ID: " + nodeId);
          } else {
               synchronized(this.sessionsById) {
                    Iterator iterator = nodeSessions.iterator();

                    while(iterator.hasNext()) {
                         ISession session = (ISession)iterator.next();
                         this.sessionsById.remove(session.getId());
                    }

               }
          }
     }

     public void clearClusterData() {
          this.sessionsById.clear();
          this.sessionsByNode.clear();
     }

     public String getName() {
          return this.serviceName;
     }

     public void setName(String name) {
          this.serviceName = name;
     }

     public void handleMessage(Object message) {
          throw new UnsupportedOperationException("Not implemented in this class!");
     }

     public ISession createSession(SocketChannel connection) {
          ISession session = new Session();
          session.setSessionManager(this);
          session.setConnection(connection);
          session.setMaxIdleTime(this.engine.getConfiguration().getDefaultMaxSessionIdleTime());
          session.setNodeId(this.config.isClustered() ? this.engine.getClusterManager().getLocalNodeName() : "");
          session.setType(SessionType.DEFAULT);
          session.setReconnectionSeconds(this.engine.getConfiguration().getGlobalReconnectionSeconds());
          IPacketQueue packetQueue = new NonBlockingPacketQueue(this.engine.getConfiguration().getSessionPacketQueueMaxSize());
          packetQueue.setPacketQueuePolicy(this.packetQueuePolicy);
          session.setPacketQueue(packetQueue);
          return session;
     }

     public ISession createConnectionlessSession() {
          ISession session = new Session();
          session.setSessionManager(this);
          session.setNodeId(this.config.isClustered() ? this.engine.getClusterManager().getLocalNodeName() : "");
          session.setType(SessionType.VOID);
          session.setConnected(true);
          return session;
     }

     public ISession createBlueBoxSession() {
          ISession session = new Session();
          session.setSessionManager(this);
          session.setMaxIdleTime(this.engine.getConfiguration().getDefaultMaxSessionIdleTime());
          session.setNodeId(this.config.isClustered() ? this.engine.getClusterManager().getLocalNodeName() : "");
          session.setType(SessionType.BLUEBOX);
          IPacketQueue packetQueue = new NonBlockingPacketQueue(this.engine.getConfiguration().getSessionPacketQueueMaxSize());
          packetQueue.setPacketQueuePolicy(this.packetQueuePolicy);
          session.setPacketQueue(packetQueue);
          return session;
     }

     public int getLocalSessionCount() {
          return this.localSessions.size();
     }

     public int getNodeSessionCount(String nodeId) {
          List nodeSessionList = (List)this.sessionsByNode.get(nodeId);
          if (nodeSessionList == null) {
               throw new BitZeroEngineRuntimeException("Can't find session count for requested node in the cluster. Node not found: " + nodeId);
          } else {
               return nodeSessionList.size();
          }
     }

     private void applySessionCleaning() {
          if (this.getLocalSessionCount() > 0) {
               Iterator iterator = this.getAllLocalSessions().iterator();

               while(iterator.hasNext()) {
                    ISession session = (ISession)iterator.next();
                    if (session != null && !session.isFrozen()) {
                         if (session.isMarkedForEviction()) {
                              this.terminateSession(session);
                              this.logger.info("Terminated idle logged-in session: " + session);
                         } else if (session.isIdle()) {
                              if (session.isLoggedIn()) {
                                   this.logger.debug("Firing Client Disconnection " + session);
                                   session.setMarkedForEviction();
                                   this.dispatchSessionIdleEvent(session);
                              } else {
                                   this.terminateSession(session);
                                   this.logger.debug("Removed idle session: " + session);
                              }
                         }
                    }
               }
          }

          Event event = new Event("sessionIdleCheckComplete");
          this.engine.dispatchEvent(event);
     }

     public void terminateSession(ISession session) {
          if (session.getType() == SessionType.DEFAULT) {
               SocketChannel connection = session.getConnection();
               session.setReconnectionSeconds(0);

               try {
                    if (connection.socket() != null) {
                         connection.socket().shutdownInput();
                         connection.socket().shutdownOutput();
                         connection.close();
                    }

                    session.setConnected(false);
               } catch (IOException var4) {
                    this.logger.warn("Failed closing connection while removing idle Session: " + session);
               }
          } else if (session.isWebsocket()) {
               IWebSocketChannel channel = (IWebSocketChannel)session.getSystemProperty("wsChannel");
               channel.close();
               return;
          }

          this.removeSession(session);
          this.dispatchLostSessionEvent(session);
     }

     private void dispatchLostSessionEvent(ISession closedSession) {
          Event event = new Event("sessionLost");
          event.setParameter("session", closedSession);
          this.engine.dispatchEvent(event);
     }

     private void dispatchSessionIdleEvent(ISession idleSession) {
          Event event = new Event("sessionIdle");
          event.setParameter("session", idleSession);
          this.engine.dispatchEvent(event);
     }

     private void dispatchSessionReconnectionTryEvent(ISession session) {
          Event event = new Event("sessionReconnectionTry");
          event.setParameter("session", session);
          this.engine.dispatchEvent(event);
     }

     public ISession createWebSocketSession(Object channel) {
          Session session = new Session();
          session.setSessionManager(this);
          session.setMaxIdleTime(this.engine.getConfiguration().getDefaultMaxSessionIdleTime());
          session.setNodeId(this.config.isClustered() ? this.engine.getClusterManager().getLocalNodeName() : "");
          session.setType(SessionType.WEBSOCKET);
          session.setConnected(true);
          IPacketQueue packetQueue = new NonBlockingPacketQueue(this.engine.getConfiguration().getSessionPacketQueueMaxSize());
          packetQueue.setPacketQueuePolicy(this.packetQueuePolicy);
          session.setPacketQueue(packetQueue);
          session.setSystemProperty("wsChannel", channel);
          return session;
     }

     private final class SessionCleaner implements ITaskHandler {
          public void doTask(Task task) throws Exception {
               DefaultSessionManager.this.applySessionCleaning();
          }

          private SessionCleaner() {
          }

          SessionCleaner(SessionCleaner sessioncleaner) {
               this();
          }
     }
}
