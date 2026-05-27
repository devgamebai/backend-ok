package com.vinplay.usercore.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.statics.Consts;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class GameThirdPartyInit {
	
	private static final Logger logger = Logger.getLogger(GameThirdPartyInit.class);
	
	public static String enviroment;
	
	public static int NUM_ACCOUNT_REG = "dev".equals(enviroment) ? 200 : 1000;
	
	public static String CREATE_USER_TIME = "20";
	/* start SA init config */
	public static String SA_SECRET_KEY;
	public static String SA_MD5_KEY;
	public static String SA_ENCRYPT_KEY;
	public static String SA_GENERIC_API_URL;
	public static String SA_GET_BET_API_URL;
	public static String SA_CLIENT;
	public static String SA_LOBBY_CODE;
	/* end SA init config */

	/* start eBET init config */
	public static String PRIVATE_KEY;
	public static String PUBLIC_KEY;
	public static String HOST_URL;
	public static Integer CHANNEL_ID;
	public static boolean EBET_STATUS;
	/* end eBET init config */

	/* start GG-Banca init config */
	public static String CAGENT;
	public static String ENCRYPT_KEY;
	public static String MD5_KEY;
	public static String TRANSFER_WALLET_URL;
	public static String REPORT_URL;
	public static String GG_START_ID_PRO;
	public static String GG_NUM_IDLE_RECORD_PRO;
	public static String DG_NUM_IDLE_RECORD_PRO;
	/* end GG-Banca init config */

	/* start IBC2 init config */
	public static int IBC2_RATE = 1000;
	public static boolean IBC2_STATUS;
	public static String IBC2_URL;
	public static String IBC2_PLAYER_PREFIX;
	public static String IBC2_OPERATORID;
	public static String IBC2_VENDOR_ID;
	public static String IBC2_START_IBCID;
	public static String IBC2_SUFFIX = "";
	public static String IBC2_CURRENTCY_ID;
	/* end IBC2 init config */

	/* start DG init config */
	public static String DG_URL;
	public static String DG_PLAYER_PREFIX;
	public static String DG_OPERATORID;
	public static String DG_VENDOR_ID;
	public static String DG_START_DGID;
	public static String DG_SUFFIX;
	/* end DG init config */

	/* start PT init config */
	public static String PT_API_URL;
	public static String PT_ENTITY_NAME;
	public static String PT_KIOSK_NAME;
	public static String PT_ADMIN_NAME;
	public static String PT_PLAYER_PREFIX;
	public static String PT_API_KEY;
	public static String PT_CURRENCY;
	public static int PT_START_ACC_ID;
	public static String PT_DEFAULT_SERVER;
	public static int PT_RATE;
	public static int PT_WITHDRAW_IS_FORCED;
	public static String PT_PAGE_ITEM;
	public static int PT_IS_FROZEN;
	public static int PT_TIME_SLEEP;
	public static String MAX_PER_PAGE;
	/* end PT init config */

	/* start TD init config */
	public static String TD_USERNAME;
	public static String TD_PASSWORD;
	public static String TD_SECRETKEY;
	public static String TD_HOST_URL;
	public static String TD_HOST_URL_REPORT;
	public static String TD_TIME_CREATE_USER;
	/* end TD init config */

	/* config AG game */
	public static boolean AG_STATUS = false;
	public static String AG_CAGENT = "JG0_AGIN";
	public static String AG_MD5 = "jg06xjvDXvnT";
	public static String AG_ENCRYPT = "jg0bCQts";
	public static String AG_DATA_API_CODE;
	public static String AG_CURRENCY = "VND";
	public static String AG_AC_TYPE;
	public static String AG_ODD_TYPE;
	public static String AG_PREFIX="slt";
	public static String AG_PREFIX_USER_AGENT="WEB_LIB_GI_JG0_AGIN";
	public static String AG_API_URL;
	public static double AG_RATE = 1000;
	public static String AG_GAME_TYPE;
	public static String AG_LANG_CODE;
	public static String AG_API_FORWARD_GAME;
	public static String AG_DATA_API_URL;
	public static String AG_TIMEZONE;
	public static String AG_REPORT_CAGENT;
	/* end config AG game */
	/* config HABA game */
	public static String BrandId;
	public static String APIKey;
	public static String Hostname;

	/* start Sports book configuration initialize */
	public static String SPORTS_BOOK_PARTNER_KEY;
	public static String SPORTS_BOOK_API_SERVER_ROOT;
	public static String SPORTS_BOOK_CURRENCY;
	public static int SPORTS_BOOK_RATE;
	public static String SPORTS_BOOK_WEB_ROOT;
	public static String SPORTS_BOOK_MOBILE_ROOT;
	public static String SPORTS_BOOK_NEW_MOBILE_ROOT;
	public static String SPORTS_BOOK_OUR_SECRET_KEY;
	public static int SPORTS_BOOK_START_USER_ID;
	public static String SPORTS_BOOK_USER_NAME_PREFIX;
	public static boolean SPORTS_BOOK_MAINTENANCE_FLAG;
	/* end Sports book configuration initialize */

	// configuration SBO
	public static String SBO_OPERATOR_TOKEN;
	public static String SBO_API_URL;
	public static String SBO_SECRET_KEY;
	public static int SBO_START_USER_ID;
	public static String SBO_USER_NAME_PREFIX;
	public static String SBO_CURRENCY;
	public static int SBO_RATE;
	public static String SBO_AGENT;
	public static boolean SBO_STATUS;

	/* start WM init config */
	public static boolean WM_STATUS;
	public static int WM_RATE;
	public static String WM_URL;
	public static String WM_PLAYER_PREFIX;
	public static String WM_OPERATORID;
	public static String WM_VENDOR_ID;
	public static int WM_START_IBCID;
	public static String WM_SUFFIX;
	public static String WM_TIMEZONE;
	/* end WM init config */

	// configuration ESPORT
	public static String ESPORT_API_URL;
	public static String ESPORT_PUBLIC_TOKEN;
	public static String ESPORT_SECRET_TOKEN;
	public static String ESPORT_PARTNER_ID;
	public static String ESPORT_PRIVATE_TOKEN;
	public static int ESPORT_RATE;
	public static int ESPORT_START_USER_ID;
	public static String ESPORT_USER_NAME_PREFIX;
	public static String ESPORT_START_JOB;

	/* config SG game */
	public static String SG_URL;
	public static String SG_CERT;
	public static String SG_AGENTID;
	public static String SG_CURRENCY;
	public static String SG_LIMIT_ID;
	public static String SG_PLAYER_PREFIX;;
	public static String SG_LOG_URL;
	public static int SG_RATE;
	/* end config SG game */

	/* start BBIN init config */
	public static String BBIN_TIMEZONE;
	public static int BBIN_CUR_PAGE;
	public static int BBIN_PAGE_SIZE;
	public static String BBIN_API_URL;
	public static String BBIN_API_URL_LOGIN;
	public static String BBIN_WEBSITE_NAME;
	public static String BBIN_UPPER_NAME;
	public static String BBIN_KEY_REPORT;
	public static String BBIN_PREFIX;

	/* start V8 init config */
	public static int V8_NUM_ACCOUNT_REG;
	public static String V8_URL;
	public static String V8_PLAYER_PREFIX;
	public static String V8_OPERATORID;
	public static int V8_START_V8ID;
	public static String V8_SUFFIX;
	public static String V8_LOG_API_URL;
	public static String V8_DES_KEY;
	public static String V8_MD5_KEY;
	public static double V8_RATE;
	public static String V8_ODDS_TYPE;
	public static String V8_LINE_CODE;
	/* end V8 init config */

	/* start BOLE init config */
	public static String BOLE_URL;
	public static String BOLE_PLAYER_PREFIX;
	public static String BOLE_OPERATORID;
	public static String BOLE_START_BOLEID;
	public static String BOLE_SUFFIX;
	public static String BOLE_ACCESS_KEYID;
	public static String BOLE_ACCESS_KEYSECRET;
	public static double BOLE_RATE;
	public static String BOLE_ODDS_TYPE;
	public static String BOLE_LINE_CODE;
	public static String BOLE_START_JOB;
	/* end BOLE init config */
	
	private static void bbinInit(JSONObject props) throws JSONException {
		BBIN_TIMEZONE = props.getString("BBIN_TIMEZONE");
		BBIN_CUR_PAGE = Integer.parseInt(props.getString("BBIN_CUR_PAGE"));
		BBIN_PAGE_SIZE = Integer.parseInt(props.getString("BBIN_PAGE_SIZE"));

		BBIN_API_URL = props.getString("BBIN_API_URL");
		BBIN_API_URL_LOGIN = props.getString("BBIN_API_URL_LOGIN");
		BBIN_WEBSITE_NAME = props.getString("BBIN_WEBSITE_NAME");
		BBIN_UPPER_NAME = props.getString("BBIN_UPPER_NAME");
		BBIN_KEY_REPORT = props.getString("BBIN_KEY_REPORT");
		BBIN_PREFIX = props.getString("BBIN_PREFIX");
	}

	private void initGeneral(JSONObject props) throws JSONException {
		NUM_ACCOUNT_REG = Integer.parseInt(props.getString("ALL_NUM_ACCOUNT_REG"));
	}

	private static void esportInit(JSONObject props) throws JSONException {
		ESPORT_API_URL = props.getString("ESPORT_API_URL");
		ESPORT_PUBLIC_TOKEN = props.getString("ESPORT_PUBLIC_TOKEN");
		ESPORT_PARTNER_ID = props.getString("ESPORT_PARTNER_ID");
		ESPORT_PRIVATE_TOKEN = props.getString("ESPORT_PRIVATE_TOKEN");
		ESPORT_RATE = Integer.parseInt(props.getString("ESPORT_RATE"));
		ESPORT_START_USER_ID = Integer.parseInt(props.getString("ESPORT_START_USER_ID"));
		ESPORT_USER_NAME_PREFIX = props.getString("ESPORT_USER_NAME_PREFIX");
		ESPORT_SECRET_TOKEN = props.getString("ESPORT_SECRET_TOKEN");
		ESPORT_START_JOB = props.getString("ESPORT_START_JOB");
	}

	private static void tdInit(JSONObject props) throws JSONException {
		TD_USERNAME = props.getString("TD_USERNAME");
		TD_PASSWORD = props.getString("TD_PASSWORD");
		TD_SECRETKEY = props.getString("TD_SECRETKEY");
		TD_HOST_URL = props.getString("TD_HOST_URL");
		TD_TIME_CREATE_USER = props.getString("TD_TIME_CREATE_USER");
		TD_HOST_URL_REPORT = props.getString("TD_HOST_URL_REPORT");
	}

	private static void cmdInit(JSONObject props) throws JSONException {
		SPORTS_BOOK_API_SERVER_ROOT = props.getString("SPORTS_BOOK_API_SERVER_ROOT");
		SPORTS_BOOK_PARTNER_KEY = props.getString("SPORTS_BOOK_PARTNER_KEY");
		SPORTS_BOOK_CURRENCY = props.getString("SPORTS_BOOK_CURRENCY");
		SPORTS_BOOK_RATE = Integer.parseInt(props.getString("SPORTS_BOOK_RATE"));
		SPORTS_BOOK_WEB_ROOT = props.getString("SPORTS_BOOK_WEB_ROOT");
		SPORTS_BOOK_MOBILE_ROOT = props.getString("SPORTS_BOOK_MOBILE_ROOT");
		SPORTS_BOOK_NEW_MOBILE_ROOT = props.getString("SPORTS_BOOK_NEW_MOBILE_ROOT");
		SPORTS_BOOK_OUR_SECRET_KEY = props.getString("SPORTS_BOOK_OUR_SECRET_KEY");
		SPORTS_BOOK_START_USER_ID = Integer.parseInt(props.getString("SPORTS_BOOK_START_USER_ID"));
		SPORTS_BOOK_USER_NAME_PREFIX = props.getString("SPORTS_BOOK_USER_NAME_PREFIX");
		SPORTS_BOOK_MAINTENANCE_FLAG = "1".equals(props.getString("SPORTS_BOOK_MAINTENANCE_FLAG"))?  true : false;
	}

	private static void sboInit(JSONObject props) throws JSONException {
		SBO_CURRENCY = props.has("SBO_CURRENCY") ? props.getString("SBO_CURRENCY") : null;
		SBO_OPERATOR_TOKEN = props.has("SBO_OPERATOR_TOKEN") ? props.getString("SBO_OPERATOR_TOKEN") : null;
		SBO_API_URL = props.has("SBO_API_URL") ? props.getString("SBO_API_URL") : null;
		SBO_START_USER_ID = props.has("SBO_START_USER_ID") ? Integer.parseInt(props.getString("SBO_START_USER_ID")) : 0;
		SBO_USER_NAME_PREFIX = props.has("SBO_USER_NAME_PREFIX") ? props.getString("SBO_USER_NAME_PREFIX") : null;
		SBO_SECRET_KEY = props.has("SBO_SECRET_KEY") ? props.getString("SBO_SECRET_KEY") : null;
		SBO_RATE = props.has("SBO_RATE") ? Integer.parseInt(props.getString("SBO_RATE")) : 0;
		SBO_STATUS = props.has("SBO_STATUS") ? Boolean.parseBoolean(props.getString("SBO_STATUS")) : false;
		SBO_AGENT = props.has("SBO_AGENT") ? props.getString("SBO_AGENT") : null;
	}

	private static void habaInit(JSONObject props) throws JSONException {
		// HABA
		BrandId = props.getString("BRAND_ID_HABA");
		APIKey = props.getString("API_KEY_HABA");
		Hostname = props.getString("HOST_NAME_HABA");
	}

	private static void ebetInit(JSONObject props) throws JSONException {
		// EBET
		EBET_STATUS = props.has("EBET_STATUS") ? Boolean.parseBoolean(props.getString("EBET_STATUS")) : false;
		PRIVATE_KEY = props.has("EBET_PRIVATE_KEY") ? props.getString("EBET_PRIVATE_KEY") : null;
		PUBLIC_KEY = props.has("EBET_PUBLIC_KEY") ? props.getString("EBET_PUBLIC_KEY") : null;
		HOST_URL = props.has("EBET_HOST_URL") ? props.getString("EBET_HOST_URL") : null;
		CHANNEL_ID = props.has("EBET_CHANNEL_ID") ? Integer.parseInt(props.getString("EBET_CHANNEL_ID")) : 0;
	}

	private static void ptInit(JSONObject propsConfig) throws JSONException {
		// Init PT config
		PT_DEFAULT_SERVER = propsConfig.getString("PT_DEFAULT_SERVER");
		PT_API_URL = propsConfig.getString("PT_API_URL");
		PT_ENTITY_NAME = propsConfig.getString("PT_ENTITY_NAME");
		PT_KIOSK_NAME = propsConfig.getString("PT_KIOSK_NAME");
		PT_ADMIN_NAME = propsConfig.getString("PT_ADMIN_NAME");
		PT_PLAYER_PREFIX = propsConfig.getString("PT_PLAYER_PREFIX");
		PT_API_KEY = propsConfig.getString("PT_API_KEY");
		PT_CURRENCY = propsConfig.getString("PT_CURRENCY");
		PT_START_ACC_ID = Integer.parseInt(propsConfig.getString("PT_START_ACC_ID"));
		PT_DEFAULT_SERVER = propsConfig.getString("PT_DEFAULT_SERVER");

		PT_RATE = Integer.parseInt(propsConfig.getString("PT_RATE"));
		PT_WITHDRAW_IS_FORCED = Integer.parseInt(propsConfig.getString("PT_WITHDRAW_IS_FORCED"));
		PT_PAGE_ITEM = propsConfig.getString("PT_PAGE_ITEM");
		PT_IS_FROZEN = Integer.parseInt(propsConfig.getString("PT_IS_FROZEN"));
		PT_TIME_SLEEP = Integer.parseInt(propsConfig.getString("PT_TIME_SLEEP"));
		MAX_PER_PAGE = propsConfig.getString("MAX_PER_PAGE");
	}

	private static void ibc2Init(JSONObject props) throws JSONException {
		IBC2_STATUS = props.getInt("STATUS")==0 ? false : true;
		IBC2_URL = props.getString("IBC2_URL");
		IBC2_PLAYER_PREFIX = props.getString("IBC2_PLAYER_PREFIX");
		IBC2_OPERATORID = props.getString("IBC2_OPERATORID");
		IBC2_VENDOR_ID = props.getString("IBC2_VENDOR_ID");
		// START_IBCID + CURRENCY_ID can be stored as either String or Integer in
		// game_config JSON — tolerate both (fixes JSONException that crashed
		// thirdParty container startup).
		IBC2_START_IBCID = String.valueOf(props.get("START_IBCID"));
		IBC2_CURRENTCY_ID = String.valueOf(props.get("CURRENCY_ID"));
		IBC2_SUFFIX = props.getString("IBC2_SUFFIX");
	}

	private static void ggInit(JSONObject props) throws JSONException {
		CAGENT = props.getString("GG_CAGENT");
		ENCRYPT_KEY = props.getString("GG_ENCRYPT_KEY");
		MD5_KEY = props.getString("GG_MD5_KEY");
		TRANSFER_WALLET_URL = props.getString("GG_TRANSFER_WALLET_URL");
		REPORT_URL = props.getString("GG_REPORT_URL");
		GG_NUM_IDLE_RECORD_PRO = props.getString("GG_NUM_IDLE_RECORD");
		GG_START_ID_PRO = props.getString("GG_START_ID");
	}

	private static void agInit(JSONObject props) throws JSONException {
		// Note : AG just have only one environment
		AG_STATUS = props.getBoolean("AG_STATUS");
		AG_REPORT_CAGENT = props.getString("AG_REPORT_CAGENT");
		AG_DATA_API_URL = props.getString("AG_DATA_API_URL");
		AG_DATA_API_CODE = props.getString("AG_DATA_API_CODE");
		AG_CURRENCY = props.getString("AG_CURRENCY");
		AG_LANG_CODE = props.getString("AG_LANG_CODE");
		AG_GAME_TYPE = props.getString("AG_GAME_TYPE");
		AG_PREFIX = props.getString("AG_PREFIX");
		AG_TIMEZONE = props.getString("AG_TIMEZONE");
		AG_PREFIX_USER_AGENT = props.getString("AG_PREFIX_USER_AGENT");

		AG_CAGENT = props.getString("AG_CAGENT");
		AG_MD5 = props.getString("AG_MD5");
		AG_ENCRYPT = props.getString("AG_ENCRYPT");
		AG_AC_TYPE = props.getString("AG_AC_TYPE");
		AG_ODD_TYPE = props.getString("AG_ODD_TYPE");
		AG_API_URL = props.getString("AG_API_URL");
		AG_API_FORWARD_GAME = props.getString("AG_API_FORWARD_GAME");

	}

	/* start SA init config */
	private static void saInit(JSONObject props) throws JSONException {
		SA_SECRET_KEY = props.getString("SA_SECRET_KEY");
		SA_MD5_KEY = props.getString("SA_MD5_KEY");
		SA_ENCRYPT_KEY = props.getString("SA_ENCRYPT_KEY");
		SA_GENERIC_API_URL = props.getString("SA_GENERIC_API_URL");
		SA_GET_BET_API_URL = props.getString("SA_GET_BET_API_URL");
		SA_CLIENT = props.getString("SA_CLIENT");
		SA_LOBBY_CODE = props.getString("SA_LOBBY_CODE");
	}

	/* start SG init config */
	private static void sgInit(JSONObject props) throws JSONException {
		SG_URL = props.getString("SG_URL");
		SG_CERT = props.getString("SG_CERT");
		SG_AGENTID = props.getString("SG_AGENTID");
		SG_CURRENCY = props.getString("SG_CURRENCY");
		SG_LIMIT_ID = props.getString("SG_LIMIT_ID");
		SG_PLAYER_PREFIX = props.getString("SG_PLAYER_PREFIX");
		SG_LOG_URL = props.getString("SG_LOG_URL");
		SG_RATE = Integer.parseInt(props.getString("SG_RATE"));
	}

	private static void wmInit(JSONObject props) throws JSONException {
		WM_STATUS = props.getBoolean("WM_STATUS");
		WM_RATE = props.getInt("WM_RATE");
		WM_URL = props.getString("WM_URL");
		WM_PLAYER_PREFIX = props.getString("WM_PLAYER_PREFIX");
		WM_OPERATORID = props.getString("WM_OPERATORID");
		WM_VENDOR_ID = props.getString("WM_VENDOR_ID");
		WM_START_IBCID =  props.getInt("WM_START_IBCID");
		WM_SUFFIX = props.getString("WM_SUFFIX");// "_test";
		WM_TIMEZONE = props.getString("WM_TIMEZONE");
	}

	private static void v8Init(JSONObject props) throws JSONException {
		V8_NUM_ACCOUNT_REG = Integer.parseInt(props.getString("V8_NUM_ACCOUNT_REG"));
		V8_URL = props.getString("V8_URL");
		V8_PLAYER_PREFIX = props.getString("V8_PLAYER_PREFIX");
		V8_OPERATORID = props.getString("V8_OPERATORID");
		V8_START_V8ID = Integer.parseInt(props.getString("V8_START_V8ID"));
		V8_SUFFIX = props.getString("V8_SUFFIX");
		V8_LOG_API_URL = props.getString("V8_LOG_API_URL");
		V8_DES_KEY = props.getString("V8_DES_KEY");
		V8_MD5_KEY = props.getString("V8_MD5_KEY");
		V8_RATE = Double.parseDouble(props.getString("V8_RATE"));
		V8_ODDS_TYPE = props.getString("V8_ODDS_TYPE");
		V8_LINE_CODE = props.getString("V8_LINE_CODE");
	}

	private static void boleInit(JSONObject props) throws JSONException {
		BOLE_URL = props.getString("BOLE_URL");
		BOLE_PLAYER_PREFIX = props.getString("BOLE_PLAYER_PREFIX");
		BOLE_OPERATORID = props.getString("BOLE_OPERATORID");
		BOLE_START_BOLEID = props.getString("BOLE_START_BOLEID");
		BOLE_SUFFIX = props.getString("BOLE_SUFFIX");
		BOLE_ACCESS_KEYID = props.getString("BOLE_ACCESS_KEYID");
		BOLE_ACCESS_KEYSECRET = props.getString("BOLE_ACCESS_KEYSECRET");
		BOLE_RATE = Double.parseDouble(props.getString("BOLE_RATE"));
		BOLE_ODDS_TYPE = props.getString("BOLE_ODDS_TYPE");
		BOLE_LINE_CODE = props.getString("BOLE_LINE_CODE");
		BOLE_START_JOB = props.getString("BOLE_START_JOB");
	}

	/** Functional interface for a provider init that may throw JSONException. */
	@FunctionalInterface
	private interface InitTask { void run() throws Exception; }

	/** Runs a provider init; logs + swallows on failure so one bad section
	 *  doesn't crash the container. Provider stays at its default (disabled) state. */
	private static void safeInit(String name, InitTask task) {
		try {
			task.run();
		} catch (Throwable t) {
			System.err.println("[GameThirdPartyInit] SKIP provider '" + name +
					"' due to config issue: " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}
	}

	private static String initGameAccess() throws IOException {
		StringBuilder fullStr = new StringBuilder();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(Consts.GAME_CONFIG_FILE))) {
			String line;
			while ((line = br.readLine()) != null) {
				fullStr.append(line);
			}
		}
		return fullStr.toString();
	}
	
	public static void init() throws SQLException, JSONException, ParseException {
		String thirdConfig = null;
		try {
			thirdConfig = initGameAccess();
		} catch (IOException e) {
			GameConfigDao dao = new GameConfigDaoImpl();
			thirdConfig = dao.getGameCommon("thirdparty_game");
		}
		JSONObject propsConfig = new JSONObject(thirdConfig);
		enviroment = propsConfig.getString("environment");
		CREATE_USER_TIME = propsConfig.getString("CREATE_USER_TIME");
		JSONObject ibc2 = propsConfig.has("ibc2") ? propsConfig.getJSONObject("ibc2") : new JSONObject();
		JSONObject wm = propsConfig.has("wm") ? propsConfig.getJSONObject("wm") : new JSONObject();
		JSONObject ag = propsConfig.has("ag") ? propsConfig.getJSONObject("ag") : new JSONObject();
		JSONObject cmd = propsConfig.has("cmd") ? propsConfig.getJSONObject("cmd") : new JSONObject();
		JSONObject sbo = propsConfig.has("sbo") ? propsConfig.getJSONObject("sbo") : new JSONObject();
		JSONObject ebet = propsConfig.has("ebet") ? propsConfig.getJSONObject("ebet") : new JSONObject();
		// Each provider init wrapped in try/catch — config sections in the DB are
		// often partial / wrong-typed, and we don't want one dead provider to
		// crash the entire container. Failed ones just log and leave the
		// provider disabled (matches GameCommon.init pattern).
		safeInit("ibc2", () -> ibc2Init(ibc2));
		safeInit("ag",   () -> agInit(ag));
		safeInit("wm",   () -> wmInit(wm));
		safeInit("cmd",  () -> cmdInit(cmd));
		safeInit("sbo",  () -> sboInit(sbo));
		safeInit("ebet", () -> ebetInit(ebet));
//		// init PT
//		ptInit(propsConfig);
//		// init AG
//		agInit(propsConfig);
//		// init SA
//		saInit(propsConfig);
//		// init GG
//		ggInit(propsConfig);
//
//		// esport
//		esportInit(propsConfig);
//		// init SG
//		sgInit(propsConfig);
//		// init BBin
//		bbinInit(propsConfig);
//		// init V8
//		v8Init(propsConfig);
//		// init Bole
//		boleInit(propsConfig);
		logger.info("Init Data successfull");
	}

	public static String getValueStr(String key) throws KeyNotFoundException {
		HazelcastInstance instance = HazelcastClientFactory.getInstance();
		IMap map = instance.getMap("cacheConfig");
		if (map.containsKey(key)) {
			return (String) map.get(key);
		}
		throw new KeyNotFoundException();
	}

	public static int getValueInt(String key) throws KeyNotFoundException, NumberFormatException {
		HazelcastInstance instance = HazelcastClientFactory.getInstance();
		IMap map = instance.getMap("cacheConfig");
		if (map.containsKey(key)) {
			return Integer.parseInt((String) map.get(key));
		}
		throw new KeyNotFoundException();
	}

	public static double getValueDouble(String key) throws KeyNotFoundException, NumberFormatException {
		HazelcastInstance instance = HazelcastClientFactory.getInstance();
		IMap map = instance.getMap("cacheConfig");
		if (map.containsKey(key)) {
			return Double.parseDouble((String) map.get(key));
		}
		throw new KeyNotFoundException();
	}


}
