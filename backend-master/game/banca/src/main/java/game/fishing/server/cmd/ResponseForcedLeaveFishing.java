package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;

public class ResponseForcedLeaveFishing extends BaseMsg {
    public FishingPlayer player;

    public ResponseForcedLeaveFishing() {
        super(CmdDefine.FISHING_FORCED_LEAVE_GAME);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(player.getId());
        putStr(bf, player.getName());
        bf.putInt(player.getwChairID());
        bf.putLong(player.getChips());
        return packBuffer(bf);
    }
}
