package game.modules.slot.cmd.send.galaxy;

import bitzero.server.extensions.data.BaseMsg;

public class ForceStopAutoPlayGalaxyMsg extends BaseMsg {
    public ForceStopAutoPlayGalaxyMsg() {
        super((short) 8008);
    }
}
