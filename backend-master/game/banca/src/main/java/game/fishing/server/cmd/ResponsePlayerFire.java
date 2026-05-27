
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponsePlayerFire extends BaseMsg {
    public int wChairID;
    public long currMoney;
    public int posX;
    public int posY;
    public long nTrackFishIndex;
    public int nBulletScore;


    public ResponsePlayerFire() {
        super(CmdDefine.FISHING_PLAYER_FIRE);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(wChairID);
        bf.putLong(currMoney);
        bf.putInt(posX);
        bf.putInt(posY);
        bf.putLong(nTrackFishIndex);
        bf.putInt(nBulletScore);

        return packBuffer(bf);
    }
}