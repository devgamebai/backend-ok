package game.fishing.server.services;/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 * TuanND FISHING
 */


import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import game.fishing.server.FishingGame;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class FishingGameServiceImpl {
    static FishingGameServiceImpl instance = new FishingGameServiceImpl();

    private long defaultUserIDJackpotBroken = -2;
    private int defaultPercentJackpotBroken = 1000;

    public int GAME_FISHING_TIME_CHANGE_SCENE = 210;
    public int GAME_FISHING_TIME_CHANGE_SCENE_TRIAL = 180;
    public int GAME_FISHING_MAX_FISH_EACH_PLAYER = 20;
    public int GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL = 30;
    public int GAME_FISHING_RATE_FEE = 2;
    public int GAME_FISHING_RATE_MONEY_TO_JACKPOT = 1;
    public int GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK = 2;
    public int GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY = 700;
    public int GAME_FISHING_SUPPLY_TIME = 60;
    public int GAME_FISHING_STAY_FISH_TIME = 5;
    public int GAME_FISHING_MAX_FISH_IN_GAME = 1000;
    public int GAME_FISHING_DEFAULT_NORMAL_JACKPOT = 1000000;
    public int GAME_FISHING_DEFAULT_VIP_JACKPOT = 3000000;
    public int GAME_FISHING_FEE_TO_KILL_FISH = 1;
    public int GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD = 20;
    public int GAME_FISHING_MAX_BIG_FISH_IN_MAP = 5;
    public int GAME_FISHING_MAX_EFFECT_FISH_IN_MAP = 3;
    public int GAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME = 1000000;
    public int GAME_FISHING_DEFAULT_GAMEBANK = 0;
    public int GAME_FISHING_DEFAULT_GAMEBANK_TRIAL = 100000000;
    public int GAME_FISHING_DEFAULT_BULLET_COOLDOWN = 200;
    public int GAME_FISHING_DEFAULT_BULLET_VELOCTITY = 15000;
    public int GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER = 15000;
    public int GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN = 200;
    public int GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN = 5;
    public int GAME_FISHING_DEFAULT_RATE_HP_JACKPOT = 3;
    public int GAME_FISHING_RATE_MAX_HP_SMALL_FISH = 5;
    public int GAME_FISHING_BULLET_TO_JACKPOT_BROKEN = 150;
    public double GAME_FISHING_RATE_SCORE_SMALL_FISH = 0.85;
    private long normalJackpotHP = 0;
    private long vipJackpotHP = 0;
    private long normalJackpot = 0;
    private long vipJackpot = 0;
    private MiniGameService mgService = new MiniGameServiceImpl();

    public FishingGameServiceImpl() {
        CacheServiceImpl cacheService = new CacheServiceImpl();
        try {
            normalJackpot = cacheService.getValueInt(Games.HAM_CA_MAP.getName() + "_vin_100");
            vipJackpot = cacheService.getValueInt(Games.HAM_CA_MAP.getName() + "_vin_1000");
            normalJackpotHP = mgService.getFund(Games.HAM_CA_MAP.getName() + "_vin_100");
            vipJackpotHP = mgService.getFund(Games.HAM_CA_MAP.getName() + "_vin_1000");
            mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_100", normalJackpotHP);
            mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_1000", vipJackpotHP);
        } catch (Exception e) {
            Debug.trace((Object) ("Fish jackpot exception: " + e.getMessage()));
        }
    }

    public void setUserIDGame(long userID) {
        this.defaultUserIDJackpotBroken = userID;
    }

    public void setPercent(int vl) {
        this.defaultPercentJackpotBroken = vl;
    }

    public long getUserIDGame() {
        return this.defaultUserIDJackpotBroken;
    }

    public int getPercent() {
        return this.defaultPercentJackpotBroken;
    }

    public int getGAME_FISHING_TIME_CHANGE_SCENE() {
        return GAME_FISHING_TIME_CHANGE_SCENE;
    }

    public void setGAME_FISHING_TIME_CHANGE_SCENE(int GAME_FISHING_TIME_CHANGE_SCENE) {
        this.GAME_FISHING_TIME_CHANGE_SCENE = GAME_FISHING_TIME_CHANGE_SCENE;
    }

    public int getGAME_FISHING_TIME_CHANGE_SCENE_TRIAL() {
        return GAME_FISHING_TIME_CHANGE_SCENE_TRIAL;
    }

    public void setGAME_FISHING_TIME_CHANGE_SCENE_TRIAL(int GAME_FISHING_TIME_CHANGE_SCENE_TRIAL) {
        this.GAME_FISHING_TIME_CHANGE_SCENE_TRIAL = GAME_FISHING_TIME_CHANGE_SCENE_TRIAL;
    }

    public int getGAME_FISHING_MAX_FISH_EACH_PLAYER() {
        return GAME_FISHING_MAX_FISH_EACH_PLAYER;
    }

    public void setGAME_FISHING_MAX_FISH_EACH_PLAYER(int GAME_FISHING_MAX_FISH_EACH_PLAYER) {
        this.GAME_FISHING_MAX_FISH_EACH_PLAYER = GAME_FISHING_MAX_FISH_EACH_PLAYER;
    }

    public int getGAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL() {
        return GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL;
    }

    public void setGAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL(int GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL) {
        this.GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL = GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL;
    }

    public int getGAME_FISHING_RATE_FEE() {
        return GAME_FISHING_RATE_FEE;
    }

    public void setGAME_FISHING_RATE_FEE(int GAME_FISHING_RATE_FEE) {
        this.GAME_FISHING_RATE_FEE = GAME_FISHING_RATE_FEE;
    }

    public int getGAME_FISHING_RATE_MONEY_TO_JACKPOT() {
        return GAME_FISHING_RATE_MONEY_TO_JACKPOT;
    }

    public void setGAME_FISHING_RATE_MONEY_TO_JACKPOT(int GAME_FISHING_RATE_MONEY_TO_JACKPOT) {
        this.GAME_FISHING_RATE_MONEY_TO_JACKPOT = GAME_FISHING_RATE_MONEY_TO_JACKPOT;
    }

    public int getGAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK() {
        return GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK;
    }

    public void setGAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK(int GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK) {
        this.GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK = GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK;
    }

    public int getGAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY() {
        return GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY;
    }

    public void setGAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY(int GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY) {
        this.GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY = GAME_FISHING_NUM_OF_SHOOT_TO_RECEIVE_SUPPLY;
    }

    public int getGAME_FISHING_SUPPLY_TIME() {
        return GAME_FISHING_SUPPLY_TIME;
    }

    public void setGAME_FISHING_SUPPLY_TIME(int GAME_FISHING_SUPPLY_TIME) {
        this.GAME_FISHING_SUPPLY_TIME = GAME_FISHING_SUPPLY_TIME;
    }

    public int getGAME_FISHING_STAY_FISH_TIME() {
        return GAME_FISHING_STAY_FISH_TIME;
    }

    public void setGAME_FISHING_STAY_FISH_TIME(int GAME_FISHING_STAY_FISH_TIME) {
        this.GAME_FISHING_STAY_FISH_TIME = GAME_FISHING_STAY_FISH_TIME;
    }

    public int getGAME_FISHING_MAX_FISH_IN_GAME() {
        return GAME_FISHING_MAX_FISH_IN_GAME;
    }

    public void setGAME_FISHING_MAX_FISH_IN_GAME(int GAME_FISHING_MAX_FISH_IN_GAME) {
        this.GAME_FISHING_MAX_FISH_IN_GAME = GAME_FISHING_MAX_FISH_IN_GAME;
    }

    public int getGAME_FISHING_DEFAULT_NORMAL_JACKPOT() {
        return GAME_FISHING_DEFAULT_NORMAL_JACKPOT;
    }

    public void setGAME_FISHING_DEFAULT_NORMAL_JACKPOT(int GAME_FISHING_DEFAULT_NORMAL_JACKPOT) {
        this.GAME_FISHING_DEFAULT_NORMAL_JACKPOT = GAME_FISHING_DEFAULT_NORMAL_JACKPOT;
    }

    public int getGAME_FISHING_DEFAULT_VIP_JACKPOT() {
        return GAME_FISHING_DEFAULT_VIP_JACKPOT;
    }

    public void setGAME_FISHING_DEFAULT_VIP_JACKPOT(int GAME_FISHING_DEFAULT_VIP_JACKPOT) {
        this.GAME_FISHING_DEFAULT_VIP_JACKPOT = GAME_FISHING_DEFAULT_VIP_JACKPOT;
    }

    public int getGAME_FISHING_FEE_TO_KILL_FISH() {
        return GAME_FISHING_FEE_TO_KILL_FISH;
    }

    public void setGAME_FISHING_FEE_TO_KILL_FISH(int GAME_FISHING_FEE_TO_KILL_FISH) {
        this.GAME_FISHING_FEE_TO_KILL_FISH = GAME_FISHING_FEE_TO_KILL_FISH;
    }

    public int getGAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD() {
        return GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD;
    }

    public void setGAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD(
            int GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD) {
        this.GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD = GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD;
    }

    public int getGAME_FISHING_MAX_BIG_FISH_IN_MAP() {
        return GAME_FISHING_MAX_BIG_FISH_IN_MAP;
    }

    public void setGAME_FISHING_MAX_BIG_FISH_IN_MAP(int GAME_FISHING_MAX_BIG_FISH_IN_MAP) {
        this.GAME_FISHING_MAX_BIG_FISH_IN_MAP = GAME_FISHING_MAX_BIG_FISH_IN_MAP;
    }

    public int getGAME_FISHING_MAX_EFFECT_FISH_IN_MAP() {
        return GAME_FISHING_MAX_EFFECT_FISH_IN_MAP;
    }

    public void setGAME_FISHING_MAX_EFFECT_FISH_IN_MAP(int GAME_FISHING_MAX_EFFECT_FISH_IN_MAP) {
        this.GAME_FISHING_MAX_EFFECT_FISH_IN_MAP = GAME_FISHING_MAX_EFFECT_FISH_IN_MAP;
    }

    public int getGAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME() {
        return GAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME;
    }

    public void setGAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME(int GAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME) {
        this.GAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME = GAME_FISHING_PLAYER_GOLD_IN_TRIAL_GAME;
    }

    public int getGAME_FISHING_DEFAULT_GAMEBANK() {
        return GAME_FISHING_DEFAULT_GAMEBANK;
    }

    public void setGAME_FISHING_DEFAULT_GAMEBANK(int GAME_FISHING_DEFAULT_GAMEBANK) {
        this.GAME_FISHING_DEFAULT_GAMEBANK = GAME_FISHING_DEFAULT_GAMEBANK;
    }

    public int getGAME_FISHING_DEFAULT_GAMEBANK_TRIAL() {
        return GAME_FISHING_DEFAULT_GAMEBANK_TRIAL;
    }

    public void setGAME_FISHING_DEFAULT_GAMEBANK_TRIAL(int GAME_FISHING_DEFAULT_GAMEBANK_TRIAL) {
        this.GAME_FISHING_DEFAULT_GAMEBANK_TRIAL = GAME_FISHING_DEFAULT_GAMEBANK_TRIAL;
    }

    public int getGAME_FISHING_DEFAULT_BULLET_COOLDOWN() {
        return GAME_FISHING_DEFAULT_BULLET_COOLDOWN;
    }

    public void setGAME_FISHING_DEFAULT_BULLET_COOLDOWN(int GAME_FISHING_DEFAULT_BULLET_COOLDOWN) {
        this.GAME_FISHING_DEFAULT_BULLET_COOLDOWN = GAME_FISHING_DEFAULT_BULLET_COOLDOWN;
    }

    public int getGAME_FISHING_DEFAULT_BULLET_VELOCTITY() {
        return GAME_FISHING_DEFAULT_BULLET_VELOCTITY;
    }

    public void setGAME_FISHING_DEFAULT_BULLET_VELOCTITY(int GAME_FISHING_DEFAULT_BULLET_VELOCTITY) {
        this.GAME_FISHING_DEFAULT_BULLET_VELOCTITY = GAME_FISHING_DEFAULT_BULLET_VELOCTITY;
    }

    public int getGAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER() {
        return GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER;
    }

    public void setGAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER(
            int GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER) {
        this.GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER = GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER;
    }

    public int getGAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN() {
        return GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN;
    }

    public void setGAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN(int GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN) {
        this.GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN = GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN;
    }

    public int getGAME_FISHING_RATE_POWER_OF_SPECIAL_GUN() {
        return GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN;
    }

    public void setGAME_FISHING_RATE_POWER_OF_SPECIAL_GUN(int GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN) {
        this.GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN = GAME_FISHING_RATE_POWER_OF_SPECIAL_GUN;
    }

    public int getGAME_FISHING_DEFAULT_RATE_HP_JACKPOT() {
        return GAME_FISHING_DEFAULT_RATE_HP_JACKPOT;
    }

    public void setGAME_FISHING_DEFAULT_RATE_HP_JACKPOT(int GAME_FISHING_DEFAULT_RATE_HP_JACKPOT) {
        this.GAME_FISHING_DEFAULT_RATE_HP_JACKPOT = GAME_FISHING_DEFAULT_RATE_HP_JACKPOT;
    }

    public void setGAME_FISHING_RATE_MAX_HP_SMALL_FISH(int GAME_FISHING_RATE_MAX_HP_SMALL_FISH) {
        this.GAME_FISHING_RATE_MAX_HP_SMALL_FISH = GAME_FISHING_RATE_MAX_HP_SMALL_FISH;
    }

    public void setGAME_FISHING_BULLET_TO_JACKPOT_BROKEN(int GAME_FISHING_BULLET_TO_JACKPOT_BROKEN) {
        this.GAME_FISHING_BULLET_TO_JACKPOT_BROKEN = GAME_FISHING_BULLET_TO_JACKPOT_BROKEN;
    }

    public void setGAME_FISHING_RATE_SCORE_SMALL_FISH(double GAME_FISHING_RATE_SCORE_SMALL_FISH) {
        this.GAME_FISHING_RATE_SCORE_SMALL_FISH = GAME_FISHING_RATE_SCORE_SMALL_FISH;
    }

    public static FishingGameServiceImpl getInstance() {
        return instance;
    }


    public boolean canGenJackpot(FishingGame game) {
        if (game == null || game.getTypeOfGame() < 1 || game.getTypeOfGame() > 2) return false;

        for (Object obj : GameRoomManager.instance().allGameRooms.values()) {
            GameRoom gRoom = (GameRoom) obj;
            FishingGame fGame = (FishingGame) gRoom.getGameServer();
            if (fGame == null || fGame.getTypeOfGame() != game.getTypeOfGame() || fGame.isBossing())
                continue;
            if (fGame.hasJackpot())
                return false;
        }

        return true;
    }


    public int getNormalJackpotHP() {
        return (int) this.normalJackpotHP;
    }


    public int getVipJackpotHP() {
        return (int) this.vipJackpotHP;
    }

    public void updateNormalFishJackpot(int value) {
        try {
            this.normalJackpot += value;
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpot, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateVipFishJackpot(int value) {
        try {
            this.vipJackpot += value;
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpot, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNormalFishJackpotBank(int value) {
        //jackpotModel.updateNormalFishJackpotBank(value);
    }

    public void updateVipFishJackpotBank(int value) {
        //jackpotModel.updateVipFishJackpotBank(value);
    }

    public void updateNormalHPJackpot(int hp) {
        try {
            this.normalJackpotHP += hp;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateVipHPJackpot(int hp) {
        try {
            this.vipJackpotHP += hp;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setNormalHPJackpot(int hp) {
        try {
            this.normalJackpotHP = hp;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVipJackpotHP(int hp) {
        try {
            this.vipJackpotHP = hp;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetNormalJackpot() {
        try {
            this.normalJackpot = GAME_FISHING_DEFAULT_NORMAL_JACKPOT;
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpot, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetVipJackpot() {
        try {
            this.vipJackpot = GAME_FISHING_DEFAULT_VIP_JACKPOT;
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpot, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetNormalJackpotHP() {
        try {
            this.normalJackpotHP = GAME_FISHING_DEFAULT_NORMAL_JACKPOT;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetVipJackpotHP() {
        try {
            this.vipJackpotHP = GAME_FISHING_DEFAULT_VIP_JACKPOT;
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMoneyNormalJackpot() {
        return (int) this.normalJackpot;
    }

    public int getMoneyVipJackpot() {
        return (int) this.vipJackpot;
    }

    public void saveFishingJackpot() {
        try {
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpot, false);
            this.mgService.savePot(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpot, false);
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_100", this.normalJackpotHP);
            this.mgService.saveFund(Games.HAM_CA_MAP.getName() + "_vin_1000", this.vipJackpotHP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}