package game.fishing.server.Object;

import bitzero.server.BitZeroServer;
import game.fishing.server.FishingGame;
import game.fishing.server.services.FishingGameServiceImpl;

import java.text.DecimalFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Fish {
    private FishData m_data;
    private boolean isDead = false;
    private long index = 0;
    private ScheduledFuture<?> loopTask;
    private int fishHP = 0; // HP tương ứng 6 loại đạn
    private int score = 0;
    private double percentScore = 1;
    private FishingGame game = null;
    private volatile int fishLifeTime = 0;
    private volatile int fishTimer = 0;

    public FishingGame getGame() {
        return game;
    }

    public void setGame(FishingGame game) {
        this.game = game;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Fish() {
    }

    public void setM_data(FishData m_data) {
        this.m_data = m_data;
        this.index = m_data.nFishKey;
        int hp = 0;
        int score = 0;

        int[][] nFishMultiple = this.game.getnFishMultiple();
        if (nFishMultiple[m_data.nFishType][0] == nFishMultiple[m_data.nFishType][1])
            score = nFishMultiple[m_data.nFishType][0];
        else
            score = MyUtils.getInstances().randomWithRange(nFishMultiple[m_data.nFishType][0], nFishMultiple[m_data.nFishType][1]);

        int gameDifficult = this.game.getGameDifficult();
        if (m_data.nFishType > FishType.CaVoi.getType()) {
            hp = score + gameDifficult * 5;
            percentScore = 1;
        } else {
//            hp = score + gameDifficult + MyUtils.getInstances().randomWithRange(0, FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_MAX_HP_SMALL_FISH);
            hp = score + gameDifficult;
            percentScore = FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_SCORE_SMALL_FISH;
            if (m_data.nFishState != FishState.Normal.getState())
                hp += m_data.nFishState;
        }

        if (m_data.nFishType == FishType.KhoBau.getType())
            hp = score;

        setScore(score);
        setFishHP(hp);

        if (!m_data.isFishGroup)
            fishLifeTime = getTotalLifeTime();
        else
            fishLifeTime = 55;

        this.startLoopFish();
    }

    public void setFishHP(int fishHP) {
        this.fishHP = fishHP;

    }

    public double getScore() {
        double scr = score * percentScore;
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.valueOf(df.format(scr));
    }

    public void setScore(int score) {
        this.score = score;
    }

    private void startLoopFish() {
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    loop();
                } catch (Exception e) {
                    //LogicFishingLogger.getInstance().getLogger().error("Fish", e);
                    onDestroy();
                    game.removeFish(index);
                }
            }
        };
        loopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
    }

    private void loop() {
        if (this.isDead)
            return;
        if (this.fishTimer >= this.fishLifeTime) {
            this.onDestroy();
            game.removeFish(index);
        }
        this.fishTimer++;

    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public FishData getM_data() {
        return m_data;
    }

    public void onDestroy() {
        this.setDead(true);
        this.setFishHP(0);
        if (loopTask != null)
            loopTask.cancel(true);
    }

    public void stayFish(int time) {
        loopTask.cancel(true);
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    loop();
                } catch (Exception e) {
                    //LogicFishingLogger.getInstance().getLogger().error("Fish", e);
                    onDestroy();
                    game.removeFish(index);
                }
            }
        };
        loopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, time, 1, TimeUnit.SECONDS);
    }

    public boolean isDeadAfterBeShooted(FishingPlayer playerShooted, int nMultipleIndex, int specialGun) {

        if (this.m_data.nFishType == FishType.KhoBau.getType() && playerShooted.getNumOfShootToReciveSupply() < FishingGameServiceImpl.getInstance().GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY)
            return false;

        if (this.m_data.nFishType == FishType.JackPot.getType()) {
            if (playerShooted.getId() == FishingGameServiceImpl.getInstance().getUserIDGame()) {
                if (game.getTypeOfGame() == 1) {
                    FishingGameServiceImpl.getInstance().setNormalHPJackpot(0);
                    this.fishHP = 0;
                } else if (game.getTypeOfGame() == 2) {
                    FishingGameServiceImpl.getInstance().setVipJackpotHP(0);
                    this.fishHP = 0;
                }
            } else {
                if (game.getTypeOfGame() == 1) {
                    FishingGameServiceImpl.getInstance().updateNormalHPJackpot(game.getnMultipleValue()[nMultipleIndex]);
                    this.fishHP = FishingGameServiceImpl.getInstance().getNormalJackpotHP();
                } else if (game.getTypeOfGame() == 2) {
                    FishingGameServiceImpl.getInstance().updateVipHPJackpot(game.getnMultipleValue()[nMultipleIndex]);
                    this.fishHP = FishingGameServiceImpl.getInstance().getVipJackpotHP();
                }
            }
        } else {
            if (specialGun < BulletEffect.Other.getType()) {
                int power = Math.abs(specialGun) * FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN;
                this.fishHP -= power;
            } else
                this.fishHP--;
        }
        return (this.fishHP <= 0);
    }

    private int getTotalLifeTime() {
        int totalTime = 0;
        for (TagBezierPoint tbp : m_data.TBzierPoint) {
            totalTime += tbp.Time;
        }
        return totalTime / 1000;
    }

    @Override
    public String toString() {
        return "Fish{" +
                "index=" + index +
                ", type=" + m_data.nFishType +
                ", score=" + score +
                '}';
    }
}

