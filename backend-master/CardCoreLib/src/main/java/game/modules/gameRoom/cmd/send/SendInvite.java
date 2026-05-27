/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendInvite
extends BaseMsg {
    public int roomID;
    public int maxUserPerRoom;
    public long moneyBet;
    public String inviter;
    public int rule;

    public SendInvite() {
        super((short)3011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.roomID);
        bf.put((byte)this.maxUserPerRoom);
        this.putLong(bf, this.moneyBet);
        this.putStr(bf, this.inviter);
        bf.putInt(this.rule);
        return this.packBuffer(bf);
    }
}

