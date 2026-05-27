package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendDeadPiece extends BaseMsg {
   public String key = "x";

   public SendDeadPiece() {
      super((short)3126);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      this.putStr(bf, this.key);
      return this.packBuffer(bf);
   }
}
