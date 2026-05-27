/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.slot.cmd.rev.audition;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class PlayAuditionCmd
extends BaseCmd {
    public int betValue;
    public String lines;

    public PlayAuditionCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.betValue = bf.getInt();
        this.lines = this.readString(bf);
    }
}

