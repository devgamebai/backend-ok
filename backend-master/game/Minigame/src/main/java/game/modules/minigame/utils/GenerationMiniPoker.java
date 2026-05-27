/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.Deck
 *  com.vinplay.cardlib.models.GroupType
 *  com.vinplay.cardlib.models.Rank
 *  com.vinplay.cardlib.models.Suit
 *  com.vinplay.cardlib.utils.CardLibUtils
 */
package game.modules.minigame.utils;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.Deck;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.models.Rank;
import com.vinplay.cardlib.models.Suit;
import com.vinplay.cardlib.utils.CardLibUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GenerationMiniPoker {
    private int[] tile = new int[]{523985, 783985, 933985, 970985, 989985, 994985, 998985, 999835, 999985, 999995, 1000000};
    private int[] prizes = new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

    public List<Card> randomCards2() {
        Random rd = new Random();
        int n = rd.nextInt(1000000);
        int prize = 11;
        for (int i = 0; i < this.tile.length; ++i) {
            if (n >= this.tile[i]) continue;
            prize = this.prizes[i];
            break;
        }
        switch (prize) {
            case 1: {
                return GenerationMiniPoker.randomThungPhaSanhJDenA();
            }
            case 2: {
                return GenerationMiniPoker.randomThungPhaSanhNho();
            }
            case 3: {
                return GenerationMiniPoker.randomTuQuy();
            }
            case 4: {
                return this.randomCuLu();
            }
            case 5: {
                return this.randomThung();
            }
            case 6: {
                return this.randomSanh();
            }
            case 7: {
                return this.randomSamCo();
            }
            case 8: {
                return this.randomHaiDoi();
            }
            case 9: {
                return this.randomMotDoiTo();
            }
        }
        return this.randomTruot();
    }

    public static List<Card> randomThungPhaSanhA() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int idRank = 8;
        int n = rd.nextInt(4);
        Suit suit = Suit.getSuitFromId((int)n);
        for (int i = 0; i < 5; ++i) {
            cards.add(new Card(Rank.getRankFromId((int)(idRank + i)), suit));
        }
        Collections.shuffle(cards);
        return cards;
    }

    public static List<Card> randomThungPhaSanhJDenK() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int n = rd.nextInt(4);
        int idRank = n + 4;
        n = rd.nextInt(4);
        Suit suit = Suit.getSuitFromId((int)n);
        for (int i = 0; i < 5; ++i) {
            cards.add(new Card(Rank.getRankFromId((int)(idRank + i)), suit));
        }
        Collections.shuffle(cards);
        return cards;
    }

    public static List<Card> randomThungPhaSanhJDenA() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int n = rd.nextInt(4);
        int idRank = n + 5;
        n = rd.nextInt(4);
        Suit suit = Suit.getSuitFromId((int)n);
        for (int i = 0; i < 5; ++i) {
            cards.add(new Card(Rank.getRankFromId((int)(idRank + i)), suit));
        }
        Collections.shuffle(cards);
        return cards;
    }

    public static List<Card> randomThungPhaSanhNho() {
        int n;
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int idRank = n = rd.nextInt(5);
        n = rd.nextInt(4);
        Suit suit = Suit.getSuitFromId((int)n);
        for (int i = 0; i < 5; ++i) {
            cards.add(new Card(Rank.getRankFromId((int)(idRank + i)), suit));
        }
        Collections.shuffle(cards);
        return cards;
    }

    public static List<Card> randomTuQuy() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int n = rd.nextInt(13);
        Deck deck = new Deck();
        cards.add(deck.popCard(Rank.getRankFromId((int)n), Suit.Clubs));
        cards.add(deck.popCard(Rank.getRankFromId((int)n), Suit.Diamonds));
        cards.add(deck.popCard(Rank.getRankFromId((int)n), Suit.Hearts));
        cards.add(deck.popCard(Rank.getRankFromId((int)n), Suit.Spades));
        cards.add(deck.deal());
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomCuLu() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        int n = rd.nextInt(13);
        Deck deck = new Deck();
        for (int i = 0; i < 3; ++i) {
            Card card = null;
            while (card == null) {
                int s = rd.nextInt(4);
                card = deck.popCard(Rank.getRankFromId((int)n), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        int n1 = n;
        while (n1 == n) {
            n1 = rd.nextInt(13);
        }
        for (int i = 0; i < 2; ++i) {
            Card card = null;
            while (card == null) {
                int s = rd.nextInt(4);
                card = deck.popCard(Rank.getRankFromId((int)n1), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomThung() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        Deck deck = new Deck();
        int s = rd.nextInt(4);
        for (int i = 0; i < 5; ++i) {
            Card card = null;
            while (card == null) {
                int r = rd.nextInt(13);
                card = deck.popCard(Rank.getRankFromId((int)r), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomSanh() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        Deck deck = new Deck();
        int r = rd.nextInt(9);
        for (int i = 0; i < 5; ++i) {
            Card card = null;
            while (card == null) {
                int s = rd.nextInt(4);
                card = deck.popCard(Rank.getRankFromId((int)(r + i)), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomSamCo() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        boolean loop = true;
        while (loop) {
            Deck deck = new Deck();
            int r = rd.nextInt(13);
            for (int i = 0; i < 3; ++i) {
                Card card = null;
                while (card == null) {
                    int s = rd.nextInt(4);
                    card = deck.popCard(Rank.getRankFromId((int)r), Suit.getSuitFromId((int)s));
                }
                cards.add(card);
            }
            cards.add(deck.deal());
            cards.add(deck.deal());
            if (CardLibUtils.calculateTypePoker(cards) == GroupType.ThreeOfKind) {
                loop = false;
                continue;
            }
            cards.clear();
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomHaiDoi() {
        int s;
        int i;
        Card card;
        int r1;
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        Deck deck = new Deck();
        int r2 = r1 = rd.nextInt(13);
        while (r2 == r1) {
            r2 = rd.nextInt(13);
        }
        for (i = 0; i < 2; ++i) {
            card = null;
            while (card == null) {
                s = rd.nextInt(4);
                card = deck.popCard(Rank.getRankFromId((int)r1), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        for (i = 0; i < 2; ++i) {
            card = null;
            while (card == null) {
                s = rd.nextInt(4);
                card = deck.popCard(Rank.getRankFromId((int)r2), Suit.getSuitFromId((int)s));
            }
            cards.add(card);
        }
        boolean loop = true;
        while (loop) {
            int r3 = r1;
            while (r3 == r1 || r3 == r2) {
                r3 = rd.nextInt(13);
            }
            s = rd.nextInt(4);
            Card c5 = new Card(Rank.getRankFromId((int)r3), Suit.getSuitFromId((int)s));
            cards.add(c5);
            if (CardLibUtils.calculateTypePoker(cards) == GroupType.TwoPair) {
                loop = false;
                continue;
            }
            cards.remove(c5);
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomMotDoiTo() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Random rd = new Random();
        boolean loop = true;
        while (loop) {
            int i;
            Deck deck = new Deck();
            int r1 = rd.nextInt(4) + 9;
            for (i = 0; i < 2; ++i) {
                Card card = null;
                while (card == null) {
                    int s = rd.nextInt(4);
                    card = deck.popCard(Rank.getRankFromId((int)r1), Suit.getSuitFromId((int)s));
                }
                cards.add(card);
            }
            for (i = 0; i < 3; ++i) {
                int r3 = r1;
                while (r3 == r1) {
                    r3 = rd.nextInt(13);
                }
                Card card = null;
                while (card == null) {
                    int s = rd.nextInt(4);
                    card = deck.popCard(Rank.getRankFromId((int)r3), Suit.getSuitFromId((int)s));
                }
                cards.add(card);
            }
            if (CardLibUtils.calculateTypePoker(cards) == GroupType.OnePair && CardLibUtils.pairEqualOrGreatJack(cards)) {
                loop = false;
                continue;
            }
            cards.clear();
        }
        Collections.shuffle(cards);
        return cards;
    }

    private List<Card> randomTruot() {
        List<Card> cards = new ArrayList<Card>();
        boolean loop = true;
        while (loop) {
            cards = this.randomCards();
            if ((CardLibUtils.calculateTypePoker(cards) != GroupType.OnePair || CardLibUtils.pairEqualOrGreatJack(cards)) && CardLibUtils.calculateTypePoker(cards) != GroupType.HighCard) continue;
            loop = false;
        }
        return cards;
    }

    public List<Card> randomCards() {
        ArrayList<Card> cards = new ArrayList<Card>();
        Deck deck = new Deck();
        deck.shuffle();
        for (int i = 0; i < 5; ++i) {
            Card card = deck.deal();
            cards.add(card);
        }
        return cards;
    }

    public List<Card> random() {
        ArrayList<Card> cards = new ArrayList<Card>();
        cards.add(new Card(Rank.Nine, Suit.Diamonds));
        cards.add(new Card(Rank.Seven, Suit.Diamonds));
        cards.add(new Card(Rank.Six, Suit.Diamonds));
        cards.add(new Card(Rank.Five, Suit.Diamonds));
        cards.add(new Card(Rank.Eight, Suit.Diamonds));
        return cards;
    }
}

