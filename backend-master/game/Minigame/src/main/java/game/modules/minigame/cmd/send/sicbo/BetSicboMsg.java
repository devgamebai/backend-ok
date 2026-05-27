/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.sicbo;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

/**
 * SICBO — handler 28000.
 *
 * Renamed 2026-04-12 from TaiXiuSicbo* -> Sicbo* so stack traces, log greps,
 * and bug reports unambiguously distinguish Sicbo from TaiXiu (handler 2000).
 * See docs/TAIXIU-SICBO-GAME-ARCHITECTURE.md for the architecture rules,
 * especially the hardcoded-vs-config-driven round timer pitfall that caused
 * the Apr 8-12 instability window.
 */
public class BetSicboMsg
extends BaseMsgEx {
    public long currentMoney;
    public long referenceId;
    public long betValue;
    public short moneyType;
    public String betSide;
    public short inputTime;
    public long pot;

    public BetSicboMsg() {
        super(28110);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        buffer.putLong(this.referenceId);
        buffer.putLong(this.betValue);
        buffer.putShort(this.moneyType);
        buffer.putShort(this.inputTime);
        this.putStr(buffer, this.betSide);
        buffer.putLong(this.pot);
        return this.packBuffer(buffer);
    }
}

