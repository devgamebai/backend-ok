package game.modules.slot.cmd.send.hall;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ListAutoPlayInfoMsg extends BaseMsg {
     public boolean autoKhoBau;
     public boolean autoNDV;
     public boolean autoAvenger;
     public boolean autoVQV;

     public ListAutoPlayInfoMsg() {
          super((short)10004);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          this.putBoolean(bf, this.autoKhoBau);
          this.putBoolean(bf, this.autoNDV);
          this.putBoolean(bf, this.autoAvenger);
          this.putBoolean(bf, this.autoVQV);
          return this.packBuffer(bf);
     }
}
