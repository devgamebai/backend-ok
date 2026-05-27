package game.modules.XocDia.model.bet;

import java.sql.Timestamp;

public class BetItem {
    public long uId;
    public long money;
    public Timestamp time;
    public String userName;
    public boolean isBot;

    public BetItem(long uId, long money, String userName, boolean isBot) {
        this.uId = uId;
        this.money = money;
        this.userName = userName;
        this.isBot = isBot;
        this.time = new Timestamp(System.currentTimeMillis());
    }
}
