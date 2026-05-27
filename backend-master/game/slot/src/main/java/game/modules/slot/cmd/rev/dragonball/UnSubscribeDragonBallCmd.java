package game.modules.slot.cmd.rev.dragonball;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class UnSubscribeDragonBallCmd extends BaseCmd {
    public byte roomId;

    public UnSubscribeDragonBallCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomId = bf.get();
    }
}
