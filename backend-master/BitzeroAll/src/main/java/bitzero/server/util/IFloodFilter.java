package bitzero.server.util;

import bitzero.server.controllers.SystemRequest;
import bitzero.server.core.ICoreService;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.BanMode;
import java.util.Map;

public interface IFloodFilter extends ICoreService {
     void setActive(boolean var1);

     void filterRequest(SystemRequest var1, User var2);

     void addRequestFilter(SystemRequest var1, int var2);

     boolean isRequestFiltered(SystemRequest var1);

     void clearAllFilters();

     Map getRequestTable();

     int getBanDurationMinutes();

     void setBanDurationMinutes(int var1);

     int getMaxFloodingAttempts();

     void setMaxFloodingAttempts(int var1);

     int getSecondsBeforeBan();

     void setSecondsBeforeBan(int var1);

     boolean isLogFloodingAttempts();

     void setLogFloodingAttempts(boolean var1);

     BanMode getBanMode();

     void setBanMode(BanMode var1);

     String getBanMessage();

     void setBanMessage(String var1);
}
