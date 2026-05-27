/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.gameRoom.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class JoinGameRoomCmd
extends BaseCmd {
    public int moneyType;
    public int maxUserPerRoom;
    public long moneyBet;
    public int rule = 0;

    public JoinGameRoomCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.moneyType = this.readInt(bf);
        this.maxUserPerRoom = this.readInt(bf);
        this.moneyBet = bf.getLong();
        this.rule = bf.getInt();
    }
}

