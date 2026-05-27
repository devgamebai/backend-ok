package bitzero.util.socialcontroller.business.zm;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class ZM_Feed extends ZM_API_Common {
     public ZM_Feed(ZM_Config config) {
          super(config, "/feed");
     }

     public boolean publishUserActionV2(String session_key, int template_bundle_id, JSONObject template_data) {
          Map arg = new HashMap();
          arg.put("session_key", session_key);
          arg.put("template_bundle_id", String.valueOf(template_bundle_id));
          arg.put("template_data", template_data.toString());

          try {
               this.callMethod("Feed.publishUserActionV2", arg);
               return true;
          } catch (SocialControllerException var6) {
               CommonHandle.writeErrLog((Throwable)var6);
               return false;
          }
     }
}
