/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class TranferAgentResponse {
    private String nickName;
    private long totalBuy1;
    private long totalSale1;
    private long totalFeeBuy1;
    private long totalFeeSale1;
    private int countBuy1;
    private int countSale1;
    private long totalBuy2;
    private long totalSale2;
    private long totalFeeBuy2;
    private long totalFeeSale2;
    private int countBuy2;
    private int countSale2;
    private long totalFee;
    private long totalFeeByVinplayCard;
    private long totalFeeByVinCash;
    private int percent;

    public TranferAgentResponse() {
    }

    public TranferAgentResponse(String nickName, long totalBuy1, long totalSale1, long totalFeeBuy1, long totalFeeSale1, int countBuy1, int countSale1, long totalBuy2, long totalSale2, long totalFeeBuy2, long totalFeeSale2, int countBuy2, int countSale2) {
        this.nickName = nickName;
        this.totalBuy1 = totalBuy1;
        this.totalSale1 = totalSale1;
        this.totalFeeBuy1 = totalFeeBuy1;
        this.totalFeeSale1 = totalFeeSale1;
        this.countBuy1 = countBuy1;
        this.countSale1 = countSale1;
        this.totalBuy2 = totalBuy2;
        this.totalSale2 = totalSale2;
        this.totalFeeBuy2 = totalFeeBuy2;
        this.totalFeeSale2 = totalFeeSale2;
        this.countBuy2 = countBuy2;
        this.countSale2 = countSale2;
    }

    public long getTotalFee() {
        return this.totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public long getTotalFeeByVinplayCard() {
        return this.totalFeeByVinplayCard;
    }

    public void setTotalFeeByVinplayCard(long totalFeeByVinplayCard) {
        this.totalFeeByVinplayCard = totalFeeByVinplayCard;
    }

    public long getTotalFeeByVinCash() {
        return this.totalFeeByVinCash;
    }

    public void setTotalFeeByVinCash(long totalFeeByVinCash) {
        this.totalFeeByVinCash = totalFeeByVinCash;
    }

    public int getPercent() {
        return this.percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getTotalBuy1() {
        return this.totalBuy1;
    }

    public void setTotalBuy1(long totalBuy1) {
        this.totalBuy1 = totalBuy1;
    }

    public long getTotalSale1() {
        return this.totalSale1;
    }

    public void setTotalSale1(long totalSale1) {
        this.totalSale1 = totalSale1;
    }

    public long getTotalFeeBuy1() {
        return this.totalFeeBuy1;
    }

    public void setTotalFeeBuy1(long totalFeeBuy1) {
        this.totalFeeBuy1 = totalFeeBuy1;
    }

    public long getTotalFeeSale1() {
        return this.totalFeeSale1;
    }

    public void setTotalFeeSale1(long totalFeeSale1) {
        this.totalFeeSale1 = totalFeeSale1;
    }

    public int getCountBuy1() {
        return this.countBuy1;
    }

    public void setCountBuy1(int countBuy1) {
        this.countBuy1 = countBuy1;
    }

    public int getCountSale1() {
        return this.countSale1;
    }

    public void setCountSale1(int countSale1) {
        this.countSale1 = countSale1;
    }

    public long getTotalBuy2() {
        return this.totalBuy2;
    }

    public void setTotalBuy2(long totalBuy2) {
        this.totalBuy2 = totalBuy2;
    }

    public long getTotalSale2() {
        return this.totalSale2;
    }

    public void setTotalSale2(long totalSale2) {
        this.totalSale2 = totalSale2;
    }

    public long getTotalFeeBuy2() {
        return this.totalFeeBuy2;
    }

    public void setTotalFeeBuy2(long totalFeeBuy2) {
        this.totalFeeBuy2 = totalFeeBuy2;
    }

    public long getTotalFeeSale2() {
        return this.totalFeeSale2;
    }

    public void setTotalFeeSale2(long totalFeeSale2) {
        this.totalFeeSale2 = totalFeeSale2;
    }

    public int getCountBuy2() {
        return this.countBuy2;
    }

    public void setCountBuy2(int countBuy2) {
        this.countBuy2 = countBuy2;
    }

    public int getCountSale2() {
        return this.countSale2;
    }

    public void setCountSale2(int countSale2) {
        this.countSale2 = countSale2;
    }
}

