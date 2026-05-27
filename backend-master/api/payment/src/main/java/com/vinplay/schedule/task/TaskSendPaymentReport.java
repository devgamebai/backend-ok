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
import java.util.Date;
import org.apache.log4j.Logger;

public class TaskSendPaymentReport
implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TaskSendPaymentReport.class);

    @Override
    public void run() {
        try {
            LOGGER.info((Object)("B\u1eaft \u0111\u1ea7u g\u1eedi b\u00e1o c\u00e1o thanh to\u00e1n l\u00ean Telegram - " + new Date()));
            PaymentStatisticsDaoImpl paymentStatisticsDao = new PaymentStatisticsDaoImpl();
            PaymentSummaryEntity summary = paymentStatisticsDao.getPaymentSummaryToday();
            String momo = TelegramAlert.formatDecimalRound((long)summary.getDepositMomo());
            String the = TelegramAlert.formatDecimalRound((long)summary.getDepositCard());
            String bank = TelegramAlert.formatDecimalRound((long)summary.getDepositBank());
            String tongNap = TelegramAlert.formatDecimalRound((long)summary.getTotalDeposit());
            String tongRut = TelegramAlert.formatDecimalRound((long)summary.getTotalWithdraw());
            String tyLeStr = String.format("%.2f", summary.getRatio());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = df.format(new Date());
            String profit = TelegramAlert.formatDecimalRound((long)summary.getProfit());
            TelegramAlert.sendReport((String)momo, (String)the, (String)bank, (String)tongNap, (String)tongRut, (String)tyLeStr, (String)time, (String)profit);
        }
        catch (SQLException e) {
            LOGGER.error((Object)("L\u1ed7i khi l\u1ea5y d\u1eef li\u1ec7u b\u00e1o c\u00e1o thanh to\u00e1n: " + e.getMessage()), (Throwable)e);
        }
        catch (Exception e) {
            LOGGER.error((Object)("L\u1ed7i khi g\u1eedi b\u00e1o c\u00e1o thanh to\u00e1n l\u00ean Telegram: " + e.getMessage()), (Throwable)e);
        }
    }
}

