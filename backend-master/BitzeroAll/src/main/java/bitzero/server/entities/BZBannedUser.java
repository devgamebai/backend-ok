package bitzero.server.entities;

import bitzero.server.entities.managers.BanMode;
import java.io.Serializable;
import org.slf4j.LoggerFactory;

public class BZBannedUser implements BannedUser, Serializable {
     private static final long serialVersionUID = -8591228575994508738L;
     private final long banTimeMillis;
     private final int banDurationMinutes;
     private final long banDurationMillis;
     private final String name;
     private String ipAddress;
     private final BanMode mode;
     private final String reason;
     private final String adminName;

     public BZBannedUser(User user, int durationMinutes, BanMode mode, String reason, String adminName) {
          this(user.getName(), durationMinutes, mode, reason, adminName);
          this.ipAddress = user.getIpAddress();
     }

     public BZBannedUser(User user, int durationMinutes, BanMode mode) {
          this((User)user, durationMinutes, mode, (String)null, (String)null);
          this.ipAddress = user.getIpAddress();
     }

     public BZBannedUser(String userName, int durationMinutes, BanMode mode, String reason, String adminName) {
          this.banTimeMillis = System.currentTimeMillis();
          if (durationMinutes < 1) {
               LoggerFactory.getLogger(this.getClass()).warn("Invalid ban duration: " + durationMinutes + ", Automatically converted to 24hrs.");
               durationMinutes = 1440;
          }

          this.banDurationMinutes = durationMinutes;
          this.banDurationMillis = (long)this.banDurationMinutes * 60L * 1000L;
          this.name = userName;
          this.ipAddress = null;
          this.mode = mode;
          this.adminName = adminName == null ? "[Server]" : adminName;
          this.reason = reason == null ? "{Unknown}" : reason;
     }

     public BZBannedUser(String userName, int durationMinutes, BanMode mode) {
          this((String)userName, durationMinutes, mode, (String)null, (String)null);
     }

     public String getZoneName() {
          return "";
     }

     public long getBanTimeMillis() {
          return this.banTimeMillis;
     }

     public String getIpAddress() {
          return this.ipAddress;
     }

     public BanMode getMode() {
          return this.mode;
     }

     public String getName() {
          return this.name;
     }

     public String getReason() {
          return this.reason;
     }

     public String getAdminName() {
          return this.adminName;
     }

     public int getBanDurationMinutes() {
          return this.banDurationMinutes;
     }

     public boolean isExpired() {
          return System.currentTimeMillis() > this.banTimeMillis + this.banDurationMillis;
     }

     public int getBanTime() {
          return (int)((this.banTimeMillis + this.banDurationMillis - System.currentTimeMillis()) / 1000L);
     }

     public String toString() {
          return String.format("BannedUser [name: %s, ip: %s, mode: %s, reason: %s]", this.name, this.ipAddress, this.mode, this.reason);
     }
}
