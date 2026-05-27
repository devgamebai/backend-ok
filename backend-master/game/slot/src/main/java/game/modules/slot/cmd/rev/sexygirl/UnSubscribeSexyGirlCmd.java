package game.modules.slot.cmd.rev.sexygirl;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class UnSubscribeSexyGirlCmd extends BaseCmd {
    public byte roomId;

    public UnSubscribeSexyGirlCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomId = bf.get();
    }
}
