/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.dao.Impl.PaymentStatisticsDaoImpl
 *  com.payment.entities.PaymentSummaryEntity
 *  com.vinplay.utils.TelegramAlert
 *  org.apache.log4j.Logger
 */
package com.vinplay.schedule.task;

import com.payment.dao.Impl.PaymentStatisticsDaoImpl;
import com.payment.entities.PaymentSummaryEntity;
import com.vinplay.utils.TelegramAlert;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

public class TaskSendPaymentReportDaily
implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TaskSendPaymentReportDaily.class);
    private static int lastSentDay = -1;

    @Override
    public void run() {
        try {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(11);
            int currentMinute = now.get(12);
            int currentSecond = now.get(13);
            int currentDay = now.get(5);
            if (currentHour == 23 && currentMinute == 59 && currentSecond >= 49) {
                if (lastSentDay == currentDay) {
                    return;
                }
                LOGGER.info((Object)("B\u1eaft \u0111\u1ea7u g\u1eedi b\u00e1o c\u00e1o thanh to\u00e1n cu\u1ed1i ng\u00e0y l\u00ean Telegram - " + new Date()));
                PaymentStatisticsDaoImpl paymentStatisticsDao = new PaymentStatisticsDaoImpl();
                PaymentSummaryEntity summary = paymentStatisticsDao.getPaymentSummaryToday();
                String momo = TelegramAlert.formatDecimalRound((long)summary.getDepositMomo());
                String the = TelegramAlert.formatDecimalRound((long)summary.getDepositCard());
                String bank = TelegramAlert.formatDecimalRound((long)summary.getDepositBank());
                String tongNap = TelegramAlert.formatDecimalRound((long)summary.getTotalDeposit());
                String tongRut = TelegramAlert.formatDecimalRound((long)summary.getTotalWithdraw());
                String tyLeStr = String.format("%.2f", summary.getRatio());
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = "B\u00c1O C\u00c1O CU\u1ed0I NG\u00c0Y - " + df.format(new Date());
                String profit = TelegramAlert.formatDecimalRound((long)summary.getProfit());
                TelegramAlert.sendReport((String)momo, (String)the, (String)bank, (String)tongNap, (String)tongRut, (String)tyLeStr, (String)time, (String)profit);
            }
        }
        catch (SQLException e) {
            LOGGER.error((Object)("L\u1ed7i khi l\u1ea5y d\u1eef li\u1ec7u b\u00e1o c\u00e1o thanh to\u00e1n cu\u1ed1i ng\u00e0y: " + e.getMessage()), (Throwable)e);
        }
        catch (Exception e) {
            LOGGER.error((Object)("L\u1ed7i khi g\u1eedi b\u00e1o c\u00e1o thanh to\u00e1n cu\u1ed1i ng\u00e0y l\u00ean Telegram: " + e.getMessage()), (Throwable)e);
        }
    }
}

