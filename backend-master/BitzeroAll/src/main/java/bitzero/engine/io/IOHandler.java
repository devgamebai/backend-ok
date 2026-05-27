package bitzero.engine.io;

import bitzero.engine.data.IPacket;
import bitzero.engine.sessions.ISession;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public interface IOHandler extends IFilterSupport {
     void onDataRead(ISession var1, byte[] var2);

     void onDataRead(DatagramChannel var1, SocketAddress var2, byte[] var3);

     void onDataWrite(IPacket var1);

     IProtocolCodec getCodec();

     void setCodec(IProtocolCodec var1);

     long getReadPackets();

     long getIncomingDroppedPackets();
}
