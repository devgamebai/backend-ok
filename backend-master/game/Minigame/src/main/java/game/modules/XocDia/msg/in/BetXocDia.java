package game.modules.XocDia.msg.in;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;

public class BetXocDia extends BaseCmd {
    public byte type;
    public long money;

    public BetXocDia(DataCmd cmd) {
        super(cmd);
        this.unpackData();
    }

    public void unpackData() {
        try {
            this.type = this.data.readByte();
            this.money = this.data.readLong();
        } catch (Exception e) {
            Debug.trace(new Object[]{"BetXocDia unpack error", e.getMessage()});
        }
    }
}
