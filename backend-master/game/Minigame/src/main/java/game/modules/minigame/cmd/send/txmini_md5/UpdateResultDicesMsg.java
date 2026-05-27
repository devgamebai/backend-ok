/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class UpdateResultDicesMsg
extends BaseMsgEx {
    public short result;
    public short dice1;
    public short dice2;
    public short dice3;
    public String before_md5=null;

    public UpdateResultDicesMsg() {
        super(MiniGameCMD.UPDATE_TXMINI_MD5_RESULT_DICES);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.result);
        buffer.putShort(this.dice1);
        buffer.putShort(this.dice2);
        buffer.putShort(this.dice3);
        if(before_md5!=null)
            this.putStr(buffer, before_md5);
        return this.packBuffer(buffer);
    }
}

