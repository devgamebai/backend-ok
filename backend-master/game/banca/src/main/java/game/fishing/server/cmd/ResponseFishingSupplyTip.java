
package game.fishing.server.cmd;

import bitzero.server.extensions.data.BaseMsg;
import game.fishing.server.CmdDefine;

import java.nio.ByteBuffer;

public class ResponseFishingSupplyTip extends BaseMsg {
    public int wChairID;

    public ResponseFishingSupplyTip() {
        super(CmdDefine.FISHING_SUPPLY_TIP);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = makeBuffer();
        bf.put((byte) wChairID);
        return packBuffer(bf);
    }
}