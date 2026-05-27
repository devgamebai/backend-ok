package bitzero.engine.core;

import bitzero.engine.config.SocketConfig;
import bitzero.engine.core.security.DefaultConnectionFilter;
import bitzero.engine.core.security.IConnectionFilter;
import bitzero.engine.data.BindableSocket;
import bitzero.engine.data.TransportType;
import bitzero.engine.events.Event;
import bitzero.engine.exceptions.RefusedAddressException;
import bitzero.engine.service.BaseCoreService;
import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.engine.util.Logging;
import bitzero.util.datacontroller.business.DataController;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineAcceptor extends BaseCoreService implements IEngineAcceptor, Runnable {
     private final BitZeroEngine engine;
     private final Logger logger;
     private final Logger bootLogger;
     private volatile int threadId;
     private int threadPoolSize;
     private final ExecutorService threadPool;
     private List acceptableConnections;
     private List boundSockets;
     private IConnectionFilter connectionFilter;
     private ISessionManager sessionManager;
     private IEngineReader socketReader;
     private Selector acceptSelector;
     private volatile boolean isActive;

     public EngineAcceptor() {
          this(1);
     }

     public EngineAcceptor(int threadPoolSize) {
          this.threadId = 1;
          this.threadPoolSize = 1;
          this.isActive = false;
          this.threadPoolSize = threadPoolSize;
          this.engine = BitZeroEngine.getInstance();
          this.logger = LoggerFactory.getLogger(this.getClass());
          this.bootLogger = LoggerFactory.getLogger("bootLogger");
          this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
          this.acceptableConnections = new ArrayList();
          this.boundSockets = new ArrayList();
          this.socketReader = this.engine.getEngineReader();
          this.connectionFilter = new DefaultConnectionFilter();

          try {
               this.acceptSelector = Selector.open();
               this.bootLogger.info("AcceptSelector opened");
          } catch (IOException var3) {
               this.bootLogger.warn("Problems during EngineAcceptor init: " + var3);
               Logging.logStackTrace(this.bootLogger, (Throwable)var3);
          }

     }

     public void init(Object o) {
          super.init(o);
          if (this.isActive) {
               throw new IllegalArgumentException("Object is already initialized. Destroy it first!");
          } else if (this.threadPoolSize < 1) {
               throw new IllegalArgumentException("Illegal value for a thread pool size: " + this.threadPoolSize);
          } else {
               this.sessionManager = this.engine.getSessionManager();
               this.isActive = true;
               this.initThreadPool();
               this.bootLogger.info("EngineAcceptor initialized");
               this.checkBoundSockets();
          }
     }

     public void destroy(Object o) {
          super.destroy(o);
          this.isActive = false;
          this.shutDownBoundSockets();
          List leftOvers = this.threadPool.shutdownNow();

          try {
               Thread.sleep(500L);
               this.acceptSelector.close();
          } catch (Exception var4) {
               this.bootLogger.warn("Error when shutting down Accept selector: " + var4.getMessage());
          }

          this.bootLogger.info("EngineAcceptor stopped. Unprocessed tasks: " + leftOvers.size());
     }

     private void initThreadPool() {
          for(int j = 0; j < this.threadPoolSize; ++j) {
               this.threadPool.execute(this);
          }

     }

     public void run() {
          Thread.currentThread().setName("EngineAcceptor-" + this.threadId++);

          while(this.isActive) {
               try {
                    this.acceptLoop();
               } catch (IOException var2) {
                    this.logger.info("I/O Error with Accept Selector: " + var2.getMessage());
                    Logging.logStackTrace(this.logger, (Throwable)var2);
               }
          }

          this.bootLogger.info("EngineAcceptor threadpool shutting down.");
     }

     private void acceptLoop() throws IOException {
          this.acceptSelector.select();
          Set readyKeys = this.acceptSelector.selectedKeys();
          SelectionKey key = null;
          Iterator it = readyKeys.iterator();

          while(it.hasNext()) {
               try {
                    key = (SelectionKey)it.next();
                    it.remove();
                    ServerSocketChannel ssChannel = (ServerSocketChannel)key.channel();
                    SocketChannel clientChannel = ssChannel.accept();
                    this.logger.trace("Accepted client connection on: " + ssChannel.socket().getInetAddress().getHostAddress() + ":" + ssChannel.socket().getLocalPort());
                    synchronized(this.acceptableConnections) {
                         this.acceptableConnections.add(clientChannel);
                    }
               } catch (IOException var9) {
                    this.logger.info("I/O Error during accept loop: " + var9.getMessage());
               }
          }

          if (this.isActive) {
               this.socketReader.getSelector().wakeup();
          }

     }

     public void handleAcceptableConnections() {
          if (this.acceptableConnections.size() != 0) {
               synchronized(this.acceptableConnections) {
                    Iterator it = this.acceptableConnections.iterator();

                    while(true) {
                         while(it.hasNext()) {
                              SocketChannel connection = (SocketChannel)it.next();
                              it.remove();
                              if (connection == null) {
                                   this.logger.warn("Engine Acceptor handle a null socketchannel");
                              } else {
                                   Socket socket = connection.socket();
                                   if (socket == null) {
                                        this.logger.warn("Engine Acceptor handle a null socket");
                                   } else {
                                        InetAddress iAddr = socket.getInetAddress();
                                        if (iAddr != null) {
                                             try {
                                                  this.connectionFilter.validateAndAddAddress(iAddr.getHostAddress());
                                                  connection.configureBlocking(false);
                                                  connection.socket().setTcpNoDelay(!this.engine.getConfiguration().isNagleAlgorithm());
                                                  SelectionKey selectionKey = connection.register(this.socketReader.getSelector(), 1);
                                                  ISession session = this.sessionManager.createSession(connection);
                                                  session.setSystemProperty("SessionSelectionKey", selectionKey);
                                                  this.sessionManager.addSession(session);
                                                  this.logger.info("Session created: " + session + " on Server port: " + connection.socket().getLocalPort() + " <---> " + session.getClientPort());
                                                  Event sessionAddedEvent = new Event("sessionAdded");
                                                  sessionAddedEvent.setParameter("session", session);
                                                  this.dispatchEvent(sessionAddedEvent);
                                             } catch (RefusedAddressException var11) {
                                                  this.logger.info("Refused connection. " + var11.getMessage());

                                                  try {
                                                       connection.socket().shutdownInput();
                                                       connection.socket().shutdownOutput();
                                                       connection.close();
                                                  } catch (IOException var10) {
                                                       this.logger.warn("Additional problem with refused connection. Was not able to shut down the channel: " + var10.getMessage());
                                                  }
                                             } catch (IOException var12) {
                                                  StringBuilder sb = new StringBuilder("Failed accepting connection: ");
                                                  if (connection != null && connection.socket() != null) {
                                                       sb.append(connection.socket().getInetAddress().getHostAddress());
                                                  }

                                                  this.logger.info(sb.toString());
                                             }
                                        }
                                   }
                              }
                         }

                         return;
                    }
               }
          }
     }

     public void bindSocket(SocketConfig socketConfig) throws IOException {
          if (socketConfig.getType() == TransportType.TCP) {
               this.bindTcpSocket(socketConfig.getAddress(), socketConfig.getPort());
          } else {
               if (socketConfig.getType() != TransportType.UDP) {
                    throw new UnsupportedOperationException("Invalid transport type!");
               }

               this.bindUdpSocket(socketConfig.getAddress(), socketConfig.getPort());
          }

     }

     public List getBoundSockets() {
          ArrayList list = null;
          synchronized(this.boundSockets) {
               list = new ArrayList(this.boundSockets);
               return list;
          }
     }

     public IConnectionFilter getConnectionFilter() {
          return this.connectionFilter;
     }

     public void setConnectionFilter(IConnectionFilter filter) {
          if (this.connectionFilter != null) {
               throw new IllegalStateException("A connection filter already exists!");
          } else {
               this.connectionFilter = filter;
          }
     }

     private void bindTcpSocket(String address, int port) throws IOException {
          ServerSocketChannel socketChannel = ServerSocketChannel.open();
          socketChannel.configureBlocking(false);
          socketChannel.socket().bind(new InetSocketAddress(address, port));
          socketChannel.socket().setReuseAddress(true);
          socketChannel.register(this.acceptSelector, 16);
          synchronized(this.boundSockets) {
               this.boundSockets.add(new BindableSocket(socketChannel, address, port, TransportType.TCP));
          }

          this.bootLogger.info("Added bound tcp socket --> " + address + ":" + port);
          this.setCacheBindPort(address, port);
     }

     private void bindUdpSocket(String address, int port) throws IOException {
          DatagramChannel datagramChannel = DatagramChannel.open();
          datagramChannel.configureBlocking(false);
          datagramChannel.socket().bind(new InetSocketAddress(address, port));
          datagramChannel.socket().setReuseAddress(true);
          datagramChannel.register(this.socketReader.getSelector(), 1);
          synchronized(this.boundSockets) {
               this.boundSockets.add(new BindableSocket(datagramChannel, address, port, TransportType.UDP));
          }

          this.bootLogger.info("Added bound udp socket --> " + address + ":" + port);
          this.setCacheBindPort(address, port);
     }

     private void setCacheBindPort(String address, int port) {
          try {
               DataController.getController().set("cache_binded_port__" + address + "_" + port, true);
          } catch (Exception var4) {
          }

     }

     private void bindBlueBox() throws IOException {
          throw new UnsupportedOperationException("Not supported yet!");
     }

     private void checkBoundSockets() {
          if (this.boundSockets.size() < 1) {
               this.bootLogger.error("No bound sockets! Check the boot logs for possible problems!");
          }

     }

     private void shutDownBoundSockets() {
          List problematicSockets = null;
          Iterator iterator = this.boundSockets.iterator();

          BindableSocket socket;
          while(iterator.hasNext()) {
               socket = (BindableSocket)iterator.next();

               try {
                    socket.getChannel().close();
               } catch (IOException var5) {
                    if (problematicSockets == null) {
                         problematicSockets = new ArrayList();
                    }

                    problematicSockets.add(socket);
               }
          }

          if (problematicSockets != null) {
               StringBuilder sb = new StringBuilder("Problems closing bound socket(s). The following socket(s) raised exceptions: ");
               Iterator iterator1 = problematicSockets.iterator();

               while(iterator1.hasNext()) {
                    socket = (BindableSocket)iterator1.next();
                    sb.append(socket.toString()).append(" ");
               }

               throw new RuntimeException(sb.toString());
          }
     }
}
