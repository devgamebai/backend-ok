/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.galaxy;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultGalaxyMsg
extends BaseMsgEx {
    public byte result;
    public String matrix = "";
    public String linesWin = "";
    public long prize;
    public long currentMoney;

    public ResultGalaxyMsg() {
        super(8001);
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

