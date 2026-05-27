package game.modules.slot.cmd.send.thuycung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThuyCungTotalFreeSpin extends BaseMsg {
     public int prize;
     public byte ratio;

     public ThuyCungTotalFreeSpin() {
          super((short)18011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putInt(this.prize);
          bf.put(this.ratio);
          return super.createData();
     }
}
