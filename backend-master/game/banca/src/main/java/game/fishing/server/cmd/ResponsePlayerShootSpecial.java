
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponsePlayerShootSpecial extends BaseMsg {
    public int specialID;
    public int wChairID;
    public int angle;
    public int endPosX;
    public int endPosY;



    public ResponsePlayerShootSpecial() {
        super(CmdDefine.FISHING_SHOOT_SPECIAL);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(specialID);
        bf.putInt(wChairID);
        bf.putInt(angle);
        bf.putInt(endPosX);
        bf.putInt(endPosY);
        return packBuffer(bf);
    }
}
