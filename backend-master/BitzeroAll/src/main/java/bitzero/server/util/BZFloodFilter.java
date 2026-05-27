package bitzero.server.util;

import bitzero.server.controllers.SystemRequest;
import bitzero.server.core.BaseCoreService;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.BanMode;
import bitzero.server.entities.managers.IBannedUserManager;
import bitzero.server.util.filters.RequestMonitor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZFloodFilter extends BaseCoreService implements IFloodFilter {
     private final Map requestTable = new ConcurrentHashMap();
     private final IBannedUserManager banUserManager;
     private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
     private volatile int banDurationMinutes = 60;
     private volatile int maxFloodingAttempts = 5;
     private volatile int secondsBeforeBan = 5;
     private volatile boolean logFloodingAttempts = false;
     private BanMode banMode;
     private String banMessage;

     public BZFloodFilter(IBannedUserManager manager) {
          this.banMode = BanMode.BY_NAME;
          this.banMessage = "You are being banned, too many flooding attempts.";
          super.setName(this.getClass().getName());
          this.banUserManager = manager;
          this.requestTable.put(SystemRequest.PublicMessage, 50);
     }

     public void filterRequest(SystemRequest reqType, User user) {
          if (this.active) {
               RequestMonitor monitor = (RequestMonitor)user.getSession().getSystemProperty("FloodFilterRequestTable");
               if (monitor == null) {
                    monitor = new RequestMonitor();
                    user.getSession().setSystemProperty("FloodFilterRequestTable", monitor);
               }

               Integer maxReqsPerSecond = (Integer)this.requestTable.get(reqType);
               if (maxReqsPerSecond != null) {
                    int userReqsPerSecond = monitor.updateRequest(reqType);
                    if (userReqsPerSecond >= maxReqsPerSecond) {
                         boolean isFirstOccurrence = maxReqsPerSecond - userReqsPerSecond == -1;
                         if (isFirstOccurrence) {
                              int currentFloodWarns = user.getFloodWarnings() + 1;
                              user.setFloodWarnings(currentFloodWarns);
                              if (this.logFloodingAttempts) {
                                   this.log.warn(String.format("Flooding: %s , Request: %s, User warns: %s", user, reqType, currentFloodWarns));
                              }

                              if (currentFloodWarns >= this.maxFloodingAttempts) {
                                   this.banUserManager.banUser(user, UsersUtil.getServerModerator(), this.banDurationMinutes, this.banMode, "flooding", this.banMessage, this.secondsBeforeBan);
                              }
                         }

                    }
               }
          }
     }

     public void setActive(boolean flag) {
          this.active = flag;
     }

     public void addRequestFilter(SystemRequest request, int reqPerSecond) {
          this.requestTable.put(request, reqPerSecond);
     }

     public Map getRequestTable() {
          return new HashMap(this.requestTable);
     }

     public void clearAllFilters() {
          this.requestTable.clear();
     }

     public boolean isRequestFiltered(SystemRequest request) {
          return this.requestTable.containsKey(request);
     }

     public int getBanDurationMinutes() {
          return this.banDurationMinutes;
     }

     public void setBanDurationMinutes(int banDurationMinutes) {
          this.banDurationMinutes = banDurationMinutes;
     }

     public int getMaxFloodingAttempts() {
          return this.maxFloodingAttempts;
     }

     public void setMaxFloodingAttempts(int maxFloodingAttempts) {
          this.maxFloodingAttempts = maxFloodingAttempts;
     }

     public int getSecondsBeforeBan() {
          return this.secondsBeforeBan;
     }

     public void setSecondsBeforeBan(int secondsBeforeBan) {
          this.secondsBeforeBan = secondsBeforeBan;
     }

     public boolean isLogFloodingAttempts() {
          return this.logFloodingAttempts;
     }

     public void setLogFloodingAttempts(boolean logFloodingAttempts) {
          this.logFloodingAttempts = logFloodingAttempts;
     }

     public BanMode getBanMode() {
          return this.banMode;
     }

     public void setBanMode(BanMode banMode) {
          this.banMode = banMode;
     }

     public String getBanMessage() {
          return this.banMessage;
     }

     public void setBanMessage(String banMessage) {
          this.banMessage = banMessage;
     }
}
