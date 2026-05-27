package bitzero.server.game;

import bitzero.server.entities.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseGameInvitationCallback {
     private Room game;
     private boolean leaveLastJoinedRoom;
     protected final Logger log = LoggerFactory.getLogger(this.getClass());

     public BaseGameInvitationCallback(Room game, boolean leaveLastJoinedRoom) {
          this.game = game;
          this.leaveLastJoinedRoom = leaveLastJoinedRoom;
     }

     protected Room getGame() {
          return this.game;
     }

     protected boolean isLeaveLastJoindRoom() {
          return this.leaveLastJoinedRoom;
     }
}
