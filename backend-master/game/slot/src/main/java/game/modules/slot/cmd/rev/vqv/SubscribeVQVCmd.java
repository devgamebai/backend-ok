package game.modules.slot.cmd.rev.vqv;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class SubscribeVQVCmd extends BaseCmd {
     public byte roomId;

     public SubscribeVQVCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomId = this.readByte(bf);
     }
}
