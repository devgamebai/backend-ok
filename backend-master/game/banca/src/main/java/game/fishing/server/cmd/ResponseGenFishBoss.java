package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishBossObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ResponseGenFishBoss extends BaseMsg {
    public byte groupID;
    public ArrayList<FishBossObject> listFishBoss;
    public int timeBoss;

    public ResponseGenFishBoss() {
        super(CmdDefine.FISHING_GEN_BOSS);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(timeBoss);
        bf.put(groupID);
        bf.putInt(listFishBoss.size());
        for (FishBossObject boss : listFishBoss) {
            bf.putLong(boss.getnFishKey());
            bf.putInt(boss.getnFishType());
        }
        return packBuffer(bf);
    }
}