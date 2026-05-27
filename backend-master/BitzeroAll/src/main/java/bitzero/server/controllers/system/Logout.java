package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.entities.User;

public class Logout extends BaseControllerCommand {
     public static final String KEY_ZONE_NAME = "zn";

     public Logout() {
          super(SystemRequest.Logout);
     }

     public boolean validate(IRequest request) {
          return true;
     }

     public void execute(IRequest request) throws Exception {
          User sender = this.api.getUserBySession(request.getSender());
          if (sender == null) {
               throw new IllegalArgumentException("Logout failure. Session is not logged in: " + request.getSender());
          } else {
               this.api.logout(sender);
          }
     }
}
