package bitzero.server.security;

public enum DefaultPermissionProfile implements IPermissionProfile {
     GUEST(0),
     STANDARD(1),
     MODERATOR(2),
     ADMINISTRATOR(3);

     private short id;

     private DefaultPermissionProfile(int id) {
          this.id = (short)id;
     }

     public short getId() {
          return this.id;
     }
}
