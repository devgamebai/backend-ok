package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishData;


import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResponseFishCreate extends BaseMsg {
    public ArrayList<FishData> listFish;

    public ResponseFishCreate() {
        super(CmdDefine.FISHING_CREATE_FISH);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.put((byte) listFish.size());
        for (FishData fData : listFish) {
            bf.putLong(fData.nFishKey);
            bf.putInt(fData.unCreateTime);
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

                bf.putInt( fData.TBzierPoint[i].Time);
            }
        }

        return packBuffer(bf);
    }
}
