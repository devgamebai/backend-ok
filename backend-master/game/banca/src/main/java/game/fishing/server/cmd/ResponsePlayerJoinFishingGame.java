package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;

public class ResponsePlayerJoinFishingGame extends BaseMsg {
    public FishingPlayer player;

    public ResponsePlayerJoinFishingGame() {
        super(CmdDefine.FISHING_PLAYER_JOIN_GAME);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.putInt(player.getGame().getId());
        bf.putInt(player.getId());
        putStr(bf, player.getName());
        bf.put((byte) player.getwChairID());
        bf.putLong(player.getChips());

        return packBuffer(bf);
    }
}

