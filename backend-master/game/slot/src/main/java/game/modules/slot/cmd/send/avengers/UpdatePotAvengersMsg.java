package game.modules.slot.cmd.send.avengers;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotAvengersMsg extends BaseMsg {
     public long value;
     public byte x2 = 0;

     public UpdatePotAvengersMsg() {
          super((short)4002);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putLong(this.value);
          bf.put(this.x2);
          return this.packBuffer(bf);
     }
}
