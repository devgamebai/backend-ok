package game.modules.slot.cmd.rev.thuycung;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class SubscribeThuyCungCmd extends BaseCmd {
     public byte roomId;

     public SubscribeThuyCungCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomId = this.readByte(bf);
          super.unpackData();
     }
}
