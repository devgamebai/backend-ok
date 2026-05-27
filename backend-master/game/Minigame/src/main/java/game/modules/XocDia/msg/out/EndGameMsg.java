package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Server push: end game result (cmd 8005)
 * Sent when dice are rolled and result is determined
 */
public class EndGameMsg extends BaseMsgEx {
    public long referenceId;
    public byte[] diceResult;   // 4 dice values (each 0 or 1)
    public long[] totalBets;    // total bets per door [6]
    public long fund;

    public EndGameMsg() {
        super(GameXocDiaCmdDefine.XOCDIAFULL_END_GAME);
        this.totalBets = new long[6];
        this.diceResult = new byte[4];
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.referenceId);
        for (int i = 0; i < 4; i++) {
            bf.put(this.diceResult[i]);
        }
        for (int i = 0; i < 6; i++) {
            bf.putLong(this.totalBets[i]);
        }
        bf.putLong(this.fund);
        return this.packBuffer(bf);
    }
}
