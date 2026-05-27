/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.tour.control.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevGetTourRank
extends BaseCmd {
    public int tourId;
    public int from;
    public int to;

    public RevGetTourRank(DataCmd data) {
        super(data);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.tourId = bf.getInt();
        this.from = bf.getInt();
        this.to = bf.getInt();
    }
}

