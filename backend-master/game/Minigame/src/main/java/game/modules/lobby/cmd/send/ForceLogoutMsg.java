package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

/**
 * Bitzero wire message sent to a game client that is being force-logged-out
 * because another device just logged into the same account (SUN-767).
 *
 * <p>Command id {@code 20200} (lobby send range is 20xxx). The game client
 * (Cocos {@code sunwinkr-client}) must handle this cmd by:
 * <ol>
 *   <li>Showing a modal: "Tài khoản của bạn đã được đăng nhập tại thiết bị khác".
 *   <li>Disconnecting the bitzero socket.
 *   <li>Clearing cached accessToken and returning to the login screen.
 * </ol>
 *
 * <p>Payload fields:
 * <ul>
 *   <li>{@code reason} — string, one of {@code DUPLICATE_LOGIN / ADMIN_KICK / SECURITY}.
 *   <li>{@code newLoginIp} — IP address of the new session that kicked this one.
 *   <li>{@code newLoginTime} — epoch millis of the new login.
 * </ul>
 */
public class ForceLogoutMsg extends BaseMsgEx {
    public String reason;
    public String newLoginIp;
    public long newLoginTime;

    public ForceLogoutMsg() {
        super(20200);
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
