package game.modules.slot.cmd.send.vqv;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class VQVFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public VQVFreeDailyMsg() {
          super((short)5012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
