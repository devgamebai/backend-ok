package game.modules.slot.cmd.send.khobau;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class BigWinKhoBauMsg extends BaseMsg {
     public String username;
     public byte type;
     public long totalPrizes;
     public String timestamp;
     public short betValue;

     public BigWinKhoBauMsg() {
          super((short)2010);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          this.putStr(bf, this.username);
          bf.put(this.type);
          bf.putShort(this.betValue);
          bf.putLong(this.totalPrizes);
          this.putStr(bf, this.timestamp);
          return this.packBuffer(bf);
     }
}
