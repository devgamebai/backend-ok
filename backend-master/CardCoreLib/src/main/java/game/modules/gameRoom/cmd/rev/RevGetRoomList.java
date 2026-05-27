/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.gameRoom.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevGetRoomList
extends BaseCmd {
    public int moneyType;
    public int maxUserPerRoom;
    public long moneyBet;
    public int rule = 0;
    public int from;
    public int to;

    public RevGetRoomList(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.moneyType = this.readInt(bf);
        this.maxUserPerRoom = this.readInt(bf);
        this.moneyBet = bf.getLong();
        this.rule = this.readInt(bf);
        this.from = this.readInt(bf);
        this.to = this.readInt(bf);
    }
}

