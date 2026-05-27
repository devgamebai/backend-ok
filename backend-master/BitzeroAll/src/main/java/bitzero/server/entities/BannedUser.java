package bitzero.server.entities;

import bitzero.server.entities.managers.BanMode;

public interface BannedUser {
     long getBanTimeMillis();

     int getBanDurationMinutes();

     String getName();

     String getZoneName();

     String getIpAddress();

     BanMode getMode();

     String getReason();

     String getAdminName();

     boolean isExpired();

     int getBanTime();
}
