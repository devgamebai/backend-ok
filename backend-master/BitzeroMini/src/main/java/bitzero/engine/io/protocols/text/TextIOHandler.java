package bitzero.engine.io.protocols.text;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.data.IPacket;
import bitzero.engine.data.Packet;
import bitzero.engine.data.TransportType;
import bitzero.engine.io.AbstractIOHandler;
import bitzero.engine.sessions.ISession;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextIOHandler extends AbstractIOHandler {
     private static final String PROP_BUFFER = "buffer";
     private static final char[] END_OF_MESSAGE = new char[1];
     private static final String EOM;
     private final BitZeroEngine engine = BitZeroEngine.getInstance();
     private final Logger logger = LoggerFactory.getLogger(TextIOHandler.class);

     public TextIOHandler() {
          this.codec = new TextProtocolCodec();
          this.codec.setIOHandler(this);
     }

     public long getIncomingDroppedPackets() {
          return 0L;
     }

     public void onDataRead(DatagramChannel channel, SocketAddress address, byte[] data) {
          if (data == null) {
               throw new IllegalArgumentException("Did not expect null byte array!");
          } else {
               String fullMsg = new String(data);
               IPacket packet = new Packet();
               packet.setTransportType(TransportType.UDP);
               packet.setData(fullMsg);
               packet.setOriginalSize(fullMsg.length());
               this.codec.onPacketRead(packet);
          }
     }

     public void onDataRead(ISession session, byte[] data) {
          if (data == null) {
               throw new IllegalArgumentException("Did not expect null byte array!");
          } else {
               String rawStr = new String(data);
               StringBuilder buffer = (StringBuilder)session.getSystemProperty("buffer");
               if (buffer == null) {
                    buffer = new StringBuilder();
                    session.setSystemProperty("buffer", buffer);
               }

               buffer.append(rawStr);
               int msgCount = 0;

               for(int posEOM = buffer.indexOf(EOM); posEOM != -1; posEOM = buffer.indexOf(EOM)) {
                    ++msgCount;
                    String fullMsg = buffer.substring(0, posEOM).trim();
                    int msgSize = fullMsg.length();
                    IPacket packet = new Packet();
                    packet.setData(fullMsg);
                    packet.setSender(session);
                    packet.setOriginalSize(fullMsg.length());
                    this.codec.onPacketRead(packet);
                    buffer.delete(0, posEOM + 1);
               }

          }
     }

     public void onDataWrite(IPacket packet) {
          String message = (String)packet.getData();
          message = message + EOM;
          packet.setData(message.getBytes());
          this.engine.getEngineWriter().enqueuePacket(packet);
     }

     private String getHexDump(byte[] byteData) {
          String dump = "";

          for(int j = 0; j < byteData.length; ++j) {
               String hexByte = Integer.toHexString(byteData[j]).toUpperCase();
               if (hexByte.length() == 1) {
                    hexByte = "0" + hexByte;
               }

               dump = dump + hexByte + " ";
          }

          return dump;
     }

     static {
          EOM = new String(END_OF_MESSAGE);
     }
}
