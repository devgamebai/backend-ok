package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;

public class SendKetQuaCauHoa extends BaseMsg {
   public static final byte HET_LUOT_CAU_HOA = 1;
   public static final byte DOI_THU_TU_CHOI = 2;
   public static final byte KHONG_DU_DIEU_KIEN = 3;

   public SendKetQuaCauHoa() {
      super((short)3113);
   }
}
