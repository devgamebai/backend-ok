package bitzero.server.entities.managers;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BaseCoreService;
import bitzero.server.entities.BZBannedUser;
import bitzero.server.entities.BannedUser;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.exceptions.ExceptionMessageComposer;
import bitzero.server.util.ClientDisconnectionReason;
import bitzero.server.util.IDisconnectionReason;
import bitzero.server.util.UsersUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BZBannedUserManager extends BaseCoreService implements IBannedUserManager {
     private static String STORAGE_DEFAULT_CLASS = "bitzero.server.entities.managers.BZBannedUserStorage";
     private static final int KICK_TIMER_CLEANER_TASK_INTERVAL = 12;
     private static final long KICK_TIMER_MAX_LENGTH = 86400000L;
     private static final int BAN_AUTOSAVE_INTERVAL = 1;
     private static final int BAN_EXPIRER_TASK_INTERVAL = 2;
     private final Map kickHistoryByZone = new ConcurrentHashMap();
     private Map bannedUsersByName;
     private Map bannedUsersByIp;
     private BitZeroServer bz;
     private final Runnable kickHistoryCleanerTask = new KickHistoryCleanerTask();
     private final Runnable banExpirerTask = new BanExpirerTask();
     private final Runnable banAutoSaverTask = new BanDataAutoSaveTask();
     private final Thread shutDownHandler = new ShutDownHandler();
     private final Logger logger = LoggerFactory.getLogger(this.getClass());
     private boolean autoRemoveBan = false;
     private boolean persistent = false;
     private IBannedUserStorage storage;

     public void init(Object o) {
          super.init(o);

          try {
               Class clazz = Class.forName(STORAGE_DEFAULT_CLASS);
               this.storage = (IBannedUserStorage)clazz.newInstance();
          } catch (Exception var4) {
               ExceptionMessageComposer composer = new ExceptionMessageComposer(var4);
               composer.setDescription("the specified persistence class for the BannedUserManager cannot be found or instantiated");
               composer.setPossibleCauses("double check the fully qualified class name and make sure it implements the IBannedUserPersister interface.");
               this.logger.error(composer.toString());
          }

          this.storage.init();

          try {
               BanUserData storageData = this.storage.load();
               this.bannedUsersByIp = storageData.getBannedUsersByIp();
               this.bannedUsersByName = storageData.getBannedUsersByName();
               this.logger.info("BanUser data loaded: " + this.getTotalRecords() + " records.");
          } catch (Exception var5) {
               this.bannedUsersByIp = new ConcurrentHashMap();
               this.bannedUsersByName = new ConcurrentHashMap();
               if (var5 instanceof FileNotFoundException) {
                    this.logger.info("No BannedUser data available, starting with a clean DB.");
               } else {
                    this.logger.warn("Failure loading the BannedUser DB: " + var5);
               }
          }

          this.bz = BitZeroServer.getInstance();
          this.bz.getTaskScheduler().scheduleAtFixedRate(this.kickHistoryCleanerTask, 12, 12, TimeUnit.HOURS);
          this.bz.getTaskScheduler().scheduleAtFixedRate(this.banExpirerTask, 2, 2, TimeUnit.HOURS);
          this.bz.getTaskScheduler().scheduleAtFixedRate(this.banAutoSaverTask, 1, 1, TimeUnit.HOURS);
          Runtime.getRuntime().addShutdownHook(this.shutDownHandler);
     }

     public void destroy(Object o) {
          super.destroy(o);
          this.storage.destroy();
     }

     public void kickUser(User userToKick, User modUser, String kickMessage, int delaySeconds) {
          this.kickUser(userToKick, modUser, kickMessage, delaySeconds, false);
     }

     public void kickUser(User userToKick, User modUser, String kickMessage, int delaySeconds, boolean isBan) {
          if (!userToKick.isBeingKicked()) {
               userToKick.setBeingKicked(true);
               IDisconnectionReason disconnectionReason = isBan ? ClientDisconnectionReason.BAN : ClientDisconnectionReason.KICK;
               this.addUserToKickHistory(userToKick);
               userToKick.setReconnectionSeconds(0);
               if (delaySeconds <= 0) {
                    userToKick.disconnect(disconnectionReason);
               } else {
                    if (kickMessage == null || kickMessage.length() == 0) {
                         kickMessage = " unknow reason ! ";
                    }

                    modUser = modUser != null ? modUser : UsersUtil.getServerModerator();
                    this.bz.getAPIManager().getBzApi().sendModeratorMessage(modUser, kickMessage, (String[])null, Arrays.asList(userToKick.getSession()));
                    this.bz.getTaskScheduler().schedule(new KickTaskRunner(userToKick, disconnectionReason), delaySeconds, TimeUnit.SECONDS);
               }
          }
     }

     public void banUser(User userToBan, User modUser, int durationMinutes, BanMode mode, String reason, String banMessage, int delaySeconds) {
          modUser = modUser != null ? modUser : UsersUtil.getServerModerator();
          BannedUser bannedUser = new BZBannedUser(userToBan, durationMinutes, mode, reason, modUser.getName());
          this.addBannedUser(bannedUser);
          if (banMessage == null || banMessage.length() == 0) {
               banMessage = " unknow reason ! ";
          }

          this.kickUser(userToBan, modUser, banMessage, delaySeconds, true);
          String msg = String.format("User: %s is banned. Reason: %s", userToBan.getName(), reason);
          this.logger.info(msg);
     }

     public void banUser(String userName, int durationMinutes, BanMode mode, String reason, String adminName) {
          BannedUser bannedUser = new BZBannedUser(userName, durationMinutes, mode, reason, adminName);
          this.addBannedUser(bannedUser);
     }

     public void banUser(String userName, int durationMinutes, BanMode mode, String reason) {
          this.banUser(userName, durationMinutes, mode, reason, (String)null);
     }

     private void addBannedUser(BannedUser banned) {
          BanMode mode = banned.getMode();
          String banKey = null;
          Map bannedUsersMap = null;
          if (mode == BanMode.BY_ADDRESS) {
               banKey = banned.getIpAddress();
               bannedUsersMap = this.bannedUsersByIp;
          } else {
               banKey = banned.getName();
               bannedUsersMap = this.bannedUsersByName;
          }

          bannedUsersMap.put(banKey, banned);
     }

     public int getKickCount(String name, int rangeInSeconds) {
          int kickCount = 0;
          long timeRangeMillis = (long)(rangeInSeconds * 1000);
          long rangeCheck = System.currentTimeMillis() - timeRangeMillis;
          Map kickHistory = this.kickHistoryByZone;
          if (kickHistory != null) {
               List kickTimeList = (List)kickHistory.get(name);
               if (kickTimeList != null && kickTimeList.size() > 0) {
                    Iterator iterator = kickTimeList.iterator();

                    while(iterator.hasNext()) {
                         long kickTime = (Long)iterator.next();
                         if (kickTime > rangeCheck) {
                              ++kickCount;
                         }
                    }
               }
          }

          return kickCount;
     }

     public boolean isIpBanned(String ipAddress) {
          boolean isBanned = false;
          BannedUser bUser = (BannedUser)this.bannedUsersByIp.get(ipAddress);
          if (bUser != null && !bUser.isExpired()) {
               isBanned = true;
          }

          return isBanned;
     }

     public int getBanDuration(String userName, BanMode mode) {
          int duration = 0;
          BannedUser bUser = null;
          if (mode == BanMode.BY_ADDRESS) {
               bUser = (BannedUser)this.bannedUsersByIp.get(userName);
          } else {
               bUser = (BannedUser)this.bannedUsersByName.get(userName);
          }

          if (bUser != null) {
               duration = bUser.getBanTime();
          }

          return duration;
     }

     public boolean isNameBanned(String userName) {
          boolean isBanned = false;
          BannedUser bUser = (BannedUser)this.bannedUsersByName.get(userName);
          if (bUser != null && !bUser.isExpired()) {
               isBanned = true;
          }

          return isBanned;
     }

     public void removeBannedUser(String id, BanMode mode) {
          if (mode == BanMode.BY_ADDRESS) {
               this.bannedUsersByIp.remove(id);
          } else {
               this.bannedUsersByName.remove(id);
          }

     }

     public List getBannedUsersByIp() {
          return new ArrayList(this.bannedUsersByIp.values());
     }

     public List getBannedUsersByName(String zoneName) {
          return new ArrayList(this.bannedUsersByName.values());
     }

     public boolean isAutoRemoveBan() {
          return this.autoRemoveBan;
     }

     public boolean isPersistent() {
          return this.persistent;
     }

     public void setAutoRemoveBan(boolean flag) {
          this.autoRemoveBan = flag;
     }

     public void setPersistent(boolean flag) {
          this.persistent = flag;
     }

     public void setPersistenceClass(String className) {
          if (this.active) {
               throw new BZRuntimeException("Cannot change the BannedUserManager persistence class at runtime! Please change it in the configuration and restart the server.");
          } else {
               if (className != null && className.length() > 0) {
                    STORAGE_DEFAULT_CLASS = className;
               }

          }
     }

     public void sendWarningMessage(User recipient, User senderMod, String message) {
          if (senderMod == null) {
               senderMod = UsersUtil.getServerModerator();
          }

          this.bz.getAPIManager().getBzApi().sendModeratorMessage(senderMod, message, (String[])null, Arrays.asList(recipient.getSession()));
     }

     private void addUserToKickHistory(User user) {
          Map kickHistory = this.kickHistoryByZone;
          List kickTimeList = (List)kickHistory.get(user.getName());
          if (kickTimeList == null) {
               kickTimeList = new ArrayList();
               kickHistory.put(user.getName(), kickTimeList);
          }

          synchronized(kickTimeList) {
               ((List)kickTimeList).add(System.currentTimeMillis());
          }
     }

     private int getTotalRecords() {
          int tot = this.bannedUsersByIp.size();
          tot += this.bannedUsersByName.size();
          return tot;
     }

     private final class ShutDownHandler extends Thread {
          public void run() {
               try {
                    BZBannedUserManager.this.storage.save(new BanUserData(BZBannedUserManager.this.bannedUsersByName, BZBannedUserManager.this.bannedUsersByIp));
                    BZBannedUserManager.this.logger.info("BanUser data saved.");
               } catch (IOException var2) {
                    BZBannedUserManager.this.logger.warn("Failed saving BanUserData on server quit: " + var2);
               }

          }

          public ShutDownHandler() {
          }
     }

     static final class KickTaskRunner implements Runnable {
          private final User target;
          private final IDisconnectionReason reason;

          public void run() {
               if (this.target != null) {
                    this.target.disconnect(this.reason);
               }

          }

          public KickTaskRunner(User target, IDisconnectionReason reason) {
               this.target = target;
               this.reason = reason;
          }
     }

     private final class KickHistoryCleanerTask implements Runnable {
          public void run() {
               if (BZBannedUserManager.this.logger.isDebugEnabled()) {
                    BZBannedUserManager.this.logger.debug("KickCleanerTask running");
               }

               try {
                    long timeRange = System.currentTimeMillis() - 86400000L;
                    Map kickHistory = BZBannedUserManager.this.kickHistoryByZone;
                    Iterator iter = kickHistory.values().iterator();

                    while(true) {
                         List kickTimeList;
                         do {
                              if (!iter.hasNext()) {
                                   return;
                              }

                              kickTimeList = (List)iter.next();
                         } while(kickTimeList.size() <= 0);

                         synchronized(kickTimeList) {
                              Iterator iter2 = kickTimeList.iterator();

                              while(iter2.hasNext()) {
                                   long kickTime = (Long)iter2.next();
                                   if (kickTime < timeRange) {
                                        iter2.remove();
                                   }
                              }

                              if (kickTimeList.size() == 0) {
                                   iter.remove();
                              }
                         }
                    }
               } catch (Exception var12) {
                    BZBannedUserManager.this.logger.warn("Unexpected exception: " + var12);
               }
          }

          public KickHistoryCleanerTask() {
          }
     }

     private final class BanExpirerTask implements Runnable {
          public void run() {
               try {
                    if (BZBannedUserManager.this.logger.isDebugEnabled()) {
                         BZBannedUserManager.this.logger.debug("BanExpirer running");
                    }

                    this.cleanExpiredBanByIP();
                    this.cleanExpiredBanByName();
               } catch (Exception var2) {
                    BZBannedUserManager.this.logger.warn("Problem in BanExpirer iteration: " + var2);
               }

          }

          private void cleanExpiredBanByIP() {
               int removedItems = this.expirerLoop(BZBannedUserManager.this.bannedUsersByIp);
               if (removedItems > 0) {
                    BZBannedUserManager.this.logger.debug("Removed " + removedItems + " expired banned users by IP");
               }

          }

          private void cleanExpiredBanByName() {
               int removedItems = this.expirerLoop(BZBannedUserManager.this.bannedUsersByName);
               if (removedItems > 0) {
                    BZBannedUserManager.this.logger.debug("Removed " + removedItems + " expired banned users by name ");
               }

          }

          private int expirerLoop(Map data) {
               int count = 0;
               Iterator iterator = data.entrySet().iterator();

               while(iterator.hasNext()) {
                    Entry entry = (Entry)iterator.next();
                    BannedUser bUser = (BannedUser)entry.getValue();
                    if (bUser.isExpired()) {
                         data.remove(entry.getKey());
                         ++count;
                    }
               }

               return count;
          }

          public BanExpirerTask() {
          }
     }

     private final class BanDataAutoSaveTask implements Runnable {
          public void run() {
               try {
                    long t1 = System.currentTimeMillis();
                    BZBannedUserManager.this.storage.save(new BanUserData(BZBannedUserManager.this.bannedUsersByName, BZBannedUserManager.this.bannedUsersByIp));
                    if (BZBannedUserManager.this.logger.isDebugEnabled()) {
                         BZBannedUserManager.this.logger.debug("Ban User data autosave done in " + (System.currentTimeMillis() - t1) + "ms.");
                    }
               } catch (Exception var3) {
                    BZBannedUserManager.this.logger.warn("Banned User Data auto-save failed: " + var3);
               }

          }

          public BanDataAutoSaveTask() {
          }
     }
}
