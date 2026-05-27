
package game.fishing.server.cmd;


import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.services.FishingGameServiceImpl;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ResponseUpdateFishingGame extends BaseMsg {
    public ArrayList<Long> listFish = new ArrayList<>();
    public int[] nMultipleIndex;

    public ResponseUpdateFishingGame() {
        super(CmdDefine.FISHING_UPDATE_GAME);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(listFish.size());
        for (int i = 0; i < listFish.size(); i++)
            bf.putLong(listFish.get(i));

        bf.put((byte) nMultipleIndex.length);
        for (int i = 0; i < nMultipleIndex.length; i++)
            bf.put((byte) nMultipleIndex[i]);

        bf.putLong(FishingGameServiceImpl.getInstance().getMoneyNormalJackpot());
        bf.putLong(FishingGameServiceImpl.getInstance().getMoneyVipJackpot());
        return packBuffer(bf);
    }
}
