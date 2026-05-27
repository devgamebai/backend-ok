package game.util;

import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.config.VBeePath;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigGame {
     private static final String CONFIG_GAME_PATH = "config/config_game.properties";
     public static final String ENABLE_ALERT_MSG = "enable_alert_msg";
     public static final String UPDATE_JACKPOT_TIME = "update_jackpot_time";
     public static final String UPDATE_LOG_CCU = "update_log_ccu";
     public static final String MAX_PRIZE_FREE_DAILY = "max_prize_free_daily";
     public static final String KHO_BAU_SO_TIEN_NAP_TOI_THIEU_NO_HU_10K = "KhoBau_so_tien_nap_toi_thieu_no_hu_10k";
     public static final String KHO_BAU_INIT_POT_VALUES = "KhoBau_init_pot_values";
     public static final String KHO_BAU_TIME_X2 = "KhoBau_time_x2";
     public static final String KHO_BAU_DAYS_X2 = "KhoBau_days_x2";
     public static final String KHO_BAU_SO_LAN_NO_HU = "KhoBau_so_lan_no_hu";
     public static final String KHO_BAU_LAST_DAY_GIO_VANG = "KhoBau_last_day_gio_vang";
     public static final String KHO_BAU_BOT_100 = "KhoBau_bot_100";
     public static final String KHO_BAU_BOT_1000 = "KhoBau_bot_1000";
     public static final String KHO_BAU_BOT_10000 = "KhoBau_bot_10000";
     public static final String KHO_BAU_NUM_BOT_100 = "KhoBau_num_bot_100";
     public static final String KHO_BAU_NUM_BOT_1000 = "KhoBau_num_bot_1000";
     public static final String KHO_BAU_NUM_BOT_10000 = "KhoBau_num_bot_10000";
     public static final String NU_DIEP_VIEN_SO_TIEN_NAP_TOI_THIEU_NO_HU_10K = "NuDiepVien_so_tien_nap_toi_thieu_no_hu_10k";
     public static final String NU_DIEP_VIEN_INIT_POT_VALUES = "NuDiepVien_init_pot_values";
     public static final String NU_DIEP_VIEN_TIME_X2 = "NuDiepVien_time_x2";
     public static final String NU_DIEP_VIEN_DAYS_X2 = "NuDiepVien_days_x2";
     public static final String NU_DIEP_VIEN_SO_LAN_NO_HU = "NuDiepVien_so_lan_no_hu";
     public static final String NU_DIEP_VIEN_LAST_DAY_GIO_VANG = "NuDiepVien_last_day_gio_vang";
     public static final String NU_DIEP_VIEN_BOT_100 = "NuDiepVien_bot_100";
     public static final String NU_DIEP_VIEN_BOT_1000 = "NuDiepVien_bot_1000";
     public static final String NU_DIEP_VIEN_BOT_10000 = "NuDiepVien_bot_10000";
     public static final String NU_DIEP_VIEN_NUM_BOT_100 = "NuDiepVien_num_bot_100";
     public static final String NU_DIEP_VIEN_NUM_BOT_1000 = "NuDiepVien_num_bot_1000";
     public static final String NU_DIEP_VIEN_NUM_BOT_10000 = "NuDiepVien_num_bot_10000";
     private static Properties prop = new Properties();
     private static String basePath = VBeePath.basePath;

     public static void reload() {
          try {
               InputStream input = new FileInputStream(basePath.concat(CONFIG_GAME_PATH));
               prop.load(input);
          } catch (FileNotFoundException var1) {
               Debug.trace("File config/config_game.properties not found");
          } catch (IOException var2) {
               Debug.trace("Load config/config_game.properties error");
          }

     }

     public static String getValueString(String key) {
          return prop.getProperty(key);
     }

     public static int getIntValue(String key) {
          return Integer.parseInt(getValueString(key));
     }

     public static int getIntValue(String key, int defaultValue) {
          return prop.containsKey(key) ? getIntValue(key) : defaultValue;
     }
}
