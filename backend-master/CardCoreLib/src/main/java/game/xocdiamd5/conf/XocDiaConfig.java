/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.vbee.common.utils.MapUtils
 */
package game.xocdiamd5.conf;

import bitzero.server.BitZeroServer;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.utils.MapUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class XocDiaConfig {
    private static final String propFile = "config/xocdia.properties";
    public static final int NORMAL_TYPE = 0;
    public static final int GLOBAL_TYPE = 1;
    public static final int VIP_TYPE = 2;
    public static final int UPDATE_TYPE_FUND = 1;
    public static final int UPDATE_TYPE_STATUS = 2;
    public static final int UPDATE_TYPE_FEE = 3;
    public static final int NO_FORCE = -1;
    public static final int ODD = 0;
    public static final int EVEN = 1;
    public static final int FORCE_LIST_WIN = 3;
    public static final double revenueUser = 0.98;
    private static XocDiaConfig conf = null;
    private final Runnable configTask;
    public static List<String> listCau;
    public static List<Integer> listCoinsGlobal;
    public static List<Integer> listCoinsNormal;
    public static long globalBankerRegis;
    public static long globalBankerHold;
    public static List<String> mobileAdmin;
    public static int timeTaskReloadConfig;
    public static int timeTaskBotStart;
    public static int timeTaskBotJoinRoom;
    public static int timeTaskBotInGame;
    public static double ratioBotBettingInGame;
    public static double ratioBotBuyPotInGame;
    public static double fundVipMinRegis;
    public static double fundVipMinHold;
    public static double bossFee;
    public static double bankerFee;
    public static int noPlayNumber;
    public static double ratioResult4;
    public static double ratio1;
    public static double ratio4;
    public static int minRegisBanker;
    public static int minHoldBanker;
    public static double maxBetChanLe;
    public static double maxBet1;
    public static double maxBet4;
    public static int numRandomResult;
    public static int timeBlockUser;
    public static int transMinInDay;
    public static Map<Integer, Integer> transMinInWeek;
    public static Map<Integer, Integer> mapNumBoardBoss;
    public static double ratioJoinGlobal;
    public static double ratioJoinVip;
    public static int maxUserGlobal;
    public static int revenueSysMin;
    public static int revenueSysMax;
    public static double ratioGetRevenueSysMax;
    public static int timeBotJoinRoomMin;
    public static int timeBotJoinRoomMax;
    public static int maxBotInMatchMin;
    public static int maxBotInMatchMax;
    public static int timeChangeMax;
    public static int maxBotInMatchMinGlobal;
    public static int maxBotInMatchMaxGlobal;
    public static int timeHigh;
    public static int maxBotPLay;
    public static int moneyBotMin;
    public static int moneyBotMax;
    public static int normalMaxRoom;
    public static int normalMaxNumPlayMin;
    public static int normalMaxNumPLayMax;
    public static double normalRatioBetChanLe;
    public static double normalRatioBet1;
    public static double normalRatioBet4;
    public static int normalBetChanLeMin;
    public static int normalBetChanLeMax;
    public static int normalBet1Min;
    public static int normalBet1Max;
    public static int normalBet4Min;
    public static int normalBet4Max;
    public static double normalRatioBuyPot;
    public static int normalBuyPotMin;
    public static int normalBuyPotMax;
    public static int _100BetChanLeMin;
    public static int _100BetChanLeMax;
    public static int _100Bet1Min;
    public static int _100Bet1Max;
    public static int _100Bet4Min;
    public static int _100Bet4Max;
    public static int _100BuyPotMin;
    public static int _100BuyPotMax;
    public static double bkRatioBanker;
    public static double bkRatioRequestBanker;
    public static int bkMoneyRequestBankerMin;
    public static int bkNumBotReqBankerMax;
    public static double bkRatioSellPot;
    public static int bkSellPotMin;
    public static int bkSellPotMax;
    public static double bkRatioReject;
    private static String basePath = VBeePath.basePath;

    private XocDiaConfig() {
        this.configTask = new ConfigTask();
        try {
            XocDiaConfig.init();
            XocDiaConfig.initOnce();
            BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.configTask, 60, timeTaskReloadConfig, TimeUnit.SECONDS);
            Debug.trace((Object)"Init config success");
        }
        catch (Exception e) {
            Debug.trace((Object)("Init config error: " + e.getMessage()));
            Debug.trace((Object)e);
        }
    }

    public static XocDiaConfig instance() throws IOException {
        if (conf == null) {
            conf = new XocDiaConfig();
        }
        return conf;
    }

    public static void initOnce() throws IOException {
        String[] arr2;
        String[] arr1;
        XocDiaConfig.initCau();
        Properties prop = new Properties();
        FileInputStream input = new FileInputStream(basePath.concat("config/xocdia.properties"));
        prop.load(input);
        String coinsGlobal = prop.getProperty("listCoinsGlobal");
        listCoinsGlobal = new ArrayList<Integer>();
        for (String ci1 : arr1 = coinsGlobal.split(",")) {
            listCoinsGlobal.add(Integer.parseInt(ci1));
        }
        String coinsNormal = prop.getProperty("listCoinsNormal");
        listCoinsNormal = new ArrayList<Integer>();
        for (String ci2 : arr2 = coinsNormal.split(",")) {
            listCoinsNormal.add(Integer.parseInt(ci2));
        }
        timeTaskReloadConfig = Integer.parseInt(prop.getProperty("timeTaskReloadConfig"));
        timeTaskBotStart = Integer.parseInt(prop.getProperty("timeTaskBotStart"));
        timeTaskBotJoinRoom = Integer.parseInt(prop.getProperty("timeTaskBotJoinRoom"));
    }

    public static void initCau() throws IOException {
        listCau = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(basePath.concat("config/xocdia.dat")));
        String str = null;
        while ((str = reader.readLine()) != null) {
            if (str.isEmpty()) continue;
            listCau.add(str);
        }
    }

    public static void init() throws IOException {
        String[] arr1;
        String[] arrT;
        Properties prop = new Properties();
        FileInputStream input = new FileInputStream(basePath.concat("config/xocdia.properties"));
        prop.load(input);
        String mobileAdmins = prop.getProperty("mobileAdmin");
        mobileAdmin = new ArrayList<String>();
        if (mobileAdmins != null && !mobileAdmins.trim().isEmpty()) {
            String[] arrAm;
            for (String am : arrAm = mobileAdmins.split(",")) {
                mobileAdmin.add(am);
            }
        }
        globalBankerRegis = Long.parseLong(prop.getProperty("globalBankerRegis"));
        globalBankerHold = Long.parseLong(prop.getProperty("globalBankerHold"));
        timeTaskBotInGame = Integer.parseInt(prop.getProperty("timeTaskBotInGame"));
        ratioBotBettingInGame = Double.parseDouble(prop.getProperty("ratioBotBettingInGame"));
        ratioBotBuyPotInGame = Double.parseDouble(prop.getProperty("ratioBotBuyPotInGame"));
        fundVipMinRegis = Double.parseDouble(prop.getProperty("fundVipMinRegis"));
        fundVipMinHold = Double.parseDouble(prop.getProperty("fundVipMinHold"));
        bossFee = Double.parseDouble(prop.getProperty("bossFee"));
        bankerFee = Double.parseDouble(prop.getProperty("bankerFee"));
        noPlayNumber = Integer.parseInt(prop.getProperty("noPlayNumber"));
        ratioResult4 = Double.parseDouble(prop.getProperty("ratioResult4"));
        ratio1 = Double.parseDouble(prop.getProperty("ratio1"));
        ratio4 = Double.parseDouble(prop.getProperty("ratio4"));
        minRegisBanker = Integer.parseInt(prop.getProperty("minRegisBanker"));
        minHoldBanker = Integer.parseInt(prop.getProperty("minHoldBanker"));
        maxBetChanLe = Double.parseDouble(prop.getProperty("maxBetChanLe"));
        maxBet1 = Double.parseDouble(prop.getProperty("maxBet1"));
        maxBet4 = Double.parseDouble(prop.getProperty("maxBet4"));
        numRandomResult = Integer.parseInt(prop.getProperty("numRandomResult"));
        timeBlockUser = Integer.parseInt(prop.getProperty("timeBlockUser"));
        transMinInDay = Integer.parseInt(prop.getProperty("transMinInDay"));
        String transMin = prop.getProperty("transMinInWeek");
        transMinInWeek = new HashMap<Integer, Integer>();
        for (String at1 : arrT = transMin.split(";")) {
            String[] arrT2 = at1.split(",");
            transMinInWeek.put(Integer.parseInt(arrT2[0]), Integer.parseInt(arrT2[1]));
        }
        transMinInWeek = MapUtils.sortMapIntByValue(transMinInWeek);
        String numBoardBoss = prop.getProperty("numBoardBoss");
        mapNumBoardBoss = new HashMap<Integer, Integer>();
        for (String a1 : arr1 = numBoardBoss.split(";")) {
            String[] arr2 = a1.split(",");
            mapNumBoardBoss.put(Integer.parseInt(arr2[0]), Integer.parseInt(arr2[1]));
        }
        mapNumBoardBoss = MapUtils.sortMapIntByValue(mapNumBoardBoss);
        ratioJoinGlobal = Double.parseDouble(prop.getProperty("ratioJoinGlobal"));
        ratioJoinVip = Double.parseDouble(prop.getProperty("ratioJoinVip"));
        maxUserGlobal = Integer.parseInt(prop.getProperty("maxUserGlobal"));
        revenueSysMin = Integer.parseInt(prop.getProperty("revenueSysMin"));
        revenueSysMax = Integer.parseInt(prop.getProperty("revenueSysMax"));
        ratioGetRevenueSysMax = Double.parseDouble(prop.getProperty("ratioGetRevenueSysMax"));
        timeBotJoinRoomMin = Integer.parseInt(prop.getProperty("timeBotJoinRoomMin"));
        timeBotJoinRoomMax = Integer.parseInt(prop.getProperty("timeBotJoinRoomMax"));
        maxBotInMatchMin = Integer.parseInt(prop.getProperty("maxBotInMatchMin"));
        maxBotInMatchMax = Integer.parseInt(prop.getProperty("maxBotInMatchMax"));
        timeChangeMax = Integer.parseInt(prop.getProperty("timeChangeMax"));
        maxBotInMatchMinGlobal = Integer.parseInt(prop.getProperty("maxBotInMatchMinGlobal"));
        maxBotInMatchMaxGlobal = Integer.parseInt(prop.getProperty("maxBotInMatchMaxGlobal"));
        timeHigh = Integer.parseInt(prop.getProperty("timeHigh"));
        Calendar cal = Calendar.getInstance();
        maxBotPLay = cal.get(11) < timeHigh ? Integer.parseInt(prop.getProperty("maxBotPLayLow")) : Integer.parseInt(prop.getProperty("maxBotPLayHigh"));
        moneyBotMin = Integer.parseInt(prop.getProperty("moneyBotMin"));
        moneyBotMax = Integer.parseInt(prop.getProperty("moneyBotMax"));
        normalMaxRoom = Integer.parseInt(prop.getProperty("normalMaxRoom"));
        normalMaxNumPlayMin = Integer.parseInt(prop.getProperty("normalMaxNumPlayMin"));
        normalMaxNumPLayMax = Integer.parseInt(prop.getProperty("normalMaxNumPLayMax"));
        normalRatioBetChanLe = Double.parseDouble(prop.getProperty("normalRatioBetChanLe"));
        normalRatioBet1 = Double.parseDouble(prop.getProperty("normalRatioBet1"));
        normalRatioBet4 = Double.parseDouble(prop.getProperty("normalRatioBet4"));
        normalBetChanLeMin = Integer.parseInt(prop.getProperty("normalBetChanLeMin"));
        normalBetChanLeMax = Integer.parseInt(prop.getProperty("normalBetChanLeMax"));
        normalBet1Min = Integer.parseInt(prop.getProperty("normalBet1Min"));
        normalBet1Max = Integer.parseInt(prop.getProperty("normalBet1Max"));
        normalBet4Min = Integer.parseInt(prop.getProperty("normalBet4Min"));
        normalBet4Max = Integer.parseInt(prop.getProperty("normalBet4Max"));
        normalRatioBuyPot = Double.parseDouble(prop.getProperty("normalRatioBuyPot"));
        normalBuyPotMin = Integer.parseInt(prop.getProperty("normalBuyPotMin"));
        normalBuyPotMax = Integer.parseInt(prop.getProperty("normalBuyPotMax"));
        _100BetChanLeMin = Integer.parseInt(prop.getProperty("_100BetChanLeMin"));
        _100BetChanLeMax = Integer.parseInt(prop.getProperty("_100BetChanLeMax"));
        _100Bet1Min = Integer.parseInt(prop.getProperty("_100Bet1Min"));
        _100Bet1Max = Integer.parseInt(prop.getProperty("_100Bet1Max"));
        _100Bet4Min = Integer.parseInt(prop.getProperty("_100Bet4Min"));
        _100Bet4Max = Integer.parseInt(prop.getProperty("_100Bet4Max"));
        _100BuyPotMin = Integer.parseInt(prop.getProperty("_100BuyPotMin"));
        _100BuyPotMax = Integer.parseInt(prop.getProperty("_100BuyPotMax"));
        bkRatioBanker = Double.parseDouble(prop.getProperty("bkRatioBanker"));
        bkRatioRequestBanker = Double.parseDouble(prop.getProperty("bkRatioRequestBanker"));
        bkMoneyRequestBankerMin = Integer.parseInt(prop.getProperty("bkMoneyRequestBankerMin"));
        bkNumBotReqBankerMax = Integer.parseInt(prop.getProperty("bkNumBotReqBankerMax"));
        bkRatioSellPot = Double.parseDouble(prop.getProperty("bkRatioSellPot"));
        bkSellPotMin = Integer.parseInt(prop.getProperty("bkSellPotMin"));
        bkSellPotMax = Integer.parseInt(prop.getProperty("bkSellPotMax"));
        bkRatioReject = Double.parseDouble(prop.getProperty("bkRatioReject"));
    }

    public static int getTransMinInWeek(int moneyBet) {
        int res = 0;
        for (Map.Entry<Integer, Integer> entry : transMinInWeek.entrySet()) {
            if (moneyBet < entry.getKey()) continue;
            res = entry.getValue();
            break;
        }
        return res;
    }

    private final class ConfigTask
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                XocDiaConfig.init();
                Debug.trace((Object)("Reload config maxBotPlay: " + XocDiaConfig.maxBotPLay));
            }
            catch (Throwable e) {
                Debug.trace((Object)("Reload config error: " + e.getMessage()));
                Debug.trace((Object)e);
            }
        }
    }

}

