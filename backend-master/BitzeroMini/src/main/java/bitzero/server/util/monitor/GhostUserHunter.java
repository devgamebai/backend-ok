package bitzero.server.util.monitor;

import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.server.BitZeroServer;
import bitzero.server.api.IBZApi;
import bitzero.server.entities.User;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GhostUserHunter implements IGhostUserHunter {
     private final BitZeroServer bz = BitZeroServer.getInstance();
     private final IBZApi api;
     private ISessionManager sm;
     private final Logger log = LoggerFactory.getLogger(this.getClass());
     private static final String EOL = System.getProperty("line.separator");
     private static final int TRIGGER_INTERVAL_SEC = 900;
     private static final int TOT_CYCLES = 90;
     private int cycleCounter = 0;

     public GhostUserHunter() {
          this.api = this.bz.getAPIManager().getBzApi();
     }

     public void hunt() {
          if (this.sm == null) {
               this.sm = this.bz.getSessionManager();
          }

          if (++this.cycleCounter >= 90) {
               this.cycleCounter = 0;
               List ghosts = this.searchGhosts();
               if (ghosts.size() > 0) {
                    this.log.info(this.buildReport(ghosts));
               }

               Iterator iterator = ghosts.iterator();

               while(iterator.hasNext()) {
                    User ghost = (User)iterator.next();
                    this.api.disconnectUser(ghost);
               }

          }
     }

     private List searchGhosts() {
          List allUsers = this.bz.getUserManager().getAllUsers();
          List ghosts = new ArrayList();
          Iterator iterator = allUsers.iterator();

          while(true) {
               User u;
               ISession sess;
               do {
                    do {
                         if (!iterator.hasNext()) {
                              return ghosts;
                         }

                         u = (User)iterator.next();
                    } while(u.isNpc());

                    sess = u.getSession();
               } while((sess == null || this.sm.containsSession(sess)) && !sess.isIdle() && !sess.isMarkedForEviction());

               ghosts.add(u);
          }
     }

     private String buildReport(List ghosts) {
          StringBuilder sb = new StringBuilder("GHOST REPORT");
          sb.append(EOL).append("Total ghosts: ").append(ghosts.size()).append(EOL);

          try {
               Iterator iterator = ghosts.iterator();

               while(iterator.hasNext()) {
                    User ghost = (User)iterator.next();
                    sb.append(ghost.getUniqueId()).append(", ").append(ghost.getName()).append(", Connected: ").append(ghost.getSession().isConnected()).append(", Idle: ").append(ghost.getSession().isIdle()).append(", Marked: ").append(ghost.getSession().isMarkedForEviction()).append(", Frozen: ").append(ghost.getSession().isFrozen()).append(", sessById: ").append(this.sm.getSessionById(ghost.getUniqueId()));
               }
          } catch (Exception var5) {
          }

          return sb.toString();
     }
}
