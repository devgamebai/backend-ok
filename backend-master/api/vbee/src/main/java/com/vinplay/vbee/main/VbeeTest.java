/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.CardGroup
 *  com.vinplay.cardlib.models.Deck
 *  com.vinplay.cardlib.models.Hand
 *  com.vinplay.cardlib.utils.CardLibUtils
 */
package com.vinplay.vbee.main;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.CardGroup;
import com.vinplay.cardlib.models.Deck;
import com.vinplay.cardlib.models.Hand;
import com.vinplay.cardlib.utils.CardLibUtils;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;

public class VbeeTest {
    public static void main(String[] args) throws SQLException {
        int cnt0 = 0;
        int cnt2 = 0;
        int cnt3 = 0;
        int cnt4 = 0;
        int cnt5 = 0;
        int cnt6 = 0;
        int cnt7 = 0;
        int cnt8 = 0;
        int cnt9 = 0;
        for (int j = 5; j <= 1000000; ++j) {
            try {
                Deck deck = new Deck();
                Hand hand = new Hand();
                for (int i = 0; i < 11; ++i) {
                    hand.addCard(deck.deal());
                }
                CardGroup cg = CardLibUtils.calculatePoker((List)hand.getCards());
                if (!(cnt0 == 0 && cg.getType() == 0 || cnt2 == 0 && cg.getType() == 1 || cnt3 == 0 && cg.getType() == 2 || cnt4 == 0 && cg.getType() == 3 || cnt5 == 0 && cg.getType() == 4 || cnt6 == 0 && cg.getType() == 5 || cnt7 == 0 && cg.getType() == 6 || cnt8 == 0 && cg.getType() == 7) && (cnt9 != 0 || cg.getType() != 8)) continue;
                if (cg.getType() == 0) {
                    ++cnt0;
                }
                if (cg.getType() == 1) {
                    ++cnt2;
                }
                if (cg.getType() == 2) {
                    ++cnt3;
                }
                if (cg.getType() == 3) {
                    ++cnt4;
                }
                if (cg.getType() == 4) {
                    ++cnt5;
                }
                if (cg.getType() == 5) {
                    ++cnt6;
                }
                if (cg.getType() == 6) {
                    ++cnt7;
                }
                if (cg.getType() == 7) {
                    ++cnt8;
                }
                if (cg.getType() == 8) {
                    ++cnt9;
                }
                System.out.println(hand.cardsToString());
                System.out.println(cg.getType());
                System.out.println(cg.getHand().cardsToString());
                System.out.println("===================================");
                continue;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

