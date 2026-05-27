package game.modules.slot.cmd.send.candy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class CandyTotalFreeSpin extends BaseMsg {
     public int prize;
     public byte ratio;

     public CandyTotalFreeSpin() {
          super((short)9011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putInt(this.prize);
          bf.put(this.ratio);
          return super.createData();
     }
}
