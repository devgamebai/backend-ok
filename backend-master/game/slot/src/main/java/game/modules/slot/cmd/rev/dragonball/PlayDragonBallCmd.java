package game.modules.slot.cmd.rev.dragonball;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class PlayDragonBallCmd extends BaseCmd {
    public String lines;

    public PlayDragonBallCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
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
