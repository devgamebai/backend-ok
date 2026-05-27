package game.modules.slot.cmd.rev.tamhung;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class UnSubscribeTamHungCmd extends BaseCmd {
     public byte roomId;

     public UnSubscribeTamHungCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.roomId = bf.get();
     }
}
