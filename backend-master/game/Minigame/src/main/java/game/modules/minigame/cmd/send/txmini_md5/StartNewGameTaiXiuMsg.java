/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class StartNewGameTaiXiuMsg
        extends BaseMsgEx {
    public long referenceId;
    public short remainTimeRutLoc;
    public String md5 = null;

    public StartNewGameTaiXiuMsg() {
        super(MiniGameCMD.START_NEW_GAME_TXMINI_MD5);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.referenceId);
        buffer.putShort(this.remainTimeRutLoc);
        if (md5 != null)
            this.putStr(buffer, this.md5);
        return this.packBuffer(buffer);
    }
}

