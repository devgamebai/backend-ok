package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendThachDau extends BaseMsg {
   public String name;
   public int moneyBet;

   public SendThachDau() {
      super((short)3106);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      this.putStr(bf, this.name);
      bf.putInt(this.moneyBet);
      return this.packBuffer(bf);
   }
}
