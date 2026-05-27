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

public class AutoPlayAuditionCmd
extends BaseCmd {
    public byte autoPlay;
    public String lines;

    public AutoPlayAuditionCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.autoPlay = bf.get();
        this.lines = this.readString(bf);
    }
}

