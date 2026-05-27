package game.modules.slot.cmd.rev.khobau;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class MinimizeKhoBauCmd extends BaseCmd {
     public byte roomId;

     public MinimizeKhoBauCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomId = this.readByte(bf);
     }
}
