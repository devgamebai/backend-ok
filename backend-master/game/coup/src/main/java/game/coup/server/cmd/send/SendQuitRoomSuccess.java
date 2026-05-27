package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;

public class SendQuitRoomSuccess extends BaseMsg {
   public SendQuitRoomSuccess() {
      super((short)3006);
   }
}
