package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Server push: current session info (cmd 8004)
 * Sent when player joins and periodically during game
 */
public class InfoSessionMsg extends BaseMsgEx {
    public long referenceId;        // session/round ID
    public byte status;             // 0=BET, 1=DICE, 2=PAY
    public int timeRemaining;       // seconds remaining in current phase
    public long[] totalBets;        // total bets per door [6]
    public long fund;               // current jackpot fund
    public int playerCount;         // number of players in room
    public byte[] lastDiceResult;   // last round dice result (4 bytes), null if first round

    public InfoSessionMsg() {
        super(GameXocDiaCmdDefine.XOCDIAFULL_INFO_SESSION);
        this.totalBets = new long[6];
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.referenceId);
        bf.put(this.status);
        bf.putInt(this.timeRemaining);
        for (int i = 0; i < 6; i++) {
            bf.putLong(this.totalBets[i]);
        }
        bf.putLong(this.fund);
        bf.putInt(this.playerCount);
        // last dice result
        if (this.lastDiceResult != null && this.lastDiceResult.length == 4) {
            bf.put((byte) 1); // has result
            for (int i = 0; i < 4; i++) {
                bf.put(this.lastDiceResult[i]);
            }
        } else {
            bf.put((byte) 0); // no result
        }
        return this.packBuffer(bf);
    }
}
