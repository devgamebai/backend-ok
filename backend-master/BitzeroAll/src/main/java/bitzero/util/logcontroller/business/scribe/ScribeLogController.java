package bitzero.util.logcontroller.business.scribe;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import bitzero.util.config.bean.ConstantMercury;
import bitzero.util.logcontroller.business.ILogController;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.scribe.LogEntry;
import org.apache.scribe.scribe.Client;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScribeLogController implements ILogController, Runnable {
     public static final boolean IS_METRICLOG = ConfigHandle.instance().getLong("isMetriclog") == 1L;
     protected TSocket socket;
     protected TTransport transport;
     protected TProtocol protocol;
     protected Client client;
     protected List entrys = new ArrayList();
     protected BlockingQueue requestQueue;
     protected ExecutorService threadPool;
     private final Logger logger;
     private volatile boolean isActive;
     private volatile int Num = 0;

     public ScribeLogController() {
          this.socket = new TSocket(ConfigHandle.instance().get("lservers"), ConstantMercury.SCRIBE_PORT, 30);
          this.transport = new TFramedTransport(this.socket);
          this.protocol = new TBinaryProtocol(this.transport, false, false);
          this.client = new Client(this.protocol, this.protocol);
          this.isActive = IS_METRICLOG;
          this.logger = LoggerFactory.getLogger("scriber");
          this.requestQueue = new LinkedBlockingQueue();
          this.threadPool = Executors.newSingleThreadExecutor();
          this.threadPool.execute(this);
     }

     public void writeLog(LogMode mode, String data) {
          if (IS_METRICLOG) {
               this.requestQueue.add(new LogEntry(ConfigHandle.instance().get(mode.value() + "_log_category"), data));
               if (this.Num > 36000) {
                    synchronized(this.entrys) {
                         this.requestQueue.clear();
                    }

                    this.Num = 0;
               }

          }
     }

     public Boolean flushAll_backup() {
          if (this.entrys.isEmpty()) {
               return true;
          } else {
               try {
                    if (!this.transport.isOpen()) {
                         this.transport.open();
                    }

                    this.client.Log(this.entrys);
                    this.entrys.clear();
                    this.transport.close();
                    this.Num = 0;
                    return true;
               } catch (TTransportException var2) {
                    CommonHandle.writeWarnLog(var2);
                    ++this.Num;
                    return false;
               } catch (TException var3) {
                    CommonHandle.writeWarnLog(var3);
                    ++this.Num;
                    return false;
               } catch (Exception var4) {
                    CommonHandle.writeWarnLog(var4);
                    ++this.Num;
                    return false;
               }
          }
     }

     public Boolean flushAll() {
          if (this.entrys.isEmpty()) {
               return true;
          } else {
               try {
                    if (!this.transport.isOpen()) {
                         this.transport.open();
                    }

                    this.client.Log(this.entrys);
                    this.entrys.clear();
                    this.transport.close();
                    this.Num = 0;
                    return true;
               } catch (Exception var3) {
                    Debug.warn(ExceptionUtils.getStackTrace(var3));
                    ++this.Num;
                    String category = ConfigHandle.instance().get("local_category");
                    if (category != null && !category.equals("")) {
                         this.writeLocalLog(category, this.entrys);
                    }

                    this.entrys.clear();
                    return false;
               }
          }
     }

     private synchronized void writeLocalLog(String category, List entrys) {
          BufferedWriter writer = null;

          try {
               String timeLog = (new SimpleDateFormat("yyyyMMdd_HH")).format(Calendar.getInstance().getTime());

               for(int i = 0; i < entrys.size(); ++i) {
                    (new File(category + ((LogEntry)entrys.get(i)).getCategory() + "/")).mkdirs();
                    File logFile = new File(category + ((LogEntry)entrys.get(i)).getCategory() + "/log-" + timeLog + ".txt");
                    writer = new BufferedWriter(new FileWriter(logFile, true));
                    writer.write(((LogEntry)entrys.get(i)).getMessage());
                    writer.newLine();
                    writer.close();
               }
          } catch (Exception var15) {
               Debug.warn("WRITE LOCAL LOG EXCEPTION");
               Debug.warn(ExceptionUtils.getStackTrace(var15));
          } finally {
               try {
                    writer.close();
               } catch (Exception var14) {
               }

          }

     }

     public void run() {
          Thread.currentThread().setName("Scriber Log");

          while(this.isActive) {
               try {
                    LogEntry entry = (LogEntry)this.requestQueue.take();
                    this.entrys.add(entry);
                    this.flushAll();
               } catch (Throwable var2) {
                    this.logger.warn("Problems in Scriber Log main loop: " + var2 + ", Thread: " + Thread.currentThread());
               }
          }

          System.out.print("ScribeLogController threadpool shutting down.");
     }

     public void writeLog(String mode, String data) {
          if (IS_METRICLOG) {
               String catalog = ConfigHandle.instance().get(mode);
               if (catalog == null || catalog.length() < 2) {
                    catalog = mode;
               }

               this.requestQueue.add(new LogEntry(catalog, data));
               if (this.Num > 36000) {
                    synchronized(this.entrys) {
                         this.requestQueue.clear();
                    }

                    this.Num = 0;
               }

          }
     }

     public void writeLog(LogMode mode, String data, String folderName) {
     }
}
