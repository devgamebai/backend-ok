package game.modules.slot.cmd.send.candy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotCandyMsg extends BaseMsg {
     public long value;
     public byte x2 = 0;

     public UpdatePotCandyMsg() {
          super((short)9002);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putLong(this.value);
          bf.put(this.x2);
          return this.packBuffer(bf);
     }
}
