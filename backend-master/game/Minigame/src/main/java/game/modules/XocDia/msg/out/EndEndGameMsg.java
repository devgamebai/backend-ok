package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Server push: end of end game phase (cmd 8006)
 * Signals the start of a new round
 */
public class EndEndGameMsg extends BaseMsgEx {
    public long nextReferenceId;

    public EndEndGameMsg() {
        super(GameXocDiaCmdDefine.XOCDIAFULL_END_END_GAME);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.nextReferenceId);
        return this.packBuffer(bf);
    }
}
