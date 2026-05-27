package com.vinplay.dal.utils;

/**
 * Shared transaction code generator for all deposit/withdrawal types.
 * Format: {prefix} + base36(timestamp) = 10 chars total
 *
 * Prefixes:
 *   DB = Deposit Bank
 *   DC = Deposit Crypto
 *   BW = Bank Withdrawal
 *   CW = Crypto Withdrawal
 */
public class TxCodeGenerator {

    public static String bankDeposit() {
        return "DB" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    public static String cryptoDeposit() {
        return "DC" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    public static String bankWithdraw() {
        return "BW" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    public static String cryptoWithdraw() {
        return "CW" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }
}
