/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateJackpotMsg
extends BaseMsgEx {
    public long potMiniPoker100 = 0L;
    public long potMiniPoker1000 = 0L;
    public long potMiniPoker10000 = 0L;
    public long potPokeGo100 = 0L;
    public long potPokeGo1000 = 0L;
    public long potPokeGo10000 = 0L;
    public long potKhoBau100 = 0L;
    public long potKhoBau1000 = 0L;
    public long potKhoBau10000 = 0L;
    public long potNDV100 = 0L;
    public long potNDV1000 = 0L;
    public long potNDV10000 = 0L;
    public long potAvengers100 = 0L;
    public long potAvengers1000 = 0L;
    public long potAvengers10000 = 0L;
    public long vqv100 = 0L;
    public long vqv1000 = 0L;
    public long vqv10000 = 0L;

    public UpdateJackpotMsg() {
        super(20101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.potMiniPoker100);
        this.putLong(bf, this.potMiniPoker1000);
        this.putLong(bf, this.potMiniPoker10000);
        this.putLong(bf, this.potPokeGo100);
        this.putLong(bf, this.potPokeGo1000);
        this.putLong(bf, this.potPokeGo10000);
        this.putLong(bf, this.potKhoBau100);
        this.putLong(bf, this.potKhoBau1000);
        this.putLong(bf, this.potKhoBau10000);
        this.putLong(bf, this.potNDV100);
        this.putLong(bf, this.potNDV1000);
        this.putLong(bf, this.potNDV10000);
        this.putLong(bf, this.potAvengers100);
        this.putLong(bf, this.potAvengers1000);
        this.putLong(bf, this.potAvengers10000);
        this.putLong(bf, this.vqv100);
        this.putLong(bf, this.vqv1000);
        this.putLong(bf, this.vqv10000);
        return this.packBuffer(bf);
    }
}

