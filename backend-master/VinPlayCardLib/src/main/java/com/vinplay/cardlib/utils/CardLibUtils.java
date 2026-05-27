/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.utils;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.CardGroup;
import com.vinplay.cardlib.models.CardMap;
import com.vinplay.cardlib.models.CardMapNew;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.models.Hand;
import com.vinplay.cardlib.models.Rank;
import java.util.List;

public class CardLibUtils {
    public static GroupType calculateTypePoker(List<Card> cards) {
        Hand hand = new Hand(cards);
        CardMap cM = new CardMap();
        CardGroup cardGroup = cM.getCardGroup(hand);
        return GroupType.findGroupType(cardGroup.getType());
    }

    public static CardGroup calculatePoker(List<Card> cards) {
        Hand hand = new Hand(cards);
        CardMapNew cM = new CardMapNew();
        CardGroup cardGroup = cM.getCardGroup(hand);
        return cardGroup;
    }

    public static boolean pairEqualOrGreatJack(List<Card> cards) {
        for (int i = 0; i < cards.size() - 1; ++i) {
            for (int j = i + 1; j < cards.size(); ++j) {
                if (cards.get(i).getRank() != cards.get(j).getRank() || cards.get(i).getRank().getRank() < Rank.Jack.getRank()) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean isStraightFlushJack(List<Card> cards) {
        for (int i = 0; i < cards.size(); ++i) {
            if (cards.get(i).getRank() != Rank.Jack) continue;
            return true;
        }
        return false;
    }

    public static int[] cardsToIntArray(List<Card> cards) {
        int[] arr = new int[cards.size()];
        for (int i = 0; i < cards.size(); ++i) {
            arr[i] = cards.get(i).getCode();
        }
        return arr;
    }

    public static String cardsToString(List<Card> cards) {
        StringBuilder builder = new StringBuilder();
        builder.append(cards.get(0).getCode() + 1);
        for (int i = 1; i < cards.size(); ++i) {
            builder.append(",");
            builder.append(cards.get(i).getCode() + 1);
        }
        return builder.toString();
    }

    public static String intArrayToString(int[] input) {
        if (input == null || input.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(input[0]);
        for (int i = 1; i < input.length; ++i) {
            builder.append(",");
            builder.append(input[i]);
        }
        return builder.toString();
    }
}

