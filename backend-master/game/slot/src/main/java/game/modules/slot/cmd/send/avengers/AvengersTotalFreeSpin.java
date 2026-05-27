package game.modules.slot.cmd.send.avengers;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class AvengersTotalFreeSpin extends BaseMsg {
     public int prize;
     public byte ratio;

     public AvengersTotalFreeSpin() {
          super((short)4011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putInt(this.prize);
          bf.put(this.ratio);
          return super.createData();
     }
}
