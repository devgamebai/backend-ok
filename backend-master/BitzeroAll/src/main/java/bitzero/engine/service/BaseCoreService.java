package bitzero.engine.service;

import bitzero.engine.events.IEvent;
import bitzero.engine.events.IEventDispatcher;
import bitzero.engine.events.IEventListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class BaseCoreService implements IService, IEventDispatcher {
     private String serviceName;
     private Map listenersByEvent = new ConcurrentHashMap();

     public void init(Object obj) {
     }

     public void destroy(Object o) {
          this.listenersByEvent.clear();
     }

     public String getName() {
          return this.serviceName;
     }

     public void setName(String name) {
          this.serviceName = name;
     }

     public void handleMessage(Object obj) {
     }

     public synchronized void addEventListener(String eventType, IEventListener listener) {
          Set listeners = (Set)this.listenersByEvent.get(eventType);
          if (listeners == null) {
               listeners = new CopyOnWriteArraySet();
               this.listenersByEvent.put(eventType, listeners);
          }

          ((Set)listeners).add(listener);
     }

     public boolean hasEventListener(String eventType) {
          boolean found = false;
          Set listeners = (Set)this.listenersByEvent.get(eventType);
          if (listeners != null && listeners.size() > 0) {
               found = true;
          }

          return found;
     }

     public void removeEventListener(String eventType, IEventListener listener) {
          Set listeners = (Set)this.listenersByEvent.get(eventType);
          if (listeners != null) {
               listeners.remove(listener);
          }

     }

     public void dispatchEvent(IEvent event) {
          Set listeners = (Set)this.listenersByEvent.get(event.getName());
          if (listeners != null && listeners.size() > 0) {
               Iterator iterator = listeners.iterator();

               while(iterator.hasNext()) {
                    IEventListener listenerObj = (IEventListener)iterator.next();
                    listenerObj.handleEvent(event);
               }
          }

     }
}
