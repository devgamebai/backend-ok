
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class RequestPlayerFireSpecial extends BaseCmd {
    public int specialID;
    public int m_ChairID;
    public int angle;
    public int endPosX;
    public int endPosY;

    public RequestPlayerFireSpecial(DataCmd data) {
        super(data);
    }

    @Override
    public void unpackData() {
        ByteBuffer bf = makeBuffer();
        specialID = readInt(bf);
        m_ChairID = readInt(bf);
        angle = readInt(bf);
        endPosX = readInt(bf);
        endPosY = readInt(bf);
    }
}
