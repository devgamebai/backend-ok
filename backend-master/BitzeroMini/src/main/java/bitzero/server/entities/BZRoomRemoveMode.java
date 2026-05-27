package bitzero.server.entities;

public enum BZRoomRemoveMode {
     DEFAULT,
     WHEN_EMPTY,
     WHEN_EMPTY_AND_CREATOR_IS_GONE,
     NEVER_REMOVE;

     public static BZRoomRemoveMode fromString(String id) {
          return valueOf(id.toUpperCase());
     }
}
