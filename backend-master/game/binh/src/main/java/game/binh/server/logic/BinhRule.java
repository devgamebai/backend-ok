/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.binh.server.logic;

import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.PlayerCard;
import game.binh.server.logic.SoSanhChi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinhRule {
    private static Logger logger = LoggerFactory.getLogger((String)"BinhLogic");

    public static synchronized int SoSanhChi2(GroupCard gc1, GroupCard gc2) {
        if (gc1.kiemtraBo(1) < gc2.kiemtraBo(1)) {
            return 1;
        }
        if (gc1.kiemtraBo(1) > gc2.kiemtraBo(1)) {
            return -1;
        }
        if (gc1.kiemtraBo(1) == gc2.kiemtraBo(1)) {
            switch (gc1.kiemtraBo(1)) {
                case 8: 
                case 12: {
                    int max1 = gc1.GetMaxNumber();
                    int max2 = gc2.GetMaxNumber();
                    if (max1 > max2) {
                        return 1;
                    }
                    if (max1 < max2) {
                        return -1;
                    }
                    if (max1 != max2 || max1 != 14) break;
                    max1 = gc1.GetSecondMaxNumber();
                    max2 = gc2.GetSecondMaxNumber();
                    if (max1 > max1) {
                        return 1;
                    }
                    if (max1 >= max2) break;
                    return -1;
                }
                case 9: 
                case 10: 
                case 11: 
                case 13: 
                case 14: 
                case 15: 
                case 16: {
                    int smaller = gc1.cards.size();
                    if (gc1.cards.size() > gc2.cards.size()) {
                        smaller = gc2.cards.size();
                    }
                    for (int i = 0; i < smaller; ++i) {
                        Card c1 = gc1.cards.get(i);
                        Card c2 = gc2.cards.get(i);
                        if (c1.SO > c2.SO) {
                            return 1;
                        }
                        if (c1.SO >= c2.SO) continue;
                        return -1;
                    }
                }
            }
        }
        return 0;
    }

    public static synchronized int SoSanhChi1(GroupCard gc1, GroupCard gc2) {
        if (gc1.kiemtraBo(0) < gc2.kiemtraBo(0)) {
            return 1;
        }
        if (gc1.kiemtraBo(0) > gc2.kiemtraBo(0)) {
            return -1;
        }
        if (gc1.kiemtraBo(0) == gc2.kiemtraBo(0)) {
            switch (gc1.kiemtraBo(0)) {
                case 8: 
                case 12: {
                    for (int i = gc1.cards.size() - 1; i >= 0; --i) {
                        Card c1 = gc1.cards.get(i);
                        Card c2 = gc2.cards.get(i);
                        if (c1.SO > c2.SO) {
                            return 1;
                        }
                        if (c1.SO >= c2.SO) continue;
                        return -1;
                    }
                }
                case 9: 
                case 10: 
                case 11: 
                case 13: 
                case 14: 
                case 15: 
                case 16: {
                    int smaller = gc1.cards.size();
                    if (gc1.cards.size() > gc2.cards.size()) {
                        smaller = gc2.cards.size();
                    }
                    for (int i = 0; i < smaller; ++i) {
                        Card c1 = gc1.cards.get(i);
                        Card c2 = gc2.cards.get(i);
                        if (c1.SO > c2.SO) {
                            return 1;
                        }
                        if (c1.SO >= c2.SO) continue;
                        return -1;
                    }
                }
            }
        }
        return 0;
    }

    public static synchronized SoSanhChi BinhChiMode1(GroupCard gc1, GroupCard gc2, int chi) {
        int kind1 = gc1.kiemtraBo(0);
        int kind2 = gc2.kiemtraBo(0);
        SoSanhChi sc = new SoSanhChi();
        int kq = BinhRule.SoSanhChi1(gc1, gc2);
        int rate = 1;
        if (kind1 == 8 || kind2 == 8) {
            if (chi == 1) {
                rate = 10;
            } else if (chi == 2) {
                rate = 20;
            }
        } else if (kind1 == 9 || kind2 == 9) {
            rate = 8;
            if (chi == 2) {
                rate = 16;
            }
        } else if (kind1 == 10 || kind2 == 10) {
            if (chi == 2) {
                rate = 4;
            }
        } else if ((kind1 == 13 || kind2 == 13) && chi == 3) {
            rate = 6;
        }
        sc.chiCount1 = kq * rate;
        sc.chiCount2 = -1 * kq * rate;
        return sc;
    }

    public static synchronized SoSanhChi BinhChiMode2(GroupCard gc1, GroupCard gc2, int chi) {
        int kq = BinhRule.SoSanhChi2(gc1, gc2);
        int kind1 = gc1.kiemtraBo(1);
        int kind2 = gc2.kiemtraBo(1);
        SoSanhChi sc = new SoSanhChi();
        int rate = 1;
        if (kind1 == 8 || kind2 == 8) {
            if (chi == 1) {
                rate = 10;
            } else if (chi == 2) {
                rate = 20;
            }
        } else if (kind1 == 9 || kind2 == 9) {
            rate = 8;
            if (chi == 2) {
                rate = 16;
            }
            if (gc1.coTuQuyAt() || gc2.coTuQuyAt()) {
                rate = 20;
            }
        } else if (kind1 == 10 || kind2 == 10) {
            if (chi == 2) {
                rate = 4;
            }
        } else if (kind1 == 13 || kind2 == 13) {
            rate = 1;
            if (chi == 3) {
                rate = 6;
                if (gc1.hasA() && kind1 == 13 || kind2 == 13 && gc2.hasA()) {
                    rate = 20;
                }
            }
        }
        sc.chiCount1 = kq * rate;
        sc.chiCount2 = -1 * kq * rate;
        return sc;
    }

    public static int demChiPhatBinhLung2(PlayerCard pc, int chi) {
        int sc = 0;
        GroupCard gc1 = null;
        switch (chi) {
            case 1: {
                gc1 = pc.ChiMot();
                break;
            }
            case 2: {
                gc1 = pc.ChiHai();
                break;
            }
            case 3: {
                gc1 = pc.ChiBa();
            }
        }
        int kind1 = gc1.kiemtraBo(1);
        switch (kind1) {
            case 8: {
                if (chi == 1) {
                    sc += 10;
                }
                if (chi != 2) break;
                sc += 20;
                break;
            }
            case 9: {
                if (chi == 1) {
                    sc += 8;
                }
                if (chi != 2) break;
                sc += 16;
                break;
            }
            case 10: {
                if (chi == 2) {
                    sc += 4;
                    break;
                }
                ++sc;
                break;
            }
            case 13: {
                if (chi == 3) {
                    sc += 6;
                    break;
                }
                ++sc;
                break;
            }
            default: {
                ++sc;
            }
        }
        return sc;
    }

    public static int demChiPhatBinhLung1(PlayerCard pc, int chi) {
        int sc = 0;
        GroupCard gc1 = null;
        switch (chi) {
            case 1: {
                gc1 = pc.ChiMot();
                break;
            }
            case 2: {
                gc1 = pc.ChiHai();
                break;
            }
            case 3: {
                gc1 = pc.ChiBa();
            }
        }
        int kind1 = gc1.kiemtraBo(0);
        switch (kind1) {
            case 8: {
                if (chi == 1) {
                    sc += 10;
                }
                if (chi != 2) break;
                sc += 20;
                break;
            }
            case 9: {
                if (gc1.coTuQuyAt()) {
                    sc += 20;
                    break;
                }
                if (chi == 1) {
                    sc += 8;
                    break;
                }
                if (chi != 2) break;
                sc += 16;
                break;
            }
            case 10: {
                if (chi == 2) {
                    sc += 4;
                    break;
                }
                ++sc;
                break;
            }
            case 13: {
                if (chi == 3) {
                    if (gc1.hasA()) {
                        sc += 20;
                        break;
                    }
                    sc += 6;
                    break;
                }
                ++sc;
                break;
            }
            default: {
                ++sc;
            }
        }
        return sc;
    }

    public static SoSanhChi BinhChiLungMode1(PlayerCard pc1, PlayerCard pc2, int chi) {
        SoSanhChi sc = new SoSanhChi();
        int playerCardKind1 = pc1.GetPlayerCardsKind(0);
        int playerCardKind2 = pc2.GetPlayerCardsKind(0);
        if (playerCardKind1 == 6 && playerCardKind2 == 7) {
            sc.chiCount1 = BinhRule.demChiPhatBinhLung1(pc1, chi);
            sc.chiCount2 = -sc.chiCount1;
        }
        if (playerCardKind1 == 7 && playerCardKind2 == 6) {
            sc.chiCount1 = -BinhRule.demChiPhatBinhLung1(pc2, chi);
            sc.chiCount2 = -sc.chiCount1;
        }
        return sc;
    }

    public static SoSanhChi BinhChiLungMode2(PlayerCard pc1, PlayerCard pc2, int chi) {
        SoSanhChi sc = new SoSanhChi();
        int playerCardKind1 = pc1.GetPlayerCardsKind(1);
        int playerCardKind2 = pc2.GetPlayerCardsKind(1);
        if (playerCardKind1 == 6 && playerCardKind2 == 7) {
            sc.chiCount1 = BinhRule.demChiPhatBinhLung2(pc1, chi);
            sc.chiCount2 = -sc.chiCount1;
        }
        if (playerCardKind1 == 7 && playerCardKind2 == 6) {
            sc.chiCount1 = -BinhRule.demChiPhatBinhLung2(pc2, chi);
            sc.chiCount2 = -sc.chiCount1;
        }
        return sc;
    }

    public static synchronized SoSanhChi BinhLungMode1(PlayerCard pc1, PlayerCard pc2) {
        SoSanhChi sc = new SoSanhChi();
        SoSanhChi sc1 = BinhRule.BinhChiLungMode1(pc1, pc2, 1);
        SoSanhChi sc2 = BinhRule.BinhChiLungMode1(pc1, pc2, 2);
        SoSanhChi sc3 = BinhRule.BinhChiLungMode1(pc1, pc2, 3);
        sc.chiCount1 = 2 * (sc1.chiCount1 + sc2.chiCount1 + sc3.chiCount1);
        sc.chiCount2 = 2 * (sc1.chiCount2 + sc2.chiCount2 + sc3.chiCount2);
        return sc;
    }

    public static synchronized SoSanhChi BinhLungMode2(PlayerCard pc1, PlayerCard pc2) {
        SoSanhChi sc = new SoSanhChi();
        SoSanhChi sc1 = BinhRule.BinhChiLungMode2(pc1, pc2, 1);
        SoSanhChi sc2 = BinhRule.BinhChiLungMode2(pc1, pc2, 2);
        SoSanhChi sc3 = BinhRule.BinhChiLungMode2(pc1, pc2, 3);
        sc.chiCount1 = 2 * (sc1.chiCount1 + sc2.chiCount1 + sc3.chiCount1);
        sc.chiCount2 = 2 * (sc1.chiCount2 + sc2.chiCount2 + sc3.chiCount2);
        return sc;
    }

    public static GroupCard timBaCaiSanh(GroupCard gc) {
        GroupCard baSanh = new GroupCard();
        ArrayList<Integer> listSo = new ArrayList<Integer>();
        for (int i = 0; i < gc.cards.size(); ++i) {
            Card c = gc.cards.get(i);
            listSo.add(c.SO);
        }
        List<Integer> listSanh = BinhRule.timTatCaSanh(listSo);
        if (listSanh == null) {
            return null;
        }
        boolean[] used = new boolean[gc.cards.size()];
        block1 : for (int i = 0; i < listSanh.size(); ++i) {
            int x = listSanh.get(i);
            for (int j = 0; j < gc.cards.size(); ++j) {
                if (used[j]) continue;
                Card c = gc.cards.get(j);
                if (c.SO != x) continue;
                baSanh.cards.add(c);
                used[j] = true;
                continue block1;
            }
        }
        return baSanh;
    }

    private static List<Integer> subtract(List<Integer> parent, List<Integer> sub) {
        int i;
        ArrayList<Integer> copy = new ArrayList<Integer>(parent.size());
        for (i = 0; i < parent.size(); ++i) {
            Integer cur = parent.get(i);
            copy.add(cur);
        }
        for (i = 0; i < sub.size(); ++i) {
            Integer x = sub.get(i);
            copy.remove(x);
        }
        return copy;
    }

    private static boolean checkSanh(List<Integer> listSo) {
        int prev = -10;
        int begin = -10;
        for (int i = 0; i < listSo.size(); ++i) {
            int cur = listSo.get(i);
            if (begin == -10) {
                begin = cur;
                prev = cur;
                continue;
            }
            if (!BinhRule.isNextTo(begin, prev, cur)) {
                return false;
            }
            prev = cur;
        }
        return true;
    }

    private static List<Integer> timTatCaSanh(List<Integer> listSo) {
        Collections.sort(listSo);
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < listSo.size(); ++i) {
            List<Integer> sanh51 = BinhRule.timSanh5(i, listSo);
            if (sanh51 == null) continue;
            result.clear();
            result.addAll(sanh51);
            List<Integer> newList = BinhRule.subtract(listSo, sanh51);
            for (int j = 0; j < newList.size(); ++j) {
                List<Integer> sanh52 = BinhRule.timSanh5(j, newList);
                if (sanh52 == null) continue;
                result.addAll(sanh52);
                List<Integer> lastList = BinhRule.subtract(newList, sanh52);
                if (BinhRule.checkSanh(lastList)) {
                    result.addAll(lastList);
                    return result;
                }
                result = BinhRule.subtract(result, sanh52);
            }
        }
        return null;
    }

    private static List<Integer> timSanh5(int from, List<Integer> listSo) {
        ArrayList<Integer> longest = new ArrayList<Integer>();
        int size = listSo.size();
        int prev = -10;
        int begin = -10;
        for (int i = from; i < from + size; ++i) {
            int index = i % size;
            int cur = listSo.get(index);
            if (longest.size() == 0) {
                begin = cur;
                longest.add(cur);
                prev = cur;
            } else if (BinhRule.isNextTo(begin, prev, cur)) {
                longest.add(cur);
                prev = cur;
            }
            if (longest.size() == 5) break;
        }
        if (longest.size() == 5) {
            return longest;
        }
        return null;
    }

    private static boolean isNextTo(int begin, int prev, int next) {
        if (next == prev + 1) {
            return true;
        }
        return begin == 2 && next == 14 || begin == 14 && next == 2 && prev != 2;
    }

    public static boolean isMauBinh(int kind) {
        return kind >= 0 && kind <= 5;
    }

    public static long GetPlayerCardMauBinhRate(int kind) {
        int rate = 1;
        switch (kind) {
            case 0: {
                rate = 72;
                break;
            }
            case 1: {
                rate = 30;
                break;
            }
            case 2: {
                rate = 24;
                break;
            }
            case 3: {
                rate = 18;
                break;
            }
            case 4: {
                rate = 18;
                break;
            }
            case 5: {
                rate = 18;
            }
        }
        return rate;
    }

    public static long getSoLaThangAt(int soLaAt) {
        if (soLaAt == 0) {
            return -4L;
        }
        if (soLaAt == 2) {
            return 4L;
        }
        if (soLaAt == 3) {
            return 8L;
        }
        if (soLaAt == 4) {
            return 12L;
        }
        return 0L;
    }
}

