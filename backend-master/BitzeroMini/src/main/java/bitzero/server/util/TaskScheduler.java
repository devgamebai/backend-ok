package bitzero.server.util;

import bitzero.engine.service.IService;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskScheduler implements IService {
     private static AtomicInteger schedulerId = new AtomicInteger(0);
     private final ScheduledThreadPoolExecutor taskScheduler;
     private final String serviceName;
     private final Logger logger = LoggerFactory.getLogger(this.getClass());

     public TaskScheduler(int threadPoolSize) {
          this.serviceName = "TaskScheduler-" + schedulerId.getAndIncrement();
          this.taskScheduler = new ScheduledThreadPoolExecutor(threadPoolSize);
     }

     public void init(Object o) {
          this.logger.info(this.serviceName + " started.");
     }

     public void destroy(Object o) {
          List awaitingExecution = this.taskScheduler.shutdownNow();
          this.logger.info(this.serviceName + " stopping. Tasks awaiting execution: " + awaitingExecution.size());
     }

     public String getName() {
          return this.serviceName;
     }

     public void handleMessage(Object obj) {
     }

     public void setName(String s) {
     }

     public ScheduledFuture schedule(Runnable task, int delay, TimeUnit unit) {
          this.logger.debug("Task scheduled: " + task + ", " + delay + " " + unit);
          return this.taskScheduler.schedule(task, (long)delay, unit);
     }

     public ScheduledFuture scheduleAtFixedRate(Runnable task, int initialDelay, int period, TimeUnit unit) {
          return this.taskScheduler.scheduleAtFixedRate(task, (long)initialDelay, (long)period, unit);
     }

     public Boolean remove(Runnable task) {
          return this.taskScheduler.remove(task);
     }

     public void resizeThreadPool(int threadPoolSize) {
          this.taskScheduler.setCorePoolSize(threadPoolSize);
     }

     public int getThreadPoolSize() {
          return this.taskScheduler.getCorePoolSize();
     }
}
