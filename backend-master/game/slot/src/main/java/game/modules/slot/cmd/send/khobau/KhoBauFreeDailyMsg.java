package game.modules.slot.cmd.send.khobau;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class KhoBauFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public KhoBauFreeDailyMsg() {
          super((short)2012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
