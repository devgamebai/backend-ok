package game.third.usecase.config;

import com.google.gson.Gson;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import java.sql.SQLException;

/**
 * Loads third-party game integration configs.
 *
 * GSC+ config is env-driven: if GSC_OPERATOR_URL is set in the environment,
 * all fields are built from env vars so staging/production can be switched
 * without touching the database. Otherwise falls back to the `game_config`
 * table (row name='gsc_game') for backward compatibility.
 *
 * Env vars: GSC_OPERATOR_URL, GSC_OPERATOR_CODE, GSC_SECRET_KEY,
 * GSC_LOBBY_URL, GSC_CURRENCY, GSC_USERNAME, GSC_PASSWORD,
 * GSC_EXCHANGE_RATE, GSC_TAX.
 */
public class ThirdPartyLoad {
    private static GSCConfig gscConfig;
    private static PeachTeaConfig peachTeaConfig;
    private static Game568winConfig game568winConfig;

    public static GSCConfig getGscConfig() {
        if (gscConfig == null) {
            gscConfig = buildFromEnv();
            if (gscConfig == null) {
                GameConfigDaoImpl gameConfigDao = new GameConfigDaoImpl();
                try {
                    String configStr = gameConfigDao.getGameCommon("gsc_game");
                    gscConfig = new Gson().fromJson(configStr, GSCConfig.class);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return gscConfig;
    }

    /** Returns a fully-populated GSCConfig if env vars are set, else null. */
    private static GSCConfig buildFromEnv() {
        String url = System.getenv("GSC_OPERATOR_URL");
        if (url == null || url.isEmpty()) return null;

        GSCConfig c = new GSCConfig();
        c.setOperatorUrl(url);
        c.setOperatorCode(envOr("GSC_OPERATOR_CODE", "G7A1"));
        c.setSecretKey(envOr("GSC_SECRET_KEY", ""));
        c.setOperatorLobbyUrl(envOr("GSC_LOBBY_URL", ""));
        c.setCurrency(envOr("GSC_CURRENCY", "IDR2"));
        c.setUsername(envOr("GSC_USERNAME", "SUNKR"));
        c.setPassword(envOr("GSC_PASSWORD", ""));
        try {
            c.setExchangeRate(Double.parseDouble(envOr("GSC_EXCHANGE_RATE", "1.0")));
        } catch (NumberFormatException e) {
            c.setExchangeRate(1.0);
        }
        try {
            c.setTax(Integer.parseInt(envOr("GSC_TAX", "0")));
        } catch (NumberFormatException e) {
            c.setTax(0);
        }
        return c;
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static PeachTeaConfig getPeachTeaConfig() {
        if (peachTeaConfig == null) {
            GameConfigDaoImpl gameConfigDao = new GameConfigDaoImpl();
            try {
                String configStr = gameConfigDao.getGameCommon("peachTea_game");
                peachTeaConfig = new Gson().fromJson(configStr, PeachTeaConfig.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return peachTeaConfig;
    }

    public static Game568winConfig getGame568winConfig() {
        if (game568winConfig == null) {
            GameConfigDaoImpl gameConfigDao = new GameConfigDaoImpl();
            try {
                String configStr = gameConfigDao.getGameCommon("game568win_game");
                game568winConfig = new Gson().fromJson(configStr, Game568winConfig.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return game568winConfig;
    }
}
