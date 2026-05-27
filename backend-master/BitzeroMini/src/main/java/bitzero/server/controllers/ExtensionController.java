package bitzero.server.controllers;

import bitzero.engine.controllers.AbstractController;
import bitzero.engine.io.IRequest;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.entities.managers.IExtensionManager;
import bitzero.server.exceptions.ExceptionMessageComposer;
import bitzero.server.exceptions.SFSExtensionException;
import bitzero.server.extensions.IBZExtension;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.PacketType;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionController extends AbstractController {
     public static final String KEY_EXT_CMD = "c";
     public static final String KEY_EXT_PARAMS = "p";
     public static final String KEY_ROOMID = "r";
     private final Logger logger = LoggerFactory.getLogger(this.getClass());
     private final BitZeroServer bz = BitZeroServer.getInstance();
     private IExtensionManager extensionManager;

     public void init(Object o) {
          super.init(o);
          this.extensionManager = this.bz.getExtensionManager();
     }

     public void destroy(Object o) {
          super.destroy(o);
          this.extensionManager = null;
     }

     public void processRequest(IRequest request) throws Exception {
          this.processRegularRequest(request);
     }

     public void processRegularRequest(IRequest request) throws Exception {
          this.logger.debug(request.toString());
          long t1 = System.nanoTime();
          short cmd = (Short)request.getId();
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          User sender = this.bz.getUserManager().getUserBySession(request.getSender());
          IBZExtension extension = this.bz.getExtensionManager().getMainExtension();
          if (sender != null) {
               sender.updateLastRequestTime();
          }

          String logSender = sender == null ? request.getSender().toString() : sender.getName();
          LoggerFactory.getLogger("request").debug("Extension call cmdId: " + cmd + " - from : " + logSender);

          try {
               BitZeroServer.getInstance().getPacketCount().addPacket(PacketType.INCOMING, cmd);
               if (sender == null) {
                    extension.doLogin(cmd, request.getSender(), params);
               } else if (cmd != 1) {
                    extension.handleClientRequest(cmd, sender, params);
               }
          } catch (Exception var13) {
               ExceptionMessageComposer composer = new ExceptionMessageComposer(var13);
               composer.setDescription("Error while handling client request in extension: " + extension.getName());
               composer.addInfo("Extension Cmd: " + request.getId());
               this.logger.error(composer.toString());
          }

          long t2 = System.nanoTime();
          double delta = (double)(t2 - t1) / 1000000.0D;
          this.logger.debug("Extension call executed in: " + delta);
          if (delta > 1000.0D) {
               this.logger.warn("Slow  Extension call executed : " + delta + "in Cmd " + cmd);
          }

     }

     protected void processWebsocketRequest(IRequest request) throws Exception {
          if (this.logger.isDebugEnabled()) {
               this.logger.debug(request.toString());
          }

          long t1 = System.nanoTime();
          User sender = this.bz.getUserManager().getUserBySession(request.getSender());
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
               IBZExtension extension = this.bz.getExtensionManager().getMainExtension();
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
}
