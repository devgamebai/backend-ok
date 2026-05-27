package bitzero.server.security;

import bitzero.server.controllers.SystemRequest;
import bitzero.server.entities.User;

public interface PrivilegeManager {
     boolean isActive();

     void setActive(boolean var1);

     void setPermissionProfile(BZPermissionProfile var1);

     void removePermissionProfile(short var1);

     void removePermissionProfile(String var1);

     boolean containsPermissionProfile(short var1);

     boolean containsPermissionProfile(String var1);

     BZPermissionProfile getPermissionProfile(short var1);

     BZPermissionProfile getPermissionProfile(String var1);

     boolean isRequestAllowed(User var1, SystemRequest var2);

     boolean isFlagSet(User var1, SystemPermission var2);
}
