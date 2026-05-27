package bitzero.engine.io;

import bitzero.engine.data.IPacket;

public interface IProtocolCodec {
     void onPacketRead(IPacket var1);

     void onPacketWrite(IResponse var1);

     IOHandler getIOHandler();

     void setIOHandler(IOHandler var1);
}
