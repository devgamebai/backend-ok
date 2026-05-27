package bitzero.engine.core;

import bitzero.engine.data.IPacket;
import bitzero.engine.io.IOHandler;
import bitzero.engine.sessions.ISession;

public interface IEngineWriter {
     IOHandler getIOHandler();

     void setIOHandler(IOHandler var1);

     void continueWriteOp(ISession var1);

     void enqueuePacket(IPacket var1);

     long getDroppedPacketsCount();

     long getWrittenBytes();

     long getWrittenPackets();

     int getQueueSize();

     int getThreadPoolSize();
}
