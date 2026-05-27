

package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class RequestFishCatch extends BaseCmd {
    public int m_index;
    public int m_nMultipleIndex;
    public long[] listFishCatch;


    public RequestFishCatch(DataCmd data) {
        super(data);
    }

    @Override
    public void unpackData() {
        ByteBuffer bf = makeBuffer();
        m_index = readInt(bf);
        m_nMultipleIndex = readInt(bf);
        int length = readInt(bf);
        listFishCatch = new long[length];
        for (int i = 0; i < length; i++) {
            listFishCatch[i] = readLong(bf);
        }

    }
}
