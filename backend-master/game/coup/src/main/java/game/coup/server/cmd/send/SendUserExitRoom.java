package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUserExitRoom extends BaseMsg {
   public byte nChair;
   public String nickName;

   public SendUserExitRoom() {
      super((short)3119);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put(this.nChair);
      this.putStr(bf, this.nickName);
      return this.packBuffer(bf);
   }
}
