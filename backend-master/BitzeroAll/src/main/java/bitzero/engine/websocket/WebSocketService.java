package bitzero.engine.websocket;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.service.BaseCoreService;
import bitzero.engine.websocket.netty.WebSocketBoot;

public class WebSocketService extends BaseCoreService {
     private volatile boolean inited = false;
     private final WebSocketStats webSocketStats = new WebSocketStats();
     private final WebSocketProtocolCodec protocolCodec;
     private final boolean isActive = true;

     public WebSocketService() {
          this.protocolCodec = new WebSocketProtocolCodec(this.webSocketStats);
     }

     public void init(Object o) {
          if (this.inited) {
               throw new IllegalArgumentException("Service is already initialized. Destroy it first!");
          } else {
               this.inited = true;
                o = BitZeroEngine.getInstance().getConfiguration().getWebSocketEngineConfig();
               new WebSocketBoot((WebSocketConfig)o, this.protocolCodec);
          }
     }

     public void destroy(Object o) {
          super.destroy(o);
     }

     public boolean isActive() {
          return this.isActive;
     }

     public WebSocketStats getWebSocketStats() {
          return this.webSocketStats;
     }

     public WebSocketProtocolCodec getProtocolCodec() {
          return this.protocolCodec;
     }
}
