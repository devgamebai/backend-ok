package game.modules.XocDia.msg.out;

import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;

/**
 * Server push: new player joined room (cmd 8003)
 */
public class NewPlayerMsg extends BaseMsgEx {
    public int playerCount;
    public String playerName;

    public NewPlayerMsg() {
        super(GameXocDiaCmdDefine.NEW_PLAYER_JOIN_ROOM);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.playerCount);
        if (this.playerName != null) {
            this.putStr(bf, this.playerName);
        } else {
            this.putStr(bf, "");
        }
        return this.packBuffer(bf);
    }
}
