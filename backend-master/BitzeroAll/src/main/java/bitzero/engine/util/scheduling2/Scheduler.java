package bitzero.engine.util.scheduling2;

import bitzero.engine.service.IService;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements IService {
     private static AtomicInteger schedulerId = new AtomicInteger(0);
     private String serviceName;
     private Logger logger;
     private final ScheduledThreadPoolExecutor executor;

     public Scheduler(int poolSize) {
          this(poolSize, (Logger)null);
     }

     public Scheduler(int poolSize, Logger customLogger) {
          if (poolSize < 1) {
               throw new IllegalArgumentException("Cannot create a thread pool of size: " + poolSize);
          } else {
               this.serviceName = "Scheduler-" + schedulerId.getAndIncrement();
               this.executor = new ScheduledThreadPoolExecutor(poolSize);
               if (this.logger == null) {
                    this.logger = LoggerFactory.getLogger("bootLogger");
               } else {
                    this.logger = customLogger;
               }

          }
     }

     public void init(Object o) {
          this.logger.info(this.serviceName + " started.");
     }

     public void destroy(Object o) {
          List awaitingExecution = this.executor.shutdownNow();
          this.logger.info(this.serviceName + " stopping. Tasks awaiting execution: " + awaitingExecution.size());
     }

     public String getName() {
          return this.serviceName;
     }

     public void handleMessage(Object message) {
          throw new UnsupportedOperationException(this.serviceName + ": operation not supported.");
     }

     public void setName(String name) {
          this.serviceName = name;
     }

     public void resizeThreadPool(int poolSize) {
          this.executor.setCorePoolSize(poolSize);
     }

     public int getThreadPoolSize() {
          return this.executor.getCorePoolSize();
     }

     public int getActiveThreadCount() {
          return this.executor.getActiveCount();
     }
}
