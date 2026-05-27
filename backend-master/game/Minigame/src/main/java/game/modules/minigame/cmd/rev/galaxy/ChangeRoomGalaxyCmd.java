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

public class ChangeRoomGalaxyCmd
extends BaseCmd {
    public byte roomLeavedId;
    public byte roomJoinedId;

    public ChangeRoomGalaxyCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomLeavedId = this.readByte(bf);
        this.roomJoinedId = this.readByte(bf);
    }
}

