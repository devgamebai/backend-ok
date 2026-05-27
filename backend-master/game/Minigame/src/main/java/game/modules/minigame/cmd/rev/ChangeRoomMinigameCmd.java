/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.minigame.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class ChangeRoomMinigameCmd
extends BaseCmd {
    public short gameId;
    public short lastRoomId;
    public short newRoomId;

    public ChangeRoomMinigameCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer buffer = this.makeBuffer();
        this.gameId = buffer.getShort();
        this.lastRoomId = buffer.getShort();
        this.newRoomId = buffer.getShort();
    }
}

