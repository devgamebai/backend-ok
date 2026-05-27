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
import java.util.LinkedList;
import java.util.List;

public class SendRoomList
extends BaseMsg {
    public List<GameRoom> roomList = new LinkedList<GameRoom>();

    public SendRoomList() {
        super((short)3014);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        int size = this.roomList.size();
        long jackpot = 0;
        bf.putShort((short)size);
        for (int i = 0; i < size; ++i) {
            GameRoom room = this.roomList.get(i);
            bf.putInt(room.getId());
            bf.put((byte)room.getUserCount());
            bf.put((byte)room.setting.limitPlayer);
            bf.putInt(room.setting.maxUserPerRoom);
            bf.put((byte)room.setting.moneyType);
            bf.putInt((int)room.setting.moneyBet);
            // Always write requiredMoney (4 bytes) for fixed-layout parsing
            if (room.setting.requiredMoney != 0) {
                bf.putInt((int)room.setting.requiredMoney);
            } else if (GameUtils.gameName.equalsIgnoreCase("Poker")) {
                bf.putInt((int)(40 * room.setting.moneyBet));
            } else if (GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                bf.putInt((int)(5 * room.setting.moneyBet));
            } else {
                bf.putInt(0);
            }
            bf.put((byte)room.setting.rule);
            this.putStr(bf, room.setting.roomName);
            if (room.setting.password.length() > 0) {
                this.putBoolean(bf, Boolean.valueOf(true));
            } else {
                this.putBoolean(bf, Boolean.valueOf(false));
            }
            long fund = XocDiaGameUtils.getFundByName(room.setting.roomName, room.getId(), room.setting.rule);
            bf.putLong(fund);
            jackpot += fund;
        }
        // Jackpot — FE reads this as the last long after the room list
        // Use total boss funds across all rooms, or HuVang if active
        try {
            if (GameUtils.isHuVang) {
                game.modules.gameRoom.entities.ThongTinHuVang huVang = game.modules.gameRoom.entities.ThongTinHuVang.instance();
                if (huVang != null && huVang.dangChayHu()) {
                    long huVangAmount = huVang.getGoldAmount(GameUtils.gameName);
                    if (huVangAmount > 0) jackpot = huVangAmount;
                }
            }
        } catch (Exception e) {}
        bf.putLong(jackpot);
        return this.packBuffer(bf);
    }
}

