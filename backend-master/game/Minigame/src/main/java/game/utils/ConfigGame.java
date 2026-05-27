/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.utils;

import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.config.VBeePath;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigGame {
    private static final String CONFIG_GAME_PATH = "config/config_game.properties";
    public static final String ENABLE_ALERT_MSG = "enable_alert_msg";
    public static final String UPDATE_JACKPOT_TIME = "update_jackpot_time";
    public static final String UPDATE_LOG_CCU = "update_log_ccu";
    public static final String CHAT_MIN_VP_REQUIRE = "chat_min_vp_require";
    public static final String MIN_SO_CCU_GIAM = "min_so_ccu_giam";
    public static final String TX_NUM_RD = "tx_num_rd";
    public static final String TX_REVENUE_PERCENT = "tx_revenue_percent";
    public static final String TX_ENABLE_BALANCE = "tx_enable_balance";
    public static final String TX_MIN_MONEY_FORCE_USER_WIN = "tx_min_money_force_user_win";
    public static final String TX_MIN_FEE = "tx_min_fee";
    public static final String TX_MAX_FEE = "tx_max_fee";
    public static final String INTERVAL_RESET_BALANCE = "interval_reset_balance";
    public static final String TX_VIP_MIN_VIN = "tx_vip_min_vin";
    public static final String TX_VIP_MAX_VIN = "tx_vip_max_vin";
    public static final String TX_VIP_MIN_VALUE_VIN = "tx_vip_min_value_vin";
    public static final String TX_VIP_MAX_VALUE_VIN = "tx_vip_max_value_vin";
    public static final String TX_VIP_STEP_BETTING_VIN = "tx_vip_step_betting_vin";
    public static final String TX_VIP_MIN_BETTING_TIME = "tx_vip_min_betting_time";
    public static final String TX_VIP_MAX_BETTING_TIME = "tx_vip_max_betting_time";
    public static final String TX_MIN_BOT_BETTING_VIN = "tx_min_bot_betting_vin";
    public static final String TX_MAX_BOT_BETTING_VIN = "tx_max_bot_betting_vin";
    public static final String TX_MIN_BOT_BETTING_XU = "tx_min_bot_betting_xu";
    public static final String TX_MAX_BOT_BETTING_XU = "tx_max_bot_betting_xu";
    public static final String TX_MIN_BET_VALUE_VIN = "tx_min_bet_value_vin";
    public static final String TX_MAX_BET_VALUE_VIN = "tx_max_bet_value_vin";
    public static final String TX_MIN_BET_VALUE_XU = "tx_min_bet_value_xu";
    public static final String TX_MAX_BET_VALUE_XU = "tx_max_bet_value_xu";
    public static final String TX_MIN_BETTING_TIME = "tx_min_betting_time";
    public static final String TX_MAX_BETTING_TIME = "tx_max_betting_time";
    public static final String TX_STEP_BETTING_VIN = "tx_step_betting_vin";
    public static final String TX_STEP_BETTING_XU = "tx_step_betting_xu";
    public static final String TX_BLACK_LIST = "tx_black_list";
    public static final String TX_BLACK_LIST_PERCENT = "tx_black_list_percent";
    public static final String TX_MAX_BLACK_LIST_LOST = "tx_max_black_list_lost";
    public static final String TX_MIN_MONEY_BLACK_LIST = "tx_min_money_black_list";
    public static final List<String> blackList = new ArrayList<String>();
    public static final String TX_WHITE_LIST = "tx_white_list";
    public static final String TX_WHITE_LIST_PERCENT = "tx_white_list_percent";
    public static final String TX_MAX_WHITE_LIST_WIN = "tx_max_white_list_win";
    public static final String TX_MIN_MONEY_WHITE_LIST = "tx_min_money_white_list";
    public static final List<String> whiteList = new ArrayList<String>();
    public static final String BC_MIN_BOT = "bc_min_bot";
    public static final String BC_MAX_BOT = "bc_max_bot";
    public static final String BC_MIN_RATIO = "bc_min_ratio";
    public static final String BC_MAX_RATIO = "bc_max_ratio";
    public static final String BC_MIN_BETTING_TIME = "bc_min_betting_time";
    public static final String BC_MAX_BETTING_TIME = "bc_max_betting_time";
    public static final String BC_MAX_BET_SIDE = "bc_max_bet_side";
    public static final String CP_TPSA = "cp_tpsa";
    public static final String CP_TI_LE_TPS = "cp_ti_le_tps";
    public static final String MINI_POKER_THREAD_POOL_PER_ROOM_VIN = "mini_poker_thread_pool_per_room_vin";
    public static final String MINI_POKER_THREAD_POOL_PER_ROOM_XU = "mini_poker_thread_pool_per_room_xu";
    public static final String MINI_POKER_SO_TIEN_NAP_TOI_THIEU_NO_HU_10K = "mini_poker_so_tien_nap_toi_thieu_no_hu_10k";
    public static final String MINI_POKER_INIT_POT_VALUES = "mini_poker_init_pot_values";
    public static final String MINI_POKER_TIME_X2 = "mini_poker_time_x2";
    public static final String MINI_POKER_DAYS_X2 = "mini_poker_days_x2";
    public static final String MINI_POKER_LAST_DAY_X2 = "mini_poker_last_day_x2";
    public static final String MINI_POKER_BOT_100 = "mini_poker_bot_100";
    public static final String MINI_POKER_BOT_1000 = "mini_poker_bot_1000";
    public static final String MINI_POKER_BOT_10000 = "mini_poker_bot_10000";
    public static final String MINI_POKER_NUM_BOT_100 = "mini_poker_num_bot_100";
    public static final String MINI_POKER_NUM_BOT_1000 = "mini_poker_num_bot_1000";
    public static final String MINI_POKER_NUM_BOT_10000 = "mini_poker_num_bot_10000";
    public static final String POKE_GO_THREAD_POOL_PER_ROOM_VIN = "poke_go_thread_pool_per_room_vin";
    public static final String POKE_GO_THREAD_POOL_PER_ROOM_XU = "poke_go_thread_pool_per_room_xu";
    public static final String POKE_GO_SO_TIEN_NAP_TOI_THIEU_NO_HU_10K = "poke_go_so_tien_nap_toi_thieu_no_hu_10k";
    public static final String POKE_GO_INIT_POT_VALUES = "poke_go_init_pot_values";
    public static final String POKE_GO_TIME_X2 = "poke_go_time_x2";
    public static final String POKE_GO_DAYS_X2 = "poke_go_days_x2";
    public static final String POKE_GO_SO_LAN_NO_HU = "poke_go_so_lan_no_hu";
    public static final String POKE_LAST_DAY_GIO_VANG = "poke_last_day_gio_vang";
    public static final String POKE_GO_BOT_100 = "poke_go_bot_100";
    public static final String POKE_GO_BOT_1000 = "poke_go_bot_1000";
    public static final String POKE_GO_BOT_10000 = "poke_go_bot_10000";
    public static final String POKE_GO_NUM_BOT_100 = "poke_go_num_bot_100";
    public static final String POKE_GO_NUM_BOT_1000 = "poke_go_num_bot_1000";
    public static final String POKE_GO_NUM_BOT_10000 = "poke_go_num_bot_10000";
	public static final String TaiXiuForceResult = "tx_force_result";
    public static final String TaiXiuForceRatio = "tx_force_ratio";
    //private static Properties prop = new Properties();
    //private static JSONObject json = new JSONObject();
    public static boolean ENABLE_FILTER_CHAT = false;
    public static List<String> badwords = new ArrayList<String>();
    public static int TIME_BLOCK_CHAT = 10080;
    private static Properties prop = new Properties();
    private static String basePath = VBeePath.basePath;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void reload() {
        try {
            List<String> list;
            String[] arr;
            List<String> list2;
            FileInputStream input = new FileInputStream(basePath.concat(CONFIG_GAME_PATH));
            prop.load(input);
            String str = ConfigGame.getValueString(TX_BLACK_LIST);
            if (str != null) {
                blackList.clear();
                arr = str.split(",");
                list2 = list = blackList;
                synchronized (list2) {
                    for (String u : arr) {
                        blackList.add(u.trim());
                    }
                }
            }
            if ((str = ConfigGame.getValueString(TX_WHITE_LIST)) != null) {
                whiteList.clear();
                arr = str.split(",");
                list2 = list = whiteList;
                synchronized (list2) {
                    for (String u : arr) {
                        whiteList.add(u.trim());
                    }
                }
            }
            ConfigGame.loadChatConfig();
        }
        catch (FileNotFoundException e) {
            Debug.trace("File config/config_game.properties not found");
        }
        catch (IOException e) {
            Debug.trace("Load config/config_game.properties error");
        }
    }

    public static String getValueString(String key) {
        return prop.getProperty(key);
    }

    public static int getIntValue(String key) {
        String val = ConfigGame.getValueString(key);
        if (val == null || val.equals("null") || val.isEmpty()) {
            System.err.println("[ConfigGame] WARNING: key '" + key + "' is null/missing, returning 0");
            return 0;
        }
        return Integer.parseInt(val);
    }

    public static float getFloatValue(String key) {
        String val = ConfigGame.getValueString(key);
        if (val == null || val.equals("null") || val.isEmpty()) return 0f;
        return Float.parseFloat(val);
    }

    public static int getIntValue(String key, int defaultValue) {
        if (prop.containsKey(key)) {
            return ConfigGame.getIntValue(key);
        }
        return defaultValue;
    }

    public static float getFloatValue(String key, float defaultValue) {
        if (prop.containsKey(key)) {
            return ConfigGame.getFloatValue(key);
        }
        return defaultValue;
    }

    public static int getIntValueBaseMin(String key, int minValue) {
        int value = minValue;
        if (prop.containsKey(key)) {
            value = ConfigGame.getIntValue(key);
        }
        if (value < minValue) {
            value = minValue;
        }
        return value;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean inBlackList(String nickname) {
        List<String> list;
        List<String> list2 = list = blackList;
        synchronized (list2) {
            for (String u : blackList) {
                if (!u.equalsIgnoreCase(nickname)) continue;
                return true;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean inWhiteList(String nickname) {
        List<String> list;
        List<String> list2 = list = whiteList;
        synchronized (list2) {
            for (String u : whiteList) {
                if (!u.equalsIgnoreCase(nickname)) continue;
                return true;
            }
        }
        return false;
    }

    private static void loadChatConfig() {
        String[] arr;
        ENABLE_FILTER_CHAT = Boolean.parseBoolean(prop.getProperty("enable_filter_chat").trim());
        TIME_BLOCK_CHAT = ConfigGame.getIntValue("time_block_chat", 10080);
        badwords.clear();
        String badwordStr = prop.getProperty("bad_word");
        for (String str : arr = badwordStr.split(",")) {
            badwords.add(str);
        }
    }

    public static boolean checkBadword(String input) {
        if (ENABLE_FILTER_CHAT) {
            for (String str : badwords) {
                if (!input.contains(str)) continue;
                return true;
            }
        }
        return false;
    }
}

