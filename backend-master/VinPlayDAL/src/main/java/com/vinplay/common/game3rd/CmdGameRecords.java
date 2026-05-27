/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.common.game3rd;

import java.io.Serializable;

public class CmdGameRecords
implements Serializable {
    private static final long serialVersionUID = -4212165843685005896L;
    private long id;
    private String sourcename;
    private String referenceno;
    private long soctransid;
    private String isfirsthalf;
    private long transdate;
    private String ishomegive;
    private String isbethome;
    private double betamount;
    private double outstanding;
    private double hdp;
    private double odds;
    private String currency;
    private double winamount;
    private double exchangerate;
    private String winlosestatus;
    private String transtype;
    private String dangerstatus;
    private double memcommission;
    private String betip;
    private int homescore;
    private int awayscore;
    private int runhomescore;
    private int runawayscore;
    private String isrunning;
    private String rejectreason;
    private String sporttype;
    private int choice;
    private long workingdate;
    private String oddstype;
    private long matchdate;
    private int hometeamid;
    private int awayteamid;
    private int leagueid;
    private String specialid;
    private int statuschange;
    private long stateupdatets;
    private double memcommissionset;
    private String iscashout;
    private double cashouttotal;
    private double cashouttakeback;
    private double cashoutwinloseamount;
    private int betsource;
    private String aosexcluding;
    private double mmrpercent;
    private long matchid;
    private String matchgroupid;
    private String betremarks;
    private String isspecial;
    private String betdate;
    private String settleddate;
    private String loginname;
    private double stake;
    private double payout;
    private double realbet;

    public double getStake() {
        return this.stake;
    }

    public void setStake(double stake) {
        this.stake = stake;
    }

    public double getPayout() {
        return this.payout;
    }

    public void setPayout(double payout) {
        this.payout = payout;
    }

    public double getRealbet() {
        return this.realbet;
    }

    public void setRealbet(double realbet) {
        this.realbet = realbet;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSourcename() {
        return this.sourcename;
    }

    public void setSourcename(String sourcename) {
        this.sourcename = sourcename;
    }

    public String getReferenceno() {
        return this.referenceno;
    }

    public void setReferenceno(String referenceno) {
        this.referenceno = referenceno;
    }

    public long getSoctransid() {
        return this.soctransid;
    }

    public void setSoctransid(long soctransid) {
        this.soctransid = soctransid;
    }

    public String getIsfirsthalf() {
        return this.isfirsthalf;
    }

    public void setIsfirsthalf(String isfirsthalf) {
        this.isfirsthalf = isfirsthalf;
    }

    public long getTransdate() {
        return this.transdate;
    }

    public void setTransdate(long transdate) {
        this.transdate = transdate;
    }

    public String getIshomegive() {
        return this.ishomegive;
    }

    public void setIshomegive(String ishomegive) {
        this.ishomegive = ishomegive;
    }

    public String getIsbethome() {
        return this.isbethome;
    }

    public void setIsbethome(String isbethome) {
        this.isbethome = isbethome;
    }

    public double getBetamount() {
        return this.betamount;
    }

    public void setBetamount(double betamount) {
        this.betamount = betamount;
    }

    public double getOutstanding() {
        return this.outstanding;
    }

    public void setOutstanding(double outstanding) {
        this.outstanding = outstanding;
    }

    public double getHdp() {
        return this.hdp;
    }

    public void setHdp(double hdp) {
        this.hdp = hdp;
    }

    public double getOdds() {
        return this.odds;
    }

    public void setOdds(double odds) {
        this.odds = odds;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getWinamount() {
        return this.winamount;
    }

    public void setWinamount(double winamount) {
        this.winamount = winamount;
    }

    public double getExchangerate() {
        return this.exchangerate;
    }

    public void setExchangerate(double exchangerate) {
        this.exchangerate = exchangerate;
    }

    public String getWinlosestatus() {
        return this.winlosestatus;
    }

    public void setWinlosestatus(String winlosestatus) {
        this.winlosestatus = winlosestatus;
    }

    public String getTranstype() {
        return this.transtype;
    }

    public void setTranstype(String transtype) {
        this.transtype = transtype;
    }

    public String getDangerstatus() {
        return this.dangerstatus;
    }

    public void setDangerstatus(String dangerstatus) {
        this.dangerstatus = dangerstatus;
    }

    public double getMemcommission() {
        return this.memcommission;
    }

    public void setMemcommission(double memcommission) {
        this.memcommission = memcommission;
    }

    public String getBetip() {
        return this.betip;
    }

    public void setBetip(String betip) {
        this.betip = betip;
    }

    public int getHomescore() {
        return this.homescore;
    }

    public void setHomescore(int homescore) {
        this.homescore = homescore;
    }

    public int getAwayscore() {
        return this.awayscore;
    }

    public void setAwayscore(int awayscore) {
        this.awayscore = awayscore;
    }

    public int getRunhomescore() {
        return this.runhomescore;
    }

    public void setRunhomescore(int runhomescore) {
        this.runhomescore = runhomescore;
    }

    public int getRunawayscore() {
        return this.runawayscore;
    }

    public void setRunawayscore(int runawayscore) {
        this.runawayscore = runawayscore;
    }

    public String getIsrunning() {
        return this.isrunning;
    }

    public void setIsrunning(String isrunning) {
        this.isrunning = isrunning;
    }

    public String getRejectreason() {
        return this.rejectreason;
    }

    public void setRejectreason(String rejectreason) {
        this.rejectreason = rejectreason;
    }

    public String getSporttype() {
        return this.sporttype;
    }

    public void setSporttype(String sporttype) {
        this.sporttype = sporttype;
    }

    public int getChoice() {
        return this.choice;
    }

    public void setChoice(int choice) {
        this.choice = choice;
    }

    public long getWorkingdate() {
        return this.workingdate;
    }

    public void setWorkingdate(long workingdate) {
        this.workingdate = workingdate;
    }

    public String getOddstype() {
        return this.oddstype;
    }

    public void setOddstype(String oddstype) {
        this.oddstype = oddstype;
    }

    public long getMatchdate() {
        return this.matchdate;
    }

    public void setMatchdate(long matchdate) {
        this.matchdate = matchdate;
    }

    public int getHometeamid() {
        return this.hometeamid;
    }

    public void setHometeamid(int hometeamid) {
        this.hometeamid = hometeamid;
    }

    public int getAwayteamid() {
        return this.awayteamid;
    }

    public void setAwayteamid(int awayteamid) {
        this.awayteamid = awayteamid;
    }

    public int getLeagueid() {
        return this.leagueid;
    }

    public void setLeagueid(int leagueid) {
        this.leagueid = leagueid;
    }

    public String getSpecialid() {
        return this.specialid;
    }

    public void setSpecialid(String specialid) {
        this.specialid = specialid;
    }

    public int getStatuschange() {
        return this.statuschange;
    }

    public void setStatuschange(int statuschange) {
        this.statuschange = statuschange;
    }

    public long getStateupdatets() {
        return this.stateupdatets;
    }

    public void setStateupdatets(long stateupdatets) {
        this.stateupdatets = stateupdatets;
    }

    public double getMemcommissionset() {
        return this.memcommissionset;
    }

    public void setMemcommissionset(double memcommissionset) {
        this.memcommissionset = memcommissionset;
    }

    public String getIscashout() {
        return this.iscashout;
    }

    public void setIscashout(String iscashout) {
        this.iscashout = iscashout;
    }

    public double getCashouttotal() {
        return this.cashouttotal;
    }

    public void setCashouttotal(double cashouttotal) {
        this.cashouttotal = cashouttotal;
    }

    public double getCashouttakeback() {
        return this.cashouttakeback;
    }

    public void setCashouttakeback(double cashouttakeback) {
        this.cashouttakeback = cashouttakeback;
    }

    public double getCashoutwinloseamount() {
        return this.cashoutwinloseamount;
    }

    public void setCashoutwinloseamount(double cashoutwinloseamount) {
        this.cashoutwinloseamount = cashoutwinloseamount;
    }

    public int getBetsource() {
        return this.betsource;
    }

    public void setBetsource(int betsource) {
        this.betsource = betsource;
    }

    public String getAosexcluding() {
        return this.aosexcluding;
    }

    public void setAosexcluding(String aosexcluding) {
        this.aosexcluding = aosexcluding;
    }

    public double getMmrpercent() {
        return this.mmrpercent;
    }

    public void setMmrpercent(double mmrpercent) {
        this.mmrpercent = mmrpercent;
    }

    public long getMatchid() {
        return this.matchid;
    }

    public void setMatchid(long matchid) {
        this.matchid = matchid;
    }

    public String getMatchgroupid() {
        return this.matchgroupid;
    }

    public void setMatchgroupid(String matchgroupid) {
        this.matchgroupid = matchgroupid;
    }

    public String getBetremarks() {
        return this.betremarks;
    }

    public void setBetremarks(String betremarks) {
        this.betremarks = betremarks;
    }

    public String getIsspecial() {
        return this.isspecial;
    }

    public void setIsspecial(String isspecial) {
        this.isspecial = isspecial;
    }

    public String getBetdate() {
        return this.betdate;
    }

    public void setBetdate(String betdate) {
        this.betdate = betdate;
    }

    public String getSettleddate() {
        return this.settleddate;
    }

    public void setSettleddate(String settleddate) {
        this.settleddate = settleddate;
    }

    public String getLoginname() {
        return this.loginname;
    }

    public void setLoginname(String loginname) {
        this.loginname = loginname;
    }
}

