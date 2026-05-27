/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.api.IBZApi
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.impl.BauCuaServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.apache.log4j.Logger
 */
package game.modules.minigame.utils;

import bitzero.server.api.IBZApi;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.BauCuaServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.minigame.cmd.send.UpdateUserInfoMsg;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class BauCuaUtils {
    private static Logger logger = Logger.getLogger((String)"csvToiChonCa");
    private static long[] prizes = new long[]{100000L, 50000L, 20000L, 10000L, 10000L};
    private static final String FORMAT_TOI_CHON_CA = "%s,\t%d,\t%d,\t%d,\t%d,\t%d,\t%s";

    public static void rewardToiChonCa() {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat startTimeFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        SimpleDateFormat endTimeFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        Calendar cal = Calendar.getInstance();
        cal.add(5, -1);
        String yesterday = df.format(cal.getTime());
        String startTime = startTimeFormat.format(cal.getTime());
        String endTime = endTimeFormat.format(cal.getTime());
        BauCuaServiceImpl bcService = new BauCuaServiceImpl();
        UserServiceImpl userService = new UserServiceImpl();
        List list = bcService.getTopToiChonCa(startTime, endTime);
        int rank = 1;
        Debug.trace("List toi chon ca: " + list.size());
        for (int i = 0; i < list.size() && i < prizes.length; ++i) {
            ToiChonCa player = (ToiChonCa)list.get(i);
            Debug.trace("Tra thuong toi chon ca: " + player.username);
            MoneyResponse response = userService.updateMoney(player.username, prizes[i], "vin", "BauCua", "T\u00c3\u00b4i ch\u00e1\u00bb\ufffdn c\u00c3\u00a1 - Tr\u00e1\u00ba\u00a3 th\u00c6\u00b0\u00e1\u00bb\u0178ng", "Ng\u00c3\u00a0y " + yesterday, 0L, null, TransType.NO_VIPPOINT);
            if (response != null && response.isSuccess()) {
//                List<User> users = new ArrayList<>();
//                users.add(ExtensionUtility.getExtension().getApi().getUserByName(player.username));

                List<User> users = ExtensionUtility.getExtension().getApi().getUserByName(player.username);

                if (users != null) {
                    UpdateUserInfoMsg msg = new UpdateUserInfoMsg();
                    msg.currentMoney = response.getCurrentMoney();
                    msg.moneyType = 1;
                    ExtensionUtility.getExtension().sendUsers((BaseMsg)msg, users);
                }
                BauCuaUtils.log(player.username, rank, player.soCa, player.soVan, prizes[i], response.getCurrentMoney());
            }
            ++rank;
        }
    }

    private static void log(String username, int rank, int soCa, int soVan, long prize, long moneyAfterUpdated) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        String str = String.format(FORMAT_TOI_CHON_CA, username, rank, soCa, soVan, prize, moneyAfterUpdated, df.format(new Date()));
        System.out.println(str);
        logger.debug(str);
    }
}

