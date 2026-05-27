package bitzero.util.socialcontroller.business.zm;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.bean.UserInfo;
import bitzero.util.socialcontroller.business.ISocialController;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ZM_SocialController implements ISocialController {
     private ZM_Config config = new ZM_Config();
     private ZM_Session zmSession;
     private ZM_User zmUser;
     private ZM_Friends zmFriend;
     private ZM_Feed zmFeed;

     public ZM_SocialController() {
          this.config.apiURL = ConfigHandle.instance().get("zm_api_url");
          this.config.apiKey = ConfigHandle.instance().get("zm_api_key");
          this.config.secret = ConfigHandle.instance().get("zm_api_secret");
          this.config.timeout = 30L;
          this.zmSession = new ZM_Session(this.config);
          this.zmUser = new ZM_User(this.config);
          this.zmFriend = new ZM_Friends(this.config);
          this.zmFeed = new ZM_Feed(this.config);
     }

     public long getLoggedInUser(String sessionKey) throws SocialControllerException {
          return this.zmSession.getLoggedInUser(sessionKey);
     }

     public UserInfo getUserInfo(Long userId, String filter) throws SocialControllerException {
          return (UserInfo)this.zmUser.getUserInfo(String.valueOf(userId), filter).get(0);
     }

     public List getFriendList(String sessionId) throws SocialControllerException {
          return this.zmFriend.getFriendList(sessionId);
     }

     public List getUserInfo(List uids, String filter) throws SocialControllerException {
          return this.zmUser.getUserInfo(uids, filter);
     }

     public boolean pushFeed(String session_key, int template_bundle_id, String message) {
          try {
               JSONObject json = (new JSONObject()).put("message", message);
               return this.zmFeed.publishUserActionV2(session_key, template_bundle_id, json);
          } catch (JSONException var5) {
               CommonHandle.writeErrLog((Throwable)var5);
               return false;
          }
     }

     public boolean publishUserActionV2(String session_key, int template_bundle_id, JSONObject template_data) {
          return this.zmFeed.publishUserActionV2(session_key, template_bundle_id, template_data);
     }

     public boolean feedOpenApi2(String session_key, int template_bundle_id, String message, String name, String href, String caption, String description, String media_type, String media_src, String media_href) {
          try {
               JSONObject template_data = new JSONObject();
               template_data.put("message", this.convertToEntityHtml(message));
               JSONObject attachment = new JSONObject();
               attachment.put("name", this.convertToEntityHtml(name));
               attachment.put("href", href);
               attachment.put("caption", caption);
               attachment.put("description", this.convertToEntityHtml(description));
               JSONArray media = new JSONArray();
               media.put((new JSONObject()).put("type", media_type).put("src", media_src).put("href", media_href));
               attachment.put("media", media);
               template_data.put("attachment", attachment);
               return this.zmFeed.publishUserActionV2(session_key, template_bundle_id, template_data);
          } catch (JSONException var14) {
               CommonHandle.writeErrLog((Throwable)var14);
               return false;
          }
     }

     private String convertToEntityHtml(String s) {
          char[] arrChar = s.toCharArray();
          char maxChar = 127;
          StringBuilder buffer = new StringBuilder();

          for(int i = 0; i < arrChar.length; ++i) {
               if (arrChar[i] > maxChar) {
                    buffer.append("&#");
                    buffer.append(arrChar[i]);
               } else {
                    buffer.append(arrChar[i]);
               }
          }

          return buffer.toString();
     }
}
