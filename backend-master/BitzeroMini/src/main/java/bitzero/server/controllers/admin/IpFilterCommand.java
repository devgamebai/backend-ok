package bitzero.server.controllers.admin;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.io.IRequest;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.sessions.DefaultReconnectionManager;
import bitzero.engine.sessions.ISession;
import bitzero.server.config.DefaultConstants;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.cmd.IpFilterCmd;
import bitzero.server.controllers.system.cmd.SendHandsake;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class IpFilterCommand extends BaseControllerCommand {
     public IpFilterCommand() {
          super(SystemRequest.IpFilterCommand);
     }

     public boolean validate(IRequest request) {
          return this.checkSuperAdmin(request.getSender());
     }

     public void execute(IRequest request) throws Exception {
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          IpFilterCmd ipCmd = new IpFilterCmd(params);
          ipCmd.unpackData();
          if (ipCmd.ip.length() > 8) {
               if (ipCmd.mode.equals("add")) {
                    if (ipCmd.targetList.equals("white")) {
                         BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().addWhiteListAddress(ipCmd.ip);
                    } else if (ipCmd.targetList.equals("black")) {
                         BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().addBannedAddress(ipCmd.ip);
                    }
               } else if (ipCmd.mode.equals("remove")) {
                    if (ipCmd.targetList.equals("white")) {
                         BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().removeWhiteListAddress(ipCmd.ip);
                    } else if (ipCmd.targetList.equals("black")) {
                         BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().removeBannedAddress(ipCmd.ip);
                    }
               } else {
                    int seconds;
                    if (ipCmd.mode.equals("max")) {
                         seconds = Integer.valueOf(ipCmd.targetList);
                         if (seconds > 0) {
                              BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().setMaxConnectionsPerIp(seconds);
                         }
                    } else if (!ipCmd.mode.equals("mobileReconnect")) {
                         if (ipCmd.mode.equals("reconnectTime")) {
                              seconds = Integer.valueOf(ipCmd.targetList);
                              if (seconds > 0) {
                                   this.bz.getConfigurator().getServerSettings().userReconnectionSeconds = seconds;
                              }
                         } else if (ipCmd.mode.equals("realTCP")) {
                              seconds = Integer.valueOf(ipCmd.targetList);
                              if (seconds > 0) {
                                   DefaultReconnectionManager.ONLY_REAL_TCP = true;
                              } else {
                                   DefaultReconnectionManager.ONLY_REAL_TCP = false;
                              }
                         }
                    }
               }
          } else {
               ISession sender = request.getSender();
               SendHandsake msg = new SendHandsake();
               msg.sessionToken = BitZeroEngine.getInstance().getEngineAcceptor().getConnectionFilter().getGhostList();
               IResponse response = new Response();
               response.setId(this.getId());
               response.setRecipients(sender);
               response.setContent(msg.createData());
               response.setTargetController(DefaultConstants.CORE_SYSTEM_CONTROLLER_ID);
               response.write();
          }

     }
}
