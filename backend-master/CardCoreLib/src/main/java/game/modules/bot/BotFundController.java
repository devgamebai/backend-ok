/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package game.modules.bot;

import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import game.xocdia.conf.XocDiaConfig;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class BotFundController {
    private static BotFundController instance = null;
    private static String keyFund = "fundXocDia";
    public long[] listFund;

    public static BotFundController getInstance() {
        if (instance == null) {
            instance = new BotFundController();
        }
        return instance;
    }

    private BotFundController() {
        try {
            this.listFund = this.getFunds(keyFund);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
    }

    public long getBotFund(long betLevel) {
        if (XocDiaConfig.listBetLevel.containsKey("" + betLevel)) {
            return this.listFund[XocDiaConfig.listBetLevel.get("" + betLevel)];
        }
        return -1L;
    }

    public void changeFund(long betLevel, long money) {
        String keyFundBetLevel = keyFund + "_" + betLevel;
        if (XocDiaConfig.listBetLevel.containsKey("" + betLevel)) {
            int n = XocDiaConfig.listBetLevel.get("" + betLevel);
            this.listFund[n] = this.listFund[n] + money;
            long currentMoneyFund = this.listFund[XocDiaConfig.listBetLevel.get("" + betLevel)];
            try {
                this.saveFund(keyFundBetLevel, currentMoneyFund, betLevel);
            }
            catch (Exception e) {
                Debug.trace((Object[])new Object[]{e});
            }
        }
    }

    public void saveFund(String fundName, long value, long betLevel) throws IOException, TimeoutException, InterruptedException {
        UpdateFundMessage msg = new UpdateFundMessage();
        msg.fundName = fundName;
        msg.newValue = value;
        MessageBusFactory.get("queue_fund").publish("queue_fund", (BaseMessage)msg, (int)110);
    }

    public long[] getFunds(String fundName) throws SQLException {
        ArrayList<Long> result = new ArrayList<Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_funds WHERE minigame_funds.fund_name like '" + fundName + "%'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("value"));
            }
            rs.close();
            stmt.close();
        }
        long[] arr = new long[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            arr[i] = (Long)result.get(i);
        }
        return arr;
    }
}

