
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponseChangeScene extends BaseMsg {
    public int cbBackIndex;

    public ResponseChangeScene() {
        super(CmdDefine.FISHING_CHANGE_SCENE);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(cbBackIndex);
        return packBuffer(bf);
    }
}