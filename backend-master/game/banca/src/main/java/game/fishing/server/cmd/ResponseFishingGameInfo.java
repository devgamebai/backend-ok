package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.FishingGame;
import game.fishing.server.Object.Fish;
import game.fishing.server.Object.FishData;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ResponseFishingGameInfo extends BaseMsg {
    public FishingGame game;
    public FishingPlayer player;


    public ResponseFishingGameInfo() {
        super(CmdDefine.FISHING_GAME_INFO);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.put((byte) game.getTypeOfGame());
        bf.put((byte) game.getCbBackIndex());
        bf.put((byte) game.getnFishMultiple().length);
        for (int[] i : game.getnFishMultiple()) {
            bf.putInt(i[0]);
            bf.putInt(i[1]);
        }

        bf.put((byte) game.getnMultipleIndex().length);
        for (int i : game.getnMultipleIndex())
            bf.put((byte) i);
        bf.putInt(game.getnBulletCoolingTime());
        bf.putInt(game.getnBulletVelocity());

        bf.put((byte) game.getnMultipleValue().length);
        for (int i : game.getnMultipleValue())
            bf.putInt(i);

        bf.putInt((int) game.getId());
        bf.put((byte) player.getwChairID());
        bf.putLong(player.getChips());
        List<Fish> listFish = new ArrayList<>();

        for (Fish fish : game.getListFish()) {
            if (fish != null && !fish.isDead())
                listFish.add(fish);
        }
        bf.put((byte) listFish.size());
        for (Fish fish : listFish) {
            FishData fData = fish.getM_data();
            bf.putLong(fData.nFishKey);
            bf.putInt((int)(fData.lCreateTime - player.getTimeJoinGame()));
            bf.put((byte) fData.nFishType);
            bf.put((byte) fData.nFishState);
            bf.put(fData.bRepeatCreate ? (byte) 1 : 0);
            bf.putDouble(fData.fRotateAngle);
            bf.putInt((int) fData.PointOffSet.x);
            bf.putInt((int) fData.PointOffSet.y);
            bf.put((byte) fData.nBezierCount);
            for (int i = 0; i < fData.nBezierCount; i++) {
                bf.putInt((int) fData.TBzierPoint[i].BeginPoint.x);
                bf.putInt((int) fData.TBzierPoint[i].BeginPoint.y);

                bf.putInt((int) fData.TBzierPoint[i].EndPoint.x);
                bf.putInt((int) fData.TBzierPoint[i].EndPoint.y);

                bf.putInt((int) fData.TBzierPoint[i].KeyOne.x);
                bf.putInt((int) fData.TBzierPoint[i].KeyOne.y);

                bf.putInt((int) fData.TBzierPoint[i].KeyTwo.x);
                bf.putInt((int) fData.TBzierPoint[i].KeyTwo.y);

                bf.putInt(fData.TBzierPoint[i].Time);
            }
        }

        return packBuffer(bf);
    }
}
