package bitzero.server.security;

import bitzero.server.controllers.SystemRequest;
import bitzero.server.entities.User;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZPrivilegeManager implements PrivilegeManager {
     private final Map privilegeProfiles = new ConcurrentHashMap();
     private final Logger log = LoggerFactory.getLogger(this.getClass());
     private volatile boolean active;

     public boolean isActive() {
          return this.active;
     }

     public void setActive(boolean active) {
          this.active = active;
     }

     public boolean containsPermissionProfile(short profileId) {
          return this.privilegeProfiles.containsKey(profileId);
     }

     public boolean containsPermissionProfile(String profileName) {
          boolean found = false;
          Short id = this.findIdFromName(profileName);
          if (id != null) {
               found = this.containsPermissionProfile(id);
          }

          return found;
     }

     public BZPermissionProfile getPermissionProfile(short profileId) {
          return (BZPermissionProfile)this.privilegeProfiles.get(profileId);
     }

     public BZPermissionProfile getPermissionProfile(String profileName) {
          Short id = this.findIdFromName(profileName);
          return id == null ? null : (BZPermissionProfile)this.privilegeProfiles.get(id);
     }

     public void removePermissionProfile(short permId) {
          this.privilegeProfiles.remove(permId);
     }

     public void removePermissionProfile(String profileName) {
          Short id = this.findIdFromName(profileName);
          if (id != null) {
               this.privilegeProfiles.remove(id);
          }

     }

     public void setPermissionProfile(BZPermissionProfile profile) {
          if (this.privilegeProfiles.containsKey(profile.getId())) {
               this.log.warn("Profile with duplicate ID: " + profile.getId() + ", name: " + profile.getName() + " was not added to the manager");
          } else {
               this.privilegeProfiles.put(profile.getId(), profile);
          }
     }

     public boolean isRequestAllowed(User user, SystemRequest request) {
          if (!this.isActive()) {
               return true;
          } else {
               boolean success = false;
               BZPermissionProfile profile = (BZPermissionProfile)this.privilegeProfiles.get(user.getPrivilegeId());
               if (profile != null) {
                    success = profile.isRequestAllowed(request);
               }

               return success;
          }
     }

     public boolean isFlagSet(User user, SystemPermission permission) {
          boolean success = false;
          BZPermissionProfile profile = (BZPermissionProfile)this.privilegeProfiles.get(user.getPrivilegeId());
          if (profile != null) {
               success = profile.isFlagSet(permission);
          }

          return success;
     }

     private Short findIdFromName(String name) {
          Short profileId = null;
          Iterator iterator = this.privilegeProfiles.values().iterator();

          while(iterator.hasNext()) {
               BZPermissionProfile profile = (BZPermissionProfile)iterator.next();
               if (profile.getName().equals(name)) {
                    profileId = profile.getId();
                    break;
               }
          }

          return profileId;
     }

     public void dump() {
          Iterator iterator = this.privilegeProfiles.keySet().iterator();

          while(iterator.hasNext()) {
               Short id = (Short)iterator.next();
               System.out.println(id + ":");
               BZPermissionProfile profile = (BZPermissionProfile)this.privilegeProfiles.get(id);
               System.out.println("\tAllowed Sys Req:");
               SystemRequest[] aobj;
               int k = (aobj = SystemRequest.values()).length;

               for(int i = 0; i < k; ++i) {
                    SystemRequest sysReq = aobj[i];
                    if (profile.isRequestAllowed(sysReq)) {
                         System.out.println("\t\t" + sysReq);
                    }
               }

               System.out.println("\tPermission Flags:");
               SystemPermission[] aobj2;
               k = (aobj2 = SystemPermission.values()).length;

               for(int j = 0; j < k; ++j) {
                    SystemPermission perm = aobj2[j];
                    if (profile.isFlagSet(perm)) {
                         System.out.println("\t\t" + perm);
                    }
               }
          }

     }
}
