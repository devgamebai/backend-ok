/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.CardGroup
 *  com.vinplay.cardlib.models.Hand
 *  com.vinplay.cardlib.models.Rank
 *  com.vinplay.cardlib.utils.CardLibUtils
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.CardGroup;
import com.vinplay.cardlib.models.Hand;
import com.vinplay.cardlib.models.Rank;
import com.vinplay.cardlib.utils.CardLibUtils;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.dao.CaoThapDao;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;

public class CaoThapDaoImpl
implements CaoThapDao {
    @Override
    public void logCaoThap(LogCaoThapMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_cao_thap");
        Document doc = new Document();
        doc.append("trans_id", (Object)message.transId);
        doc.append("nick_name", (Object)message.nickname);
        doc.append("pot_bet", (Object)message.potBet);
        doc.append("step", (Object)message.step);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("result", (Object)message.result);
        doc.append("prize", (Object)message.prize);
        doc.append("cards", (Object)message.cards);
        doc.append("current_pot", (Object)message.currentPot);
        doc.append("current_fund", (Object)message.currentFund);
        doc.append("money_type", (Object)message.moneyType);
        doc.append("time_log", (Object)VinPlayUtils.getCurrentDateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void logCaoThapWin(LogCaoThapWinMessage message) {
        String[] arrCard;
        int gt = -1;
        String handStr = "";
        int rank1 = -1;
        int rank2 = -1;
        int rank3 = -1;
        int rank4 = -1;
        int rank5 = -1;
        ArrayList<Card> cardLst = new ArrayList<Card>();
        String cardStr = message.cards.substring(0, message.cards.length() - 1);
        for (String cd : arrCard = cardStr.split(",")) {
            if (cd.trim().isEmpty()) continue;
            cardLst.add(new Card(Integer.parseInt(cd.trim())));
        }
        try {
            if (message.moneyType == 1 && cardLst.size() >= 5) {
                CardGroup cg = CardLibUtils.calculatePoker(cardLst);
                gt = cg.getType();
                Hand hand = cg.getHand();
                rank1 = ((Card)hand.getCards().get(0)).getRank().getRank();
                rank2 = ((Card)hand.getCards().get(1)).getRank().getRank();
                rank3 = ((Card)hand.getCards().get(2)).getRank().getRank();
                rank4 = ((Card)hand.getCards().get(3)).getRank().getRank();
                rank5 = ((Card)hand.getCards().get(4)).getRank().getRank();
                handStr = hand.cardsToString();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_cao_thap_win");
        Document doc = new Document();
        doc.append("trans_id", (Object)message.transId);
        doc.append("nick_name", (Object)message.nickname);
        doc.append("bet_value", (Object)message.betValue);
        doc.append("result", (Object)message.result);
        doc.append("prize", (Object)message.prize);
        doc.append("cards", (Object)cardStr);
        doc.append("money_type", (Object)message.moneyType);
        doc.append("time_log", (Object)VinPlayUtils.getCurrentDateTime());
        doc.append("group_type", (Object)gt);
        doc.append("hand", (Object)handStr);
        doc.append("rank_1", (Object)rank1);
        doc.append("rank_2", (Object)rank2);
        doc.append("rank_3", (Object)rank3);
        doc.append("rank_4", (Object)rank4);
        doc.append("rank_5", (Object)rank5);
        doc.append("money_win", (Object)(message.prize - message.betValue));
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }
}

