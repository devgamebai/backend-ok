package game.modules.slot.cmd.rev.tamhung;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class AutoPlayTamHungCmd extends BaseCmd {
     public byte autoPlay;
     public String lines;

     public AutoPlayTamHungCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.autoPlay = bf.get();
          this.lines = this.readString(bf);
     }
}
