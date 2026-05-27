package game.queue;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

/**
 * Bitzero wire message sent to a card-game client that is being
 * force-logged-out because another device just logged into the same
 * account (SUN-816 / SUN-767).
 *
 * <p>Shared by every CardCoreLib-based game server (poker, tlmn, sam, binh,
 * lieng, baicao, bacay, caro, cotuong, coup, pokertour, xizach, xocdia).
 *
 * <p>Command id {@code 20200}. Payload layout matches the Minigame and slot
 * variants so the Cocos client handler can be implemented once:
 * <pre>
 *   str  reason      // "DUPLICATE_LOGIN" / "ADMIN_KICK" / "SECURITY"
 *   str  newLoginIp  // may be empty
 *   long newLoginTime
 * </pre>
 */
public class ForceLogoutMsg extends BaseMsg {
    public String reason;
    public String newLoginIp;
    public long newLoginTime;

    public ForceLogoutMsg() {
        super((short) 20200);
    }

    @Override
    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.reason != null ? this.reason : "");
        this.putStr(bf, this.newLoginIp != null ? this.newLoginIp : "");
        bf.putLong(this.newLoginTime);
        return this.packBuffer(bf);
    }
}
