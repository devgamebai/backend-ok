package game.modules.slot.cmd.rev.khobau;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class PlayKhoBauCmd extends BaseCmd {
     public int betValue;
     public String lines;

     public PlayKhoBauCmd(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          // FE (Slot3) sends only lines string, no betValue int prefix.
          // Try reading as string first; if it looks like lines, use it.
          // Otherwise fall back to int+string format for backward compat.
          int pos = bf.position();
          try {
               String maybeLines = this.readString(bf);
               if (maybeLines != null && maybeLines.contains(",")) {
                    this.lines = maybeLines;
                    this.betValue = 0; // room provides betValue
               } else {
                    // Fall back: first field was betValue
                    bf.position(pos);
                    this.betValue = bf.getInt();
                    this.lines = this.readString(bf);
               }
          } catch (Exception e) {
               bf.position(pos);
               this.betValue = bf.getInt();
               this.lines = this.readString(bf);
          }
     }
}
