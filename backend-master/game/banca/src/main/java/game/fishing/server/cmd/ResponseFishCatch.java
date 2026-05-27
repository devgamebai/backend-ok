
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishCatchObject;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class ResponseFishCatch extends BaseMsg {
    public ArrayList<FishCatchObject> fishArr;
    public FishingPlayer player;
    public int m_nMultipleIndex;
    public int m_nMultipleValue;

    public ResponseFishCatch() {
        super(CmdDefine.FISH_CATCH);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.put((byte) player.getwChairID());
        bf.putLong(player.getChips());
        bf.put((byte) m_nMultipleIndex);
        bf.put((byte) m_nMultipleValue);
        bf.put((byte) fishArr.size());
        for (FishCatchObject obj : fishArr) {
            bf.putLong(obj.nFishKey);
            bf.putInt((int)obj.nFishScore);
        }
        return packBuffer(bf);
    }
}