package bitzero.engine.websocket;

import bitzero.engine.data.IPacket;
import bitzero.engine.data.Packet;
import bitzero.engine.io.IOHandler;
import bitzero.engine.io.IRequest;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Request;
import bitzero.engine.io.protocols.AbstractProtocolCodec;
import bitzero.engine.sessions.ISession;
import bitzero.engine.util.ByteUtils;
import bitzero.server.BitZeroServer;
import bitzero.server.controllers.SystemRequest;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class WebSocketProtocolCodec extends AbstractProtocolCodec {
     private static final String CONTROLLER_ID = "c";
     private static final String ACTION_ID = "a";
     private static final String PARAM_ID = "p";
     private final WebSocketStats webSocketStats;

     public WebSocketProtocolCodec(WebSocketStats wss) {
          this.webSocketStats = wss;
     }

     public void onPacketRead(IPacket packet) {
          if (packet == null) {
               throw new IllegalStateException("Protocol Codec didn't expect a null packet!");
          } else {
               ByteBuffer requestObject = null;
               if (packet.isTcp()) {
                    ByteBuffer buff = (ByteBuffer)packet.getData();
                    byte[] rawPacket = buff.array();
                    if (rawPacket.length < 1024 && BitZeroServer.isDebug()) {
                         this.logger.debug(ByteUtils.fullHexDump(rawPacket));
                    }

                    try {
                         requestObject = buff;
                         buff.rewind();
                    } catch (Exception var6) {
                         this.logger.warn("Error deserializing request: " + var6);
                    }
               } else if (packet.isUdp()) {
                    requestObject = (ByteBuffer)packet.getData();
               }

               if (requestObject != null) {
                    this.logger.debug(requestObject.toString());
                    this.dispatchRequest(requestObject, packet);
               }

          }
     }

     public void onPacketWrite(IResponse response) {
          byte[] binData = (byte[])((byte[])response.getContent());
          ByteBuffer packetBuffer = ByteBuffer.allocate(3 + binData.length);
          packetBuffer.put((Byte)response.getTargetController());
          packetBuffer.putShort((Short)response.getId());
          packetBuffer.put(binData);
          IPacket packet = new Packet();
          packet.setId((Short)response.getId());
          packet.setTransportType(response.getTransportType());
          packet.setData(packetBuffer.array());
          packet.setRecipients(response.getRecipients());
          if (response.getRecipients().size() > 0 && this.logger.isDebugEnabled()) {
               this.logger.debug("{OUT}: " + SystemRequest.fromId(response.getId()));
          }

          byte[] rawPacket = (byte[])((byte[])packet.getData());
          if (rawPacket.length < 1024 && BitZeroServer.isDebug()) {
               this.logger.debug(ByteUtils.fullHexDump(rawPacket));
          }

          ChannelBuffer cb = ChannelBuffers.wrappedBuffer(rawPacket);
          Iterator var7 = response.getRecipients().iterator();

          while(var7.hasNext()) {
               Object tmp = var7.next();
               ISession session = (ISession)tmp;
               IWebSocketChannel channel = (IWebSocketChannel)session.getSystemProperty("wsChannel");
               channel.write(cb);
               session.addWrittenBytes((long)rawPacket.length);
               int bytesLen = rawPacket.length;
               this.webSocketStats.addWrittenPackets(1);
               this.webSocketStats.addWrittenBytes(bytesLen);
          }

     }

     private void dispatchRequest(ByteBuffer requestObject, IPacket packet) {
          if (requestObject.capacity() < 3) {
               throw new IllegalStateException("Request rejected: No Controller ID in request!");
          } else {
               IRequest request = new Request();
               Object controllerKey = null;
               requestObject.get();
               requestObject.getShort();
               controllerKey = requestObject.get();
               request.setId(requestObject.getShort());
               request.setContent(requestObject.compact());
               request.setSender(packet.getSender());
               request.setTransportType(packet.getTransportType());
               this.dispatchRequestToController(request, controllerKey);
          }
     }

     public IOHandler getIOHandler() {
          throw new UnsupportedOperationException("Now Allowed!");
     }

     public void setIOHandler(IOHandler handler) {
          throw new UnsupportedOperationException("Now Allowed!");
     }
}
