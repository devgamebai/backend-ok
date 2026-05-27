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

public class BetTaiXiuCmd
extends BaseCmd {
    public int userId;
    public long referenceId;
    public long betValue;
    public short moneyType;
    public short betSide;
    public short inputTime;

    public BetTaiXiuCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer buffer = this.makeBuffer();
        this.userId = this.readInt(buffer);
        this.referenceId = buffer.getLong();
        this.betValue = buffer.getLong();
        this.moneyType = this.readShort(buffer);
        this.betSide = this.readShort(buffer);
        this.inputTime = this.readShort(buffer);
    }
}

