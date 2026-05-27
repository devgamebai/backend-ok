package bitzero.server.controllers.admin;

import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.cmd.PoolSizeCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class SetPoolSize extends BaseControllerCommand {
     public SetPoolSize() {
          super(SystemRequest.SetPoolSize);
     }

     public boolean validate(IRequest request) {
          return this.checkSuperAdmin(request.getSender());
     }

     public void execute(IRequest request) throws Exception {
          ISession sender = request.getSender();
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          PoolSizeCmd poolCmd = new PoolSizeCmd(params);
          poolCmd.unpackData();
     }
}
