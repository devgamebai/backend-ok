/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.tienlen.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevDanhBai
extends BaseCmd {
    public boolean boluot;
    public byte[] cards;

    public RevDanhBai(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.boluot = this.readBoolean(bf);
        if (!this.boluot) {
            this.cards = this.readByteArray(bf);
        }
    }
}

