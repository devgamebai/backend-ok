/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.xocdia.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class CheatCmd
extends BaseCmd {
    public byte dince1;
    public byte dince2;
    public byte dince3;
    public byte dince4;

    public CheatCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.dince1 = this.readByte(bf);
        this.dince2 = this.readByte(bf);
        this.dince3 = this.readByte(bf);
        this.dince4 = this.readByte(bf);
    }
}

