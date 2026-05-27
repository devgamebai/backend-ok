package game.modules.slot.cmd.send.ndv;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class NDVFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public NDVFreeDailyMsg() {
          super((short)3012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
