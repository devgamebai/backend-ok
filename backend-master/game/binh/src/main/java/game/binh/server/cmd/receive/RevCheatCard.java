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

public class RevCheatCard
extends BaseCmd {
    public boolean isCheat = false;
    public byte[] cards;

    public RevCheatCard(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.isCheat = this.readBoolean(bf);
        if (this.isCheat) {
            this.cards = this.readByteArray(bf);
        }
    }
}

