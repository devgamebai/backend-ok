/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.player.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class LoginCmd
extends BaseCmd {
    public static final int ERROR_SESSIONKEY = 1;
    public static final int ERROR_BAN = 2;
    public static final int ERROR_MAINTAIN = 3;
    public String nickname;
    public String sessionKey;

    public LoginCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.nickname = this.readString(bf);
        this.sessionKey = this.readString(bf);
    }
}

