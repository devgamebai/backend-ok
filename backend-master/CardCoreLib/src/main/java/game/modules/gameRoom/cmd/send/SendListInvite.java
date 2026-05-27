/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendListInvite
extends BaseMsg {
    public static final int MAX_USER_PER_INVITE = 10;
    public static final String LAST_TIME_INVITE = "LAST_TIME_INVITE";
    public String[] listName;
    public long[] listMoney;

    public SendListInvite() {
        super((short)3010);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStringArray(bf, this.listName);
        this.putLongArray(bf, this.listMoney);
        return this.packBuffer(bf);
    }
}

