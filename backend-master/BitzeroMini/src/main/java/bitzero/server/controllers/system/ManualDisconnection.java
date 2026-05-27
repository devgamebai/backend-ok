package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.entities.User;

public class ManualDisconnection extends BaseControllerCommand {
     public ManualDisconnection() {
          super(SystemRequest.ManualDisconnection);
     }

     public boolean validate(IRequest request) {
          return true;
     }

     public void execute(IRequest request) throws Exception {
          User sender = this.api.getUserBySession(request.getSender());
          if (sender != null) {
               sender.setReconnectionSeconds(0);
          }

     }
}
