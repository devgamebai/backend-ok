/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.minigame.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class LotteryCmd
extends BaseCmd {
    public String num;
    public long mode;
    public long betValue;

    public LotteryCmd(DataCmd data) {
        super(data);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer buffer = this.makeBuffer();
        this.num = this.readString(buffer);
        this.mode = this.readLong(buffer);
        this.betValue = this.readLong(buffer);
    }
}

