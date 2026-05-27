/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BauCuaInfoMsg
extends BaseMsgEx {
    public long referenceId;
    public byte remainTime;
    public boolean bettingState;
    public String potData;
    public String betData;
    public String lichSuPhien;
    public byte dice1;
    public byte dice2;
    public byte dice3;
    public byte xPot;
    public byte xValue;
    public byte room;

    public BauCuaInfoMsg() {
        super(5005);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.referenceId);
        bf.put(this.remainTime);
        this.putBoolean(bf, Boolean.valueOf(this.bettingState));
        this.putStr(bf, this.potData);
        this.putStr(bf, this.betData);
        this.putStr(bf, this.lichSuPhien);
        bf.put(this.dice1);
        bf.put(this.dice2);
        bf.put(this.dice3);
        bf.put(this.xPot);
        bf.put(this.xValue);
        bf.put(this.room);
        return this.packBuffer(bf);
    }
}

