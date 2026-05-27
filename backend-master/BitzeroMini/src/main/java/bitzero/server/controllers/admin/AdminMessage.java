package bitzero.server.controllers.admin;

import bitzero.engine.sessions.ISession;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;

public class AdminMessage extends BaseAdminCommand {
     public AdminMessage() {
          super(SystemRequest.AdminMessage);
     }

     public void handleRequest(ISession sender, DataCmd cmd) {
          this.api.sendAdminMessage((User)null, cmd.readString(), new String[]{"Notify User "}, this.bz.getUserManager().getAllSessions());
     }
}
