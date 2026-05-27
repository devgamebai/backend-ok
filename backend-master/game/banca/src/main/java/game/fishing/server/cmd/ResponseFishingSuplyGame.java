
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponseFishingSuplyGame extends BaseMsg {
    public int wChairID;
    public int lSupplyCount;
    public int nSupplyType;

    public ResponseFishingSuplyGame() {
        super(CmdDefine.FISHING_SUPLY_GAME);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.put((byte) wChairID);
        bf.put((byte)lSupplyCount);
        bf.put((byte)nSupplyType);
        return packBuffer(bf);
    }
}