package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTakeTurn extends BaseMsg {
   public int chair = 20;
   public byte[] from;
   public byte[] to;
   public String key;
   public String die = "none";
   public int result;
   public boolean isTrans = false;
   public String newKey = "";

   public SendTakeTurn() {
      super((short)3101);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put((byte)this.chair);
      this.putByteArray(bf, this.from);
      this.putByteArray(bf, this.to);
      this.putStr(bf, this.key);
      this.putStr(bf, this.die);
      bf.put((byte)this.result);
      this.putBoolean(bf, this.isTrans);
      this.putStr(bf, this.newKey);
      return this.packBuffer(bf);
   }
}
