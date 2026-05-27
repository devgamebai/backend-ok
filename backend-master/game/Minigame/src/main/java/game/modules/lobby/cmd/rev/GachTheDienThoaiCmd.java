/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.lobby.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class GachTheDienThoaiCmd
extends BaseCmd {
    public byte provider;
    public String serial;
    public String pin;
    public String menhgia;

    public GachTheDienThoaiCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.provider = bf.get();
        this.serial = this.readString(bf);
        this.pin = this.readString(bf);
        this.menhgia = this.readString(bf);
    }
}

