package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.coup.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendUpdateMatch extends BaseMsg {
   public byte chair;
   public boolean[] hasInfoAtChair = new boolean[20];
   public GamePlayer[] pInfos = new GamePlayer[20];

   public SendUpdateMatch() {
      super((short)3123);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put(this.chair);
      this.putBooleanArray(bf, this.hasInfoAtChair);

      for(int i = 0; i < 20; ++i) {
         if (this.hasInfoAtChair[i]) {
            GamePlayer gp = this.pInfos[i];
            bf.putInt(gp.playerStatus);
         }
      }

      return this.packBuffer(bf);
   }
}
