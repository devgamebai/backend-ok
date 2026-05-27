package bitzero.server.controllers;

public enum SuperUserSendMode {
     TO_USER(0),
     TO_ROOM(1),
     TO_GROUP(2),
     TO_ZONE(3);

     private int modeId;

     private SuperUserSendMode(int id) {
          this.modeId = id;
     }

     public int getId() {
          return this.modeId;
     }

     public static SuperUserSendMode fromId(int id) {
          SuperUserSendMode mode = null;
          SuperUserSendMode[] asuperusersendmode;
          int j = (asuperusersendmode = values()).length;

          for(int i = 0; i < j; ++i) {
               SuperUserSendMode item = asuperusersendmode[i];
               if (item.getId() == id) {
                    mode = item;
                    break;
               }
          }

          return mode;
     }
}
