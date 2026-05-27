package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendEndGame extends BaseMsg {
   public byte result;
   public byte winChair;
   public long moneyWin;
   public long moneyLost;
   public int countdown;
   public long[] moneyArray = new long[2];

   public SendEndGame() {
      super((short)3103);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put(this.result);
      bf.put(this.winChair);
      bf.putLong(this.moneyWin);
      bf.putLong(this.moneyLost);
      bf.put((byte)this.countdown);
      this.putLongArray(bf, this.moneyArray);
      return this.packBuffer(bf);
   }
}
