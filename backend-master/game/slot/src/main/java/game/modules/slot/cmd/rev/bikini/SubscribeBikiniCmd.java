package game.modules.slot.cmd.rev.bikini;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class SubscribeBikiniCmd extends BaseCmd {
    public byte roomId;

    public SubscribeBikiniCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomId = this.readByte(bf);
        super.unpackData();
    }
}
