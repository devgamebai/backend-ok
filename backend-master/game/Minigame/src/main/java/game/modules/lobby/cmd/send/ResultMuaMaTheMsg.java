/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultMuaMaTheMsg
extends BaseMsgEx {
    public long currentMoney;
    public String softpin;

    public ResultMuaMaTheMsg() {
        super(20035);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        this.putStr(bf, this.softpin);
        return this.packBuffer(bf);
    }
}

