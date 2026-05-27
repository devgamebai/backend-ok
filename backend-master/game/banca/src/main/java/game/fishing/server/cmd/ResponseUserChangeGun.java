package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponseUserChangeGun extends BaseMsg {
    public int wChairID;
    public int nMultipleIndex;

    public ResponseUserChangeGun() {
        super(CmdDefine.FISHING_CHANGE_GUN);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(wChairID);
        bf.putInt(nMultipleIndex);
        return packBuffer(bf);
    }
}