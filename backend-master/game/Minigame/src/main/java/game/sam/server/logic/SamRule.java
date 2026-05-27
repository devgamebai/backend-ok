/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic;

import game.sam.server.logic.Card;
import game.sam.server.logic.GroupCard;
import java.util.List;

public class SamRule {
    public static byte[] timBoDanhRa(GroupCard cards) {
        return cards.toByteArray();
    }

    public static byte[] chonBoDoBai(GroupCard boDanh, GroupCard boDo) {
        if (boDo.BO == 1) {
            return SamRule.chonBoDoBaiMot(boDanh, boDo);
        }
        return null;
    }

    private static byte[] chonBoDoBaiMot(GroupCard boDanh, GroupCard boDo) {
        byte[] cards = new byte[boDanh.cards.size()];
        return cards;
    }
}

