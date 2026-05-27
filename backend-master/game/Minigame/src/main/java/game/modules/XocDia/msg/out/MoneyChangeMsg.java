package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Server push: money change after session (cmd 8015)
 * Sent to each player who won/lost after the round
 */
public class MoneyChangeMsg extends BaseMsgEx {
    public long currentMoney;   // player's current balance after settlement
    public long winMoney;       // amount won (0 if lost)
    public byte door;           // which door the win came from

    public MoneyChangeMsg() {
        super(GameXocDiaCmdDefine.MONEY_CHANGE_AFTER_SESSION);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        bf.putLong(this.winMoney);
        bf.put(this.door);
        return this.packBuffer(bf);
    }
}
