
package game.fishing.server.cmd;


import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponseFishingJackpotBroken extends BaseMsg {
    public String playerName;
    public long jackpotValue;

    public ResponseFishingJackpotBroken() {
        super(CmdDefine.FISHING_JACKPOT_BROKEN);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        putStr(bf, playerName);
        bf.putLong(jackpotValue);
        return packBuffer(bf);
    }
}