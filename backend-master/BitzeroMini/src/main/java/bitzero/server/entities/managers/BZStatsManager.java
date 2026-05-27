package bitzero.server.entities.managers;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.sessions.ISession;
import bitzero.engine.websocket.WebSocketService;
import bitzero.engine.websocket.WebSocketStats;
import bitzero.server.BitZeroServer;
import bitzero.server.util.stats.INetworkTrafficMeter;
import bitzero.server.util.stats.NetworkTrafficMeter;
import bitzero.server.util.stats.TrafficType;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZStatsManager implements IStatsManager {
     private INetworkTrafficMeter inMeter;
     private INetworkTrafficMeter outMeter;
     private BitZeroEngine engine;
     private BitZeroServer bz;
     private NetworkStatsExecutor statsExecutor;
     private ScheduledFuture taskControl;
     private WebSocketService webSocketService;
     private WebSocketStats wsStats;

     public long getTotalOutBytesWebsocket() {
          return this.wsStats.getWrittenBytes();
     }

     public long getTotalInBytesWebsocket() {
          return this.wsStats.getReadBytes();
     }

     public long getTotalOutPacketsWebsocket() {
          return this.wsStats.getWrittenPackets();
     }

     public long getTotalInPacketsWebsocket() {
          return this.wsStats.getReadPackets();
     }

     public long getTotalIncomingDroppedWebsocketFrames() {
          return this.wsStats.getDroppedInFrames();
     }

     public long getTotalIncomingDroppedWebsocketPackets() {
          return this.wsStats.getDroppedInPackets();
     }

     public void init(Object o) {
          this.engine = BitZeroEngine.getInstance();
          this.bz = BitZeroServer.getInstance();
          this.inMeter = new NetworkTrafficMeter(TrafficType.INCOMING);
          this.outMeter = new NetworkTrafficMeter(TrafficType.OUTGOING);
          this.statsExecutor = new NetworkStatsExecutor();
          this.taskControl = this.bz.getTaskScheduler().scheduleAtFixedRate(this.statsExecutor, 0, 1, TimeUnit.MINUTES);
          this.webSocketService = (WebSocketService)this.engine.getServiceByName("webSocketEngine");
          this.wsStats = this.webSocketService.getWebSocketStats();
     }

     public void destroy(Object obj) {
          this.taskControl.cancel(true);
     }

     public long getTotalInPackets() {
          return this.engine.getEngineReader().getReadPackets();
     }

     public long getTotalOutPackets() {
          return this.engine.getEngineWriter().getWrittenPackets();
     }

     public long getTotalInBytes() {
          return this.engine.getEngineReader().getReadBytes();
     }

     public long getTotalOutBytes() {
          return this.engine.getEngineWriter().getWrittenBytes();
     }

     public long getTotalOutgoingDroppedPackets() {
          return this.engine.getEngineWriter().getDroppedPacketsCount();
     }

     public INetworkTrafficMeter getIncomingTrafficMeter() {
          return this.inMeter;
     }

     public INetworkTrafficMeter getOutgoingTrafficMeter() {
          return this.outMeter;
     }

     public long getTotalIncomingDroppedPackets() {
          return this.engine.getEngineReader().getIOHandler().getIncomingDroppedPackets();
     }

     public ConnectionStats getSessionStats() {
          List allSessions = this.bz.getSessionManager().getAllLocalSessions();
          int socketCount = 0;
          int npcCount = 0;
          int bboxCount = 0;
          int wsCount = 0;
          Iterator iterator = allSessions.iterator();

          while(iterator.hasNext()) {
               ISession session = (ISession)iterator.next();
               if (session.isMobile()) {
                    ++npcCount;
               } else if (session.isWebsocket()) {
                    ++wsCount;
               } else {
                    ++socketCount;
               }
          }

          return new ConnectionStats(socketCount, npcCount, npcCount, wsCount);
     }

     public ConnectionStats getUserStats() {
          return this.getSessionStats();
     }

     public boolean isActive() {
          return true;
     }

     public String getName() {
          return "StatsManager Service";
     }

     public void handleMessage(Object message) {
          throw new UnsupportedOperationException("Not available");
     }

     public void setName(String name) {
          throw new UnsupportedOperationException("Not available");
     }

     private class NetworkStatsExecutor implements Runnable {
          private final Logger log = LoggerFactory.getLogger(NetworkStatsExecutor.class);

          public void run() {
               try {
                    BZStatsManager.this.inMeter.onTick();
                    BZStatsManager.this.outMeter.onTick();
                    BZStatsManager.this.bz.getExtensionManager().monitor();
               } catch (Exception var2) {
                    this.log.warn("Unexpected exception: " + var2 + ". NetworkStatsExecutor will resume on next call.");
               }

          }

          public NetworkStatsExecutor() {
          }
     }
}
