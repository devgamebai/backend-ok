package bitzero.server.core;

import java.util.Map;

public class BZEvent implements IBZEvent {
     private final IBZEventType type;
     private final Map params;

     public BZEvent(IBZEventType type) {
          this(type, (Map)null);
     }

     public BZEvent(IBZEventType type, Map params) {
          this.type = type;
          this.params = params;
     }

     public IBZEventType getType() {
          return this.type;
     }

     public Object getParameter(IBZEventParam id) {
          Object param = null;
          if (this.params != null) {
               param = this.params.get(id);
          }

          return param;
     }

     public String toString() {
          return String.format("{ %s, Params: %s }", this.type, this.params == null ? "none" : this.params.keySet());
     }
}
