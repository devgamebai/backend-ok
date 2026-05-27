/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import java.nio.ByteBuffer;
import java.util.List;

public class GameRoomConfigMsg
extends BaseMsg {
    public GameRoomConfigMsg() {
        super((short)3003);
    }

    public byte[] createData() {
        GameRoomSetting setting;
        int i;
        ByteBuffer bf = this.makeBuffer();
        List<GameRoomSetting> gameRoomSettingList = GameRoomManager.instance().getRoomConfigList();
        int size = gameRoomSettingList.size();
        bf.putShort((short)size);
        for (i = 0; i < size; ++i) {
            setting = gameRoomSettingList.get(i);
            bf.putInt(setting.maxUserPerRoom);
            bf.put((byte)setting.moneyType);
            bf.putLong(setting.moneyBet);
            if (setting.requiredMoney != 0) {
                bf.putLong(setting.requiredMoney);
            } else if (GameUtils.gameName.equalsIgnoreCase("Poker")) {
                bf.putLong(40 * setting.moneyBet);
            } else if (GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                bf.putLong(5 * setting.moneyBet);
            }
            bf.putInt(GameRoomManager.instance().getUserCount(setting));
        }
        for (i = 0; i < size; ++i) {
            setting = gameRoomSettingList.get(i);
            bf.put((byte)setting.rule);
        }
        return this.packBuffer(bf);
    }
}

