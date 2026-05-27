/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.xizach.server.GamePlayer;
import game.xizach.server.logic.GroupCard;
import game.xizach.server.sPlayerInfo;
import java.nio.ByteBuffer;

public class SendChuyenGiaiDoan3
extends BaseMsg {
    public byte[] cards = new byte[0];
    public GamePlayer chuongGp = null;

    public SendChuyenGiaiDoan3() {
        super((short)3125);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)30);
        GamePlayer gp = this.chuongGp;
        this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
        return this.packBuffer(bf);
    }
}

