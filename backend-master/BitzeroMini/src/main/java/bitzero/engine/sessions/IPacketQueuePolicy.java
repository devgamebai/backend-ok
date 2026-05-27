package bitzero.engine.sessions;

import bitzero.engine.data.IPacket;
import bitzero.engine.exceptions.PacketQueueWarning;

public interface IPacketQueuePolicy {
     void applyPolicy(IPacketQueue var1, IPacket var2) throws PacketQueueWarning;
}
