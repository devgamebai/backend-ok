package game.modules.slot.cmd.send.thuycung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThuyCungFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public ThuyCungFreeDailyMsg() {
          super((short)18012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
