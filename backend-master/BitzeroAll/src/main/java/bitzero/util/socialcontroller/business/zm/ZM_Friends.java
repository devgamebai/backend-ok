package bitzero.util.socialcontroller.business.zm;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ZM_Friends extends ZM_API_Common {
     public ZM_Friends(ZM_Config config) {
          super(config, "/friends");
     }

     private static List parseRespone(String respone) throws SocialControllerException {
          try {
               JSONObject json = new JSONObject(respone);
               ArrayList result = new ArrayList();
               JSONArray arrJson = json.getJSONArray("uid");

               for(int i = 0; i < arrJson.length(); ++i) {
                    result.add(Long.parseLong(arrJson.getString(i)));
               }

               return result;
          } catch (JSONException var5) {
               CommonHandle.writeErrLog((Throwable)var5);
               CommonHandle.writeErrLog("Zingme return json format error: " + respone);
               throw new SocialControllerException(-3, "loi JSON");
          }
     }

     public List getFriendList(String sessionId) throws SocialControllerException {
          HashMap args = new HashMap();
          args.put("session_key", sessionId);
          String respone = this.callMethod("Friends.getLists", args);
          return parseRespone(respone);
     }
}
