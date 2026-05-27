package game.coup.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevTakeTurn extends BaseCmd {
   public byte[] from;
   public byte[] to;

   public RevTakeTurn(DataCmd dataCmd) {
      super(dataCmd);
      this.unpackData();
   }

   public void unpackData() {
      ByteBuffer bf = this.makeBuffer();
      this.from = this.readByteArray(bf);
      this.to = this.readByteArray(bf);
   }
}
