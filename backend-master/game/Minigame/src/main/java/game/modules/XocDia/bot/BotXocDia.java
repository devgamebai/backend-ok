package game.modules.XocDia.bot;

import game.modules.XocDia.model.XocDiaBetModel;
import bitzero.util.common.business.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BotXocDia {
    private java.util.List<BotUser> listBot;
    public ArrayList<ArrayList<Long>> listBet;
    public ArrayList<ArrayList<Integer>> indexBet;
    public ArrayList<ArrayList<Integer>> distributionBet;
    public int timeStartBetFun;
    public static int BOT_IN_ROOM = 20;
    public static int MIX = 5;
    public Map<String, BotUser> listBotInRoomCoDinh;
    public Map<String, BotUser> listBotInRoomInOut;

    public BotXocDia() {
        this.listBot = new ArrayList<BotUser>();
        this.listBet = new ArrayList<ArrayList<Long>>();
        this.indexBet = new ArrayList<ArrayList<Integer>>();
        this.distributionBet = new ArrayList<ArrayList<Integer>>();
        this.timeStartBetFun = 0;
        this.listBotInRoomCoDinh = new HashMap<String, BotUser>();
        this.listBotInRoomInOut = new HashMap<String, BotUser>();

        // Initialize default bots
        for (int i = 0; i < BOT_IN_ROOM; i++) {
            BotUser bot = new BotUser(i + 1000, "player_" + (i + 1));
            this.listBot.add(bot);
            this.listBotInRoomCoDinh.put(bot.display_name, bot);
        }
    }

    public void setupBetFun() {
        this.listBet.clear();
        this.indexBet.clear();
        this.distributionBet.clear();
        Random rd = new Random();
        this.timeStartBetFun = rd.nextInt(15) + 5; // start betting between 5-20 seconds into round
    }

    public void betFun() {
        try {
            Random rd = new Random();
            XocDiaBetModel betModel = XocDiaBetModel.getInstance();
            for (BotUser bot : this.listBot) {
                if (rd.nextInt(100) < 30) { // 30% chance each bot bets
                    byte door = (byte) rd.nextInt(4); // doors 0-3 for chan/le/3-1/1-3
                    long betAmount = (rd.nextInt(10) + 1) * 1000L;
                    betModel.bet(bot.id, bot.display_name, door, betAmount, true);
                }
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"BotXocDia betFun error", e.getMessage()});
        }
    }

    public void sendAllBotInOutJoinRoom() {
        // Notify room about bot joins (visual only)
    }

    public void sendAllBotInOutLeaveRoom() {
        // Notify room about bot leaves (visual only)
    }
}
