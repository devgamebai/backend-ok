/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.minigame.cmd.rev.baucua;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class ChangeRoomBauCuaCmd
extends BaseCmd {
    public byte roomLeavedId;
    public byte roomJoinedId;

    public ChangeRoomBauCuaCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomLeavedId = bf.get();
        this.roomJoinedId = bf.get();
    }
}

