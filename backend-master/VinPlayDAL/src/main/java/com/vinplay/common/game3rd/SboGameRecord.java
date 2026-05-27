/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.common.game3rd;

import java.io.Serializable;
import java.util.Date;

public class SboGameRecord
implements Serializable {
    private String refno;
    private String player_name;
    private String nick_name;
    private String sporttype;
    private Date ordertime;
    private Date winlostdate;
    private Date modifydate;
    private Double odds;
    private String oddsstyle;
    private Long stake;
    private Long actualstake;
    private String currency;
    private String status;
    private Long winlose;
    private Double turnover;
    private String ishalfwonlose;
    private String islive;
    private Long maxwinwithoutactualstakerecord;
    private String ip;
    private String betoption;
    private String markettype;
    private Double hdp;
    private String league;
    private String match;
    private String livescore;
    private String htscore;
    private String ftscore;
    private Long payout;
    private Long effective_bet;
    private Long valid_stake;

    public String getRefno() {
        return this.refno;
    }

    public void setRefno(String refno) {
        this.refno = refno;
    }

    public String getPlayer_name() {
        return this.player_name;
    }

    public void setPlayer_name(String player_name) {
        this.player_name = player_name;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getSporttype() {
        return this.sporttype;
    }

    public void setSporttype(String sporttype) {
        this.sporttype = sporttype;
    }

    public Date getOrdertime() {
        return this.ordertime;
    }

    public void setOrdertime(Date ordertime) {
        this.ordertime = ordertime;
    }

    public Date getWinlostdate() {
        return this.winlostdate;
    }

    public void setWinlostdate(Date winlostdate) {
        this.winlostdate = winlostdate;
    }

    public Date getModifydate() {
        return this.modifydate;
    }

    public void setModifydate(Date modifydate) {
        this.modifydate = modifydate;
    }

    public Double getOdds() {
        return this.odds;
    }

    public void setOdds(Double odds) {
        this.odds = odds;
    }

    public String getOddsstyle() {
        return this.oddsstyle;
    }

    public void setOddsstyle(String oddsstyle) {
        this.oddsstyle = oddsstyle;
    }

    public Long getStake() {
        return this.stake;
    }

    public void setStake(Long stake) {
        this.stake = stake;
    }

    public Long getActualstake() {
        return this.actualstake;
    }

    public void setActualstake(Long actualstake) {
        this.actualstake = actualstake;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinlose() {
        return this.winlose;
    }

    public void setWinlose(Long winlose) {
        this.winlose = winlose;
    }

    public Double getTurnover() {
        return this.turnover;
    }

    public void setTurnover(Double turnover) {
        this.turnover = turnover;
    }

    public String getIshalfwonlose() {
        return this.ishalfwonlose;
    }

    public void setIshalfwonlose(String ishalfwonlose) {
        this.ishalfwonlose = ishalfwonlose;
    }

    public String getIslive() {
        return this.islive;
    }

    public void setIslive(String islive) {
        this.islive = islive;
    }

    public Long getMaxwinwithoutactualstakerecord() {
        return this.maxwinwithoutactualstakerecord;
    }

    public void setMaxwinwithoutactualstakerecord(Long maxwinwithoutactualstakerecord) {
        this.maxwinwithoutactualstakerecord = maxwinwithoutactualstakerecord;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBetoption() {
        return this.betoption;
    }

    public void setBetoption(String betoption) {
        this.betoption = betoption;
    }

    public String getMarkettype() {
        return this.markettype;
    }

    public void setMarkettype(String markettype) {
        this.markettype = markettype;
    }

    public Double getHdp() {
        return this.hdp;
    }

    public void setHdp(Double hdp) {
        this.hdp = hdp;
    }

    public String getLeague() {
        return this.league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getMatch() {
        return this.match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getLivescore() {
        return this.livescore;
    }

    public void setLivescore(String livescore) {
        this.livescore = livescore;
    }

    public String getHtscore() {
        return this.htscore;
    }

    public void setHtscore(String htscore) {
        this.htscore = htscore;
    }

    public String getFtscore() {
        return this.ftscore;
    }

    public void setFtscore(String ftscore) {
        this.ftscore = ftscore;
    }

    public Long getPayout() {
        return this.payout;
    }

    public void setPayout(Long payout) {
        this.payout = payout;
    }

    public Long getEffective_bet() {
        return this.effective_bet;
    }

    public void setEffective_bet(Long effective_bet) {
        this.effective_bet = effective_bet;
    }

    public Long getValid_stake() {
        return this.valid_stake;
    }

    public void setValid_stake(Long valid_stake) {
        this.valid_stake = valid_stake;
    }

    public SboGameRecord() {
    }

    public SboGameRecord(String refno, String player_name, String nick_name, String sporttype, Date ordertime, Date winlostdate, Date modifydate, Double odds, String oddsstyle, Long stake, Long actualstake, String currency, String status, Long winlose, Double turnover, String ishalfwonlose, String islive, Long maxwinwithoutactualstakerecord, String ip, String betoption, String markettype, Double hdp, String league, String match, String livescore, String htscore, String ftscore, Long payout, Long effective_bet, Long valid_stake) {
        this.refno = refno;
        this.player_name = player_name;
        this.nick_name = nick_name;
        this.sporttype = sporttype;
        this.ordertime = ordertime;
        this.winlostdate = winlostdate;
        this.modifydate = modifydate;
        this.odds = odds;
        this.oddsstyle = oddsstyle;
        this.stake = stake;
        this.actualstake = actualstake;
        this.currency = currency;
        this.status = status;
        this.winlose = winlose;
        this.turnover = turnover;
        this.ishalfwonlose = ishalfwonlose;
        this.islive = islive;
        this.maxwinwithoutactualstakerecord = maxwinwithoutactualstakerecord;
        this.ip = ip;
        this.betoption = betoption;
        this.markettype = markettype;
        this.hdp = hdp;
        this.league = league;
        this.match = match;
        this.livescore = livescore;
        this.htscore = htscore;
        this.ftscore = ftscore;
        this.payout = payout;
        this.effective_bet = effective_bet;
        this.valid_stake = valid_stake;
    }

    public String toString() {
        return "SboGameRecord [refno=" + this.refno + ", player_name=" + this.player_name + ", nick_name=" + this.nick_name + ", sporttype=" + this.sporttype + ", ordertime=" + this.ordertime + ", winlostdate=" + this.winlostdate + ", modifydate=" + this.modifydate + ", odds=" + this.odds + ", oddsstyle=" + this.oddsstyle + ", stake=" + this.stake + ", actualstake=" + this.actualstake + ", currency=" + this.currency + ", status=" + this.status + ", winlose=" + this.winlose + ", turnover=" + this.turnover + ", ishalfwonlose=" + this.ishalfwonlose + ", islive=" + this.islive + ", maxwinwithoutactualstakerecord=" + this.maxwinwithoutactualstakerecord + ", ip=" + this.ip + ", betoption=" + this.betoption + ", markettype=" + this.markettype + ", hdp=" + this.hdp + ", league=" + this.league + ", match=" + this.match + ", livescore=" + this.livescore + ", htscore=" + this.htscore + ", ftscore=" + this.ftscore + ", payout=" + this.payout + ", effective_bet=" + this.effective_bet + ", valid_stake=" + this.valid_stake + "]";
    }
}

