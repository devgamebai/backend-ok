package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.system.cmd.ControlParamCmd;
import bitzero.server.core.BZEvent;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class BanUserChat extends BaseControllerCommand {
     public BanUserChat() {
          super(SystemRequest.BanUserChat);
     }

     public boolean validate(IRequest request) {
          return this.checkSuperAdmin(request.getSender());
     }

     public void execute(IRequest request) {
          DataCmd dataparams = new DataCmd(((ByteBuffer)request.getContent()).array());
          ControlParamCmd params = new ControlParamCmd(dataparams);
          params.unpackData();
          Map evtParams = new HashMap();
          evtParams.put(BZEventParam.RECIPIENT, params.command);
          evtParams.put(BZEventParam.COMMAND, params.param);
          IBZEvent banUserChat = new BZEvent(BZEventType.BAN_USER_CHAT, evtParams);
          this.bz.getEventManager().dispatchEvent(banUserChat);
     }

     protected Boolean checkSuperAdmin(ISession session) {
          return session.getProperty("SuperAdmin") != null ? true : false;
     }
}
