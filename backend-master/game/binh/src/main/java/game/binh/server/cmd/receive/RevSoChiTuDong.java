/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.binh.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevSoChiTuDong
extends BaseCmd {
    public byte[] chi1;
    public byte[] chi2;
    public byte[] chi3;

    public RevSoChiTuDong(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.chi1 = this.readByteArray(bf);
        this.chi2 = this.readByteArray(bf);
        this.chi3 = this.readByteArray(bf);
    }
}

