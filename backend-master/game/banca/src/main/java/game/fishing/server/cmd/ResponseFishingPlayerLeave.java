
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;

public class ResponseFishingPlayerLeave extends BaseMsg {
    public FishingPlayer player;

    public ResponseFishingPlayerLeave() {
        super(CmdDefine.FISHING_LEAVE_GAME);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(player.getId());
        putStr(bf, player.getName());
        bf.putInt(player.getwChairID());
        bf.putLong(player.getCurrentMoney());
        return packBuffer(bf);
    }
}