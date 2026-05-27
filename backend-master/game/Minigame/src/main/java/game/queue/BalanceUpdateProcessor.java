package game.queue;

import bitzero.util.ExtensionUtility;
import bitzero.server.entities.User;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.NotiGameMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import game.modules.minigame.cmd.send.UpdateUserInfoMsg;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Consumes NotiGameMessage from queue_action_minigame.
 * Pushes updated balance to connected game clients in real-time.
 */
public class BalanceUpdateProcessor implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("game");

    public Boolean execute(Param<byte[]> param) {
        try {
            byte[] body = (byte[]) param.get();
            NotiGameMessage msg = (NotiGameMessage) BaseMessage.fromBytes(body);

            if (msg.nicknames == null || msg.nicknames.isEmpty()) {
                return true;
            }

            IMap<String, UserCacheModel> userMap = HazelcastClientFactory.getInstance().getMap("users");

            for (String nickname : msg.nicknames) {
                try {
                    List<User> users = ExtensionUtility.getExtension().getApi().getUserByName(nickname);
                    if (users == null || users.isEmpty()) {
                        continue; // user not connected to this game server
                    }

                    UserCacheModel cache = userMap.get(nickname);
                    if (cache == null) {
                        continue;
                    }

                    UpdateUserInfoMsg update = new UpdateUserInfoMsg();
                    update.currentMoney = cache.getVin();
                    update.moneyType = 1; // vin
                    ExtensionUtility.getExtension().send(update, users.get(0));

                    logger.info("Pushed balance update to " + nickname + " vin=" + cache.getVin());
                } catch (Exception userErr) {
                    logger.warn("Balance push failed for " + nickname + ": " + userErr.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("BalanceUpdateProcessor error", e);
            return false;
        }
    }
}
