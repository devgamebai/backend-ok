package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;

public class ForceStopAutoPlayAuditionMsg extends BaseMsg {
    public ForceStopAutoPlayAuditionMsg() {
        super((short) 11008);
    }
}
