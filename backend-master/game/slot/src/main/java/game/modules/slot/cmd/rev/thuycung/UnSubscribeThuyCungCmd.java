package game.modules.slot.cmd.rev.thuycung;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class UnSubscribeThuyCungCmd extends BaseCmd {
     public byte roomId;

     public UnSubscribeThuyCungCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomId = bf.get();
     }
}
