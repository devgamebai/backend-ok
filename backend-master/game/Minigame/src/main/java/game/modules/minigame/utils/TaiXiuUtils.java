package game.modules.minigame.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import game.modules.description.TaiXiuDescription.TaiXiuDescriptionUtils;
import game.modules.minigame.cmd.send.sicbo.ResultSicbo;
import org.apache.log4j.Logger;

import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;

public class TaiXiuUtils {
    private static Logger loggerTaiXiu = Logger.getLogger("csvBetTaiXiu");
    private static final String FORMAT_LOG_BET_TAI_XIU = "%d,\t%s,\t%d,\t%d,\t%d,\t%s,\t%s";

    public static String buildLichSuPhien(List<ResultTaiXiu> input, int number) {
        int end = input.size();
        int start = end - number > 0 ? end - number : 0;
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; ++i) {
            ResultTaiXiu entry = input.get(i);
            builder.append(entry.dice1);
            builder.append(",");
            builder.append(entry.dice2);
            builder.append(",");
            builder.append(entry.dice3);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static List<ResultSicbo> buildLichSuPhienSicbo(List<ResultTaiXiu> input, int number) {
        int end = input.size();
        int start = end - number > 0 ? end - number : 0;
        List<ResultSicbo> histories = new ArrayList<>();
        for (int i = start; i < end; ++i) {
            ResultTaiXiu entry = input.get(i);
            ResultSicbo rs = new ResultSicbo();
            List<Integer> e = new ArrayList<>();
            e.add(entry.dice1);
            e.add(entry.dice2);
            e.add(entry.dice3);
            rs.setResult(e);
            histories.add(rs);
        }
        return histories;
    }

    public static String logLichSuPhien(List<ResultTaiXiu> input, int number) {
        int end = input.size();
        int start = end - number > 0 ? end - number : 0;
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; ++i) {
            ResultTaiXiu entry = input.get(i);
            builder.append(entry.result);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static void logBetTaiXiu(TransactionTaiXiuDetail tran) {
        String moneyType = tran.moneyType == 1 ? "vin" : "xu";
        TaiXiuUtils.logBetTaiXiu(tran.referenceId, tran.username, tran.betValue, tran.betSide, tran.inputTime, moneyType);
    }

    public static void logBetTaiXiu(long referenceId, String nickname, long betValue, int betSide, int inputTime, String moneyType) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        String str = String.format(FORMAT_LOG_BET_TAI_XIU, referenceId, nickname, betValue, betSide, inputTime, moneyType, df.format(new Date()));
        loggerTaiXiu.debug((Object)str);
    }

    /**
     * Gửi log tiền qua RMQ (queue_log_report_user_balance) để trừ cột taixiu / taixiu_sicbo trong log_report_user
     * khi hoàn tiền cân cửa (calculateMoneyReturn). actionName phải là TaiXiuHoanTien để khớp LogSumReportUserSQLProcessor.
     */
    public static void publishLogReportCuaRefund(long referenceId, int userId, String username, long refund,
            String moneyTypeStr, int gameId, boolean isBot) {
        if (refund <= 0L || username == null || username.isEmpty() || userId == 0) {
            return;
        }
        try {
            String desc = TaiXiuDescriptionUtils.getTaiXiuRefundDescription(String.valueOf(gameId), referenceId);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(userId, username,
                    "TaiXiuHoanTien",
                    String.valueOf(gameId),
                    0L,
                    refund,
                    moneyTypeStr,
                    desc,
                    0L,
                    false,
                    isBot);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
        } catch (Exception e) {
            loggerTaiXiu.error("publishLogReportCuaRefund error ref=" + referenceId + " nick=" + username + " refund=" + refund, e);
        }
    }
}

