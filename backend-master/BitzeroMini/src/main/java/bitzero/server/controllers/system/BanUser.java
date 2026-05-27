package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.system.cmd.ControlParamCmd;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.BanMode;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;
import java.util.List;

public class BanUser extends BaseControllerCommand {
     public static final String KEY_USER_ID = "u";
     public static final String KEY_MESSAGE = "m";
     public static final String KEY_DELAY = "d";
     public static final String KEY_BAN_MODE = "b";
     public static final String KEY_BAN_DURATION_HOURS = "dh";

     public BanUser() {
          super(SystemRequest.BanUser);
     }

     public boolean validate(IRequest request) {
          return this.checkSuperAdmin(request.getSender());
     }

     public void execute(IRequest request) throws Exception {
          DataCmd dataparams = new DataCmd(((ByteBuffer)request.getContent()).array());
          ControlParamCmd params = new ControlParamCmd(dataparams);
          params.unpackData();
          if (params.param.length >= 4) {
               int banDuration = Integer.valueOf(params.param[0]) >= 0 ? Integer.valueOf(params.param[0]) : 60;
               List userToBans = this.bz.getUserManager().getUserByName(params.command);
               if (userToBans != null) {
                    User userToBan = (User)userToBans.get(0);
                    if (banDuration == 0) {
                         this.bz.getBannedUserManager().removeBannedUser(params.command, BanMode.fromString(params.param[2]));
                    } else if (userToBan == null && banDuration > 0) {
                         this.bz.getBannedUserManager().banUser(params.command, banDuration, BanMode.fromString(params.param[2]), params.param[1]);
                    } else {
                         if (banDuration > 0) {
                              this.api.banUser(userToBan, (User)null, params.param[1], BanMode.fromString(params.param[2]), banDuration, Integer.valueOf(params.param[3]));
                         }

                    }
               }
          }
     }
}
