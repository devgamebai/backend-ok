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

public class UnsubscribeMiniGameCmd
extends BaseCmd {
    public short gameId;
    public short roomId;

    public UnsubscribeMiniGameCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer buffer = this.makeBuffer();
        this.gameId = this.readShort(buffer);
        this.roomId = this.readShort(buffer);
    }
}

