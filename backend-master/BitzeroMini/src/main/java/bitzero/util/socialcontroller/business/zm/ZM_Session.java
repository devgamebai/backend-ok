package bitzero.util.socialcontroller.business.zm;

import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.HashMap;

public class ZM_Session extends ZM_API_Common {
     public ZM_Session(ZM_Config config) {
          super(config, "/sessions");
     }

     public long getLoggedInUser(String sessionKey) throws SocialControllerException {
          long userId = -1L;
          HashMap args = new HashMap();
          args.put("session_key", sessionKey);
          String result = this.callMethod("Users.getLoggedInUser", args);

          try {
               userId = Long.parseLong(result);
          } catch (Exception var7) {
          }

          return userId;
     }
}
