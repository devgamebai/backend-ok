package bitzero.engine.core;

import bitzero.engine.io.IOHandler;
import bitzero.engine.service.BaseCoreService;
import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.engine.util.Logging;
import bitzero.engine.util.NetworkServices;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineReader extends BaseCoreService implements IEngineReader, Runnable {
     private final BitZeroEngine engine;
     private final Logger logger;
     private final Logger bootLogger;
     private int threadPoolSize;
     private final ExecutorService threadPool;
     private ISessionManager sessionManager;
     private IEngineAcceptor socketAcceptor;
     private IEngineWriter socketWriter;
     private Selector readSelector;
     private IOHandler ioHandler;
     private volatile boolean isActive;
     private volatile long readBytes;
     private volatile int threadId;

     public EngineReader() {
          this(1);
     }

     public EngineReader(int nThreads) {
          this.threadId = 0;
          this.isActive = false;
          this.readBytes = 0L;
          this.threadPoolSize = nThreads;
          this.threadPool = Executors.newSingleThreadExecutor();
          this.engine = BitZeroEngine.getInstance();
          this.logger = LoggerFactory.getLogger(this.getClass());
          this.bootLogger = LoggerFactory.getLogger("bootLogger");

          try {
               this.readSelector = Selector.open();
               this.bootLogger.info("TCP Selector opened");
          } catch (IOException var3) {
               this.bootLogger.error("Failed opening Read Selector: " + var3.toString());
               var3.printStackTrace();
          }

     }

     public void init(Object o) {
          super.init(o);
          if (this.isActive) {
               throw new IllegalArgumentException("Object is already initialized. Destroy it first!");
          } else {
               this.sessionManager = this.engine.getSessionManager();
               this.socketAcceptor = this.engine.getEngineAcceptor();
               this.socketWriter = this.engine.getEngineWriter();
               this.isActive = true;
               this.initThreadPool();
               this.bootLogger.info("IOHandler: " + this.ioHandler);
               this.bootLogger.info("EngineReader started");
          }
     }

     public void destroy(Object o) {
          super.destroy(o);
          this.isActive = false;
          List leftOvers = this.threadPool.shutdownNow();

          try {
               Thread.sleep(500L);
               this.readSelector.close();
          } catch (Exception var4) {
               this.bootLogger.warn("Error when shutting down Accept TCP selector: " + var4.getMessage());
               Logging.logStackTrace(this.bootLogger, (Throwable)var4);
          }

          this.bootLogger.info("EngineReader stopped. Unprocessed tasks: " + leftOvers.size());
     }

     public void initThreadPool() {
          for(int j = 0; j < this.threadPoolSize; ++j) {
               this.threadPool.execute(this);
          }

     }

     public void run() {
          ByteBuffer readBuffer = NetworkServices.allocateBuffer(this.engine.getConfiguration().getReadMaxBufferSize(), this.engine.getConfiguration().getReadBufferType());
          Thread.currentThread().setName("EngineReader-" + this.threadId++);

          while(this.isActive) {
               try {
                    this.socketAcceptor.handleAcceptableConnections();
                    this.readIncomingSocketData(readBuffer);
                    Thread.sleep(5L);
               } catch (Throwable var3) {
                    this.logger.warn("Problems in EngineReader main loop: " + var3 + ", Thread: " + Thread.currentThread());
                    Logging.logStackTrace(this.logger, var3);
               }
          }

          this.bootLogger.info("EngineReader threadpool shutting down.");
     }

     private void readIncomingSocketData(ByteBuffer readBuffer) {
          SocketChannel channel = null;
          SelectionKey key = null;

          try {
               int readyKeyCount = this.readSelector.selectNow();
               if (readyKeyCount > 0) {
                    Set readyKeys = this.readSelector.selectedKeys();
                    Iterator it = readyKeys.iterator();

                    while(it.hasNext()) {
                         key = (SelectionKey)it.next();
                         it.remove();
                         if (key.isValid()) {
                              channel = (SocketChannel)key.channel();
                              readBuffer.clear();

                              try {
                                   this.readTcpData(channel, key, readBuffer);
                              } catch (IOException var8) {
                                   this.closeTcpConnection(channel);
                                   this.logger.info("Socket closed: " + channel);
                              }
                         }
                    }
               }
          } catch (ClosedSelectorException var9) {
               this.logger.debug("Selector is closed!");
          } catch (CancelledKeyException var10) {
               this.logger.debug("Cancelled Key!");
          } catch (IOException var11) {
               this.logger.warn("I/O reading/selection error: " + var11);
               Logging.logStackTrace(this.logger, (Throwable)var11);
          } catch (Exception var12) {
               this.logger.warn("Generic reading/selection error: " + var12);
               Logging.logStackTrace(this.logger, (Throwable)var12);
          }

     }

     private void readTcpData(SocketChannel channel, SelectionKey key, ByteBuffer readBuffer) throws IOException {
          ISession session = this.sessionManager.getLocalSessionByConnection(channel);
          if (key.isWritable()) {
               key.interestOps(1);
               this.socketWriter.continueWriteOp(session);
          }

          if (key.isReadable()) {
               readBuffer.clear();
               long byteCount = 0L;
               byteCount = (long)channel.read(readBuffer);
               if (byteCount == -1L) {
                    this.closeTcpConnection(channel);
               } else if (byteCount > 0L) {
                    session.setLastReadTime(System.currentTimeMillis());
                    this.readBytes += byteCount;
                    session.addReadBytes(byteCount);
                    readBuffer.flip();
                    byte[] binaryData = new byte[readBuffer.limit()];
                    readBuffer.get(binaryData);
                    this.ioHandler.onDataRead(session, binaryData);
               }

          }
     }

     private void closeTcpConnection(SelectableChannel channel) throws IOException {
          channel.close();
          if (channel instanceof SocketChannel) {
               this.sessionManager.onSocketDisconnected((SocketChannel)channel);
          }

     }

     private void readUdpData(DatagramChannel channel) throws IOException {
     }

     public IOHandler getIOHandler() {
          return this.ioHandler;
     }

     public Selector getSelector() {
          return this.readSelector;
     }

     public void setIoHandler(IOHandler handler) {
          if (handler == null) {
               throw new IllegalStateException("IOHandler was already injected!");
          } else {
               this.ioHandler = handler;
          }
     }

     public long getReadBytes() {
          return this.readBytes;
     }

     public long getReadPackets() {
          return this.ioHandler.getReadPackets();
     }
}
