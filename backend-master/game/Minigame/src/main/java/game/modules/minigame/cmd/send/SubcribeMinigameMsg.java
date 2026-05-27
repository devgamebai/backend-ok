/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SubcribeMinigameMsg
extends BaseMsgEx {
    public int gameId;
    public int moneyType;
    public long potValue;

    public SubcribeMinigameMsg() {
        super(2000);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.gameId);
        bf.putInt(this.moneyType);
        return this.packBuffer(bf);
    }
}

