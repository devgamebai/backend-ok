package bitzero.util.socialcontroller.business.zm;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.bean.UserInfo;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ZM_User extends ZM_API_Common {
     public ZM_User(ZM_Config config) {
          super(config, "/users");
     }

     private static List parseRespone(String respone) {
          try {
               List result = new LinkedList();
               JSONArray jsonArr = new JSONArray(respone);

               for(int i = 0; i < jsonArr.length(); ++i) {
                    JSONObject json = jsonArr.getJSONObject(i);
                    UserInfo info = new UserInfo();
                    if (json.has("firstname")) {
                         info.setFirstname(json.getString("firstname"));
                    }

                    if (json.has("gender")) {
                         info.setGender(json.getString("gender"));
                    }

                    if (json.has("headurl")) {
                         info.setHeadurl(json.getString("headurl"));
                    }

                    if (json.has("lastname")) {
                         info.setLastname(json.getString("lastname"));
                    }

                    if (json.has("profile_url")) {
                         info.setProfile_url(json.getString("profile_url"));
                    }

                    if (json.has("tinyurl")) {
                         info.setTinyurl(json.getString("tinyurl"));
                    }

                    if (json.has("userid")) {
                         info.setUserId(json.getString("userid"));
                    }

                    if (json.has("username")) {
                         info.setUsername(json.getString("username"));
                    }

                    if (json.has("displayname")) {
                         info.setDisplayname(json.getString("displayname"));
                    }

                    if (json.has("dob")) {
                         info.setDob(json.getString("dob"));
                    }

                    if (json.has("status")) {
                         info.setStatus(json.getString("status"));
                    }

                    result.add(info);
               }

               return result;
          } catch (JSONException var6) {
               return null;
          }
     }

     public List getUserInfo(List uids, String filter) throws SocialControllerException {
          return (List)(uids.size() > 0 ? this.getUserInfo(this.idListToString(uids), filter) : new LinkedList());
     }

     public List getUserInfo(String uids, String filter) throws SocialControllerException {
          HashMap args = new HashMap();
          args.put("uids", uids);
          args.put("fields", filter);

          try {
               String respone = this.callMethod("Users.getInfo", args);
               return parseRespone(respone);
          } catch (SocialControllerException var5) {
               CommonHandle.writeErrLog((Throwable)var5);
               return new LinkedList();
          }
     }

     private String idListToString(List uids) {
          StringBuilder sb = new StringBuilder();

          for(int i = 0; i < uids.size(); ++i) {
               if (i > 0) {
                    sb.append(",").append(uids.get(i));
               } else {
                    sb.append(uids.get(i));
               }
          }

          return sb.toString();
     }
}
