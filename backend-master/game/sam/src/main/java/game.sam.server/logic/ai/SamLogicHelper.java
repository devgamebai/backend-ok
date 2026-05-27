/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic.ai;

import game.sam.server.logic.ai.CardGroup;
import game.sam.server.logic.ai.SamCard;
import java.util.ArrayList;
import java.util.List;

public class SamLogicHelper {
    public static int compareCard(SamCard card1, SamCard card2) {
        if (card1.so > card1.so) {
            return 1;
        }
        if (card1.so == card2.so) {
            return 0;
        }
        return -1;
    }

    public static Boolean kiemtraDanh(List<SamCard> cards) {
        CardGroup group = new CardGroup(cards);
        if (group.typeGroup != -1) {
            return true;
        }
        return false;
    }

    public static Boolean kiemtraChatQuan(CardGroup a, CardGroup b) {
        if (a.typeGroup != b.typeGroup) {
            if (a.typeGroup == 1 && a.cards.get((int)0).so == 12 && b.typeGroup == 5) {
                return true;
            }
            if (a.typeGroup == 2 && a.cards.get((int)0).so == 12 && b.typeGroup == 6) {
                return true;
            }
            return false;
        }
        switch (a.typeGroup) {
            case 1: 
            case 2: 
            case 3: {
                return SamCard.isGreater(b.cards.get(0), a.cards.get(0));
            }
            case 4: {
                List<SamCard> sanhArrayA = a.makeSanhArray();
                List<SamCard> sanhArrayB = b.makeSanhArray();
                if (sanhArrayA.size() != sanhArrayB.size() || sanhArrayA.get((int)(sanhArrayA.size() - 1)).so >= sanhArrayB.get((int)(sanhArrayB.size() - 1)).so) {
                    return false;
                }
                return true;
            }
            case 5: {
                return SamCard.isGreater(b.cards.get(0), a.cards.get(0));
            }
            case 6: {
                return false;
            }
        }
        return false;
    }

    public static int changeSo(int so) {
        if (so == 11 || so == 12) {
            return so - 13;
        }
        return so;
    }

    public static List<SamCard> recommend(List<SamCard> inCards, List<SamCard> cardHandon, SamCard select) {
        List<SamCard> cards = inCards;
        List<SamCard> cardsB = cardHandon;
        int len = inCards.size();
        CardGroup groupA = new CardGroup(cards);
        int num2 = 0;
        for (int i = 0; i < cardHandon.size(); ++i) {
            if (cardHandon.get((int)i).so != 12) continue;
            ++num2;
        }
        List<SamCard> recommend = new ArrayList<SamCard>();
        if (num2 + inCards.size() >= cardHandon.size() && select.so != 12 && num2 > 0) {
            return recommend;
        }
        switch (groupA.typeGroup) {
            case 1: {
                if (cards.size() == 1 && cards.get((int)0).so == 12) {
                    ArrayList<SamCard> res = new ArrayList<SamCard>();
                    for (int i = 0; i < cardHandon.size(); ++i) {
                        if (cardsB.get((int)i).so != select.so) continue;
                        res.add(cardsB.get(i));
                    }
                    if (res.size() == 4) {
                        return res;
                    }
                    if (select.so > groupA.cards.get((int)0).so) {
                        return recommend;
                    }
                    return recommend;
                }
                if (cards.size() != 1) break;
                if (select.so > groupA.cards.get((int)0).so) {
                    recommend.add(new SamCard(select.id));
                }
                return recommend;
            }
            case 2: 
            case 3: {
                ArrayList<SamCard> res = new ArrayList<SamCard>();
                res.add(select);
                for (int i = 0; i < cardsB.size(); ++i) {
                    if (cardsB.get((int)i).so != select.so || cardsB.get((int)i).id == select.id || res.size() >= cards.size()) continue;
                    res.add(cardsB.get(i));
                }
                if (res.size() == cards.size() && ((SamCard)res.get((int)(len - 1))).so > cards.get((int)(len - 1)).so) {
                    return res;
                }
                return recommend;
            }
            case 5: {
                return recommend;
            }
            case 4: {
                recommend = SamLogicHelper.timsanhdocchatduoc(cards, cardHandon, select);
                return recommend;
            }
        }
        return recommend;
    }

    public static List<SamCard> timsanhdocchatduoc(List<SamCard> cards, List<SamCard> handOn, SamCard select) {
        CardGroup group = new CardGroup(cards);
        List<SamCard> cardArray = group.makeSanhArray();
        int len = cardArray.size();
        ArrayList<SamCard> res = new ArrayList<SamCard>();
        res.add(select);
        ArrayList<SamCard> res2 = new ArrayList<SamCard>();
        for (int j = -2; j <= 9; ++j) {
            int countSanh = 0;
            res2 = new ArrayList();
            Boolean holdSelect = false;
            if ((double)(j + len - 1) <= Math.floor(cardArray.get((int)(len - 1)).id / 4)) continue;
            for (int i = 0; i < cardArray.size(); ++i) {
                Boolean res3 = false;
                for (int k = 0; k < handOn.size(); ++k) {
                    int kkk;
                    int kk = SamCard.convertSo(handOn.get((int)k).so);
                    if (kk != (kkk = SamCard.convertSo(j + i)) || j + i >= 12) continue;
                    res3 = true;
                    if (handOn.get((int)k).so == select.so) {
                        res2.add(select);
                        holdSelect = true;
                        break;
                    }
                    res2.add(handOn.get(k));
                    break;
                }
                if (!res3.booleanValue()) continue;
                ++countSanh;
            }
            if (countSanh != cardArray.size() || !holdSelect.booleanValue()) continue;
            res = res2;
            break;
        }
        return res;
    }

    public static List<SamCard> timdoituquychat2(List<SamCard> cards, List<SamCard> cardHandon, SamCard cardselect) {
        return new ArrayList<SamCard>();
    }
}

