package bitzero.server.entities.managers;

import bitzero.server.core.ICoreService;
import bitzero.server.entities.User;
import java.util.List;

public interface IBannedUserManager extends ICoreService {
     boolean isAutoRemoveBan();

     void setAutoRemoveBan(boolean var1);

     boolean isPersistent();

     void setPersistent(boolean var1);

     void setPersistenceClass(String var1);

     void kickUser(User var1, User var2, String var3, int var4);

     void kickUser(User var1, User var2, String var3, int var4, boolean var5);

     void banUser(User var1, User var2, int var3, BanMode var4, String var5, String var6, int var7);

     void banUser(String var1, int var2, BanMode var3, String var4);

     void banUser(String var1, int var2, BanMode var3, String var4, String var5);

     void sendWarningMessage(User var1, User var2, String var3);

     int getKickCount(String var1, int var2);

     boolean isNameBanned(String var1);

     boolean isIpBanned(String var1);

     void removeBannedUser(String var1, BanMode var2);

     List getBannedUsersByIp();

     List getBannedUsersByName(String var1);

     int getBanDuration(String var1, BanMode var2);
}
