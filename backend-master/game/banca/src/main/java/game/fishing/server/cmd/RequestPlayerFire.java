package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class RequestPlayerFire extends BaseCmd {
    public int m_ChairID;
    public int nMultipleIndex;
    public long m_fishIndex;
    public int m_index;
    public int posX;
    public int posY;

    public RequestPlayerFire(DataCmd data) {
        super(data);
    }

    @Override
    public void unpackData() {
        ByteBuffer bf = makeBuffer();
        m_ChairID = readInt(bf);
        nMultipleIndex = readInt(bf);
        m_fishIndex = readLong(bf);
        m_index = readInt(bf);
        posX = readInt(bf);
        posY = readInt(bf);

    }
}
