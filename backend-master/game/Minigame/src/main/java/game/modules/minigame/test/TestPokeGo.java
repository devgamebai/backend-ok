/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.test;

import game.modules.minigame.entities.pokego.Award;
import game.modules.minigame.entities.pokego.AwardsOnLine;
import game.modules.minigame.entities.pokego.Item;
import game.modules.minigame.entities.pokego.Line;
import game.modules.minigame.entities.pokego.Lines;
import game.modules.minigame.utils.PokeGoUtils;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class TestPokeGo {
    public long pot = 500000L;
    public long fund = 3000000L;
    public long initPotValue = 500000L;
    public long hanMuc = -3000000L;
    private long betValue = 100L;
    private short moneyType;
    private Lines lines = new Lines();
    public int numNoHu = 0;
    public int numNoHuForce = 0;
    public int numThangLon = 0;
    public int numTruot = 0;
    public long numKhongDuTraThuong = 0L;
    public long maxHu = 0L;
    public long minHu = 1000000L;
    public int triplePokeBall = 0;
    public int triplePikaChu = 0;
    public int tripleBubasaur = 0;
    public int tripleClefable = 0;
    public int tripleMouse = 0;
    public int doubleMouse = 0;
    public int tripleToGePi = 0;
    public int doubleToGePi = 0;
    public long tongTraThuong = 0L;

    private boolean validateRecharge(String username) {
        return true;
    }

    public void play(String username, String linesStr) {
        int result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = 1000000L;
        int soLanNoHu = 20000;
        Random rd;
        long totalBetValue = (long)lineArr.length * this.betValue;
        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= 1000000L) {
                    long fee = totalBetValue * 2L / 100L;
                    long moneyToPot = totalBetValue * 1L / 100L;
                    long moneyToFund = totalBetValue - fee - moneyToPot;
                    this.fund += moneyToFund;
                    this.pot += moneyToPot;
                    boolean enoughPair = false;
                    ArrayList<AwardsOnLine> awardsOnLines = new ArrayList<AwardsOnLine>();
                    long totalPrizes = 0L;
                    block10 :
                    while (!enoughPair) {
                        int n;
                        awardsOnLines.clear();
                        totalPrizes = 0L;
                        result = 0;
                        boolean forceNoHu = false;
                        if (lineArr.length >= 5) {
                            if ((soLanNoHu > 0) && (fund > initPotValue * 2L)) {
                                rd = new Random();
                                if (rd.nextInt(soLanNoHu) == 0)
                                    forceNoHu = true;
                            }
                        }
                        Item[][] matrix = forceNoHu ? PokeGoUtils.generateMatrixNoHu(lineArr) : PokeGoUtils.generateMatrix();
                        for (int i = 0; i < lineArr.length; ++i) {
                            Object entry = lineArr[i];
                            ArrayList<Award> awardList = new ArrayList<Award>();
                            Line line = PokeGoUtils.getLine(this.lines, matrix, Integer.parseInt((String)entry));
                            PokeGoUtils.calculateLine(line, awardList);
                            for (Award award : awardList) {
                                int money = (int)(award.getRatio() * (float)this.betValue);
                                AwardsOnLine aol = new AwardsOnLine(award, money, line.getName());
                                awardsOnLines.add(aol);
                            }
                        }
                        StringBuilder builderLinesWin = new StringBuilder();
                        StringBuilder builderPrizesOnLine = new StringBuilder();
                        for (AwardsOnLine entry2 : awardsOnLines) {
                            if (entry2.getAward() == Award.TRIPLE_POKER_BALL) {
                                if (this.moneyType == 1 && this.betValue == 10000L && !this.validateRecharge(username)) continue block10;
                                result = 3;
                                totalPrizes += this.pot;
                            } else {
                                totalPrizes += entry2.getMoney();
                            }
                            builderLinesWin.append(",");
                            builderLinesWin.append(entry2.getLineId());
                            builderPrizesOnLine.append(",");
                            builderPrizesOnLine.append(entry2.getMoney());
                        }
                        if (builderLinesWin.length() > 0) {
                            builderLinesWin.deleteCharAt(0);
                        }
                        if (builderPrizesOnLine.length() > 0) {
                            builderPrizesOnLine.deleteCharAt(0);
                        }
                        if (result == 3) {
                            if (this.fund - this.initPotValue < 0L) {
                                ++this.numKhongDuTraThuong;
                                continue;
                            }
                        } else if (this.fund - totalPrizes < 0L) {
                            ++this.numKhongDuTraThuong;
                            continue;
                        }
                        enoughPair = true;
                        block14 : for (AwardsOnLine entry2 : awardsOnLines) {
                            switch (entry2.getAward()) {
                                case TRIPLE_POKER_BALL: {
                                    ++this.triplePokeBall;
                                    continue block14;
                                }
                                case TRIPLE_PIKACHU: {
                                    ++this.triplePikaChu;
                                    continue block14;
                                }
                                case TRIPLE_BULBASAUR: {
                                    ++this.tripleBubasaur;
                                    continue block14;
                                }
                                case TRIPLE_CLEFABLE: {
                                    ++this.tripleClefable;
                                    continue block14;
                                }
                                case TRIPLE_MOUSE: {
                                    ++this.tripleMouse;
                                    continue block14;
                                }
                                case DOUBLE_MOUSE: {
                                    ++this.doubleMouse;
                                    continue block14;
                                }
                                case TRIPLE_TOGEPI: {
                                    ++this.tripleToGePi;
                                    continue block14;
                                }
                                case DOUBLE_TOGEPI: {
                                    ++this.doubleToGePi;
                                    continue block14;
                                }
                            }
                        }
                        if (totalPrizes > 0L) {
                            if (result == 3) {
                                System.out.println("NO HU= " + this.pot + ", FUND= " + this.fund + ", FORCE= " + forceNoHu);
                                if (this.maxHu < this.pot) {
                                    this.maxHu = this.pot;
                                }
                                if (this.minHu > this.pot) {
                                    this.minHu = this.pot;
                                }
                                if (forceNoHu) {
                                    ++this.numNoHuForce;
                                }
                                ++this.numNoHu;
                                this.pot = this.initPotValue;
                                this.fund -= this.initPotValue;
                            } else {
                                this.fund -= totalPrizes;
                            }
                            if (totalPrizes >= this.betValue * 1000L) {
                                result = 2;
                                ++this.numThangLon;
                            } else {
                                result = 1;
                            }
                        }
                        this.tongTraThuong += totalPrizes;
                    }
                } else {
                    result = 102;
                }
            } else {
                result = 101;
            }
        } else {
            result = 101;
        }
        if (result == 0) {
            ++this.numTruot;
        }
    }

    public static void main(String[] args) {
        String username = "tuyennd";
        String lines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        long soLan = 1000000L;
        TestPokeGo test = new TestPokeGo();
        int i = 0;
        while ((long)i < soLan) {
            test.play(username, lines);
            ++i;
        }
        long tongCuoc = test.betValue * 20L * soLan;
        System.out.println("TONG CUOC= " + tongCuoc);
        System.out.println("TONG TRA THUONG= " + test.tongTraThuong);
        System.out.println("% TRA THUONG= " + (float)test.tongTraThuong / (float)tongCuoc * 100.0f);
        System.out.println("FUND= " + test.fund);
        System.out.println("POT= " + test.pot);
        System.out.println("======================================");
        System.out.println("SO LAN NO HU= " + test.numNoHu);
        System.out.println("SO LAN FORCE NO HU= " + test.numNoHuForce);
        System.out.println("MIN HU= " + test.minHu + ", MAX HU= " + test.maxHu);
        System.out.println("SO LAN THANG LON= " + test.numThangLon);
        System.out.println("SO LAN TRUOT= " + test.numTruot);
        System.out.println("% QUAY TRUOT= " + (float)test.numTruot / (float)soLan * 100.0f);
        System.out.println("SO LAN KHONG DU TRA THUONG= " + test.numKhongDuTraThuong);
        System.out.println("% KHONG DU TRA THUONG= " + (float)test.numKhongDuTraThuong / (float)soLan * 100.0f);
        System.out.println("======================================");
        System.out.println("TRIPLE POKEBALl= " + test.triplePokeBall);
        System.out.println("TRIPLE PIKACHU= " + test.triplePikaChu);
        System.out.println("TRIPLE BUBASAUR= " + test.tripleBubasaur);
        System.out.println("TRIPLE CLEFABLE= " + test.tripleClefable);
        System.out.println("TRIPLE MOUSE= " + test.tripleMouse);
        System.out.println("DOUBLE MOUSE= " + test.doubleMouse);
        System.out.println("TRIPLE TOGEPI= " + test.tripleToGePi);
        System.out.println("DOUBLE TOGEPI= " + test.doubleToGePi);
    }

}

