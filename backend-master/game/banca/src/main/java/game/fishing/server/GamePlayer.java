package game.fishing.server;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import org.json.JSONObject;

public class GamePlayer {

    protected int chair;
    protected int id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String name;
    public boolean reqQuitRoom = false;
    public boolean isTrialGame = false;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public long timeJoinRoom = 0L;
    private volatile int playerStatus = 0;

    public long getChips() {
        if (!isTrialGame)
            this.chips = this.gameMoneyInfo.currentMoney;
        return this.chips;
    }

    public long getCurrentMoney(){
        return this.gameMoneyInfo.currentMoney;
    }

    public int getId() {
        return this.id;
    }

    public void setChips(long chips) {
        this.chips = chips;
    }

    protected long chips = 0;


    public void setPlayerStatus(int playerStatus) {
        this.playerStatus = playerStatus;
    }

    public int getPlayerStatus() {
        return this.playerStatus;
    }

    public User getUser() {
        return this.user;
    }

    public PlayerInfo getPlayerInfo() {
        return this.pInfo;
    }


    public void takeChair(User user, PlayerInfo pInfo, GameMoneyInfo moneyInfo, boolean isTrialGame) {
        this.user = user;
        this.id = pInfo.userId;
        this.pInfo = pInfo;
        this.gameMoneyInfo = moneyInfo;
        this.isTrialGame = isTrialGame;
        if (isTrialGame)
            this.chips = 1000000;
        else
            this.chips = moneyInfo.currentMoney;
        this.reqQuitRoom = false;
        this.name = pInfo.nickName;
        user.setProperty("user_chair", this.chair);
    }

    public boolean isPlaying() {
        return this.playerStatus == 3;
    }

    public boolean hasUser() {
        return this.playerStatus != 0;
    }


    public boolean checkMoneyCanPlay() {
        return this.gameMoneyInfo.moneyCheck();
    }

    public String toString() {
        try {
            JSONObject e = this.toJSONObject();
            return e != null ? e.toString() : "{}";
        } catch (Exception var2) {
            return "{}";
        }
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject e = new JSONObject();
            e.put("reqQuitRoom", this.reqQuitRoom);
            e.put("playerStatus", this.playerStatus);
            if (this.gameMoneyInfo != null) {
                e.put("gameMoneyInfo", this.gameMoneyInfo.toJSONObject());
            }

            return e;
        } catch (Exception var2) {
            return null;
        }
    }

}
