package bitzero.server.controllers.admin;

import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.extensions.data.SimpleMsg;
import java.util.List;

public class CheckOnline extends BaseAdminCommand {
     public CheckOnline() {
          super(SystemRequest.CheckOnline);
     }

     public void handleRequest(ISession sender, DataCmd cmd) {
          List users = this.bz.getUserManager().getUserByName(cmd.readString());
          if (users != null) {
               User userToBan = (User)users.get(0);
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
}
