/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
 *  com.vinplay.dal.service.LoDeService
 *  com.vinplay.dal.service.impl.LoDeServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.utils.TelegramAlert
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.Response
 *  org.apache.http.util.TextUtils
 */
package game.modules.minigame;

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
import com.vinplay.dal.service.LoDeService;
import com.vinplay.dal.service.impl.LoDeServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.utils.TelegramAlert;
import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.minigame.cmd.rev.LotteryCmd;
import game.modules.minigame.model.LotteryMode;
import game.modules.minigame.model.LotteryResult;
import game.modules.minigame.room.MGRoom;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.util.TextUtils;

public class LotteryModule
extends BaseClientRequestHandler {
    private static LotteryModule _instance;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private UserService userService = new UserServiceImpl();
    private LoDeService loDeService = new LoDeServiceImpl();
    private String moneyTypeStr;

    public static LotteryModule getInstance() {
        return _instance;
    }

    public void init() {
        Debug.trace((Object[])new Object[]{"SERVER READY TASK RUNNING..."});
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
        this.moneyTypeStr = "vin";
        // SUN-LOTTERY PR-3: when LOTTERY_ENGINE_ENABLED=1, the lottery-api
        // Spring module owns the 18:35 Hanoi scrape via DrawScheduler (cron
        // zone="Asia/Ho_Chi_Minh"). We MUST NOT also start the legacy
        // scheduler in that case — both would scrape and double-credit.
        // When the flag is OFF, legacy scheduler runs as before.
        if (isLotteryEngineBridgeEnabled()) {
            Debug.trace((Object[])new Object[]{"LotteryModule: engine bridge ENABLED — legacy scheduler suppressed"});
            return;
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long initialDelay18h40PM = this.calculateInitialDelay(18, 35, 0);
        scheduler.scheduleAtFixedRate(LotteryModule::getResultLottery, initialDelay18h40PM, 86400000L, TimeUnit.MILLISECONDS);
        LotteryModule.getResultLottery();
    }

    /**
     * Reflective check of {@code com.sunwinkr.lottery.api.wire.LotteryBridgeFeatureFlag.isEnabled()}.
     * Reflective so this legacy module does not pick up a hard runtime
     * dependency on the lottery-api jar — it falls back to the legacy
     * path if the lottery-api jar is absent from the classpath.
     */
    private static boolean isLotteryEngineBridgeEnabled() {
        try {
            Class<?> flag = Class.forName("com.sunwinkr.lottery.api.wire.LotteryBridgeFeatureFlag");
            Object out = flag.getMethod("isEnabled").invoke(null);
            return out instanceof Boolean && (Boolean) out;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void getResultLottery() {
        try {
            LoDeServiceImpl lotteryService = new LoDeServiceImpl();
            UserServiceImpl prvUserService = new UserServiceImpl();
            String jsonData = null;
            String url = System.getenv("LOTTERY_API_URL") != null ? System.getenv("LOTTERY_API_URL") : "http://lottery-api:49111/api/v1";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).get().build();
            Response response = client.newCall(request).execute();
            jsonData = response.body().string();
            Gson gson = new Gson();
            LotteryResult lotteryResult = (LotteryResult)gson.fromJson(jsonData, LotteryResult.class);
            String checkRs = lotteryService.getLatestResult(lotteryResult.getTime());
            if (checkRs == null) {
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                lotteryService.saveLotteryResult(jsonData, format.parse(lotteryResult.getTime()));
            } else {
                jsonData = checkRs;
            }
            lotteryResult = (LotteryResult)gson.fromJson(jsonData, LotteryResult.class);
            System.out.println(lotteryResult);
            List<LotteryMessage> lotteryMessages = lotteryService.getLotteryTicket(lotteryResult.getTime());
            for (LotteryMessage lotteryMessage : lotteryMessages) {
                // SUN-1295: prefer the per-bet snapshot stored on the row.
                // For legacy rows where the snapshot columns are NULL, the
                // overload falls back to the live LotteryMode enum (matches
                // pre-1295 behaviour for those rows only).
                long prize = LotteryModule.getPrize(lotteryResult, lotteryMessage);

                // SUN-1306: 2-record ledger split.
                // Record 1 (debit at buyTicket) is already written. Here we
                // write Record 2 (settle confirmation) \u2014 even on losses \u2014
                // with enriched detail (mode + ticket) so player/agency
                // history shows both rows for every bet.
                game.modules.minigame.model.LotteryMode betMode =
                        game.modules.minigame.model.LotteryMode.findLotteryModeById(
                                (int) lotteryMessage.getMode());
                String modeName = betMode != null ? betMode.getName() : ("Mode " + lotteryMessage.getMode());
                String detail = modeName + " \u2014 s\u1ed1 " + lotteryMessage.getTicket();
                long txTs = new Date().getTime();

                if (prize > 0L) {
                    prvUserService.updateMoney(lotteryMessage.getNickName(), prize, "vin", "LoDe",
                            "L\u00f4 \u0110\u1ec1",
                            "Th\u1eafng c\u01b0\u1ee3c L\u00f4 \u0110\u1ec1 (" + detail + ")",
                            0L, Long.valueOf(txTs), TransType.START_TRANS);
                } else {
                    // Loss: write a 0-exchange settle row directly to the
                    // log_money_user_vin queue. Balance is unchanged, but the
                    // ledger gets a "confirmation" row so the user's history
                    // is symmetric with winning tickets.
                    try {
                        com.vinplay.vbee.common.models.cache.UserCacheModel u =
                                prvUserService.getUser(lotteryMessage.getNickName());
                        long currentVin = prvUserService.getMoneyUserCache(lotteryMessage.getNickName(), "vin");
                        com.vinplay.vbee.common.messages.LogMoneyUserMessage settleLog =
                                new com.vinplay.vbee.common.messages.LogMoneyUserMessage(
                                        u != null ? u.getId() : 0,
                                        lotteryMessage.getNickName(),
                                        "K\u1ebft qu\u1ea3 L\u00f4 \u0110\u1ec1",
                                        "LoDe",
                                        currentVin,
                                        0L,
                                        "vin",
                                        "Thua c\u01b0\u1ee3c L\u00f4 \u0110\u1ec1 (" + detail + ")",
                                        0L,
                                        false,
                                        false);
                        com.vinplay.vbee.common.messagebus.MessageBusFactory.get("queue_log_money")
                                .publish("queue_log_money", settleLog, 601);
                    } catch (Throwable t) {
                        Debug.warn((Object[]) new Object[]{"LoDe loss-settle log publish failed: " + t.getMessage()});
                    }
                }
                lotteryService.updatePrize(lotteryMessage.getId(), prize);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Debug.warn((Object[])new Object[]{e});
        }
        catch (ParseException e) {
            e.printStackTrace();
            Debug.warn((Object[])new Object[]{e});
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

    private void userDis(User user) {
        MGRoom room = (MGRoom)user.getProperty("MGROOM_TAI_XIU_INFO");
        if (room != null) {
            room.quitRoom(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        // SUN-LOTTERY PR-3: when bridge enabled, delegate cmds 30000/30001
        // to the lottery-api engine via reflective bean lookup. Legacy
        // path runs when bridge is OFF — preserves rollback story.
        if (isLotteryEngineBridgeEnabled()) {
            try {
                dispatchToBridge(user, dataCmd);
                return;
            } catch (Throwable t) {
                Debug.warn((Object[])new Object[]{"Bridge dispatch failed; falling back to legacy", t});
                // Fall through to legacy on bridge failure.
            }
        }
        switch (dataCmd.getId()) {
            case 30000: {
                break;
            }
            case 30001: {
                LocalTime currentTime = LocalTime.now();
                LocalTime startTime = LocalTime.of(17, 0);
                LocalTime endTime = LocalTime.of(19, 0);
                if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                    System.out.println("Operation rejected due to time restriction.");
                    break;
                }
                this.buyTicket(user, dataCmd);
                break;
            }
        }
    }

    /**
     * Reflective dispatch to {@code com.sunwinkr.lottery.api.wire.LotteryModuleBridge}
     * via the Spring context held by {@code LotteryApiApplication}. Keeps
     * this BitZero module free of a hard Spring dependency.
     */
    private static void dispatchToBridge(User user, DataCmd dataCmd) throws Exception {
        Class<?> appCls = Class.forName("com.sunwinkr.lottery.api.LotteryApiApplication");
        Object ctx = appCls.getMethod("contextHolder").invoke(null);
        if (ctx == null) {
            throw new IllegalStateException("lottery-api Spring context not running");
        }
        Class<?> bridgeCls = Class.forName("com.sunwinkr.lottery.api.wire.LotteryModuleBridge");
        Object bridge = ctx.getClass().getMethod("getBean", Class.class).invoke(ctx, bridgeCls);
        String nickname = user.getName();
        int cmdId = dataCmd.getId();
        if (cmdId == 30000) {
            bridgeCls.getMethod("snapshot", String.class).invoke(bridge, nickname);
        } else if (cmdId == 30001) {
            game.modules.minigame.cmd.rev.LotteryCmd lc = new game.modules.minigame.cmd.rev.LotteryCmd(dataCmd);
            String num = lc.num == null ? "" : lc.num;
            bridgeCls.getMethod("bet", String.class, long.class, int.class, String.class, long.class)
                .invoke(bridge, nickname, (long) user.getId(), (int) lc.mode, num, lc.betValue);
        }
    }

    public void buyTicket(User user, DataCmd dataCmd) {
        LotteryCmd lotteryCmd = new LotteryCmd(dataCmd);
        if (TextUtils.isEmpty((CharSequence)lotteryCmd.num)) {
            return;
        }
        // SUN-1295: snapshot the rate + prize multiplier from LotteryMode at
        // PURCHASE time and store them on the lode row. Settle reads back from
        // the row, never the live enum, so any future rate/prize change can't
        // retroactively rewrite a pending bet's payout.
        LotteryMode mode = LotteryMode.findLotteryModeById((int)lotteryCmd.mode);
        if (mode == null) return;  // unknown mode \u2192 silently drop (matches legacy)
        long userBet = lotteryCmd.betValue;
        int rateAtPurchase = mode.getRate();
        int prizeMultiplierAtPurchase = mode.getPrizeMultiplier();
        long finalBetValue = userBet * (long) rateAtPurchase;

        long currentMoney = this.userService.getMoneyUserCache(user.getName(), this.moneyTypeStr);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM, yyyy HH:mm:ss", new Locale("vi", "VN"));
        TelegramAlert.sendMessage((String)("Ng\u01b0\u1eddi ch\u01a1i: " + user.getName() + " c\u01b0\u1ee3c " + userBet + " v\u00e0o " + mode.getName() + " s\u1ed1 " + lotteryCmd.num + " v\u00e0o l\u00fac " + LocalDateTime.now().format(formatter)));
        if (currentMoney > finalBetValue) {
            MoneyResponse res = new MoneyResponse(false, "1001");
            res = this.userService.updateMoney(user.getName(), -finalBetValue, this.moneyTypeStr, "LoDe", "L\u00f4 \u0110\u1ec1", "C\u01b0\u1ee3c " + lotteryCmd.num + " \n " + mode.getName(), 0L, Long.valueOf(new Date().getTime()), TransType.START_TRANS);
            if (res.isSuccess()) {
                this.loDeService.saveTransactionLode(
                        (long) user.getId(), user.getName(),
                        finalBetValue, lotteryCmd.mode, lotteryCmd.num, 0L,
                        userBet, rateAtPurchase, prizeMultiplierAtPurchase);
                System.out.println("Buy ticket success");
            }
        }
    }

    /**
     * SUN-1295 settle entry: prize calc using the per-bet snapshot stored on
     * the lode row. Pulls {@code rate_at_purchase} and {@code prize_multiplier}
     * out of the message; falls back to the live {@link LotteryMode} enum for
     * legacy rows where those columns are still NULL.
     *
     * <p>Why this overload exists: the original {@code getPrize(rs, mode,
     * betValue, num)} read the prize multiplier from a hard-coded constant in
     * each switch case and the rate from {@link LotteryMode#getRate()} at
     * settle time. If ops changed those between purchase and settle, pending
     * bets settled at the new numbers — a classic time-of-check/time-of-use
     * bug. The snapshot fixes that by routing the resolved (rate, prizeMul)
     * through this overload.
     */
    public static long getPrize(LotteryResult rs, com.vinplay.vbee.common.messages.minigame.LotteryMessage msg) {
        if (msg == null) return 0L;
        long mode = msg.getMode();
        long betValue = msg.getBetValue();
        String num = msg.getTicket();
        Integer snapRate = msg.getRateAtPurchase();
        Integer snapPrizeMul = msg.getPrizeMultiplier();
        LotteryMode lm = LotteryMode.findLotteryModeById((int) mode);
        int rate = (snapRate != null) ? snapRate.intValue() : (lm != null ? lm.getRate() : 1);
        int prizeMul = (snapPrizeMul != null) ? snapPrizeMul.intValue() : (lm != null ? lm.getPrizeMultiplier() : 0);
        return computePrize(rs, mode, betValue, num, rate, prizeMul);
    }

    /** Legacy 4-arg overload kept for {@link LotteryModuleTest} and external callers. */
    public static long getPrize(LotteryResult rs, long mode, long betValue, String num) {
        LotteryMode lm = LotteryMode.findLotteryModeById((int) mode);
        int rate = lm != null ? lm.getRate() : 1;
        int prizeMul = lm != null ? lm.getPrizeMultiplier() : 0;
        return computePrize(rs, mode, betValue, num, rate, prizeMul);
    }

    /**
     * SUN-1295 prize computation core. Receives the resolved (rate, prizeMul)
     * — either snapshotted from the row or read from the live enum —
     * so the formula is identical regardless of source.
     *
     * <p>Closed-form invariant for the multi-prize modes (1, 2):
     * {@code prize = matches * betValue * prizeMul / rate}, because at
     * purchase the wallet was charged {@code betValue = userBet * rate}, so
     * the {@code /rate} cancels and per-match payout = {@code userBet *
     * prizeMul}.
     */
    private static long computePrize(LotteryResult rs, long mode, long betValue, String num,
                                     int rate, int prizeMul) {
        long prize = 0L;
        try {
            List<String> rs24 = rs.getResults().get24();
            List<String> rs27 = rs.getResults().get27();
            String db = rs.getResults().getĐB().get(0);
            String de = db.substring(db.length() - 2);
            switch ((int)mode) {
                case 1: {
                    long isVictory1 = rs27.stream().filter(item -> item.endsWith(num)).count();
                    if (isVictory1 <= 0L) break;
                    prize = isVictory1 * betValue * (long) prizeMul / (long) rate;
                    break;
                }
                case 2: {
                    long isVictory = rs24.stream().filter(item -> item.endsWith(num)).count();
                    if (isVictory <= 0L) break;
                    prize = isVictory * betValue * (long) prizeMul / (long) rate;
                    break;
                }
                case 3: {
                    List<String> nums3 = Arrays.asList(num.split(","));
                    int isVictory3 = 0;
                    for (String item2 : nums3) {
                        if (!rs27.stream().anyMatch(n -> n.endsWith(item2))) continue;
                        ++isVictory3;
                    }
                    if (isVictory3 < 2) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 4: {
                    List<String> nums4 = Arrays.asList(num.split(","));
                    int isVictory4 = 0;
                    for (String item3 : nums4) {
                        if (!rs27.stream().anyMatch(n -> n.endsWith(item3))) continue;
                        ++isVictory4;
                    }
                    if (isVictory4 < 3) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 5: {
                    List<String> nums5 = Arrays.asList(num.split(","));
                    int isVictory5 = 0;
                    for (String item4 : nums5) {
                        if (!rs27.stream().anyMatch(n -> n.endsWith(item4))) continue;
                        ++isVictory5;
                    }
                    if (isVictory5 < 3) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 6: {
                    if (!de.substring(0, 1).equals(num)) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 7: {
                    if (!de.substring(1).equals(num)) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 8: {
                    if (!rs.getResults().getĐB().stream().anyMatch(item -> item.endsWith(num))) break;
                    // Mode 8 historically divides by DUOI's rate; DUOI=1 today, kept for back-compat.
                    prize = betValue * (long) prizeMul / (long) LotteryMode.DUOI.getRate();
                    break;
                }
                case 9: {
                    if (!de.equals(num.substring(0, 2))) break;
                    prize = betValue * (long) prizeMul;
                    break;
                }
                case 11: {
                    boolean rx10 = rs.getResults().getĐB().stream().anyMatch(item -> item.endsWith(num));
                    if (!rx10) break;
                    prize = betValue * (long) prizeMul;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return prize;
    }
}
