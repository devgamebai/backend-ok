package game.modules.slot.cmd.rev.galaxy;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class PlayGalaxyCmd extends BaseCmd {
    public String lines;

    public PlayGalaxyCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        // Client (Slot3x3Gem) sends betValue(int4) before lines string.
        // Skip it — betValue is determined by the room, not the packet.
        if (bf.remaining() > 4) {
            int maybeBetValue = bf.getInt();
            // Peek: if next 2 bytes look like a valid string length, we skipped betValue correctly.
            // If not (legacy client sends only string), rewind.
            if (bf.remaining() >= 2) {
                int strLen = ((bf.get(bf.position()) & 0xFF) << 8) | (bf.get(bf.position() + 1) & 0xFF);
                if (strLen > bf.remaining() - 2 || strLen < 0) {
                    // Not a valid string after skip — rewind (legacy client without betValue)
                    bf.position(bf.position() - 4);
                }
            }
        }
        this.lines = this.readString(bf);
    }
}
