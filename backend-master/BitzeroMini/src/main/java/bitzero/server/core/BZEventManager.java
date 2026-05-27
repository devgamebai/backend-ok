package bitzero.server.core;

import bitzero.util.common.business.CommonHandle;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BZEventManager extends BaseCoreService implements IBZEventManager {
     private int corePoolSize = 20;
     private int maxPoolSize = 30;
     private int threadKeepAliveTime = 60;
     private final ThreadPoolExecutor threadPool;
     private final Map listenersByEvent = new ConcurrentHashMap();
     private final Logger logger = LoggerFactory.getLogger(BZEventManager.class);

     public BZEventManager() {
          this.setName("BZEventManager");
          this.threadPool = new ThreadPoolExecutor(this.corePoolSize, this.maxPoolSize, (long)this.threadKeepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue());
     }

     public void init(Object o) {
          super.init(o);
          this.logger.info(this.name + " initalized");
     }

     public void destroy(Object o) {
          super.destroy(o);
          this.listenersByEvent.clear();
          this.logger.info(this.name + " shut down.");
     }

     public void setThreadPoolSize(int poolSize) {
          this.threadPool.setCorePoolSize(poolSize);
     }

     public synchronized void addEventListener(IBZEventType type, IBZEventListener listener) {
          Set listeners = (Set)this.listenersByEvent.get(type);
          if (listeners == null) {
               listeners = new CopyOnWriteArraySet();
               this.listenersByEvent.put(type, listeners);
          }

          ((Set)listeners).add(listener);
     }

     public boolean hasEventListener(IBZEventType type) {
          boolean found = false;
          Set listeners = (Set)this.listenersByEvent.get(type);
          if (listeners != null && listeners.size() > 0) {
               found = true;
          }

          return found;
     }

     public synchronized void removeEventListener(IBZEventType type, IBZEventListener listener) {
          Set listeners = (Set)this.listenersByEvent.get(type);
          if (listeners != null) {
               listeners.remove(listener);
          }

     }

     public void dispatchEvent(IBZEvent event) {
          Set listeners = (Set)this.listenersByEvent.get(event.getType());
          if (listeners != null && listeners.size() > 0) {
               Iterator iterator = listeners.iterator();

               while(iterator.hasNext()) {
                    IBZEventListener listener = (IBZEventListener)iterator.next();
                    this.threadPool.execute(new BZEventRunner(listener, event));
               }
          }

     }

     public void dispatchImmediateEvent(IBZEvent event) {
          Set listeners = (Set)this.listenersByEvent.get(event.getType());
          if (listeners != null && listeners.size() > 0) {
               Iterator iterator = listeners.iterator();

               while(iterator.hasNext()) {
                    IBZEventListener listener = (IBZEventListener)iterator.next();
                    this.run(listener, event);
               }
          }

     }

     private void run(IBZEventListener listener, IBZEvent event) {
          try {
               listener.handleServerEvent(event);
          } catch (Exception var4) {
               CommonHandle.writeErrLog((Throwable)var4);
               LoggerFactory.getLogger(this.getClass()).warn("Error in event immediate handler: " + var4 + ", Event: " + event + " Listener: " + listener);
          }

     }

     public Executor getThreadPool() {
          return this.threadPool;
     }

     private static final class BZEventRunner implements Runnable {
          private final IBZEventListener listener;
          private final IBZEvent event;

          public void run() {
               try {
                    this.listener.handleServerEvent(this.event);
               } catch (Exception var2) {
                    CommonHandle.writeErrLog((Throwable)var2);
                    LoggerFactory.getLogger(this.getClass()).warn("Error in event handler: " + var2 + ", Event: " + this.event + " Listener: " + this.listener);
               }

          }

          public BZEventRunner(IBZEventListener listener, IBZEvent event) {
               this.listener = listener;
               this.event = event;
          }
     }
}
