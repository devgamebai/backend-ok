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

public class CreateGameRoomCmd
extends BaseCmd {
    public int moneyType;
    public int maxUserPerRoom;
    public long moneyBet;
    public int rule = 0;
    public int limitPlayer;
    public int roomId = -1;
    public String password;
    public String roomName;
    public long moneyRequire;

    public CreateGameRoomCmd(DataCmd dataCmd) {
        super(dataCmd);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer bf = this.makeBuffer();
        this.moneyType = this.readInt(bf);
        this.maxUserPerRoom = this.readInt(bf);
        this.moneyBet = bf.getLong();
        this.rule = this.readInt(bf);
        this.limitPlayer = this.readInt(bf);
        this.password = this.readString(bf);
        this.roomName = this.readString(bf);
        this.moneyRequire = this.readLong(bf);
    }
}

