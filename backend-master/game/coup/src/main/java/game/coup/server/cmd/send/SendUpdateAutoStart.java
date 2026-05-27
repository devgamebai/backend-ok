package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUpdateAutoStart extends BaseMsg {
   public boolean isAutoStart;
   public byte autoStartTime;

   public SendUpdateAutoStart() {
      super((short)3107);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      this.putBoolean(bf, this.isAutoStart);
      bf.put(this.autoStartTime);
      return this.packBuffer(bf);
   }
}
