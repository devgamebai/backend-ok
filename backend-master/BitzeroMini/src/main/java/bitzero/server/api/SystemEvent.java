package bitzero.server.api;

import java.util.Map;

public class SystemEvent {
     public SystemEventType type;
     private Object source;
     private Map params;

     public SystemEvent(Object source, Map params) {
          this.source = source;
          this.params = params;
     }

     public Map getParams() {
          return this.params;
     }

     public static enum SystemEventType {
          LOGIN,
          DISCONNECT,
          CREATE_ROOM,
          JOIN_ROOM,
          DELETE_ROOM,
          OUT_ROOM,
          JOIN_CHANNEL,
          OUT_CHANNEL;
     }
}
