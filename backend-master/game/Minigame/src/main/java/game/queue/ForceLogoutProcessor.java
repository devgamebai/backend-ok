package game.queue;

import bitzero.server.entities.User;
import bitzero.util.ExtensionUtility;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.ForceLogoutMessage;
import game.modules.lobby.cmd.send.ForceLogoutMsg;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Consumes {@link ForceLogoutMessage} from {@code queue_force_logout} and
 * disconnects the target player's bitzero session(s) on this game server
 * (SUN-767).
 *
 * <p>Flow on receive:
 * <ol>
 *   <li>Find all bitzero sessions for the nickname via
 *       {@code getApi().getUserByName(nickname)}.
 *   <li>Send a {@link ForceLogoutMsg} so the game client can show the
 *       "kicked by new login" modal before the socket drops.
 *   <li>Disconnect each session so subsequent traffic is rejected.
 * </ol>
 *
 * <p>If the player is not currently connected to this game server, there's
 * nothing to do — their next HTTP API call will fail validation
 * ({@code cacheToken} no longer contains the old accessToken).
 */
public class ForceLogoutProcessor implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("game");

    @Override
    public Boolean execute(Param<byte[]> param) {
        try {
            byte[] body = (byte[]) param.get();
            ForceLogoutMessage msg = (ForceLogoutMessage) BaseMessage.fromBytes(body);
            if (msg == null || msg.nickname == null || msg.nickname.isEmpty()) {
                return true;
            }

            List<User> users = ExtensionUtility.getExtension().getApi().getUserByName(msg.nickname);
            if (users == null || users.isEmpty()) {
                logger.info("ForceLogoutProcessor: no active session for " + msg.nickname
                        + " reason=" + msg.reason + " (nothing to kick on this game server)");
                return true;
            }

            ForceLogoutMsg wire = new ForceLogoutMsg();
            wire.reason = msg.reason != null ? msg.reason : "DUPLICATE_LOGIN";
            wire.newLoginIp = msg.newLoginIp;
            wire.newLoginTime = msg.newLoginTime;

            for (User u : users) {
                try {
                    ExtensionUtility.getExtension().send(wire, u);
                } catch (Exception sendErr) {
                    logger.warn("ForceLogoutProcessor: send wire msg failed nick=" + msg.nickname
                            + " err=" + sendErr.getMessage());
                }
                try {
                    // Disconnect the old session. The client should handle the
                    // wire msg first and show the modal; the disconnect flushes
                    // any pending frames and closes the socket.
                    bitzero.server.BitZeroServer.getInstance().getAPIManager().getBzApi()
                            .disconnectUser(u);
                } catch (Exception discErr) {
                    logger.warn("ForceLogoutProcessor: disconnect failed nick=" + msg.nickname
                            + " err=" + discErr.getMessage());
                }
            }

            logger.info("ForceLogoutProcessor: kicked " + users.size() + " session(s) for "
                    + msg.nickname + " reason=" + msg.reason);
            return true;
        } catch (Exception e) {
            logger.error("ForceLogoutProcessor error", e);
            return false;
        }
    }
}
