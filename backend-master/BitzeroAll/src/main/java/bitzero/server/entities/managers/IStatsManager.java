package bitzero.server.entities.managers;

import bitzero.server.core.ICoreService;
import bitzero.server.util.stats.INetworkTrafficMeter;

public interface IStatsManager extends ICoreService {
     long getTotalOutBytes();

     long getTotalOutBytesWebsocket();

     long getTotalInBytes();

     long getTotalInBytesWebsocket();

     long getTotalOutPackets();

     long getTotalOutPacketsWebsocket();

     long getTotalInPackets();

     long getTotalInPacketsWebsocket();

     long getTotalOutgoingDroppedPackets();

     long getTotalIncomingDroppedPackets();

     long getTotalIncomingDroppedWebsocketFrames();

     long getTotalIncomingDroppedWebsocketPackets();

     INetworkTrafficMeter getIncomingTrafficMeter();

     INetworkTrafficMeter getOutgoingTrafficMeter();

     ConnectionStats getSessionStats();

     ConnectionStats getUserStats();
}
