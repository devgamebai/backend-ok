package game.fishing.server.Object;
import bitzero.util.common.business.Debug;
import game.fishing.server.FishingGame;
import game.fishing.server.GamePlayer;
import game.fishing.server.cmd.ResponseFishingSupplyTip;
import game.fishing.server.services.FishingGameServiceImpl;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class FishingPlayer extends GamePlayer {
    private int wTableID;
    private int gunID;
    private FishingGame game;
    private int[] totalBullets = new int[6];
    private long totalMoneyFire = 0;
    private long totalMoneyWon = 0;
    private int[] totalFishesCatch = new int[FishType.Max.getType()];
    private long totalMoneyFishCatch = 0;
    private int totalFeeInGame = 0;
    private int jackpotMoney = 0;
    private int gameSessionID = 0;

    public long getTimeJoinGame(){
        return this.timeJoinRoom;
    }
    public void setTimeJoinGame(long t){
        this.timeJoinRoom = t;
    }
    public int getGameSessionID() {
        return gameSessionID;
    }

    private boolean hasSpecialGun = false;

    private final ConcurrentHashMap<Integer, Bullet> listBullet = new ConcurrentHashMap<>();

    private int numOfShootToReciveSupply = 0;


    public boolean isHasSpecialGun() {
        return hasSpecialGun;
    }

    public int getJackpotMoney() {
        return jackpotMoney;
    }

    public void setJackpotMoney(int jackpotMoney) {
        this.jackpotMoney = jackpotMoney;
    }

    public void setHasSpecialGun(boolean hasSpecialGun) {
        this.hasSpecialGun = hasSpecialGun;
    }

    public void addPlayerGameFee(int fee) {
        this.totalFeeInGame += fee;
    }

    public int getTotalFeeInGame() {
        return totalFeeInGame;
    }

    public void setTotalFeeInGame(int totalFeeInGame) {
        this.totalFeeInGame = totalFeeInGame;
    }

    public void setGunID(int gunID) {
        this.gunID = gunID;
    }

    public int getwChairID() {
        return this.chair;
    }


    public void setwTableID(int wTableID) {
        this.wTableID = wTableID;
    }

    public void fire(Bullet bullet) {

        listBullet.put(bullet.getIndex(), bullet);
        totalBullets[bullet.getMutipleIndex()]++;
        totalMoneyFire += game.getnMultipleValue()[bullet.getMutipleIndex()];

        numOfShootToReciveSupply++;
        //TODO set lai pos cho tung user

        if (numOfShootToReciveSupply > 0 && (numOfShootToReciveSupply % FishingGameServiceImpl.getInstance().GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY) == 0) {
            TagBezierPoint[] arr = new TagBezierPoint[0];
            game.genSpecialFishWithTypeAndPos(FishType.KhoBau.getType(), arr);

            ResponseFishingSupplyTip res = new ResponseFishingSupplyTip();
            res.wChairID = this.chair;
            game.broadcastGame(res);
        }

    }

    public boolean removeBulletByID(int bulletID) {
        if (listBullet.remove(bulletID) != null) {
            return true;
        }
        Debug.warn(this.getId() + " Fire fail bullet invalid " + bulletID);
        return false;
    }

    public Bullet getBulletByID(int bulletID) {
        return listBullet.get(bulletID);
    }

    public FishingGame getGame() {
        return game;
    }

    public void setGame(FishingGame game) {
        this.game = game;
        if (!game.isTrialGame())
            this.gameSessionID = game.getId();
    }

    public int getNumOfShootToReciveSupply() {
        return numOfShootToReciveSupply;
    }

    public void setNumOfShootToReciveSupply(int numOfShootToReciveSupply) {
        this.numOfShootToReciveSupply = numOfShootToReciveSupply;
    }

    public void updateFishCatch(int fishType, long fishMoney) {
        totalFishesCatch[fishType]++;
        totalMoneyFishCatch += fishMoney;
    }

    public int[] getTotalBullets() {
        return totalBullets;
    }

    public long getTotalMoneyFire() {
        return totalMoneyFire;
    }

    public int[] getTotalFishesCatch() {
        return totalFishesCatch;
    }

    public long getTotalMoneyFishCatch() {
        return totalMoneyFishCatch;
    }

    public long getGoldWin() {
        return totalMoneyFishCatch - totalMoneyFire;
    }

    public long getTotalMoneyWon() {
        return totalMoneyWon;
    }

    public void addTotalWon(long money) {
        this.totalMoneyWon += money;
    }

    @Override
    public String toString() {
        return "{" +
                "\"dwUserID\":" + id +
                ",\"szNickName\":\"" + name + "\"" +
                ",\"wChairID\":" + chair +
                ",\"enterTime\":" + timeJoinRoom +
                ",\"lScore\":" + chips +
                ",\"wTableID\":" + wTableID +
                ",\"gunID\":" + gunID +
                ",\"totalMoneyFire\":" + totalMoneyFire +
                ",\"totalBullets\":" + Arrays.toString(totalBullets) +
                ",\"totalMoneyFishCatch\":" + totalMoneyFishCatch +
                ",\"totalFishesCatch\":" + Arrays.toString(totalFishesCatch) +
                '}';
    }
}
