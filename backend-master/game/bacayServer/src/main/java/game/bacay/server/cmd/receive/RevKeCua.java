/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.bacay.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevKeCua
extends BaseCmd {
    public int chair;
    public int rate;

    public RevKeCua(DataCmd data) {
        super(data);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.chair = this.readByte(bf);
        this.rate = this.readByte(bf);
    }
}

