/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.TaiXiuDAOImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.dao.impl.TaiXiuDAOImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class RewardThanhDuProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"csvThanhDuPrize");
    private long[] prizes = new long[]{2000000L, 1000000L, 500000L};
    private static final String FORMAT_THANH_DU = "%s,\t%d,\t%d,\t%d,\t%d,\t%d,\t%d,\t%s";

    public String execute(Param<HttpServletRequest> param) {
        try {
            SimpleDateFormat startTimeFormat = new SimpleDateFormat("yyyy/MM/dd 00:00:00");
            SimpleDateFormat endTimeFormat = new SimpleDateFormat("yyyy/MM/dd 23:59:59");
            Calendar cal = Calendar.getInstance();
            cal.add(5, -1);
            String startTime = startTimeFormat.format(cal.getTime());
            String endTime = endTimeFormat.format(cal.getTime());
            this.rewardThanhDu(startTime, endTime, (short)1);
            this.rewardThanhDu(startTime, endTime, (short)0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    private void rewardThanhDu(String startTime, String endTime, short type) throws SQLException {
        long moneyAfterUpdated;
        ThanhDuTXModel entry;
        TaiXiuDAOImpl txDAO = new TaiXiuDAOImpl();
        List listUser = txDAO.getTopThanhDuDaily(startTime, endTime, type);
        int rank = 0;
        if (listUser.size() > 0) {
            entry = (ThanhDuTXModel)listUser.get(rank);
            moneyAfterUpdated = this.rewardThanhDuToUser(entry, this.prizes[rank]);
            this.log(entry.username, rank + 1, type, entry.number, entry.totalValue, moneyAfterUpdated, this.prizes[rank]);
            ++rank;
        }
        if (listUser.size() > 1) {
            entry = (ThanhDuTXModel)listUser.get(rank);
            moneyAfterUpdated = this.rewardThanhDuToUser(entry, this.prizes[rank]);
            this.log(entry.username, rank + 1, type, entry.number, entry.totalValue, moneyAfterUpdated, this.prizes[rank]);
            ++rank;
        }
        if (listUser.size() > 2) {
            entry = (ThanhDuTXModel)listUser.get(rank);
            moneyAfterUpdated = this.rewardThanhDuToUser(entry, this.prizes[rank]);
            this.log(entry.username, rank + 1, type, entry.number, entry.totalValue, moneyAfterUpdated, this.prizes[rank]);
            ++rank;
        }
    }

    private long rewardThanhDuToUser(ThanhDuTXModel entry, long prize) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        UserServiceImpl userService = new UserServiceImpl();
        MoneyResponse response = userService.updateMoney(entry.username, prize, "vin", "TaiXiu", "Tr\u1ea3 th\u01b0\u1edfng th\u00e1nh d\u1ef1", "Ng\u00e0y " + format.format(new Date()), 0L, (Long)null, TransType.NO_VIPPOINT);
        if (response != null) {
            return response.getCurrentMoney();
        }
        return -1L;
    }

    private void log(String username, int rank, int type, int number, long totalValue, long prize, long moneyAfterUpdated) {
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy");
        String str = String.format(FORMAT_THANH_DU, username, rank, type, number, totalValue, prize, moneyAfterUpdated, df.format(new Date()));
        System.out.println(str);
        this.logger.debug((Object)str);
    }
}

