/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.xocdia.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class SellPotCmd
extends BaseCmd {
    public static final byte CAN_TAT = 1;
    public static final byte BAN_CHAN = 2;
    public static final byte BAN_LE = 3;
    public byte action;
    public long moneySell;

    public SellPotCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.action = this.readByte(bf);
        this.moneySell = this.readLong(bf);
    }
}

