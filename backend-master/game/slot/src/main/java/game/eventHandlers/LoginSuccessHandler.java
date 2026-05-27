package game.eventHandlers;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.IBZEvent;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseServerEventHandler;
import bitzero.util.ExtensionUtility;

public class LoginSuccessHandler extends BaseServerEventHandler {
     public void handleServerEvent(IBZEvent ibzevent) throws BZException {
          this.onLoginSuccess((User)ibzevent.getParameter(BZEventParam.USER));
     }

     private void onLoginSuccess(User user) {
          ExtensionUtility.instance().sendLoginOK(user);
     }
}
