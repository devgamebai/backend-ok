/*
 * Decompiled with CFR 0_116.
 */
package game.modules.gameRoom.entities;

import game.utils.GameUtils;

public class ThongTinThangLon {
    public String gameName = GameUtils.gameName;
    public int roomId;
    public int gameId;
    public String moneySessionId = "";
    public long MoneyAdd = 0;
    public long currentMoney = 0;
    public double rate = 0.0;
    public byte[] cards = new byte[0];
    public String nickName = "";
    public boolean noHu = false;
    public int moneyBet = 0;
    public String desc = "";

    public String toString() {
        return GameUtils.toJsonString(this);
    }
}

