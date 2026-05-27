package game.modules.slot.cmd.send.hall;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdateJackpotsMsg extends BaseMsg {
     public String json;

     public UpdateJackpotsMsg() {
          super((short)10003);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          this.putStr(bf, this.json);
          return this.packBuffer(bf);
     }
}
