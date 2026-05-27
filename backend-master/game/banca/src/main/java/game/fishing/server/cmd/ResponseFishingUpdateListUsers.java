package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;
import game.fishing.server.Object.FishingPlayer;

import java.nio.ByteBuffer;
import java.util.Set;

public class ResponseFishingUpdateListUsers extends BaseMsg {
    public Set<FishingPlayer> listPlayers;

    public ResponseFishingUpdateListUsers() {
        super(CmdDefine.FISHING_UPDATE_LIST_USERS);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();

        bf.putInt(listPlayers.size());
        for (FishingPlayer player : listPlayers) {
            bf.putInt(player.getId());
            putStr(bf, player.getName());
            bf.putInt(player.getwChairID());
            bf.putLong(player.getChips());
        }

        return packBuffer(bf);
    }
}

