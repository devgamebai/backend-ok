package bitzero.engine.util.scheduling;

import bitzero.engine.service.IService;
import bitzero.engine.util.Logging;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements IService, Runnable {
     private static AtomicInteger schedulerId = new AtomicInteger(0);
     private volatile int threadId;
     private long SLEEP_TIME;
     private ExecutorService taskExecutor;
     private LinkedList taskList;
     private LinkedList addList;
     private String serviceName;
     private Logger logger;
     private volatile boolean running;

     public Scheduler() {
          this((Logger)null);
     }

     public Scheduler(Logger customLogger) {
          this.threadId = 1;
          this.SLEEP_TIME = 250L;
          this.running = false;
          schedulerId.incrementAndGet();
          this.taskList = new LinkedList();
          this.addList = new LinkedList();
          if (this.logger == null) {
               this.logger = LoggerFactory.getLogger("bootLogger");
          } else {
               this.logger = customLogger;
          }

     }

     public Scheduler(long interval) {
          this();
          this.SLEEP_TIME = interval;
     }

     public void init(Object o) {
          this.startService();
     }

     public void destroy(Object o) {
          this.stopService();
     }

     public String getName() {
          return this.serviceName;
     }

     public void setName(String name) {
          this.serviceName = name;
     }

     public void handleMessage(Object message) {
          throw new UnsupportedOperationException("not supported in this class version");
     }

     public void startService() {
          this.running = true;
          this.taskExecutor = Executors.newSingleThreadExecutor();
          this.taskExecutor.execute(this);
     }

     public void stopService() {
          this.running = false;
          List leftOvers = this.taskExecutor.shutdownNow();
          this.taskExecutor = null;
          this.logger.info("Scheduler stopped. Unprocessed tasks: " + leftOvers.size());
     }

     public void run() {
          Thread.currentThread().setName("Scheduler" + schedulerId.get() + "-thread-" + this.threadId++);
          this.logger.info("Scheduler started: " + this.serviceName);

          while(this.running) {
               try {
                    this.executeTasks();
                    Thread.sleep(this.SLEEP_TIME);
               } catch (InterruptedException var2) {
                    this.logger.warn("Scheduler: " + this.serviceName + " interrupted.");
               } catch (Exception var3) {
                    Logging.logStackTrace(this.logger, "Scheduler: " + this.serviceName + " caught a generic exception: " + var3, var3.getStackTrace());
               }
          }

     }

     public void addScheduledTask(Task task, int interval, boolean loop, ITaskHandler callback) {
          synchronized(this.addList) {
               this.addList.add(new ScheduledTask(task, interval, loop, callback));
          }
     }

     private void executeTasks() {
          long now = System.currentTimeMillis();
          if (this.taskList.size() > 0) {
               synchronized(this.taskList) {
                    Iterator it = this.taskList.iterator();

                    while(it.hasNext()) {
                         ScheduledTask t = (ScheduledTask)it.next();
                         if (!t.task.isActive()) {
                              it.remove();
                         } else if (now >= t.expiry) {
                              try {
                                   t.callback.doTask(t.task);
                              } catch (Exception var10) {
                                   Logging.logStackTrace(this.logger, "Scheduler callback exception. Callback: " + t.callback + ", Exception: " + var10, var10.getStackTrace());
                              }

                              if (t.loop) {
                                   t.expiry += (long)(t.interval * 1000);
                              } else {
                                   it.remove();
                              }
                         }
                    }
               }
          }

          if (this.addList.size() > 0) {
               synchronized(this.taskList) {
                    this.taskList.addAll(this.addList);
                    this.addList.clear();
               }
          }

     }

     private final class ScheduledTask {
          long expiry;
          int interval;
          boolean loop;
          ITaskHandler callback;
          Task task;

          public int getInterval() {
               return this.interval;
          }

          public Task getTask() {
               return this.task;
          }

          public ITaskHandler getCallback() {
               return this.callback;
          }

          public long getExpiry() {
               return this.expiry;
          }

          public boolean isLooping() {
               return this.loop;
          }

          public ScheduledTask(Task t, int interval, boolean loop, ITaskHandler callback) {
               this.task = t;
               this.interval = interval;
               this.expiry = System.currentTimeMillis() + (long)(interval * 1000);
               this.callback = callback;
               this.loop = loop;
          }
     }
}
