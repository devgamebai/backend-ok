/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package bitzero.engine.util.scheduling;

import bitzero.engine.service.IService;
import bitzero.engine.util.Logging;
import bitzero.engine.util.scheduling.ITaskHandler;
import bitzero.engine.util.scheduling.Task;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler
implements IService,
Runnable {
    private static AtomicInteger schedulerId = new AtomicInteger(0);
    private volatile int threadId = 1;
    private long SLEEP_TIME = 250;
    private ExecutorService taskExecutor;
    private LinkedList taskList;
    private LinkedList addList;
    private String serviceName;
    private Logger logger;
    private volatile boolean running = false;

    public Scheduler() {
        this(null);
    }

    public Scheduler(Logger customLogger) {
        schedulerId.incrementAndGet();
        this.taskList = new LinkedList();
        this.addList = new LinkedList();
        this.logger = this.logger == null ? LoggerFactory.getLogger((String)"bootLogger") : customLogger;
    }

    public Scheduler(long interval) {
        this();
        this.SLEEP_TIME = interval;
    }

    @Override
    public void init(Object o) {
        this.startService();
    }

    @Override
    public void destroy(Object o) {
        this.stopService();
    }

    @Override
    public String getName() {
        return this.serviceName;
    }

    @Override
    public void setName(String name) {
        this.serviceName = name;
    }

    @Override
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
        List<Runnable> leftOvers = this.taskExecutor.shutdownNow();
        this.taskExecutor = null;
        this.logger.info("Scheduler stopped. Unprocessed tasks: " + leftOvers.size());
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Scheduler" + schedulerId.get() + "-thread-" + this.threadId++);
        this.logger.info("Scheduler started: " + this.serviceName);
        while (this.running) {
            try {
                this.executeTasks();
                Thread.sleep(this.SLEEP_TIME);
            }
            catch (InterruptedException ie) {
                this.logger.warn("Scheduler: " + this.serviceName + " interrupted.");
            }
            catch (Exception e) {
                Logging.logStackTrace(this.logger, "Scheduler: " + this.serviceName + " caught a generic exception: " + e, e.getStackTrace());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addScheduledTask(Task task, int interval, boolean loop, ITaskHandler callback) {
        LinkedList linkedList = this.addList;
        synchronized (linkedList) {
            this.addList.add(new ScheduledTask(task, interval, loop, callback));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void executeTasks() {
        LinkedList linkedList;
        long now = System.currentTimeMillis();
        if (this.taskList.size() > 0) {
            linkedList = this.taskList;
            synchronized (linkedList) {
                Iterator it = this.taskList.iterator();
                while (it.hasNext()) {
                    ScheduledTask t = (ScheduledTask)it.next();
                    if (!t.task.isActive()) {
                        it.remove();
                        continue;
                    }
                    if (now < t.expiry) continue;
                    try {
                        t.callback.doTask(t.task);
                    }
                    catch (Exception e) {
                        Logging.logStackTrace(this.logger, "Scheduler callback exception. Callback: " + t.callback + ", Exception: " + e, e.getStackTrace());
                    }
                    if (t.loop) {
                        t.expiry += (long)(t.interval * 1000);
                        continue;
                    }
                    it.remove();
                }
            }
        }
        if (this.addList.size() > 0) {
            linkedList = this.taskList;
            synchronized (linkedList) {
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

