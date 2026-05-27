package game.modules.slot.cmd.rev.vqv;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class ChangeRoomVQVCmd extends BaseCmd {
     public byte roomLeavedId;
     public byte roomJoinedId;

     public ChangeRoomVQVCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomLeavedId = this.readByte(bf);
          this.roomJoinedId = this.readByte(bf);
     }
}
