/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.player.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendBangXepHang
extends BaseMsg {
    public byte type;
    public String topDay = "[]";
    public String topWeek = "[]";
    public String topAll = "[]";
    public String topWeekWin = "[]";
    public String topAllWin = "[]";
    public String topDayWin = "[]";

    public SendBangXepHang() {
        super((short)1001);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.put(this.type);
        this.putStr(buffer, this.topWeek);
        this.putStr(buffer, this.topAll);
        this.putStr(buffer, this.topWeekWin);
        this.putStr(buffer, this.topAllWin);
        this.putStr(buffer, this.topDay);
        this.putStr(buffer, this.topDayWin);
        return this.packBuffer(buffer);
    }
}

