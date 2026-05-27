/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaGameUtils;
import java.nio.ByteBuffer;

public class SendGameRoomInfo
extends BaseMsg {
    public static final byte ERROR_ROOM_UNEXIST = 1;
    public GameRoom room = null;

    public SendGameRoomInfo() {
        super((short)3016);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        if (this.room != null) {
            bf.putInt(this.room.getId());
            bf.put((byte)this.room.getUserCount());
            bf.put((byte)this.room.setting.limitPlayer);
            bf.putInt(this.room.setting.maxUserPerRoom);
            bf.put((byte)this.room.setting.moneyType);
            bf.putInt((int)this.room.setting.moneyBet);
            if (this.room.setting.requiredMoney != 0) {
                bf.putInt((int)this.room.setting.requiredMoney);
            } else if (GameUtils.gameName.equalsIgnoreCase("Poker")) {
                bf.putInt((int)(40 * this.room.setting.moneyBet));
            } else if (GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                bf.putInt((int)(5 * this.room.setting.moneyBet));
            }
            bf.put((byte)this.room.setting.rule);
            this.putStr(bf, this.room.setting.roomName);
            if (this.room.setting.password.length() > 0) {
                this.putBoolean(bf, Boolean.valueOf(true));
            } else {
                this.putBoolean(bf, Boolean.valueOf(false));
            }
            bf.putLong(XocDiaGameUtils.getFundByName(this.room.setting.roomName, this.room.getId(), this.room.setting.rule));
        }
        return this.packBuffer(bf);
    }
}

