/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.poker.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevTakeTurn
extends BaseCmd {
    public boolean fold;
    public boolean check;
    public boolean allIn;
    public boolean follow;
    public boolean callAll;
    public long raise;

    public RevTakeTurn(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.fold = this.readBoolean(bf);
        this.check = this.readBoolean(bf);
        this.allIn = this.readBoolean(bf);
        this.follow = this.readBoolean(bf);
        this.callAll = this.readBoolean(bf);
        this.raise = bf.getLong();
    }
}

