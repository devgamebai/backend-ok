package bitzero.server.controllers.admin;

import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.extensions.data.SimpleMsg;

public class CheckOnline extends BaseAdminCommand {
     public CheckOnline() {
          super(SystemRequest.CheckOnline);
     }

     public void handleRequest(ISession sender, DataCmd cmd) {
          User userToBan = this.bz.getUserManager().getUserByName(cmd.readString());
          SimpleMsg msg = new SimpleMsg(this.getId());
          if (userToBan == null) {
               msg.putString("offline");
          } else {
               msg.putString("online");
               if (userToBan.getZone() != null) {
                    msg.putString(userToBan.getZone().getName());
               }

               if (userToBan.getJoinedRoom() != null) {
                    msg.putString(userToBan.getJoinedRoom().toString());
               }
          }

          this.send(sender, msg);
     }
}
