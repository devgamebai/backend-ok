package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Response: bet acknowledgment (cmd 8010)
 * Sent back to the bettor to confirm their bet
 */
public class BetResponseMsg extends BaseMsgEx {
    public long currentMoney;   // player's balance after bet
    public byte door;           // which door was bet on
    public long betAmount;      // how much was bet

    public BetResponseMsg() {
        super(GameXocDiaCmdDefine.BET);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        bf.put(this.door);
        bf.putLong(this.betAmount);
        return this.packBuffer(bf);
    }
}
