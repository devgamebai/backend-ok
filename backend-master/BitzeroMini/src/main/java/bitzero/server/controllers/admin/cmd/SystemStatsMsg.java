package bitzero.server.controllers.admin.cmd;

import bitzero.server.controllers.SystemRequest;
import bitzero.server.extensions.data.BaseMsg;

public class SystemStatsMsg extends BaseMsg {
     public long totalInPacket = 0L;
     public long totalOutPacket = 0L;
     public long totalInBytes = 0L;
     public long totalOutBytes = 0L;
     public long totalOutgoingDroppedPackets = 0L;
     public long totalIncomingDroppedPackets = 0L;
     public int connectionCount = 0;
     public int mobileCount = 0;
     public int totalUserCount = 0;
     public double cpuLoad = -1.0D;
     public long memLoad = -1L;
     public long totalInPacketWebsocket = 0L;
     public long totalOutPacketWebsocket = 0L;
     public long totalInBytesWebsocket = 0L;
     public long totalOutBytesWebsocket = 0L;
     public int wsCount = 0;

     public SystemStatsMsg() {
          super((Short)SystemRequest.SystemStats.getId());
     }
}
