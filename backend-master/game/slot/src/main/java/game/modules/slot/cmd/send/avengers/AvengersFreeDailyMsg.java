package game.modules.slot.cmd.send.avengers;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class AvengersFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public AvengersFreeDailyMsg() {
          super((short)4012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
