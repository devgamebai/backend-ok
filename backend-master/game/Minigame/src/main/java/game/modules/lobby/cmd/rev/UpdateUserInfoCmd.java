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

public class UpdateUserInfoCmd
extends BaseCmd {
    public String cmt;
    public String email;
    public String mobile;

    public UpdateUserInfoCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.cmt = this.readString(bf);
        this.email = this.readString(bf);
        this.mobile = this.readString(bf);
    }
}

