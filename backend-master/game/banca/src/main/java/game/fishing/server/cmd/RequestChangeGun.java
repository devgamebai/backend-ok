package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class RequestChangeGun extends BaseCmd {
    public int gunID = -1;

    public RequestChangeGun(DataCmd data) {
        super(data);
    }

    @Override
    public void unpackData() {
        ByteBuffer bf = makeBuffer();
        gunID = readInt(bf);

    }
}
