package game.modules.cluster;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import game.modules.cluster.cmd.RevSample;
import game.modules.cluster.cmd.SendSample;

public class SampleModule extends BaseClientRequestHandler {
     public void init() {
          this.getParentExtension().addEventListener(BZEventType.USER_LOGIN, this);
     }

     public void handleServerEvent(IBZEvent ibzevent) throws BZException {
          if (ibzevent.getType() == BZEventType.USER_LOGIN) {
               this.userLogin((User)ibzevent.getParameter(BZEventParam.USER));
          }

     }

     public void handleClientRequest(User user, DataCmd dataCmd) {
          switch(dataCmd.getId()) {
          case 1001:
               this.sampleProcess(user, dataCmd);
          default:
          }
     }

     private void sampleProcess(User user, DataCmd dataCmd) {
          RevSample cmd = new RevSample(dataCmd);
          SendSample msg = new SendSample();
          msg.copy(cmd);
          this.send(msg, user);
     }

     private void userLogin(User user) {
          ExtensionUtility.instance().sendLoginOK(user);
     }
}
