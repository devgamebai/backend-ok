package bitzero.engine.core;

import bitzero.engine.io.IOHandler;
import java.nio.channels.Selector;

public interface IEngineReader {
     Selector getSelector();

     IOHandler getIOHandler();

     void setIoHandler(IOHandler var1);

     long getReadBytes();

     long getReadPackets();
}
