package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;

public class SendKetQuaXinThua extends BaseMsg {
   public static final byte KHONG_DU_NUOC_DI = 1;

   public SendKetQuaXinThua() {
      super((short)3125);
   }
}
