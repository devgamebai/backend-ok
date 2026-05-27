package bitzero.engine.websocket;

import java.net.SocketAddress;
import org.jboss.netty.buffer.ChannelBuffer;

public interface IWebSocketChannel {
     void write(ChannelBuffer var1);

     SocketAddress getRemoteAddress();

     SocketAddress getLocalAddress();

     void close();
}
