package game.modules.slot.cmd.rev.candy;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class PlayCandyCmd extends BaseCmd {
     public String lines;

     public PlayCandyCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          // Client (Slot3x3) sends betValue(int4) before lines string.
          // Skip it — betValue is determined by the room, not the packet.
          if (bf.remaining() > 4) {
               int maybeBetValue = bf.getInt();
               if (bf.remaining() >= 2) {
                    int strLen = ((bf.get(bf.position()) & 0xFF) << 8) | (bf.get(bf.position() + 1) & 0xFF);
                    if (strLen > bf.remaining() - 2 || strLen < 0) {
                         bf.position(bf.position() - 4);
                    }
               }
          }
          this.lines = this.readString(bf);
     }
}
