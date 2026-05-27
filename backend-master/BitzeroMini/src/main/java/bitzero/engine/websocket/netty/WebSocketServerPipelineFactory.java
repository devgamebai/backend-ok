package bitzero.engine.websocket.netty;

import bitzero.engine.io.IProtocolCodec;
import javax.net.ssl.SSLEngine;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;

public class WebSocketServerPipelineFactory implements ChannelPipelineFactory {
     private final IProtocolCodec codec;
     private boolean isSSL = false;
     private boolean isAutoBahnTest = false;

     public WebSocketServerPipelineFactory(IProtocolCodec codec, boolean isSSL, boolean isAutoBahnTest) {
          this.isSSL = isSSL;
          this.codec = codec;
          this.isAutoBahnTest = isAutoBahnTest;
     }

     public ChannelPipeline getPipeline() throws Exception {
          ChannelPipeline pipeline = Channels.pipeline();
          if (this.isSSL) {
               SSLEngine engine = WebSocketSslServerSslContext.getInstance().getServerContext().createSSLEngine();
               engine.setUseClientMode(false);
               pipeline.addLast("ssl", new SslHandler(engine));
          }

          pipeline.addLast("decoder", new HttpRequestDecoder());
          pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
          pipeline.addLast("encoder", new HttpResponseEncoder());
          pipeline.addLast("handler", new WebSocketServerHandler(this.codec, this.isSSL, this.isAutoBahnTest));
          return pipeline;
     }
}
