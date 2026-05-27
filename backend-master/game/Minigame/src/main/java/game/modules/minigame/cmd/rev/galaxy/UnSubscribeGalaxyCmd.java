/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.minigame.cmd.rev.galaxy;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class UnSubscribeGalaxyCmd
extends BaseCmd {
    public byte roomId;

    public UnSubscribeGalaxyCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomId = bf.get();
    }
}

