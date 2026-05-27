/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  okhttp3.Call
 *  okhttp3.Callback
 *  okhttp3.MediaType
 *  okhttp3.OkHttpClient
 *  okhttp3.OkHttpClient$Builder
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 *  okhttp3.WebSocket
 *  okhttp3.WebSocketListener
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.minigame;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import game.modules.minigame.model.BetInfo;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomTaiXiuLive;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONException;
import org.json.JSONObject;

public class TaiXiuLiveModule
extends BaseClientRequestHandler {
    public MGRoomTaiXiuLive room = new MGRoomTaiXiuLive("Tai Xiu Live");
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable serverReadyTask = new ServerReadyTask();
    private final Runnable botChatTask = new ScheduleBotChatTask();
    private final CacheService cacheService = new CacheServiceImpl();
    private int count = -1;
    private int gameState = 0;
    private Date startAtDate = null;
    private Long timeBet;
    private Long timeBetCountdown;
    private boolean serverReady = false;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);
    private static TaiXiuLiveModule _instance;
    private String topBots;
    private int result = -1;
    public String token = "";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    int totalUserTai = 0;
    int totalUserXiu = 0;
    int totalAmountTai = 0;
    int totalAmountXiu = 0;

    public static TaiXiuLiveModule getInstance() {
        return _instance;
    }

    public void init() {
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.botChatTask, 1000, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.serverReadyTask, 10, TimeUnit.SECONDS);
        Debug.trace((Object[])new Object[]{"SERVER READY TASK RUNNING..."});
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long currentTimeMillis = System.currentTimeMillis();
        long initialDelay12AM = this.calculateInitialDelay(12, 0, 0);
        scheduler.scheduleAtFixedRate(() -> this.getLink2(), initialDelay12AM, 10800000L, TimeUnit.MILLISECONDS);
        this.getLink2();
        this.listenGo();
    }

    private void getLink2() {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse((String)"application/json");
        RequestBody body = RequestBody.create((MediaType)mediaType, (String)"{\"game\":\"go88\",\"username\":\"bongda7778\",\"password\":\"123456a\"}");
        Request request = new Request.Builder().url("https://game.vnpayz.com/").method("POST", body).addHeader("Content-Type", "application/json").addHeader("api-key", "11b6140e-7579-4ec5-bb8b-874ef1d956a5").build();
        client.newCall(request).enqueue(new Callback(){

            public void onFailure(Call call, IOException e) {
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.has("data")) {
                            String token;
                            TaiXiuLiveModule.getInstance().token = token = jsonObject.getJSONObject("data").getString("token");
                            if (!TaiXiuLiveModule.getInstance().room.getTokenLiveFromDb().isEmpty()) {
                                TaiXiuLiveModule.getInstance().room.clearTokenLiveToDb();
                            }
                            TaiXiuLiveModule.getInstance().room.insertTokenLiveToDb(token);
                            TaiXiuLiveModule.this.listenGo();
                        }
                    }
                    catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private static void getLink() {
        MessageDigest md;
        String userName = "vcl" + TaiXiuLiveModule.generateRandomString(8);
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] md5Bytes = md.digest(userName.getBytes());
        StringBuilder result = new StringBuilder();
        for (byte b : md5Bytes) {
            result.append(String.format("%02x", b));
        }
        String parameters = "{\"fullname\":\"" + userName + "\",\"username\":\"" + userName + "\",\"password\":\"123456\",\"app_id\":\"go88com\",\"avatar\":\"Avatar50\",\"os\":\"Windows\",\"device\":\"Computer\",\"browser\":\"chrome\",\"fg\":\"" + result.toString().substring(0, 32) + "\",\"aff_id\":\"go88com\"}";
        String url = "https://bodergatez.dsrcgoms.net/user/register.aspx";
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection)obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("authority", "bodergatez.dsrcgoms.net");
            con.setRequestProperty("accept", "*/*");
            con.setRequestProperty("accept-language", "en-US,en;q=0.9,vi;q=0.8");
            con.setRequestProperty("content-type", "text/plain;charset=UTF-8");
            con.setRequestProperty("dnt", "1");
            con.setRequestProperty("origin", "https://i.go88.us/");
            con.setRequestProperty("referer", "https://i.go88.us/");
            con.setRequestProperty("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Microsoft Edge\";v=\"121\", \"Chromium\";v=\"121\"");
            con.setRequestProperty("sec-ch-ua-mobile", "?0");
            con.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
            con.setRequestProperty("sec-fetch-dest", "empty");
            con.setRequestProperty("sec-fetch-mode", "cors");
            con.setRequestProperty("sec-fetch-site", "cross-site");
            con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0");
            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream());){
                wr.writeBytes(parameters);
                wr.flush();
            }
            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));){
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                Gson gson = new Gson();
                JsonObject jsonResponse = (JsonObject)gson.fromJson(response.toString(), JsonObject.class);
                if ("OK".equals(jsonResponse.get("status").getAsString())) {
                    JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                    if (dataArray.size() > 0) {
                        String token;
                        JsonObject dataObject = dataArray.get(0).getAsJsonObject();
                        TaiXiuLiveModule.getInstance().token = token = dataObject.get("token").getAsString();
                        if (!TaiXiuLiveModule.getInstance().room.getTokenLiveFromDb().isEmpty()) {
                            TaiXiuLiveModule.getInstance().room.clearTokenLiveToDb();
                        }
                        TaiXiuLiveModule.getInstance().room.insertTokenLiveToDb(token);
                    } else {
                        System.out.println("No data found in the response.");
                    }
                } else {
                    System.out.println("Registration was not successful: " + jsonResponse.get("message").getAsString());
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateRandomString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    private static long getNextExecutionTime(int hour, int minute, int second) {
        long todayExecutionTime;
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > (todayExecutionTime = TaiXiuLiveModule.getCurrentDateMillis(hour, minute, second))) {
            return TaiXiuLiveModule.getCurrentDateMillis(hour, minute, second) + TimeUnit.DAYS.toMillis(1L);
        }
        return todayExecutionTime;
    }

    private static long getCurrentDateMillis(int hour, int minute, int second) {
        long currentTimeMillis = System.currentTimeMillis();
        long currentDateMillis = currentTimeMillis - currentTimeMillis % 86400000L;
        return currentDateMillis + (long)(hour * 60 * 60 * 1000) + (long)(minute * 60 * 1000) + (long)(second * 1000);
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    public void listenGo() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("wss://ws-tx-g8.quayso.tv/").build();
        WebSocketListener listener = new WebSocketListener(){

            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("Connected to WebSocket!");
            }

            public void onMessage(WebSocket webSocket, String text) {
                JsonObject gameResult;
                JsonObject gameReportUser;
                JsonObject createGame;
                JsonObject table;
                JsonParser jsonParser = new JsonParser();
                JsonObject jo = (JsonObject)jsonParser.parse(text);
                if (jo.has("create-game") && jo.get("create-game").isJsonObject() && (table = (createGame = jo.getAsJsonObject("create-game")).getAsJsonObject("table")).get("id").getAsString().equals("101")) {
                    System.out.println("create-game: " + createGame);
                    TaiXiuLiveModule.this.count = -1;
                    TaiXiuLiveModule.this.result = -1;
                    TaiXiuLiveModule.this.gameState = 1;
                    TaiXiuLiveModule.this.startAtDate = new Date(createGame.get("startAt").getAsString());
                    TaiXiuLiveModule.this.timeBet = createGame.get("timeBet").getAsLong();
                    TaiXiuLiveModule.this.timeBetCountdown = createGame.get("timeBetCountdown").getAsLong();
                    TaiXiuLiveModule.this.room.startNewGame(createGame.get("roundCode").getAsLong(), TaiXiuLiveModule.this.timeBet);
                }
                if (TaiXiuLiveModule.this.startAtDate != null && jo.has("game-endbet") && jo.get("game-endbet").isJsonObject()) {
                    if (TaiXiuLiveModule.this.gameState == 1) {
                        TaiXiuLiveModule.this.room.finish();
                    }
                    TaiXiuLiveModule.this.gameState = 0;
                    JsonObject gameEndBet = jo.getAsJsonObject("game-endbet");
                    table = gameEndBet.getAsJsonObject("table");
                    if (table.get("id").getAsString().equals("101")) {
                        System.out.println("game-endbet: " + gameEndBet);
                    }
                }
                if (TaiXiuLiveModule.this.startAtDate != null && jo.has("game-report-user") && jo.get("game-report-user").isJsonObject() && (gameReportUser = jo.getAsJsonObject("game-report-user")).get("gameId").getAsString().equals("101")) {
                    JsonArray dataArray = gameReportUser.getAsJsonArray("data");
                    BetInfo[] betInfos = new BetInfo[dataArray.size()];
                    for (int i = 0; i < dataArray.size(); ++i) {
                        JsonObject betInfoObj = dataArray.get(i).getAsJsonObject();
                        String betType = betInfoObj.get("betType").getAsString();
                        int totalUser = betInfoObj.get("totalUser").getAsInt();
                        long totalAmount = Math.round(betInfoObj.get("totalAmount").getAsDouble() * 1000.0);
                        betInfos[i] = new BetInfo(betType, totalUser, totalAmount);
                    }
                    TaiXiuLiveModule.this.room.betInfo(betInfos);
                    System.out.println("game-report-user: " + gameReportUser);
                }
                if (TaiXiuLiveModule.this.startAtDate != null && jo.has("game-result") && jo.get("game-result").isJsonObject() && (table = (gameResult = jo.getAsJsonObject("game-result")).getAsJsonObject("table")).get("id").getAsString().equals("101")) {
                    System.out.println("game-result: " + gameResult);
                    String resultRaw = gameResult.get("resultRaw").getAsString();
                    String[] tokens = resultRaw.split(",");
                    short[] shorts = new short[tokens.length];
                    long totalDice = 0L;
                    for (int i = 0; i < tokens.length; ++i) {
                        shorts[i] = Short.parseShort(tokens[i].trim());
                        totalDice += (long)shorts[i];
                    }
                    TaiXiuLiveModule.this.room.result(shorts);
                    if (totalDice >= 10L) {
                        TaiXiuLiveModule.this.result = 1;
                    } else {
                        TaiXiuLiveModule.this.result = 0;
                    }
                    try {
                        TaiXiuLiveModule.this.room.saveResultTransaction(shorts, TaiXiuLiveModule.this.result);
                        TaiXiuLiveModule.this.room.reward(TaiXiuLiveModule.this.result);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("Closing WebSocket...");
                webSocket.close(1000, null);
            }

            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                TaiXiuLiveModule.this.listenGo();
                System.out.println("WebSocket connection failure: " + t.getMessage());
            }
        };
        WebSocket webSocket = client.newWebSocket(request, listener);
        webSocket.send("{\"connect\":\"" + this.token + "\"}");
    }

    private void userDis(User user) {
        MGRoom room = (MGRoom)user.getProperty("MGROOM_TAI_XIU_INFO");
        if (room != null) {
            room.quitRoom(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 29000: {
                if (!this.room.joinRoom(user)) break;
                this.room.sendGameInfo("/live_game.html?token=" + this.token, user);
                break;
            }
            case 29001: {
                this.room.quitRoom(user);
                break;
            }
            case 29006: {
                try {
                    this.room.bet(user, dataCmd);
                    break;
                }
                catch (IOException | InterruptedException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private synchronized void gameLoop() {
        try {
            if (this.gameState == 1) {
                ++this.count;
                this.room.updateRemainTime(this.timeBetCountdown - (long)this.count);
                if (this.count < 45 & this.count > 1) {
                    BetInfo[] betInfos = new BetInfo[2];
                    Random random = new Random();
                    this.totalAmountTai += random.nextInt(5000000);
                    this.totalAmountXiu += random.nextInt(5000000);
                    this.totalUserTai += random.nextInt(10);
                    this.totalUserXiu += random.nextInt(10);
                    betInfos[0] = new BetInfo("BIG", this.totalUserTai, this.totalAmountTai);
                    betInfos[1] = new BetInfo("SMALL", this.totalUserXiu, this.totalAmountXiu);
                    this.room.betInfo(betInfos);
                } else if (this.count <= 1) {
                    this.totalUserTai = 0;
                    this.totalUserXiu = 0;
                    this.totalAmountTai = 0;
                    this.totalAmountXiu = 0;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Debug.trace((Object[])new Object[]{"Exception: " + e.getMessage(), e});
        }
    }

    public long calculateInitialDelay(int targetHour, int targetMinute, int targetSecond) {
        Calendar now = Calendar.getInstance();
        Calendar nextExecutionTime = Calendar.getInstance();
        nextExecutionTime.set(11, targetHour);
        nextExecutionTime.set(12, targetMinute);
        nextExecutionTime.set(13, targetSecond);
        if (nextExecutionTime.before(now)) {
            nextExecutionTime.add(5, 1);
        }
        return nextExecutionTime.getTimeInMillis() - now.getTimeInMillis();
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                if (TaiXiuLiveModule.this.timeBetCountdown != null) {
                    TaiXiuLiveModule.this.gameLoop();
                }
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private final class ServerReadyTask
    implements Runnable {
        private ServerReadyTask() {
        }

        @Override
        public void run() {
            if (!TaiXiuLiveModule.this.serverReady) {
                Debug.trace((Object[])new Object[]{"START MINI GAME - Tx Live"});
                TaiXiuLiveModule.this.serverReady = true;
                ScheduleBotTask t = new ScheduleBotTask();
                TaiXiuLiveModule.this.executor.execute(t);
            }
        }
    }

    private final class ScheduleBotTask
    extends Thread {
        private ScheduleBotTask() {
        }

        @Override
        public void run() {
            try {
                Debug.trace((Object[])new Object[]{"Tx Live Schedule bot running ..."});
                Debug.trace((Object[])new Object[]{"Tx Live Schedule bot finished ..."});
            }
            catch (Exception ex) {
                Debug.trace((Object[])new Object[]{ex.getMessage()});
            }
        }
    }

    private final class ScheduleBotChatTask
    extends Thread {
        private ScheduleBotChatTask() {
        }

        @Override
        public void run() {
            try {
                Debug.trace((Object[])new Object[]{"Tx Live Schedule bot chat running ..."});
                Debug.trace((Object[])new Object[]{"Tx Live Schedule bot chat finished ..."});
            }
            catch (Exception ex) {
                Debug.trace((Object[])new Object[]{ex.getMessage()});
            }
        }
    }
}

