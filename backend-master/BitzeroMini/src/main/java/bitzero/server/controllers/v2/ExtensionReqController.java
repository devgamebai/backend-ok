package bitzero.server.controllers.v2;

import bitzero.engine.controllers.SimpleReqController;
import bitzero.engine.exceptions.RequestQueueFullException;
import bitzero.engine.io.IRequest;
import bitzero.engine.util.Logging;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.exceptions.ExceptionMessageComposer;
import bitzero.server.exceptions.SFSExtensionException;
import bitzero.server.extensions.IBZExtension;
import bitzero.server.util.executor.SmartExecutorConfig;
import bitzero.server.util.executor.SmartThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionReqController extends SimpleReqController {
     public static final String KEY_EXT_CMD = "c";
     public static final String KEY_EXT_PARAMS = "p";
     public static final String KEY_ROOMID = "r";
     private final Logger logger = LoggerFactory.getLogger(this.getClass());
     private final BitZeroServer sfs = BitZeroServer.getInstance();
     private ThreadPoolExecutor threadPool;
     private int qSize;

     public void init(Object o) {
          super.init(o);
          SmartExecutorConfig cfg = BitZeroServer.getInstance().getConfigurator().getServerSettings().extensionThreadPoolSettings;
          cfg.name = "Ext";
          this.threadPool = new SmartThreadPoolExecutor(cfg);
          this.logger.info(this.name + " initalized");
     }

     public void enqueueRequest(final IRequest request) throws RequestQueueFullException {
          this.threadPool.execute(new Runnable() {
               public void run() {
                    if (ExtensionReqController.this.isActive) {
                         try {
                              ExtensionReqController.this.processRequest(request);
                         } catch (Throwable var2) {
                              Logging.logStackTrace(ExtensionReqController.this.logger, var2);
                         }
                    }

               }
          });
     }

     protected void processRequest(IRequest request) throws Exception {
          if (this.logger.isDebugEnabled()) {
               this.logger.debug(request.toString());
          }

          long t1 = System.nanoTime();
          User sender = this.sfs.getUserManager().getUserBySession(request.getSender());
          if (sender != null) {
               sender.updateLastRequestTime();
          }

          ISFSObject reqObj = (ISFSObject)request.getContent();
          if (this.logger.isDebugEnabled()) {
               this.logger.debug(reqObj.getDump());
          }

          String cmd = reqObj.getUtfString("c");
          if (cmd != null && cmd.length() != 0) {
               ISFSObject params = reqObj.getSFSObject("p");
               IBZExtension extension = this.sfs.getExtensionManager().getMainExtension();
               String logSender = sender == null ? request.getSender().toString() : sender.getName();
               LoggerFactory.getLogger("request").debug("Extension call cmdId: " + cmd + " - from : " + logSender);

               try {
                    extension.handleClientRequest(cmd, sender, params);
               } catch (Exception var12) {
                    ExceptionMessageComposer composer = new ExceptionMessageComposer(var12);
                    composer.setDescription("Error while handling client request in extension: " + extension.toString());
                    composer.addInfo("Extension Cmd: " + cmd);
                    this.logger.error(composer.toString());
               }

               long t2 = System.nanoTime();
               if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Extension call executed in: " + (double)(t2 - t1) / 1000000.0D);
               }

          } else {
               throw new SFSExtensionException("Extension Request refused. Missing CMD. " + sender);
          }
     }

     public int getQueueSize() {
          return this.threadPool.getQueue().size();
     }

     public int getMaxQueueSize() {
          return this.qSize;
     }

     public void setMaxQueueSize(int size) {
          this.qSize = size;
     }

     public int getThreadPoolSize() {
          return this.threadPool.getPoolSize();
     }

     public void setThreadPoolSize(int size) {
     }

     public void handleMessage(Object message) {
     }
}
