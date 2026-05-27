/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.gameRoom.entities.ThongTinThangLon;
import java.nio.ByteBuffer;

public class SendNoHu
extends BaseMsg {
    public ThongTinThangLon info;

    public SendNoHu() {
        super((short)3007);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.info.gameName);
        bf.putLong(this.info.MoneyAdd);
        bf.putLong(this.info.currentMoney);
        this.putStr(bf, this.info.nickName);
        this.putBoolean(bf, Boolean.valueOf(this.info.noHu));
        this.putByteArray(bf, this.info.cards);
        return this.packBuffer(bf);
    }
}

