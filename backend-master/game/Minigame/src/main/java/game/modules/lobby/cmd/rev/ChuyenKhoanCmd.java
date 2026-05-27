/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.lobby.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class ChuyenKhoanCmd
extends BaseCmd {
    public String receiver;
    public long moneyExchange;
    public String description;

    public ChuyenKhoanCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.receiver = this.readString(bf);
        this.moneyExchange = bf.getLong();
        this.description = this.readString(bf);
    }
}

