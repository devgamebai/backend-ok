/*
 * Decompiled with CFR 0.144.
 */
package game.modules.gameRoom.cmd.send;

import game.BaseMsgEx;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import java.nio.ByteBuffer;
import java.util.List;

public class SendGameRoomConfig
extends BaseMsgEx {
    public SendGameRoomConfig() {
        super(3003);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        List<GameRoomSetting> gameRoomSettingList = GameRoomManager.instance().roomSettingList;
        int size = gameRoomSettingList.size();
        bf.putShort((short)size);
        for (int i = 0; i < size; ++i) {
            GameRoomSetting setting = gameRoomSettingList.get(i);
            bf.put((byte)setting.maxUserPerRoom);
            bf.put((byte)setting.moneyType);
            bf.putLong(setting.moneyBet);
            bf.putLong(setting.requiredMoney);
            bf.putInt(GameRoomManager.instance().getUserCount(setting));
        }
        return this.packBuffer(bf);
    }
}

