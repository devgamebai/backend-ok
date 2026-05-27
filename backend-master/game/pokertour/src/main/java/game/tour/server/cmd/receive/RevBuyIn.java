/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.tour.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevBuyIn
extends BaseCmd {
    public long moneyBuyIn = 0L;
    public boolean autoBuyIn = true;

    public RevBuyIn(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.moneyBuyIn = bf.getLong();
        this.autoBuyIn = this.readBoolean(bf);
    }
}

