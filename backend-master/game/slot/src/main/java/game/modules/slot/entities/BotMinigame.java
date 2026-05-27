/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.BotService
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 */
package game.modules.slot.entities;

import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.BotService;
import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class BotMinigame {
    private static List<String> bots = new ArrayList<String>();
    private static List<String> botsVip = new ArrayList<String>();
    private static UserService userService = new UserServiceImpl();
    private static BotService botService = new BotServiceImpl();
    private static long[] soDuConLai = new long[]{3000000L, 35000000L, 4000000L};

    public static void loadData() {
        BufferedReader br2;
        Exception e;
        String botName;
        BotServiceImpl service;
        try {
            br2 = new BufferedReader(new FileReader(VBeePath.basePath.concat("config/bots.txt")));
            service = new BotServiceImpl();
            while ((botName = br2.readLine()) != null) {
                try {
                    UserModel userBot = service.login(botName);
                    if (userBot == null || !userBot.isBot()) {
                        continue;
                    }
                    bots.add(botName);
                }
                catch (NoSuchAlgorithmException | SQLException ex3) {
                    Exception ex;
                    e = ex = ex3;
                    Debug.trace((Object[])new Object[]{"Load bot " + botName + " error: ", e});
                }
            }
            br2.close();
        }
        catch (FileNotFoundException br2_0) {
        }
        catch (IOException br2_1) {
            // empty catch block
        }
        try {
            br2 = new BufferedReader(new FileReader(VBeePath.basePath.concat("config/bots_vip.txt")));
            service = new BotServiceImpl();
            while ((botName = br2.readLine()) != null) {
                try {
                    UserModel userBot = service.login(botName);
                    if (userBot == null || !userBot.isBot()) {
                        continue;
                    }
                    botsVip.add(botName);
                }
                catch (NoSuchAlgorithmException | SQLException ex6) {
                    Exception ex2;
                    e = ex2 = ex6;
                    Debug.trace((Object[])new Object[]{"Load vip bot " + botName + " error: ", e});
                }
            }
            br2.close();
        }
        catch (FileNotFoundException br3) {
        }
        catch (IOException br3) {
            // empty catch block
        }
        Debug.trace((Object)("TOTAL BOTS: " + bots.size()));
    }

    public static String getRandomBot(String moneyType) {
        Random rd = new Random();
        int index = rd.nextInt(bots.size());
        String nickname = bots.get(index);
        BotMinigame.pushMoneyToBot(nickname, moneyType);
        return nickname;
    }

    private static void pushMoneyToBot(String nickname, String moneyType) {
        long currentMoney = userService.getCurrentMoneyUserCache(nickname, moneyType);
        if (currentMoney < 1000000L) {
            botService.addMoney(nickname, 10000000L, moneyType, "C\u00e1\u00bb\u2122ng ti\u00e1\u00bb\ufffdn cho bot minigame");
        } else {
            BotMinigame.banVin(nickname, moneyType, currentMoney);
        }
    }

    private static void banVin(String nickname, String moneyType, long currentMoney) {
        if (currentMoney >= 60000000L) {
            Random rd = new Random();
            int index = rd.nextInt(soDuConLai.length);
            long soDu = soDuConLai[index];
            long tienBan = currentMoney - soDu;
            botService.addMoney(nickname, -tienBan, moneyType, "Chuy\u00e1\u00bb\u0192n ti\u00e1\u00bb\ufffdn");
        }
    }

    private static void pushMoneyToBotVip(String nickname, String moneyType, long moneyPushed) {
        long currentMoney = userService.getCurrentMoneyUserCache(nickname, moneyType);
        if (currentMoney < moneyPushed) {
            botService.addMoney(nickname, moneyPushed, moneyType, "C\u00e1\u00bb\u2122ng ti\u00e1\u00bb\ufffdn cho bot minigame");
        } else {
            BotMinigame.banVin(nickname, moneyType, currentMoney);
        }
    }

    public static List<String> getBots(int amount, String moneyType) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(bots);
        for (int i = 0; i < amount; ++i) {
            Random rd = new Random();
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
        Random rd = new Random();
        for (int i = 0; i < amount; ++i) {
            int index = rd.nextInt(copyBots.size());
            results.add(copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotsVip(int amount, String moneyType) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(botsVip);
        for (int i = 0; i < amount; ++i) {
            Random rd = new Random();
            int index = rd.nextInt(copyBots.size());
            String nickname = (String)copyBots.get(index);
            BotMinigame.pushMoneyToBotVip(nickname, moneyType, 10000000L);
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotsSuperVip(int amount, String moneyType, long moneyPushed) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> copyBots = new ArrayList<String>(botsVip);
        for (int i = 0; i < amount; ++i) {
            Random rd = new Random();
            int index = rd.nextInt(copyBots.size());
            String nickname = (String)copyBots.get(index);
            BotMinigame.pushMoneyToBotVip(nickname, moneyType, moneyPushed);
            results.add((String)copyBots.remove(index));
        }
        return results;
    }

    public static List<String> getBotChat() {
        int number = 0;
        Random rd = new Random();
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
                int n2 = rd.nextInt(10);
                if (n2 == 0) {
                    n2 = rd.nextInt(botsVip.size());
                    results.add(botsVip.get(n2));
                    continue;
                }
                n2 = rd.nextInt(bots.size());
                results.add(bots.get(n2));
            }
        }
        return results;
    }

    public static boolean isNight() {
        Calendar cal = Calendar.getInstance();
        int hourOfDay = cal.get(11);
        return 2 <= hourOfDay && hourOfDay <= 8;
    }

    public static void main(String[] args) {
        BotMinigame.isNight();
    }
}

