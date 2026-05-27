package bitzero.server.controllers.admin;

import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.cmd.LogLevelCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SetLogLevel extends BaseControllerCommand {
     public SetLogLevel() {
          super(SystemRequest.SetLogLevel);
     }

     public boolean validate(IRequest request) {
          return this.checkSuperAdmin(request.getSender());
     }

     public void execute(IRequest request) throws Exception {
          ISession sender = request.getSender();
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          LogLevelCmd logCmd = new LogLevelCmd(params);
          logCmd.unpackData();

          try {
               Logger.getLogger(logCmd.logName).setLevel(Level.toLevel(logCmd.level));
               this.logger.warn("Log Catalog : " + logCmd.logName + " ,changed to LogLevel :" + logCmd.level);
          } catch (Exception var6) {
               var6.printStackTrace();
          }

     }
}
