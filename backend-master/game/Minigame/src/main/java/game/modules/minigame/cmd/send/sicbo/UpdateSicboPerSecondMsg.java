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
public class UpdateSicboPerSecondMsg extends BaseMsgEx {
    public String msg;
    public short remainTime;
    public boolean bettingState;
    public long potTai;
    public long potXiu;
    public short numBetTai;
    public short numBetXiu;
    public long totalPlayer;

    public UpdateSicboPerSecondMsg() {
        super(28112);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        this.putStr(buffer, this.msg != null ? this.msg : "");
        buffer.putShort(this.remainTime);
        this.putBoolean(buffer, Boolean.valueOf(this.bettingState));
        buffer.putLong(this.potTai);
        buffer.putLong(this.potXiu);
        buffer.putShort(this.numBetTai);
        buffer.putShort(this.numBetXiu);
        buffer.putLong(this.totalPlayer);
        return this.packBuffer(buffer);
    }
}
