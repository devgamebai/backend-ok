package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.core.BZEvent;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ServiceNotify extends BaseControllerCommand {
     public ServiceNotify() {
          super(SystemRequest.ServiceNotify);
     }

     public boolean validate(IRequest irequest) {
          return true;
     }

     public void execute(IRequest request) throws Exception {
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          short CmdId = params.readShort();
          if (CmdId == 0) {
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.VARIABLES, params);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.SERVICE_NOTIFY, evtParams));
          }

     }
}
