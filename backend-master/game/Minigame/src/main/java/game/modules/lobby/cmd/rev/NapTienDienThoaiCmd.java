/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.lobby.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class NapTienDienThoaiCmd
extends BaseCmd {
    public String mobile;
    public byte amount;
    public byte type;

    public NapTienDienThoaiCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.mobile = this.readString(bf);
        this.amount = bf.get();
        this.type = bf.get();
    }
}

