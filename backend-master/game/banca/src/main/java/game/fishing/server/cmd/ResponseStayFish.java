package game.fishing.server.cmd;


import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ResponseStayFish extends BaseMsg {
    public ArrayList<Long> listStayFish;
    public int nStayTime;

    public ResponseStayFish() {
        super(CmdDefine.FISHING_STAY_FISH);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(listStayFish.size());
        for (long nFishKey : listStayFish) {
            bf.putLong(nFishKey);
        }
        bf.putInt(nStayTime);
        return packBuffer(bf);
    }
}