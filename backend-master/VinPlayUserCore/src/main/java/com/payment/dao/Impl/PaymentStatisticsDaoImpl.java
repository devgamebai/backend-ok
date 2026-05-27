/*
 * Decompiled with CFR 0.152.
 */
package com.payment.dao.Impl;

import com.payment.dao.PaymentStatisticsDao;
import com.payment.entities.PaymentSummaryEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Date;

public class PaymentStatisticsDaoImpl
implements PaymentStatisticsDao {
    @Override
    public PaymentSummaryEntity getPaymentSummaryByNickName(String nickName) throws SQLException {
        PaymentSummaryEntity result = new PaymentSummaryEntity();
        result.setNickName(nickName);
        long totalDeposit = 0L;
        long totalWithdraw = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sqlBank = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM history_bank WHERE nick_name = ? AND status = 2";
            try (PreparedStatement stm = conn.prepareStatement(sqlBank)) {
                stm.setString(1, nickName);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) {
                        totalDeposit += rs.getLong("total");
                    }
                }
            }
            totalDeposit = (long)((double)totalDeposit / 1.35);
            String sqlTopup = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM topup WHERE nick_name = ? AND status = 2";
            try (PreparedStatement stm2 = conn.prepareStatement(sqlTopup)) {
                stm2.setString(1, nickName);
                try (ResultSet rs2 = stm2.executeQuery()) {
                    if (rs2.next()) {
                        totalDeposit += rs2.getLong("total");
                    }
                }
            }
            String sqlWithdraw = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM history_applyfor WHERE nick_name = ? AND status = 2";
            try (PreparedStatement stm3 = conn.prepareStatement(sqlWithdraw)) {
                stm3.setString(1, nickName);
                try (ResultSet rs3 = stm3.executeQuery()) {
                    if (rs3.next()) {
                        totalWithdraw = rs3.getLong("total");
                    }
                }
            }
        }
        result.setTotalDeposit(totalDeposit);
        result.setTotalWithdraw(totalWithdraw);
        return result;
    }

    @Override
    public PaymentSummaryEntity getPaymentSummaryToday() throws SQLException {
        PaymentSummaryEntity result = new PaymentSummaryEntity();
        Date date = new Date();
        int day = date.toInstant().atZone(ZoneId.systemDefault()).getDayOfMonth();
        int month = date.toInstant().atZone(ZoneId.systemDefault()).getMonthValue();
        int year = date.toInstant().atZone(ZoneId.systemDefault()).getYear();
        long totalDeposit = 0L;
        long totalWithdraw = 0L;
        long depositBank = 0L;
        long depositMomo = 0L;
        long depositCard = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sqlBank = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM history_bank WHERE day = ? AND month = ? AND year = ? AND LOWER(type) = 'bank' AND status = 2";
            try (PreparedStatement stm = conn.prepareStatement(sqlBank)) {
                stm.setInt(1, day);
                stm.setInt(2, month);
                stm.setInt(3, year);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) {
                        depositBank = rs.getLong("total");
                        totalDeposit += depositBank;
                    }
                }
            }
            String sqlMomo = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM history_bank WHERE day = ? AND month = ? AND year = ? AND LOWER(type) = 'momo' AND status = 2";
            try (PreparedStatement stm2 = conn.prepareStatement(sqlMomo)) {
                stm2.setInt(1, day);
                stm2.setInt(2, month);
                stm2.setInt(3, year);
                try (ResultSet rs2 = stm2.executeQuery()) {
                    if (rs2.next()) {
                        depositMomo = rs2.getLong("total");
                        totalDeposit += depositMomo;
                    }
                }
            }
            totalDeposit = (long)((double)totalDeposit / 1.35);
            depositMomo = (long)((double)depositMomo / 1.35);
            depositBank = (long)((double)depositBank / 1.35);
            String sqlCard = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM topup WHERE day = ? AND month = ? AND year = ? AND status = 2";
            try (PreparedStatement stm3 = conn.prepareStatement(sqlCard)) {
                stm3.setInt(1, day);
                stm3.setInt(2, month);
                stm3.setInt(3, year);
                try (ResultSet rs3 = stm3.executeQuery()) {
                    if (rs3.next()) {
                        depositCard = rs3.getLong("total");
                        totalDeposit += depositCard;
                    }
                }
            }
            String sqlWithdraw = "SELECT COALESCE(SUM(cash_real), 0) AS total FROM history_applyfor WHERE day = ? AND month = ? AND year = ? AND status = 2";
            try (PreparedStatement stm4 = conn.prepareStatement(sqlWithdraw)) {
                stm4.setInt(1, day);
                stm4.setInt(2, month);
                stm4.setInt(3, year);
                try (ResultSet rs4 = stm4.executeQuery()) {
                    if (rs4.next()) {
                        totalWithdraw = rs4.getLong("total");
                    }
                }
            }
        }
        result.setTotalDeposit(totalDeposit);
        result.setTotalWithdraw(totalWithdraw);
        result.setDepositBank(depositBank);
        result.setDepositMomo(depositMomo);
        result.setDepositCard(depositCard);
        result.setProfit(totalDeposit - totalWithdraw);
        if (totalWithdraw > 0L) {
            result.setRatio((double)result.getProfit() / (double)totalDeposit * 100.0);
        } else if (totalDeposit > 0L) {
            result.setRatio(100.0);
        } else {
            result.setRatio(0.0);
        }
        return result;
    }
}
