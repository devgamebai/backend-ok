package bitzero.server.config;

import java.util.concurrent.atomic.AtomicInteger;

public class ZoneSettings {
     private static final AtomicInteger idGenerator = new AtomicInteger();
     public int id;
     public String name;
     public boolean isCustomLogin;
     public int maxUsers;
     public int minRoomNameChars;
     public int maxRoomNameChars;
     public int maxRooms;
     public int userCountChangeUpdateInterval;
     public int userReconnectionSeconds;
     public String defaultPlayerIdGeneratorClass;

     public ZoneSettings() {
          this.name = "";
          this.isCustomLogin = false;
          this.maxUsers = 1000;
          this.minRoomNameChars = 3;
          this.maxRoomNameChars = 100;
          this.maxRooms = 500;
          this.userCountChangeUpdateInterval = 1000;
          this.userReconnectionSeconds = 10;
          this.defaultPlayerIdGeneratorClass = "";
          this.getId();
     }

     public ZoneSettings(String name) {
          this();
          this.name = name;
     }

     public synchronized int getId() {
          return this.id;
     }

     private static int setUniqueId() {
          return idGenerator.getAndIncrement();
     }
}
