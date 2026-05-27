/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.pokego;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultPokeGoMsg
extends BaseMsgEx {
    public byte result;
    public String matrix = "";
    public String linesWin = "";
    public long prize;
    public long currentMoney;

    public ResultPokeGoMsg() {
        super(7001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        this.putStr(bf, this.matrix);
        this.putStr(bf, this.linesWin);
        bf.putLong(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

