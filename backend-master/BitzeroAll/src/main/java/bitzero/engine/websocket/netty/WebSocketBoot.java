package bitzero.engine.websocket.netty;

import bitzero.engine.io.IProtocolCodec;
import bitzero.engine.util.Logging;
import bitzero.engine.websocket.WebSocketConfig;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketBoot {
     private final WebSocketConfig config;
     private final IProtocolCodec protocolCodec;
     private final Logger logger = LoggerFactory.getLogger("debug");

     public WebSocketBoot(WebSocketConfig cfg, IProtocolCodec codec) {
          this.config = cfg;
          this.protocolCodec = codec;
          if (cfg.isActive()) {
               try {
                    this.boot();
                    this.logger.info("WebSocketService started: " + this.config.getHost() + ":" + this.config.getPort());
                    if (this.config.isSSL()) {
                         this.bootSSL();
                         this.logger.info("WebSocketService SSL started: " + this.config.getHost() + ":" + this.config.getSslPort());
                    }
               } catch (Exception var4) {
                    this.logger.error("Failed starting up websocket engine: " + var4.getMessage());
                    Logging.logStackTrace(this.logger, (Throwable)var4);
               }

          }
     }

     private void boot() throws Exception {
          int webSocketPort = this.config.getPort();
          ServerBootstrap boot;
          if (!this.config.isUsingFixThreadPool()) {
               this.logger.info("Websocket Using Cached Thread Pool");
               boot = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
          } else {
               this.logger.info("Websocket Using Fixed Thread Pool");
               boot = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newFixedThreadPool(this.config.getBossThreadNum()), Executors.newFixedThreadPool(this.config.getWorkerThreadNum())));
          }

          boot.setPipelineFactory(new WebSocketServerPipelineFactory(this.protocolCodec, false, this.config.isAutoBahnTest()));
          boot.bind(new InetSocketAddress(this.config.getHost(), webSocketPort));
     }

     public void bootSSL() {
          System.setProperty("keystore.file.path", this.config.getKeyStoreFile());
          System.setProperty("keystore.file.password", this.config.getKeyStorePassword());
          int webSocketPort = this.config.getSslPort();
          ServerBootstrap boot;
          if (!this.config.isUsingFixThreadPool()) {
               this.logger.info("Websocket Using Cached Thread Pool");
               boot = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
          } else {
               this.logger.info("Websocket Using Fixed Thread Pool");
               boot = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newFixedThreadPool(this.config.getBossThreadNum()), Executors.newFixedThreadPool(this.config.getWorkerThreadNum())));
          }

          boot.setPipelineFactory(new WebSocketServerPipelineFactory(this.protocolCodec, true, this.config.isAutoBahnTest()));
          boot.bind(new InetSocketAddress(this.config.getHost(), webSocketPort));
     }
}
