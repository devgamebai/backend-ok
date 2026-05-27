package game.modules.slot.cmd.rev.sexygirl;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class PlaySexyGirlCmd extends BaseCmd {
    public String lines;

    public PlaySexyGirlCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.lines = this.readString(bf);
    }
}
