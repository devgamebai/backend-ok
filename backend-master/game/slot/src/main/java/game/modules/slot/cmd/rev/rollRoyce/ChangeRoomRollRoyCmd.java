/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.slot.cmd.rev.rollRoyce;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

import java.nio.ByteBuffer;

public class ChangeRoomRollRoyCmd
extends BaseCmd {
    public byte roomLeavedId;
    public byte roomJoinedId;

    public ChangeRoomRollRoyCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.roomLeavedId = this.readByte(bf);
        this.roomJoinedId = this.readByte(bf);
    }
}

