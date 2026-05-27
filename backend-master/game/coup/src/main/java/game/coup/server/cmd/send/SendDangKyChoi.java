package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.coup.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendDangKyChoi extends BaseMsg {
   public static final byte DANG_KY = 1;
   public static final byte HUY = 2;
   public static final byte DANG_KY_HUY = 3;
   public byte action = 1;
   public GamePlayer gp = null;

   public SendDangKyChoi() {
      super((short)3108);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      if (this.gp != null) {
         bf.put(this.action);
         bf.put((byte)this.gp.gameChair);
         this.putStr(bf, this.gp.pInfo.nickName);
         this.putLong(bf, this.gp.gameMoneyInfo.getCurrentMoneyFromCache());
      } else {
         bf.put((byte)0);
         bf.put((byte)0);
         this.putStr(bf, "");
         this.putLong(bf, 0L);
      }

      return this.packBuffer(bf);
   }
}
