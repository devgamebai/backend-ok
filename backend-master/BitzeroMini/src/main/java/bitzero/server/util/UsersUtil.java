package bitzero.server.util;

import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.Session;
import bitzero.engine.sessions.SessionType;
import bitzero.server.entities.User;

public class UsersUtil {
     private static User fakeAdminUser;
     private static User fakeModUser;
     private static volatile boolean isInited = false;

     public static boolean usersSeeEachOthers(User sender, User recipient) {
          Boolean seeEachOthers = false;
          if (recipient.getJoinedRoom().containsUser(sender)) {
               seeEachOthers = true;
          }

          return seeEachOthers;
     }

     public static User getServerAdmin() {
          if (!isInited) {
               initialize();
          }

          return fakeAdminUser;
     }

     public static User getServerModerator() {
          if (!isInited) {
               initialize();
          }

          return fakeModUser;
     }

     public static boolean isAllowedToPerformNewSearch(User user) {
          boolean ok = false;
          Long lastSearchTime = (Long)user.getSession().getSystemProperty("LastSearchTime");
          if (lastSearchTime == null) {
               ok = true;
          } else if (System.currentTimeMillis() - lastSearchTime > 1000L) {
               ok = true;
          }

          if (ok) {
               user.getSession().setSystemProperty("LastSearchTime", System.currentTimeMillis());
          }

          return ok;
     }

     private static synchronized void initialize() {
          ISession modSession = new Session();
          modSession.setType(SessionType.VOID);
          fakeModUser = new User("{Server.Mod}", modSession);
          ISession dmnSession = new Session();
          dmnSession.setType(SessionType.VOID);
          fakeAdminUser = new User("{Server.Admin}", dmnSession);
          isInited = true;
     }
}
