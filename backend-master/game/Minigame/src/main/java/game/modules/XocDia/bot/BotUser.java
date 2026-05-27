package game.modules.XocDia.bot;

import java.util.Random;

public class BotUser {
    public String display_name;
    public long id;
    public long money;
    public byte avatar;
    public byte level;
    public int numberPlayerCount;
    public int currentPlayerCount;

    public BotUser() {
        this.initData();
    }

    public BotUser(int id, String displayName) {
        this.id = id;
        this.display_name = displayName;
        this.initData();
    }

    public void randomMoneyInGame(long baseAmount) {
        Random rd = new Random();
        this.money = baseAmount + rd.nextInt(1000000);
    }

    public void initData() {
        Random rd = new Random();
        this.avatar = (byte) rd.nextInt(10);
        this.level = (byte) (rd.nextInt(50) + 1);
        this.numberPlayerCount = rd.nextInt(5) + 1;
        this.currentPlayerCount = 0;
        this.money = 100000 + rd.nextInt(10000000);
    }
}
