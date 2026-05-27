package bitzero.server.util.deadlock;

import bitzero.util.common.business.CommonHandle;
import com.google.gson.Gson;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;

public class ThreadDeadlockDetector {
     private final Timer threadCheck;
     private final ThreadMXBean mbean;
     private final Collection listeners;
     private static final int DEFAULT_DEADLOCK_CHECK_PERIOD = 60000;

     public ThreadDeadlockDetector() {
          this(60000);
     }

     public ThreadDeadlockDetector(int deadlockCheckPeriod) {
          this.threadCheck = new Timer("DeadlockDetector", true);
          this.mbean = ManagementFactory.getThreadMXBean();
          this.listeners = new CopyOnWriteArraySet();
          this.threadCheck.schedule(new TimerTask() {
               public void run() {
                    ThreadDeadlockDetector.this.checkForDeadlocks();
               }
          }, 10L, (long)deadlockCheckPeriod);
     }

     private void checkForDeadlocks() {
          long[] ids = this.findDeadlockedThreads();
          if (ids != null && ids.length > 0) {
               CommonHandle.writeErrLogDebug("Dead lock detected!");
               CommonHandle.writeErrLogDebug("");
               Thread[] threads = new Thread[ids.length];

               for(int i = 0; i < threads.length; ++i) {
                    ThreadInfo tInfo = this.mbean.getThreadInfo(ids[i]);
                    CommonHandle.writeErrLogDebug((new Gson()).toJson(tInfo));
                    CommonHandle.writeErrLogDebug("TheadId : " + tInfo.getThreadId());
                    CommonHandle.writeErrLogDebug("TheadName : " + tInfo.getThreadName());
                    CommonHandle.writeErrLogDebug("LockName : " + tInfo.getLockName());
                    CommonHandle.writeErrLogDebug("LockOwnerId : " + tInfo.getLockOwnerId());
                    CommonHandle.writeErrLogDebug("LockOwnerName : " + tInfo.getLockOwnerName());
                    threads[i] = this.findMatchingThread(tInfo);
                    CommonHandle.writeErrLogDebug("");
               }

               CommonHandle.writeErrLogDebug("Dead check");
               this.fireDeadlockDetected(threads);
          }

     }

     private long[] findDeadlockedThreads() {
          return this.mbean.isSynchronizerUsageSupported() ? this.mbean.findDeadlockedThreads() : this.mbean.findMonitorDeadlockedThreads();
     }

     private void fireDeadlockDetected(Thread[] threads) {
          Iterator var2 = this.listeners.iterator();

          while(var2.hasNext()) {
               Listener l = (Listener)var2.next();
               l.deadlockDetected(threads);
          }

     }

     private Thread findMatchingThread(ThreadInfo inf) {
          Iterator var2 = Thread.getAllStackTraces().keySet().iterator();

          Thread thread;
          do {
               if (!var2.hasNext()) {
                    throw new IllegalStateException("Deadlocked Thread not found");
               }

               thread = (Thread)var2.next();
          } while(thread.getId() != inf.getThreadId());

          return thread;
     }

     public boolean addListener(Listener l) {
          return this.listeners.add(l);
     }

     public boolean removeListener(Listener l) {
          return this.listeners.remove(l);
     }

     public interface Listener {
          void deadlockDetected(Thread[] var1);
     }
}
