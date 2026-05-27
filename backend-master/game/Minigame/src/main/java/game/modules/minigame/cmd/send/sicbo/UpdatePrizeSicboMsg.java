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
public class UpdatePrizeSicboMsg
extends BaseMsgEx {
    public short moneyType;
    public long totalMoney;
    public long currentMoney;
    public String betSide;

    public UpdatePrizeSicboMsg() {
        super(28114);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.moneyType);
        buffer.putLong(this.totalMoney);
        buffer.putLong(this.currentMoney);
        this.putStr(buffer, this.betSide);
        return this.packBuffer(buffer);
    }
}

