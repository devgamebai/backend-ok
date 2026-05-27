package bitzero.util.socialcontroller.business;

import bitzero.util.socialcontroller.bean.UserInfo;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.util.List;

public interface ISocialController {
     long getLoggedInUser(String var1) throws SocialControllerException;

     UserInfo getUserInfo(Long var1, String var2) throws SocialControllerException;

     List getUserInfo(List var1, String var2) throws SocialControllerException;

     List getFriendList(String var1) throws SocialControllerException;

     boolean feedOpenApi2(String var1, int var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10);
}
