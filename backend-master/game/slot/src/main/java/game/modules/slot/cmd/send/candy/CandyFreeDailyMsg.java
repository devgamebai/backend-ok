package game.modules.slot.cmd.send.candy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class CandyFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public CandyFreeDailyMsg() {
          super((short)9012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
