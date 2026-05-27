/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.minigame.entities;

import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BalanceMoneyTX {
    private long moneyWin = 0L;
    private long preMoneyBet = 0L;
    private long moneyBet = 0L;
    private long fee = 0L;
    private long updateTime;
    private boolean alerted = true;
    private String dateReset;

    public BalanceMoneyTX() {
        this.updateTime = System.currentTimeMillis();
    }

    public BalanceMoneyTX(long moneyWin, long moneyLoss, long fee, String dateReset) {
        this.moneyWin = moneyWin;
        this.preMoneyBet = this.moneyBet = moneyLoss;
        this.fee = fee;
        this.updateTime = System.currentTimeMillis();
        this.dateReset = dateReset;
    }

    public void addBet(long moneyBet) {
        this.moneyBet -= moneyBet;
    }

    public void addWin(long moneyWin) {
        this.moneyWin += moneyWin;
    }

    public void addFee(long fee) {
        this.fee += fee;
    }

    public int isForceBalance(boolean hasBlackList, boolean hasWhiteList) {
        Debug.trace(("Money win=" + this.moneyWin));
        Debug.trace(("Pre Money bet=" + this.preMoneyBet));
        Debug.trace(("Money bet=" + this.moneyBet));
        Debug.trace(("Money fee=" + this.fee));
        long revenueUser = this.preMoneyBet + this.moneyWin;
        Debug.trace(("revenueUser=" + revenueUser));
        if (revenueUser >= 50000000L) {
            if (!this.alerted) {
                this.alerted = true;
                GameUtils.sendAlertAndCall("Loi nhuan user vuot qua 10 trieu (" + DateTimeUtils.getCurrentTime() + "), value= " + revenueUser);
            }
        } else {
            this.alerted = false;
        }
        if ((float)revenueUser > (float)(-this.fee) * ConfigGame.getFloatValue("tx_min_fee", 1.0f)) {
            return 1;
        }
        if (hasBlackList && (float)revenueUser > (float)(-this.fee) * ConfigGame.getFloatValue("tx_max_black_list_lost", 10.0f)) {
            return -2;
        }
        if (hasWhiteList) {
            return -3;
        }
        if ((float)revenueUser < (float)(-this.fee) * ConfigGame.getFloatValue("tx_max_fee", 4.0f) && Math.abs(revenueUser) - this.fee >= (long)ConfigGame.getIntValue("tx_min_money_force_user_win", 30000000)) {
            return -1;
        }
        return 0;
    }

    public void startNewRound() {
        this.preMoneyBet = this.moneyBet;
        this.updateTime();
    }

    public void updateTime() {
        SimpleDateFormat sdf;
        Calendar cal;
        String toDay;
        if (this.updateTime < DateTimeUtils.getStartTimeToDayAsLong() && (toDay = (sdf = new SimpleDateFormat("yyyy/MM/dd")).format((cal = Calendar.getInstance()).getTime())).equalsIgnoreCase(this.dateReset)) {
            this.moneyBet = 0L;
            this.moneyWin = 0L;
            this.preMoneyBet = 0L;
            this.fee = 0L;
            int range = ConfigGame.getIntValue("interval_reset_balance", 10);
            int currentMonth = cal.get(2);
            cal.add(5, range);
            this.dateReset = sdf.format(cal.getTime());
            int tmpMonth = cal.get(2);
            if (currentMonth != tmpMonth) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/01");
                this.dateReset = sdf2.format(cal.getTime());
            }
            Debug.trace(("Reset balance next date reset= " + this.dateReset + ", range= " + range));
        }
        this.updateTime = System.currentTimeMillis();
    }

    public long getTienHeThongDangThua() {
        long revenueUser = this.preMoneyBet + this.moneyWin;
        return revenueUser + this.fee;
    }

    public long getMaxWinUser() {
        long revenueUser = this.preMoneyBet + this.moneyWin;
        return (long)((float)Math.abs(revenueUser) - (float)this.fee * ConfigGame.getFloatValue("tx_max_fee", 4.0f) / 2.0f);
    }

    public long getFee() {
        return this.fee;
    }

    public long getRevenueUser() {
        return this.preMoneyBet + this.moneyWin;
    }

    public String getDateReset() {
        return this.dateReset;
    }

    public void setDateReset(String dateReset) {
        this.dateReset = dateReset;
    }

    public class BalanceType {
        public static final int RANDOM = 0;
        public static final int HE_THONG_AM = 1;
        public static final int NGUOI_CHOI_AM = -1;
        public static final int BLACK_LIST_THUA = -2;
        public static final int WHITE_LIST_THANG = -3;
    }

}

