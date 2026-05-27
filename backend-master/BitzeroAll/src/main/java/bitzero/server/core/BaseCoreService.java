package bitzero.server.core;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseCoreService implements ICoreService {
     private static final AtomicInteger serviceId = new AtomicInteger(0);
     private static final String DEFAULT_NAME = "AnonymousService-";
     protected String name;
     protected volatile boolean active = false;

     public void init(Object o) {
          this.name = getId();
          this.active = true;
     }

     public void destroy(Object o) {
          this.active = false;
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }

     public void handleMessage(Object param) {
          throw new UnsupportedOperationException("This method should be overridden by the child class!");
     }

     public boolean isActive() {
          return this.active;
     }

     public String toString() {
          return "[Core Service]: " + this.name + ", State: " + (this.isActive() ? "active" : "not active");
     }

     private static String getId() {
          return "AnonymousService-" + serviceId.getAndIncrement();
     }
}
