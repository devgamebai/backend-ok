/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GiftCodeMsg
extends BaseMsgEx {
    public long currentMoneyVin;
    public long currentMoneyXu;
    public long moneyGiftCodeVin;
    public long moneyGiftCodeXu;

    public GiftCodeMsg() {
        super(20017);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoneyVin);
        bf.putLong(this.currentMoneyXu);
        bf.putLong(this.moneyGiftCodeVin);
        bf.putLong(this.moneyGiftCodeXu);
        return this.packBuffer(bf);
    }
}

