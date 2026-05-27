package game.modules.slot.cmd.send.khobau;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class PokeGoX2Msg extends BaseMsg {
     public String ngayX2;

     public PokeGoX2Msg() {
          super((short)2009);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          this.putStr(bf, this.ngayX2);
          return this.packBuffer(bf);
     }
}
