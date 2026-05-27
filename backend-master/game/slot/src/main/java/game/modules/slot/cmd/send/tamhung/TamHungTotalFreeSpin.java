package game.modules.slot.cmd.send.tamhung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class TamHungTotalFreeSpin extends BaseMsg {
     public int prize;
     public byte ratio;

     public TamHungTotalFreeSpin() {
          super((short)15011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putInt(this.prize);
          bf.put(this.ratio);
          return super.createData();
     }
}
