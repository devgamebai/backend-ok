/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.BotService
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.config.VBeePath
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.minigame.entities;

import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.BotService;
import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.minigame.entities.BotBauCua;
import game.modules.minigame.entities.BotTaiXiu;
import game.modules.minigame.entities.BotSicbo;
import game.utils.ConfigGame;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class BotMinigame {
    private static List<String> bots = new ArrayList<String>();
    private static List<String> botsVipDaily = new ArrayList<String>();
    private static List<String> botsVip = new ArrayList<String>();
    private static UserService userService = new UserServiceImpl();
    private static BotService botService = new BotServiceImpl();
    private static List<Integer> betValueDefault = Arrays.asList(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 15000);
    private static long[] soVinBan = new long[]{30000000L, 85000000L, 40000000L, 100000000L};
    private static long updateTime = System.currentTimeMillis();

    public static void loadData() {
        UserModel userBot;
        String botName;
        BufferedReader br22;
        BotServiceImpl service = new BotServiceImpl();
        Debug.trace((Object[])new Object[]{"Load bot bots.txt"});
        try {
            br22 = new BufferedReader(new FileReader(VBeePath.basePath + "config/bots.txt"));
            while ((botName = br22.readLine()) != null) {
                try {
                    userBot = service.login(botName);
                    if (userBot == null || !userBot.isBot()) continue;
                    bots.add(botName);
                }
                catch (NoSuchAlgorithmException | SQLException e) {
                    Debug.trace((Object[])new Object[]{"Load bot " + botName + " error: ", e});
                }
            }
            br22.close();
        }
        catch (FileNotFoundException e) {
        }
        catch (IOException e) {
            // empty catch block
        }
        Debug.trace((Object[])new Object[]{"Load bot bots_vip.txt"});
        try {
            br22 = new BufferedReader(new FileReader(VBeePath.basePath + "config/bots_vip.txt"));
            service = new BotServiceImpl();
            while ((botName = br22.readLine()) != null) {
                try {
                    userBot = service.login(botName);
                    if (userBot == null || !userBot.isBot()) continue;
                    botsVip.add(botName);
                }
                catch (NoSuchAlgorithmException | SQLException e) {
                    Debug.trace((Object[])new Object[]{"Load vip bot " + botName + " error: ", e});
                }
            }
            br22.close();
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (IOException iOException) {
            // empty catch block
        }
        Debug.trace((Object[])new Object[]{"BotMinigame loadBotsVip"});
        BotMinigame.loadBotsVip();
        Debug.trace((Object[])new Object[]{"TOTAL BOTS: " + bots.size()});
    }

    public static void loadBotsVip() {
        botsVipDaily.clear();
        int maxBotVip = ConfigGame.getIntValue("tx_vip_max_vin");
        int numBotVip = maxBotVip + 10;
        SplittableRandom rd = new SplittableRandom();
        if (numBotVip >= botsVip.size()) {
            Debug.trace((Object[])new Object[]{"Khong the tao bot vip hang ngay"});
            return;
        }
        int i = 0;
        while (i < numBotVip) {
            int n = rd.nextInt(botsVip.size());
            String bot = botsVip.get(n);
            if (bot == null) continue;
            boolean exist = false;
            for (String str : botsVipDaily) {
                if (!str.equals(bot)) continue;
                exist = true;
                break;
            }
            if (exist) continue;
            botsVipDaily.add(bot);
            ++i;
        }
    }

    public static String getRandomBot(String moneyType) {
        SplittableRandom rd = new SplittableRandom();
        int index = rd.nextInt(bots.size());
        String nickname = bots.get(index);
        BotMinigame.pushMoneyToBot(nickname, moneyType);
        return nickname;
    }

    public static void pushMoneyToBot(String nickname, String moneyType) {
        long currentMoney = userService.getCurrentMoneyUserCache(nickname, moneyType);
        if (currentMoney < 10000000L) {
            botService.addMoney(nickname, 10000000L - currentMoney, moneyType, "Chuyen tien cho bot minigame");
        } else {
            BotMinigame.banVin(nickname, moneyType, currentMoney);
        }
    }

    private static void banVin(String nickname, String moneyType, long currentMoney) {
        if (currentMoney >= 90000000L) {
            SplittableRandom rd = new SplittableRandom();
            int index = rd.nextInt(soVinBan.length);
            // Cap the withdrawal so the bot's cash can never go below
            // zero — the post-BalanceGuard MoneyResponse.setMoneyUse
            // rejects negatives with IllegalStateException, which used to
            // abort scheduleBot() mid-loop and leave botsVin empty
            // (no bots from round 2 onwards). soVinBan can be 100M while
            // the trigger floor is only 90M, so a 90-99M bot rolling the
            // 100M slot was the exact overdraft case.
            long tienBan = Math.min(soVinBan[index], currentMoney);
            if (tienBan > 0L) {
                botService.addMoney(nickname, -tienBan, moneyType, "Chuyen tien");
            }
        }
    }

    private static void pushMoneyToBotVip(String nickname, String moneyType, long moneyPushed) {
        long currentMoney = userService.getCurrentMoneyUserCache(nickname, moneyType);
        if (currentMoney < moneyPushed) {
            botService.addMoney(nickname, moneyPushed, moneyType, "Cong tien cho bot minigame");
        } else {
            BotMinigame.banVin(nickname, moneyType, currentMoney);
        }
    }

    public static List<String> getBots(int amount, String moneyType) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(bots);
        SplittableRandom rd = new SplittableRandom();
        for (int i = 0; i < amount; ++i) {
            if (copyBots.size() == 0) {
                copyBots = new ArrayList<String>(bots);
            }
            int index = rd.nextInt(copyBots.size());
            String nickname = (String)copyBots.get(index);
            BotMinigame.pushMoneyToBot(nickname, moneyType);
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotsJackPot(int amount, String moneyType) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(bots);
        SplittableRandom rd = new SplittableRandom();
        for (int i = 0; i < amount; ++i) {
            int index = rd.nextInt(copyBots.size());
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotsVip(int amount, String moneyType) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(botsVipDaily);
        if (amount > copyBots.size()) {
            amount = copyBots.size();
        }
        for (int i = 0; i < amount; ++i) {
            SplittableRandom rd = new SplittableRandom();
            int index = rd.nextInt(copyBots.size());
            String nickname = (String)copyBots.get(index);
            BotMinigame.pushMoneyToBotVip(nickname, moneyType, 600000000L);
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotsSuperVip(int amount, String moneyType, long moneyPushed) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(botsVipDaily);
        for (int i = 0; i < amount; ++i) {
            SplittableRandom rd = new SplittableRandom();
            int index = rd.nextInt(copyBots.size());
            String nickname = (String)copyBots.get(index);
            BotMinigame.pushMoneyToBotVip(nickname, moneyType, moneyPushed);
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<BotTaiXiu> getVipBotTaiXiu() {
        int betValue;
        if (updateTime < DateTimeUtils.getStartTimeToDayAsLong()) {
            BotMinigame.loadBotsVip();
        }
        updateTime = System.currentTimeMillis();
        ArrayList<BotTaiXiu> results = new ArrayList<BotTaiXiu>();
        ArrayList<Integer> betValues = new ArrayList<Integer>();
        SplittableRandom rd = new SplittableRandom();
        int numBetTai = 0;
        int minBetValue = 0;
        int maxBetValue = 0;
        int minBettingTime = 0;
        int maxBettingTime = 0;
        int minBotsVin = ConfigGame.getIntValue("tx_vip_min_vin");
        int maxBotsVin = ConfigGame.getIntValue("tx_vip_max_vin");
        if (maxBotsVin == 0) {
            return new ArrayList<BotTaiXiu>();
        }
        int totalBot = rd.nextInt(maxBotsVin - minBotsVin) + minBotsVin + 10;
        if (BotMinigame.isNight()) {
            int n = rd.nextInt(2) + 3;
            totalBot /= n;
        }
        int minBot = totalBot * 5 / 10;
        int maxBot = totalBot - minBot + 5;
        numBetTai = rd.nextInt(maxBot - minBot) + minBot;
        minBetValue = ConfigGame.getIntValue("tx_vip_min_value_vin", 25000000);
        maxBetValue = ConfigGame.getIntValue("tx_vip_max_value_vin", 70000000);
        int stepBetting = ConfigGame.getIntValue("tx_vip_step_betting_vin");
        for (betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
            betValues.add(betValue);
        }
        try {
            minBettingTime = ConfigGame.getIntValue("tx_vip_min_betting_time");
            maxBettingTime = ConfigGame.getIntValue("tx_vip_max_betting_time");
            List<String> botsName = BotMinigame.getBotsVip(totalBot, "vin");
            for (int i = 0; i < totalBot && i < botsName.size(); ++i) {
                String nickname = botsName.get(i);
                int n = rd.nextInt(betValues.size());
                betValue = (Integer)betValues.get(n);
                short bettingTime = (short)BotMinigame.randomBettingTime(minBettingTime, maxBettingTime, 80);
                short betSide = 0;
                if (i < numBetTai) {
                    betSide = 1;
                }
                BotTaiXiu bot = new BotTaiXiu(nickname, bettingTime, betValue, betSide);
                results.add(bot);
            }
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"Exception:" + ex.getMessage()});
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Debug.trace((Object[])new Object[]{sStackTrace});
        }
        return results;
    }

    public static List<BotTaiXiu> getBotTaiXiu(String moneyType) {
        SplittableRandom rd = new SplittableRandom();
        int phanTramVaoSom = 80;
        int[] arr = new int[]{60, 70, 80, 85, 90};
        int index = rd.nextInt(arr.length);
        phanTramVaoSom = arr[index];
        ArrayList<BotTaiXiu> results = new ArrayList<BotTaiXiu>();
        ArrayList<Integer> betValues = new ArrayList<Integer>(betValueDefault);
        int numBetTai = 0;
        int numBetXiu = 0;
        int minBetValue = 0;
        int maxBetValue = 0;
        int minBettingTime = 0;
        int maxBettingTime = 0;
        if (moneyType.equalsIgnoreCase("vin")) {
            int minBotsVin = ConfigGame.getIntValue("tx_min_bot_betting_vin");
            int maxBotsVin = ConfigGame.getIntValue("tx_max_bot_betting_vin");
            if (maxBotsVin == 0 || minBotsVin == 0) {
                return new ArrayList<BotTaiXiu>();
            }
            // SUN-807: QC reported "số ng chơi giống nhau 2 bên là fake lộ liễu".
            // Previous logic drew a totalBot then split via a complementary
            // formula (tai + xiu = totalBot) — sides always sum to totalBot so
            // at low counts the split looks contrived. Now each side draws an
            // INDEPENDENT random count in [min, max]. If the two draws happen
            // to collide on an identical value, jitter one side by ±1 so the
            // UI never shows exactly equal counts on both sides.
            int spanVin = Math.max(1, maxBotsVin - minBotsVin + 1);
            int n = BotMinigame.ratioTXInNight();
            Debug.trace((Object[])new Object[]{"Bot n = " + n});
            numBetTai = (minBotsVin + rd.nextInt(spanVin)) * n / 100;
            numBetXiu = (minBotsVin + rd.nextInt(spanVin)) * n / 100;
            if (numBetTai == numBetXiu) {
                numBetXiu += (numBetXiu > minBotsVin) ? -1 : 1;
            }
            Debug.trace((Object[])new Object[]{"NUM BET TAI= " + numBetTai + ", NUM BET XIU= " + numBetXiu});
            minBetValue = ConfigGame.getIntValue("tx_min_bet_value_vin");
            maxBetValue = ConfigGame.getIntValue("tx_max_bet_value_vin");
            int stepBetting = ConfigGame.getIntValue("tx_step_betting_vin");
            for (int betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
                betValues.add(betValue);
            }
            int size = betValues.size();
            if (size > 0) {
                int i;
                int countAdd = Math.max(1, size * 10 / 100);
                ArrayList<Integer> indices = new ArrayList<Integer>(size);
                for (i = 0; i < size; ++i) {
                    indices.add(i);
                }
                Collections.shuffle(indices, new Random(rd.nextLong()));
                for (i = 0; i < countAdd && i < indices.size(); ++i) {
                    int idx = (Integer)indices.get(i);
                    betValues.set(idx, betValues.get(idx) + rd.nextInt(999) + 1);
                }
            }
        } else {
            int minBotsXu = ConfigGame.getIntValue("tx_min_bot_betting_xu");
            int maxBotsXu = ConfigGame.getIntValue("tx_max_bot_betting_xu");
            if (maxBotsXu == 0) {
                return new ArrayList<BotTaiXiu>();
            }
            int totalBots = rd.nextInt(maxBotsXu - minBotsXu) + minBotsXu;
            numBetTai = rd.nextInt(totalBots);
            numBetXiu = totalBots - numBetTai;
            minBetValue = ConfigGame.getIntValue("tx_min_bet_value_xu");
            maxBetValue = ConfigGame.getIntValue("tx_max_bet_value_xu");
            int stepBetting = ConfigGame.getIntValue("tx_step_betting_xu");
            for (int betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
                betValues.add(betValue);
            }
            int sizeXu = betValues.size();
            if (sizeXu > 0) {
                int i;
                int countAdd = Math.max(1, sizeXu * 10 / 100);
                ArrayList<Integer> indices = new ArrayList<Integer>(sizeXu);
                for (i = 0; i < sizeXu; ++i) {
                    indices.add(i);
                }
                Collections.shuffle(indices, new Random(rd.nextLong()));
                for (i = 0; i < countAdd && i < indices.size(); ++i) {
                    int idx = (Integer)indices.get(i);
                    betValues.set(idx, betValues.get(idx) + rd.nextInt(999) + 1);
                }
            }
        }
        try {
            minBettingTime = ConfigGame.getIntValue("tx_min_betting_time");
            maxBettingTime = ConfigGame.getIntValue("tx_max_betting_time");
            int totalBot = numBetTai + numBetXiu;
            List<String> botsName = BotMinigame.getBots(totalBot, moneyType);
            for (int i = 0; i < totalBot && i < botsName.size(); ++i) {
                String nickname = botsName.get(i);
                int n = rd.nextInt(betValues.size());
                long betValue = betValues.get(n).intValue();
                short bettingTime = (short)BotMinigame.randomBettingTime(minBettingTime, maxBettingTime, phanTramVaoSom);
                short betSide = 0;
                if (i < numBetTai) {
                    betSide = 1;
                }
                BotTaiXiu bot = new BotTaiXiu(nickname, bettingTime, betValue, betSide);
                results.add(bot);
            }
            Debug.trace((Object[])new Object[]{"NUMBER BOTS:" + results.size()});
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"Exception:" + ex.getMessage()});
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Debug.trace((Object[])new Object[]{sStackTrace});
        }
        return results;
    }

    public static List<BotSicbo> getBotSicbo(String moneyType) {
        Random random = new Random();
        SplittableRandom rd = new SplittableRandom();
        int phanTramVaoSom = 80;
        int[] arr = new int[]{60, 70, 80, 85, 90};
        int index = rd.nextInt(arr.length);
        phanTramVaoSom = arr[index];
        ArrayList<BotSicbo> results = new ArrayList<BotSicbo>();
        ArrayList<Integer> betValues = new ArrayList<Integer>(betValueDefault);
        int numBetTai = 0;
        int numBetXiu = 0;
        int minBetValue = 0;
        int maxBetValue = 0;
        int minBettingTime = ConfigGame.getIntValue("tx_sicbo_min_betting_time", 5);
        int maxBettingTime = ConfigGame.getIntValue("tx_sicbo_max_betting_time", 33);
        if (moneyType.equalsIgnoreCase("vin")) {
            minBetValue = ConfigGame.getIntValue("tx_sicbo_min_bet_value_vin", 1000000);
            maxBetValue = ConfigGame.getIntValue("tx_sicbo_max_bet_value_vin", 5000000);
            int minBotsVin = ConfigGame.getIntValue("tx_sicbo_min_bot_betting_vin", 200);
            int maxBotsVin = ConfigGame.getIntValue("tx_sicbo_max_bot_betting_vin", 400);
            if (maxBotsVin == 0 || minBotsVin == 0) {
                return new ArrayList<BotSicbo>();
            }
            // SUN-807: QC reported "số ng chơi giống nhau 2 bên là fake lộ liễu".
            // Previous logic drew a totalBot then split via a complementary
            // formula (tai + xiu = totalBot) — sides always sum to totalBot so
            // at low counts the split looks contrived. Now each side draws an
            // INDEPENDENT random count in [min, max]. If the two draws happen
            // to collide on an identical value, jitter one side by ±1 so the
            // UI never shows exactly equal counts on both sides.
            int spanVin = Math.max(1, maxBotsVin - minBotsVin + 1);
            int n = BotMinigame.ratioTXInNight();
            Debug.trace((Object[])new Object[]{"Bot n = " + n});
            numBetTai = (minBotsVin + rd.nextInt(spanVin)) * n / 100;
            numBetXiu = (minBotsVin + rd.nextInt(spanVin)) * n / 100;
            if (numBetTai == numBetXiu) {
                numBetXiu += (numBetXiu > minBotsVin) ? -1 : 1;
            }
            Debug.trace((Object[])new Object[]{"NUM BET TAI= " + numBetTai + ", NUM BET XIU= " + numBetXiu});
            int stepBetting = ConfigGame.getIntValue("tx_sicbo_step_betting_vin", 100000);
            for (int betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
                betValues.add(betValue);
            }
        } else {
            int minBotsXu = ConfigGame.getIntValue("tx_sicbo_min_bot_xu", 1500000);
            int maxBotsXu = ConfigGame.getIntValue("tx_sicbo_min_bot_xu", 4000000);
            if (maxBotsXu == 0) {
                return new ArrayList<BotSicbo>();
            }
            int totalBots = rd.nextInt(maxBotsXu - minBotsXu) + minBotsXu;
            numBetTai = rd.nextInt(totalBots);
            numBetXiu = totalBots - numBetTai;
            minBetValue = ConfigGame.getIntValue("tx_sicbo_min_bet_value_xu", 1000000);
            maxBetValue = ConfigGame.getIntValue("tx_sicbo_max_bet_value_xu", 5000000);
            int stepBetting = ConfigGame.getIntValue("tx_sicbo_step_betting_xu", 100000);
            for (int betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
                betValues.add(betValue);
            }
        }
        try {
            int totalBot = numBetTai + numBetXiu;
            List<String> botsName = BotMinigame.getBots(totalBot, moneyType);
            for (int i = 0; i < totalBot && i < botsName.size(); ++i) {
                String nickname = botsName.get(i);
                int n = rd.nextInt(betValues.size());
                long betValue = betValues.get(n).intValue();
                short bettingTime = (short)BotMinigame.randomBettingTime(minBettingTime, maxBettingTime, phanTramVaoSom);
                short betSide = (short)(random.nextInt(52) + 1);
                long money = userService.getMoneyUserCache(nickname, "vin");
                BotSicbo bot = new BotSicbo(nickname, bettingTime, betValue, betSide, money, random.nextInt(6) + 1);
                results.add(bot);
            }
            Debug.trace((Object[])new Object[]{"NUMBER BOTS:" + results.size()});
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"Exception:" + ex.getMessage()});
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Debug.trace((Object[])new Object[]{sStackTrace});
        }
        return results;
    }

    public static List<BotSicbo> getVipBotSicbo() {
        int betValue;
        Random random = new Random();
        if (updateTime < DateTimeUtils.getStartTimeToDayAsLong()) {
            BotMinigame.loadBotsVip();
        }
        updateTime = System.currentTimeMillis();
        ArrayList<BotSicbo> results = new ArrayList<BotSicbo>();
        ArrayList<Integer> betValues = new ArrayList<Integer>();
        SplittableRandom rd = new SplittableRandom();
        int numBetTai = 0;
        int minBetValue = ConfigGame.getIntValue("tx_sicbo_vip_min_bet_value", 25000000);
        int maxBetValue = ConfigGame.getIntValue("tx_sicbo_vip_max_bet_value", 50000000);
        int stepBetting = ConfigGame.getIntValue("tx_sicbo_vip_step_betting_vin", 1000000);
        int minBettingTime = ConfigGame.getIntValue("tx_sicbo_vip_min_betting_time", 10);
        int maxBettingTime = ConfigGame.getIntValue("tx_sicbo_vip_max_betting_time", 58);
        int minBotsVin = ConfigGame.getIntValue("tx_sicbo_vip_bot_min_vin", 5);
        int maxBotsVin = ConfigGame.getIntValue("tx_sicbo_vip_bot_max_vin", 20);
        if (maxBotsVin == 0) {
            return new ArrayList<BotSicbo>();
        }
        int totalBot = rd.nextInt(maxBotsVin - minBotsVin) + minBotsVin + 10;
        if (BotMinigame.isNight()) {
            int n = rd.nextInt(2) + 3;
            totalBot /= n;
        }
        int minBot = totalBot * 5 / 10;
        int maxBot = totalBot - minBot + 5;
        numBetTai = rd.nextInt(maxBot - minBot) + minBot;
        for (betValue = minBetValue; betValue < maxBetValue; betValue += stepBetting) {
            betValues.add(betValue);
        }
        try {
            List<String> botsName = BotMinigame.getBotsVip(totalBot, "vin");
            for (int i = 0; i < totalBot && i < botsName.size(); ++i) {
                String nickname = botsName.get(i);
                int n = rd.nextInt(betValues.size());
                betValue = (Integer)betValues.get(n);
                short bettingTime = (short)BotMinigame.randomBettingTime(minBettingTime, maxBettingTime, 80);
                short betSide = (short)(random.nextInt(52) + 1);
                long money = userService.getMoneyUserCache(nickname, "vin");
                BotSicbo bot = new BotSicbo(nickname, bettingTime, betValue, betSide, money, random.nextInt(6) + 1);
                results.add(bot);
            }
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"Exception:" + ex.getMessage()});
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Debug.trace((Object[])new Object[]{sStackTrace});
        }
        return results;
    }

    private static int randomBettingTime(int minTime, int maxTime, int phanTramVaoSom) {
        SplittableRandom rd = new SplittableRandom();
        int n = rd.nextInt(100);
        if (n > phanTramVaoSom) {
            int minTime5s = maxTime - 5;
            return rd.nextInt(maxTime - minTime5s) + minTime5s;
        }
        return rd.nextInt(maxTime - minTime) + minTime;
    }

    public static List<BotBauCua> getBotBauCua(int roomId) {
        String moneyType = "xu";
        if (roomId < 3) {
            moneyType = "vin";
        }
        long baseBetValue = BotMinigame.getBaseBettingBC(roomId);
        ArrayList<BotBauCua> results = new ArrayList<BotBauCua>();
        SplittableRandom rd = new SplittableRandom();
        int minBot = 10;
        int maxBot = 80;
        int minRatio = 5;
        int maxRatio = 1000;
        int minBettingTime = 10;
        int maxBettingTime = 58;
        int numBots = rd.nextInt(maxBot - minBot) + minBot;
        int maxBetSide = 5;
        List<String> botsName = BotMinigame.getBots(numBots, moneyType);
        for (int i = 0; i < numBots && i < botsName.size(); ++i) {
            String nickname = botsName.get(i);
            short bettingTime = (short)BotMinigame.randomBettingTime(minBettingTime, maxBettingTime, 70);
            long[] betArr = new long[6];
            int j = maxBetSide;
            while (j > 0) {
                long betValue;
                short betSide = (short)rd.nextInt(6);
                if (betArr[betSide] != 0L) continue;
                betArr[betSide] = betValue = baseBetValue * (long)(rd.nextInt(maxRatio - minRatio) + minRatio);
                --j;
            }
            StringBuilder builder = new StringBuilder();
            for (j = 0; j < 6; ++j) {
                builder.append(",");
                builder.append(betArr[j]);
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(0);
            }
            BotBauCua bot = new BotBauCua(nickname, bettingTime, builder.toString());
            results.add(bot);
        }
        return results;
    }

    private static long getBaseBettingBC(int roomId) {
        switch (roomId) {
            case 0: {
                return 1000L;
            }
            case 1: {
                return 10000L;
            }
            case 2: {
                return 100000L;
            }
            case 3: {
                return 10000L;
            }
            case 4: {
                return 100000L;
            }
            case 5: {
                return 1000000L;
            }
        }
        return 1000L;
    }

    public static List<String> getBotChat() {
        int number = 0;
        SplittableRandom rd = new SplittableRandom();
        if (BotMinigame.isNight()) {
            int n = rd.nextInt(5);
            if (n == 0) {
                number = 1;
            }
        } else {
            number = rd.nextInt(3);
        }
        ArrayList<String> results = new ArrayList<String>();
        if (number > 0) {
            for (int i = 0; i < number; ++i) {
                int n = rd.nextInt(10);
                if (n == 0) {
                    n = rd.nextInt(botsVip.size());
                    results.add(botsVip.get(n));
                    continue;
                }
                n = rd.nextInt(bots.size());
                results.add(bots.get(n));
            }
        }
        return results;
    }

    public static boolean isNight() {
        Calendar cal = Calendar.getInstance();
        int hourOfDay = cal.get(11);
        return 2 <= hourOfDay && hourOfDay <= 8;
    }

    public static int ratioTXInNight() {
        Calendar cal = Calendar.getInstance();
        int hourOfDay = cal.get(11);
        SplittableRandom rd = new SplittableRandom();
        if (2 <= hourOfDay && hourOfDay <= 8) {
            switch (hourOfDay) {
                case 2: 
                case 8: {
                    return rd.nextInt(20) + 80;
                }
                case 3: 
                case 7: {
                    return rd.nextInt(30) + 50;
                }
                case 4: 
                case 5: 
                case 6: {
                    return rd.nextInt(20) + 30;
                }
            }
        }
        return 100;
    }

    public static void main(String[] args) {
        BotMinigame.isNight();
    }
}

