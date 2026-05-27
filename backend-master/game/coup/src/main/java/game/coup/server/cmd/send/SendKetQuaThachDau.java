package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;

public class SendKetQuaThachDau extends BaseMsg {
   public static final byte HET_LUOT_THACH_DAU = 1;
   public static final byte DOI_THU_TU_CHOI = 2;
   public static final byte KHONG_DU_DIEU_KIEN = 3;

   public SendKetQuaThachDau() {
      super((short)3114);
   }
}
