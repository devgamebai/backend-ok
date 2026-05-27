/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.api.IBZApi
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.Deck
 *  com.vinplay.cardlib.models.Rank
 *  com.vinplay.dal.entities.caothap.TopCaoThap
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.apache.log4j.Logger
 *  scala.util.Random
 */
package game.modules.minigame.utils;

import bitzero.server.api.IBZApi;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.Deck;
import com.vinplay.cardlib.models.Rank;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.minigame.cmd.send.UpdateUserInfoMsg;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import scala.util.Random;

public class CaoThapUtils {
    public static final byte UP = 1;
    public static final byte DOWN = 0;
    public static final double RATIO = 98.0;
    public static final double MIN_RATIO = 1.01;
    private static long[] prizes = new long[]{500000L, 200000L, 100000L};
    private static Logger logger = Logger.getLogger((String)"csvCaoThapPrize");
    private static final String FORMAT_SAN_BAI_DEP = "%s,\t%d,\t%s,\t%d,\t%s,\t%d,\t%d,\t%s";
    public static final double RATIO_NO_HU = 30.0;

    public static boolean isDoWithRatio(double ratio) {
        Random rd = new Random();
        double i = rd.nextDouble() * 100.0;
        return i < ratio;
    }

    public static List<Double> getRatio(Deck deck, Card card) {
        double ratioUp = 0.0;
        double ratioDown = 0.0;
        if (deck.getSize() > 0) {
            int cdUp = 0;
            int cdDown = 0;
            int dkSize = 0;
            for (Card cd : deck.getHand()) {
                if (cd.getRank().getRank() > card.getRank().getRank()) {
                    cdUp = (short)(cdUp + 1);
                    dkSize = (short)(dkSize + 1);
                    continue;
                }else if (cd.getRank().getRank() < card.getRank().getRank()) {
                    cdDown = (short) (cdDown + 1);
                    dkSize = (short) (dkSize + 1);
                }
            }
            if (cdUp == 0) {
                ratioUp = 0.0D;
                ratioDown = 1.0D;
            } else if (cdDown == 0) {
                ratioUp = 1.0D;
                ratioDown = 0.0D;
            } else {
                ratioUp = (double)Math.round(98.0 * (double)dkSize / (double)cdUp) / 100.0;
                if (ratioUp <= 1.0) {
                    ratioUp = 1.01D;
                }
                ratioDown = (double)Math.round(98.0 * (double)dkSize / (double)cdDown) / 100.0;
                if (ratioDown <= 1.0D) {
                    ratioDown = 1.01D;
                }
            }
        }
        ArrayList<Double> rs = new ArrayList<Double>();
        rs.add(ratioDown);
        rs.add(ratioUp);
        return rs;
    }

    public static Card randomThua(Deck deck, Card card, byte choose) {
        Card cardThua = null;
        if (deck.getSize() > 0) {
            Deck dk;
            int numUp = 0;
            int numDown = 0;
            for (Card cd : deck.getHand()) {
                if (cd.getRank().getRank() == card.getRank().getRank()) {
                    ++numUp;
                    ++numDown;
                    continue;
                }
                if (cd.getRank().getRank() > card.getRank().getRank()) {
                    ++numUp;
                    continue;
                }
                ++numDown;
            }
            while ((cardThua = (dk = new Deck(deck.getDeck(), deck.getCount())).deal()).getRank().getRank() > card.getRank().getRank() && choose == 1 && numDown > 0 || cardThua.getRank().getRank() < card.getRank().getRank() && choose == 0 && numUp > 0) {
            }
        }
        return cardThua;
    }

    public static Card randomNoA(Deck deck) {
        Card cardThua = null;
        if (deck.getSize() > 0) {
            Deck dk;
            int numA = 0;
            for (Card cd : deck.getHand()) {
                if (cd.getRank() != Rank.Ace) continue;
                ++numA;
            }
            while ((cardThua = (dk = new Deck(deck.getDeck(), deck.getCount())).deal()).getRank() == Rank.Ace && numA != deck.getSize()) {
            }
        }
        return cardThua;
    }

    public static String getCardStr(List<Card> cardList) {
        StringBuilder cards = new StringBuilder();
        for (Card cd : cardList) {
            cards.append(cd.getCode());
            cards.append(",");
        }
        return new String(cards);
    }

//    public static void reward() {
//        try {
//            SimpleDateFormat startTimeFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
//            SimpleDateFormat endTimeFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
//            Calendar cal = Calendar.getInstance();
//            cal.add(5, -1);
//            String startTime = startTimeFormat.format(cal.getTime());
//            String endTime = endTimeFormat.format(cal.getTime());
//            Debug.trace(("Tra thuong cao thap " + startTime + " - " + endTime));
//            CaoThapUtils.rewardCaoThap(startTime, endTime);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void rewardCaoThap(String startTime, String endTime) throws SQLException {
//        long moneyAfterUpdated;
//        TopCaoThap entry;
//        String actionName = "Cao Th\u00e1\u00ba\u00a5p: Tr\u00e1\u00ba\u00a3 th\u00c6\u00b0\u00e1\u00bb\u0178ng s\u00e1\u00bb\u00b1 ki\u00e1\u00bb\u2021n Th\u00c3\u00b9ng ph\u00c3\u00a1 s\u00e1\u00ba\u00a3nh";
//        CaoThapServiceImpl service = new CaoThapServiceImpl();
//        List results = service.geTopCaoThap(startTime, endTime);
//        Debug.trace("So user duoc Cao Thap tra thuong su kien thung pha sanh: " + results.size());
//        int rank = 0;
//        if (results.size() > 0) {
//            entry = (TopCaoThap)results.get(rank);
//            Debug.trace("Cao thap tra thuong: " + entry.nickname + ", " + prizes[rank]);
//            moneyAfterUpdated = CaoThapUtils.rewardCaoThapToUser(entry, prizes[rank], actionName);
//            CaoThapUtils.log(entry.nickname, rank + 1, entry.hand, entry.money, entry.timestamp, prizes[rank], moneyAfterUpdated);
//            ++rank;
//        }
//        if (results.size() > 1) {
//            entry = (TopCaoThap)results.get(rank);
//            Debug.trace(("Cao thap tra thuong: " + entry.nickname + ", " + prizes[rank]));
//            moneyAfterUpdated = CaoThapUtils.rewardCaoThapToUser(entry, prizes[rank], actionName);
//            CaoThapUtils.log(entry.nickname, rank + 1, entry.hand, entry.money, entry.timestamp, prizes[rank], moneyAfterUpdated);
//            ++rank;
//        }
//        if (results.size() > 2) {
//            entry = (TopCaoThap)results.get(rank);
//            Debug.trace("Cao thap tra thuong: " + entry.nickname + ", " + prizes[rank]);
//            moneyAfterUpdated = CaoThapUtils.rewardCaoThapToUser(entry, prizes[rank], actionName);
//            CaoThapUtils.log(entry.nickname, rank + 1, entry.hand, entry.money, entry.timestamp, prizes[rank], moneyAfterUpdated);
//            ++rank;
//        }
//    }

//    private static long rewardCaoThapToUser(TopCaoThap entry, long prize, String actionName) {
//        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
//        UserServiceImpl userService = new UserServiceImpl();
//        Calendar cal = Calendar.getInstance();
//        cal.add(5, -1);
//
//        MoneyResponse response = userService.updateMoney(entry.nickname, prize, "vin", "CaoThap", actionName, "Ng\u00c3\u00a0y " + format.format(cal.getTime()), 0L, null, TransType.NO_VIPPOINT);
//
//        List<User> u = ExtensionUtility.getExtension().getApi().getUserByName(entry.nickname);
//        if (response != null && response.isSuccess() && u != null) {
//            UpdateUserInfoMsg msg = new UpdateUserInfoMsg();
//            msg.currentMoney = response.getCurrentMoney();
//            msg.moneyType = 1;
//            ExtensionUtility.getExtension().send(msg, u.get(0));
//        }
//        if (response != null) {
//            return response.getCurrentMoney();
//        }
//        return -1L;
//    }

    private static void log(String nickname, int rank, String hand, long moneyWin, String playTime, long prize, long moneyAfterUpdated) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        String str = String.format(FORMAT_SAN_BAI_DEP, nickname, rank, hand, moneyWin, playTime, prize, moneyAfterUpdated, df.format(new Date()));
        System.out.println(str);
        logger.debug(str);
    }
}

