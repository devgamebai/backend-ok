package bitzero.engine.clustering;

import bitzero.engine.events.IEvent;
import bitzero.engine.service.IService;
import java.util.List;

public interface IClusterManager extends IService {
     String getLocalNodeName();

     void registerLocalNode();

     void removeNode(String var1);

     void clearDataStore();

     boolean isReadyForEvents();

     void dispatchClusterEvent(IEvent var1, String var2);

     void broadcastClusterEvent(IEvent var1, List var2);

     void broadcastClusterEvent(IEvent var1);
}
