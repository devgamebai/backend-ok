/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.GroupType
 *  com.vinplay.cardlib.utils.CardLibUtils
 */
package game.modules.minigame.test;

import bitzero.server.entities.User;
import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.utils.CardLibUtils;
import game.modules.minigame.utils.GenerationMiniPoker;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class TestMiniPoker {
    private int soLanNoHu = 0;
    private int soLanNoHuKhongDuTraThuong = 0;
    private int soLanKhongDuTraThuong = 0;
    private int thungPhaSanhNho = 0;
    private int tuQuy = 0;
    private int cuLu = 0;
    private int thung = 0;
    private int sanh = 0;
    private int samCo = 0;
    private int haiDoi = 0;
    private int motDoiTo = 0;
    private int motDoiNho = 0;
    private int mauThau = 0;
    private long tongTraThuong = 0L;
    private long tongTienAn = 0L;
    private GenerationMiniPoker gen = new GenerationMiniPoker();
    private int randomLoi = 0;
    private long pot = 500000L;
    private static final int initPotValue = 500000;
    private long fund = 3000000L;
    private short moneyType = 1;
    private long currentMoney = 1000000000L;
    private long betValue = 100L;
    private int khongDuTien = 0;

    private void play(String username) {
        StringBuilder builder = new StringBuilder();
        int result = 0;
        long prize = 0L;
        if (this.betValue > 0L) {
            if (this.currentMoney >= this.betValue) {
                boolean enoughToPair = false;
                long moneyToPot = this.betValue * 1L / 100L;
                long moneyToSystem = this.betValue * 2L / 100L;
                long moneyToFund = this.betValue - moneyToPot - moneyToSystem;
                this.pot += moneyToPot;
                this.fund += moneyToFund;
                block11 : while (!enoughToPair) {
                    prize = 0L;
                    long moneyExchange = 0L;
                    List<Card> cards = this.gen.randomCards2();
                    if (cards.size() != 5) {
                        System.out.println("Random loi");
                        ++this.randomLoi;
                        continue;
                    }
                    GroupType groupType = CardLibUtils.calculateTypePoker(cards);
                    if (groupType == null) continue;
                    switch (groupType) {
                        case HighCard: {
                            result = 11;
                            ++this.mauThau;
                            break;
                        }
                        case OnePair: {
                            if (CardLibUtils.pairEqualOrGreatJack(cards)) {
                                result = 10;
                                prize = (int)((float)this.betValue * 2.5f);
                                ++this.motDoiTo;
                                break;
                            }
                            result = 9;
                            prize = 0L;
                            ++this.motDoiNho;
                            break;
                        }
                        case TwoPair: {
                            result = 8;
                            prize = this.betValue * 5L;
                            ++this.haiDoi;
                            break;
                        }
                        case ThreeOfKind: {
                            result = 7;
                            prize = this.betValue * 8L;
                            ++this.samCo;
                            break;
                        }
                        case Straight: {
                            result = 6;
                            prize = this.moneyType == 1 ? this.betValue * 13L : this.betValue * 12L;
                            ++this.sanh;
                            break;
                        }
                        case Flush: {
                            result = 5;
                            prize = this.moneyType == 1 ? this.betValue * 20L : this.betValue * 18L;
                            ++this.thung;
                            break;
                        }
                        case FullHouse: {
                            result = 4;
                            prize = this.moneyType == 1 ? this.betValue * 50L : this.betValue * 40L;
                            ++this.cuLu;
                            break;
                        }
                        case FourOfKind: {
                            result = 3;
                            prize = this.moneyType == 1 ? this.betValue * 150L : this.betValue * 120L;
                            ++this.tuQuy;
                            break;
                        }
                        case StraightFlush: {
                            if (CardLibUtils.isStraightFlushJack(cards)) {
                                if (!this.checkDienKienNoHu(username)) continue block11;
                                result = 1;
                                prize = this.pot;
                                break;
                            }
                            result = 2;
                            prize = this.betValue * 1000L;
                            ++this.thungPhaSanhNho;
                        }
                    }
                    moneyExchange = prize - this.betValue;
                    long fundExchange = prize > 0L ? prize : 0L;
                    long l = fundExchange;
                    if (result == 1) {
                        if (this.fund - 500000L < 0L) {
                            ++this.soLanNoHuKhongDuTraThuong;
                            ++this.soLanKhongDuTraThuong;
                            continue;
                        }
                        ++this.soLanNoHu;
                    } else if (this.fund - fundExchange < 0L) {
                        ++this.soLanKhongDuTraThuong;
                        continue;
                    }
                    enoughToPair = true;
                    if (prize > 0L) {
                        if (result == 1) {
                            this.pot = 500000L;
                            this.fund -= 500000L;
                        } else {
                            this.fund -= fundExchange;
                        }
                    }
                    this.tongTraThuong += prize;
                    if (moneyExchange != 0L) {
                        this.currentMoney += moneyExchange;
                    }
                    if (moneyExchange > 0L) {
                        this.tongTienAn += moneyExchange;
                    }
                    builder.append(cards.get(0).toString());
                    for (int i = 1; i < cards.size(); ++i) {
                        builder.append(",");
                        builder.append(cards.get(i).toString());
                    }
                }
                this.saveFund();
                this.savePot();
            } else {
                result = 102;
                ++this.khongDuTien;
            }
        } else {
            result = 101;
        }
    }

    private boolean checkDienKienNoHu(String username) {
        return true;
    }

    private void saveFund() {
    }

    private void savePot() {
    }

    public void updatePotToUser(User user) {
    }

    public static void main(String[] args) throws IOException {
        FileWriter fw = new FileWriter("D:/fund.log");
        TestMiniPoker test = new TestMiniPoker();
        long soLanChay = 10000000L;
        long maxTime = 0L;
        int i = 0;
        while ((long)i < soLanChay) {
            if (i % 100 == 0) {
                fw.write("" + test.fund + "\n");
                fw.flush();
            }
            long startTime = System.currentTimeMillis();
            test.play("banhcuon");
            long endTime = System.currentTimeMillis();
            if (maxTime < endTime - startTime) {
                maxTime = endTime - startTime;
            }
            ++i;
        }
        fw.close();
        System.out.println("Random loi: " + test.randomLoi);
        System.out.println("MAX TIME: " + maxTime + " (ms)");
        System.out.println("So lan khong du tien: " + test.khongDuTien);
        System.out.println("Hu con lai: " + test.pot);
        System.out.println("Quy con lai: " + test.fund);
        System.out.println("So lan no hu: " + test.soLanNoHu);
        System.out.println("So lan no hu khong du tra thuong: " + test.soLanNoHuKhongDuTraThuong);
        System.out.println("So lan khong du tra thuong: " + test.soLanKhongDuTraThuong);
        System.out.println("Thung pha sanh nho: " + test.thungPhaSanhNho);
        System.out.println("Tu quy: " + test.tuQuy);
        System.out.println("Cu lu: " + test.cuLu);
        System.out.println("Thung: " + test.thung);
        System.out.println("Sanh: " + test.sanh);
        System.out.println("Sam co: " + test.samCo);
        System.out.println("Hai doi: " + test.haiDoi);
        System.out.println("Mot doi to: " + test.motDoiTo);
        System.out.println("Mot doi nho: " + test.motDoiNho);
        System.out.println("Mau thau: " + test.mauThau);
        System.out.println("Tong tien an duoc (tru di tien dat): " + test.tongTienAn);
        System.out.println("Tong tra thuong: " + test.tongTraThuong);
        System.out.println("So tien con lai nguoi choi: " + test.currentMoney);
    }

    private class ResultPoker {
        private static final short DAT_CUOC_KHONG_HOP_LE = 101;
        private static final short KHONG_DU_TIEN = 102;
        private static final short TRUOT = 0;
        private static final short NO_HU = 1;
        private static final short THUNG_PHA_SANH_NHO = 2;
        private static final short TU_QUY = 3;
        private static final short CU_LU = 4;
        private static final short THUNG = 5;
        private static final short SANH = 6;
        private static final short SAM_CO = 7;
        private static final short HAI_DOI = 8;
        private static final short MOT_DOI_NHO = 9;
        private static final short MOT_DOI_TO = 10;
        private static final short BAI_CAO = 11;

        private ResultPoker() {
        }
    }

}

