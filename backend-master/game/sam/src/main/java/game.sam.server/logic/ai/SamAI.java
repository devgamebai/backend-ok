/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.sam.server.logic.ai;

import bitzero.util.common.business.Debug;
import game.sam.server.logic.Card;
import game.sam.server.logic.GroupCard;
import game.sam.server.logic.ai.SamCard;
import game.sam.server.logic.ai.SamLogicHelper;
import java.util.ArrayList;
import java.util.List;

public class SamAI {
    public int mychair = 0;
    public boolean modeBaoSam = false;
    public List<SamCard> myCards = new ArrayList<SamCard>();
    public int enemyCardNumMin = 10;
    public int numGroup = 0;
    public boolean newRound = false;
    public List<SamCard> lastCards = new ArrayList<SamCard>();
    public static final int MAX_CARDS = 10;

    public void initStart(int chair, GroupCard gc) {
        this.mychair = chair;
        this.modeBaoSam = false;
        this.numGroup = 0;
        this.enemyCardNumMin = 10;
        this.myCards.clear();
        for (int i = 0; i < gc.cards.size(); ++i) {
            this.myCards.add(new SamCard(gc.cards.get(i)));
        }
    }

    public void throwMyCard(int chair, int cardsize, GroupCard playCard) {
        Card c;
        int i;
        this.lastCards.clear();
        for (i = 0; i < playCard.cards.size(); ++i) {
            c = playCard.cards.get(i);
            this.lastCards.add(new SamCard(c));
        }
        if (this.mychair == chair) {
            block1 : for (i = 0; i < playCard.cards.size(); ++i) {
                c = playCard.cards.get(i);
                for (int j = 0; j < this.myCards.size(); ++j) {
                    SamCard s = this.myCards.get(j);
                    if (c.ID != s.id) continue;
                    this.myCards.remove(j);
                    continue block1;
                }
            }
        } else {
            this.enemyCardNumMin = Math.min(cardsize, this.enemyCardNumMin);
        }
    }

    public boolean thinkBaoSam() {
        double kk = this.countDiemBoBaiBaoSam(new ArrayList<SamCard>());
        if (kk < 0.5) {
            this.modeBaoSam = true;
            return true;
        }
        return false;
    }

    public List<SamCard> thinkCardDanh() {
        ArrayList<SamCard> res = new ArrayList<SamCard>();
        this.numGroup = 0;
        this.countDiemBoBai(new ArrayList<SamCard>());
        if (this.newRound) {
            ArrayList<SamCard> res2 = new ArrayList<SamCard>();
            if (!this.modeBaoSam) {
                int kk = this.chonBoBaiDanh();
                for (int i = 0; i < this.myCards.size(); ++i) {
                    if (this.myCards.get((int)i).group != kk) continue;
                    res2.add(this.myCards.get(i));
                }
                res = res2;
            } else {
                int kk = this.chonBoBaiDanhBaoSam();
                for (int i = 0; i < this.myCards.size(); ++i) {
                    if (this.myCards.get((int)i).group != kk) continue;
                    res2.add(this.myCards.get(i));
                }
                res = res2;
            }
        } else if (!this.newRound) {
            int i;
            ArrayList<SamCard> inCards = new ArrayList<SamCard>();
            ArrayList<SamCard> cardDown = new ArrayList<SamCard>();
            for (i = 0; i < this.lastCards.size(); ++i) {
                inCards.add(this.lastCards.get(i));
            }
            for (i = 0; i < this.myCards.size(); ++i) {
                cardDown.add(this.myCards.get(i));
            }
            List<SamCard> res2 = this.timCardChatDuoc(inCards, cardDown);
            for (int i2 = 0; i2 < res2.size(); ++i2) {
                res.add(res2.get(i2));
            }
        }
        return res;
    }

    public int chonBoBaiDanh() {
        boolean has2;
        int j;
        double xs2;
        int i;
        int start;
        int num = this.numGroup;
        double diem = -1.0;
        int select = 0;
        int count = 0;
        int num2 = 0;
        for (int j2 = 0; j2 < this.myCards.size(); ++j2) {
            if (this.myCards.get((int)j2).so != 12) continue;
            ++num2;
        }
        int numYeu = 0;
        for (i = 1; i <= num; ++i) {
            has2 = true;
            start = -1;
            count = 0;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).group == i && this.myCards.get((int)j).so != 12) {
                    has2 = false;
                }
                if (this.myCards.get((int)j).group != i) continue;
                ++count;
                if (start != -1) continue;
                start = this.myCards.get((int)j).so;
            }
            if (count <= 0 || num2 > 0 && !has2 && count + num2 == this.myCards.size()) continue;
            double xs1 = this.xacXuatBiChan(count, start, false);
            xs2 = this.xacXuatChanDuoc(count, start, false);
            if (!(xs1 >= 0.2)) continue;
            ++numYeu;
        }
        for (i = 1; i <= num; ++i) {
            double temp;
            has2 = true;
            start = -1;
            count = 0;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).group == i && this.myCards.get((int)j).so != 12) {
                    has2 = false;
                }
                if (this.myCards.get((int)j).group != i) continue;
                ++count;
                if (start != -1) continue;
                start = this.myCards.get((int)j).so;
            }
            if (count <= 0 || num2 > 0 && !has2 && count + num2 == this.myCards.size()) continue;
            double xs1 = this.xacXuatBiChan(count, start, false);
            xs2 = this.xacXuatChanDuoc(count, start, false);
            if (numYeu <= 1 || this.enemyCardNumMin <= 2) {
                temp = -xs1 + (double)count * 0.05;
                if (!(temp > diem)) continue;
                diem = temp;
                select = i;
                continue;
            }
            temp = -xs2 + (double)count * 0.22;
            if (!(temp > diem)) continue;
            diem = temp;
            select = i;
        }
        return select;
    }

    public int chonBoBaiDanhBaoSam() {
        int num = this.numGroup;
        double diem = -10.0;
        int select = 0;
        int count = 0;
        int num2 = 0;
        for (int j = 0; j < this.myCards.size(); ++j) {
            if (this.myCards.get((int)j).so != 12) continue;
            ++num2;
        }
        for (int i = 1; i <= num; ++i) {
            boolean has2 = true;
            int start = -1;
            count = 0;
            for (int j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).group == i && this.myCards.get((int)j).so != 12) {
                    has2 = false;
                }
                if (this.myCards.get((int)j).group != i) continue;
                ++count;
                if (start != -1) continue;
                start = this.myCards.get((int)j).so;
            }
            if (count <= 0 || num2 > 0 && !has2 && count + num2 == this.myCards.size()) continue;
            double xs1 = this.xacXuatBiChan(count, start, false);
            double xs2 = this.xacXuatChanDuoc(count, start, false);
            double temp = -xs1;
            if (!(temp > diem)) continue;
            diem = temp;
            select = i;
        }
        return select;
    }

    public void sepBaiAI() {
    }

    public List<SamCard> timCardChatDuoc(List<SamCard> inCards, List<SamCard> cardHandon) {
        double diem = 100.0;
        List<SamCard> res2 = new ArrayList<SamCard>();
        for (int i = 0; i < cardHandon.size(); ++i) {
            double diemRes;
            SamCard select = cardHandon.get(i);
            List<SamCard> res = SamLogicHelper.recommend(inCards, cardHandon, select);
            if (res.size() < inCards.size() || !((diemRes = this.enemyCardNumMin <= inCards.size() ? (double)(-res.get((int)0).id) : this.countDiemBoBai(res)) < diem)) continue;
            diem = diemRes;
            res2 = res;
        }
        return res2;
    }

    public double countDiemBoBai(List<SamCard> cardOut) {
        int i;
        this.numGroup = 0;
        for (i = 0; i < this.myCards.size(); ++i) {
            this.myCards.get((int)i).mark = false;
            this.myCards.get((int)i).group = -1;
        }
        if (cardOut != null && cardOut.size() != 0) {
            for (i = 0; i < cardOut.size(); ++i) {
                for (int j = 0; j < this.myCards.size(); ++j) {
                    if (this.myCards.get((int)j).id != cardOut.get((int)i).id) continue;
                    this.myCards.get((int)j).mark = true;
                    this.myCards.get((int)j).group = -1;
                }
            }
        }
        double kk = this.xepBo();
        return kk;
    }

    public double countDiemBoBaiBaoSam(List<SamCard> cardOut) {
        int i;
        this.numGroup = 0;
        for (i = 0; i < this.myCards.size(); ++i) {
            this.myCards.get((int)i).mark = false;
            this.myCards.get((int)i).group = -1;
        }
        if (cardOut != null && cardOut.size() != 0) {
            for (i = 0; i < cardOut.size(); ++i) {
                for (int j = 0; j < this.myCards.size(); ++j) {
                    if (this.myCards.get((int)j).id != cardOut.get((int)i).id) continue;
                    this.myCards.get((int)j).mark = true;
                    this.myCards.get((int)j).group = -1;
                }
            }
        }
        double kk = this.xepBoBaoSam();
        double diemMax = 0.0;
        int maxGroup = -1;
        for (int group = 1; group <= this.numGroup; ++group) {
            int start = -1;
            int soLa = 0;
            for (int j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).group != group) continue;
                if (start == -1) {
                    start = this.myCards.get((int)j).so;
                }
                ++soLa;
            }
            if (soLa == 0) continue;
            double temp = this.countDiemBaoSam(soLa, start, false);
            Debug.trace((Object[])new Object[]{"countDiemBaoSam", temp, soLa, start});
            if (!(temp > diemMax)) continue;
            maxGroup = group;
            diemMax = temp;
        }
        Debug.trace((Object[])new Object[]{"Diem bao sam:", kk, diemMax, kk - diemMax});
        return kk - diemMax;
    }

    public double xepBo() {
        int k;
        double temp;
        int i;
        int i2;
        int countHasPush;
        int j;
        ++this.numGroup;
        int startNum = this.numGroup;
        int start = 0;
        double sum = 0.0;
        int[] doneGroup = new int[10];
        boolean[] curMark = new boolean[10];
        int[] curGroup = new int[10];
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            curMark[i2] = this.myCards.get((int)i2).mark;
            curGroup[i2] = this.myCards.get((int)i2).group;
        }
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            if (this.myCards.get((int)i2).mark.booleanValue()) continue;
            start = i2;
            break;
        }
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            if (this.myCards.get((int)i2).mark.booleanValue()) continue;
            sum += 1.0;
        }
        int numSo = 0;
        for (int i3 = 0; i3 < this.myCards.size(); ++i3) {
            if (this.myCards.get((int)i3).mark.booleanValue() || this.myCards.get((int)i3).so != this.myCards.get((int)start).so) continue;
            ++numSo;
        }
        int numSanh = 0;
        int sanhSo = this.myCards.get((int)start).so;
        for (i = 0; i < this.myCards.size(); ++i) {
            if (this.myCards.get((int)i).mark.booleanValue() || this.myCards.get((int)i).so != sanhSo || this.myCards.get((int)i).so == 12) continue;
            ++numSanh;
            ++sanhSo;
        }
        for (i = 1; i <= numSo; ++i) {
            this.numGroup = startNum;
            countHasPush = 0;
            for (k = 0; k < this.myCards.size(); ++k) {
                this.myCards.get((int)k).mark = curMark[k];
                this.myCards.get((int)k).group = curGroup[k];
            }
            this.myCards.get((int)start).mark = true;
            this.myCards.get((int)start).group = this.numGroup;
            ++countHasPush;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).mark.booleanValue() || this.myCards.get((int)j).so != this.myCards.get((int)start).so || countHasPush >= i || j == start) continue;
                ++countHasPush;
                this.myCards.get((int)j).mark = true;
                this.myCards.get((int)j).group = this.numGroup;
            }
            int countNum = 0;
            for (int ki = 0; ki < this.myCards.size(); ++ki) {
                if (this.myCards.get((int)ki).mark.booleanValue()) continue;
                ++countNum;
            }
            this.countDiem(i, this.myCards.get((int)start).so);
            temp = countNum > 0 ? this.countDiem(i, this.myCards.get((int)start).so) + this.xepBo() : this.countDiem(i, this.myCards.get((int)start).so);
            if (!(temp < sum)) continue;
            sum = temp;
            for (int iCard = 0; iCard < this.myCards.size(); ++iCard) {
                doneGroup[iCard] = this.myCards.get((int)iCard).group;
            }
        }
        for (i = 3; i <= numSanh; ++i) {
            countHasPush = 0;
            this.numGroup = startNum;
            for (k = 0; k < this.myCards.size(); ++k) {
                this.myCards.get((int)k).mark = curMark[k];
                this.myCards.get((int)k).group = curGroup[k];
            }
            this.myCards.get((int)start).mark = true;
            this.myCards.get((int)start).group = this.numGroup;
            ++countHasPush;
            sanhSo = this.myCards.get((int)start).so;
            ++sanhSo;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).mark.booleanValue() || this.myCards.get((int)j).so != sanhSo || countHasPush >= i || this.myCards.get((int)j).so == 12 || j == start) continue;
                ++countHasPush;
                ++sanhSo;
                this.myCards.get((int)j).mark = true;
                this.myCards.get((int)j).group = this.numGroup;
            }
            temp = this.countDiem(i, this.myCards.get((int)start).so) + this.xepBo();
            if (!(temp < sum)) continue;
            sum = temp;
            for (j = 0; j < this.myCards.size(); ++j) {
                doneGroup[j] = this.myCards.get((int)j).group;
            }
        }
        for (int j2 = 0; j2 < this.myCards.size(); ++j2) {
            this.myCards.get((int)j2).group = doneGroup[j2];
        }
        return sum;
    }

    public double xepBoBaoSam() {
        int countHasPush;
        double temp;
        int ki;
        int j;
        int k;
        int i;
        int i2;
        int countNum;
        ++this.numGroup;
        int startNum = this.numGroup;
        int start = 0;
        double sum = 0.0;
        boolean[] curMark = new boolean[10];
        int[] curGroup = new int[10];
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            curMark[i2] = this.myCards.get((int)i2).mark;
            curGroup[i2] = this.myCards.get((int)i2).group;
        }
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            if (this.myCards.get((int)i2).mark.booleanValue()) continue;
            start = i2;
            break;
        }
        for (i2 = 0; i2 < this.myCards.size(); ++i2) {
            if (this.myCards.get((int)i2).mark.booleanValue()) continue;
            sum += 1.0;
        }
        int numSo = 0;
        for (int i3 = 0; i3 < this.myCards.size(); ++i3) {
            if (this.myCards.get((int)i3).mark.booleanValue() || this.myCards.get((int)i3).so != this.myCards.get((int)start).so) continue;
            ++numSo;
        }
        int numSanh = 0;
        int sanhSo = this.myCards.get((int)start).so;
        for (i = 0; i < this.myCards.size(); ++i) {
            if (this.myCards.get((int)i).mark.booleanValue() || this.myCards.get((int)i).so != sanhSo || this.myCards.get((int)i).so == 12) continue;
            ++numSanh;
            ++sanhSo;
        }
        for (i = 1; i <= numSo; ++i) {
            this.numGroup = startNum;
            countHasPush = 0;
            for (k = 0; k < this.myCards.size(); ++k) {
                this.myCards.get((int)k).mark = curMark[k];
                this.myCards.get((int)k).group = curGroup[k];
            }
            this.myCards.get((int)start).mark = true;
            this.myCards.get((int)start).group = this.numGroup;
            ++countHasPush;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).mark.booleanValue() || this.myCards.get((int)j).so != this.myCards.get((int)start).so || countHasPush >= i || j == start) continue;
                ++countHasPush;
                this.myCards.get((int)j).mark = true;
                this.myCards.get((int)j).group = this.numGroup;
            }
            countNum = 0;
            for (ki = 0; ki < this.myCards.size(); ++ki) {
                if (this.myCards.get((int)ki).mark.booleanValue()) continue;
                ++countNum;
            }
            temp = countNum > 0 ? this.countDiemBaoSam(i, this.myCards.get((int)start).so, false) + this.xepBoBaoSam() : this.countDiemBaoSam(i, this.myCards.get((int)start).so, false);
            if (!(temp < sum)) continue;
            sum = temp;
        }
        for (i = 3; i <= numSanh; ++i) {
            countHasPush = 0;
            this.numGroup = startNum;
            for (k = 0; k < this.myCards.size(); ++k) {
                this.myCards.get((int)k).mark = curMark[k];
                this.myCards.get((int)k).group = curGroup[k];
            }
            this.myCards.get((int)start).mark = true;
            this.myCards.get((int)start).group = this.numGroup;
            ++countHasPush;
            sanhSo = this.myCards.get((int)start).so;
            ++sanhSo;
            for (j = 0; j < this.myCards.size(); ++j) {
                if (this.myCards.get((int)j).mark.booleanValue() || this.myCards.get((int)j).so != sanhSo || countHasPush >= i || this.myCards.get((int)j).so == 12 || j == start) continue;
                ++countHasPush;
                ++sanhSo;
                this.myCards.get((int)j).mark = true;
                this.myCards.get((int)j).group = this.numGroup;
            }
            countNum = 0;
            for (ki = 0; ki < this.myCards.size(); ++ki) {
                if (this.myCards.get((int)ki).mark.booleanValue()) continue;
                ++countNum;
            }
            temp = countNum > 0 ? this.countDiemBaoSam(i, this.myCards.get((int)start).so, true) + this.xepBoBaoSam() : this.countDiemBaoSam(i, this.myCards.get((int)start).so, true);
            if (!(temp < sum)) continue;
            sum = temp;
        }
        return sum;
    }

    public double countDiem(int soLa, int start) {
        if (soLa == 1) {
            return (double)(12 - start) / 12.0;
        }
        if (soLa == 2) {
            return (double)(12 - start) / 24.0;
        }
        return (double)(12 - start) / 12.0 / (double)soLa;
    }

    public double countDiemBaoSam(int soLa, int start, boolean isSanh) {
        return this.xacXuatBiChan(soLa, start, isSanh);
    }

    public double xacXuatBiChan(int b, int c, boolean a) {
        boolean isSanh = true;
        isSanh = !a;
        if (b == 1 && c == 12) {
            return 0.15;
        }
        if (b == 1 && c == 11) {
            return 0.75;
        }
        if (b == 1) {
            return 0.95 + (double)(12 - c) / 12.0 * 0.05;
        }
        if (b == 2 && c == 12) {
            return 0.01;
        }
        if (b == 2 && c == 11) {
            return 0.4;
        }
        if (b == 2 && c == 10) {
            return 0.75;
        }
        if (b == 2 && c == 9) {
            return 0.9;
        }
        if (b == 2) {
            return 1.0;
        }
        if (b == 3 && c == 12) {
            return 0.0;
        }
        if (b == 3 && c == 11) {
            return 0.2;
        }
        if (b == 3 && c == 10) {
            return 0.35;
        }
        if (b == 3 && !isSanh) {
            return 0.5;
        }
        if (b == 3 && isSanh) {
            return 0.8 * (double)(11 - c - b + 1) / 12.0;
        }
        if (b == 4 && !isSanh) {
            return 0.95;
        }
        if (b == 4 && isSanh) {
            return 0.7 * (double)(11 - c - b + 1) / 12.0;
        }
        if (b == 5) {
            return 0.5 * (double)(11 - c - b + 1) / 12.0;
        }
        return 0.2 * (double)(11 - c - b + 1) / 12.0;
    }

    public double xacXuatChanDuoc(int b, int c, boolean a) {
        return (double)c / 12.0;
    }

    public double xacXuatTuonBai(int b, int c, boolean a) {
        return 0.0;
    }
}

