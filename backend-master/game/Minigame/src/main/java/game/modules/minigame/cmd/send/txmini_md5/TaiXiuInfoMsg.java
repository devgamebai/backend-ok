/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class TaiXiuInfoMsg
extends BaseMsgEx {
    public short gameId;
    public short moneyType;
    public long referenceId;
    public short remainTime;
    public boolean bettingState;
    public long potTai;
    public long potXiu;
    public long myBetTai;
    public long myBetXiu;
    public short dice1 = 0;
    public short dice2 = 0;
    public short dice3 = 0;
    public short remainTimeRutLoc = 0;
    public String md5="";
    public String before_md5="";

    public TaiXiuInfoMsg() {
        super(MiniGameCMD.TXMINI_MD5_INFO);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.gameId);
        buffer.putShort(this.moneyType);
        buffer.putLong(this.referenceId);
        buffer.putShort(this.remainTime);
        this.putBoolean(buffer, Boolean.valueOf(this.bettingState));
        buffer.putLong(this.potTai);
        buffer.putLong(this.potXiu);
        buffer.putLong(this.myBetTai);
        buffer.putLong(this.myBetXiu);
        buffer.putShort(this.dice1);
        buffer.putShort(this.dice2);
        buffer.putShort(this.dice3);
        buffer.putShort(this.remainTimeRutLoc);
        putStr(buffer, md5);
        putStr(buffer, before_md5);
        return this.packBuffer(buffer);
    }
}

