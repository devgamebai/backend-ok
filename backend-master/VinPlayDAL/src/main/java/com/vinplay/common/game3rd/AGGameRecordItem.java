/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.common.game3rd;

import java.io.Serializable;

public class AGGameRecordItem
implements Serializable {
    private static final long serialVersionUID = -5522746832088383598L;
    private String billno;
    private String username;
    private String nickname;
    private String gamecode;
    private String gametype;
    private String platformtype;
    private Double bet;
    private Double validbet;
    private Double payout;
    private String bettime;
    private String payouttime;
    private Integer flag;
    private String currency;
    private String betip;
    private Integer playtype;
    private Integer devicetype;
    private String tablecode;
    private Double beforecredit;
    private String remark;
    private String round;
    private String gameresult;
    private String sport_odds;
    private String sport_competition;
    private String sport_market;
    private String sport_selection;
    private String sport_simplifiedresult;
    private String sport_types;
    private String sport_category;
    private String sport_extbillno;
    private String sport_thirdbillno;
    private Integer sport_bettype;
    private String sport_system;
    private Integer sport_live;
    private String sport_currentscore;

    public AGGameRecordItem() {
    }

    public AGGameRecordItem(String billno, String username, String nickname, String gamecode, String gametype, String platformtype, Double bet, Double validbet, Double payout, String bettime, String payouttime, Integer flag, String currency, String betip, Integer playtype, Integer devicetype, String tablecode, Double beforecredit, String remark, String round, String gameresult) {
        this.billno = billno;
        this.username = username;
        this.nickname = nickname;
        this.gamecode = gamecode;
        this.gametype = gametype;
        this.platformtype = platformtype;
        this.bet = bet;
        this.validbet = validbet;
        this.payout = payout;
        this.bettime = bettime;
        this.payouttime = payouttime;
        this.flag = flag;
        this.currency = currency;
        this.betip = betip;
        this.playtype = playtype;
        this.devicetype = devicetype;
        this.tablecode = tablecode;
        this.beforecredit = beforecredit;
        this.remark = remark;
        this.round = round;
        this.gameresult = gameresult;
    }

    public String getBillno() {
        return this.billno;
    }

    public void setBillno(String billno) {
        this.billno = billno;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGamecode() {
        return this.gamecode;
    }

    public void setGamecode(String gamecode) {
        this.gamecode = gamecode;
    }

    public String getGametype() {
        return this.gametype;
    }

    public void setGametype(String gametype) {
        this.gametype = gametype;
    }

    public String getPlatformtype() {
        return this.platformtype;
    }

    public void setPlatformtype(String platformtype) {
        this.platformtype = platformtype;
    }

    public Double getBet() {
        return this.bet;
    }

    public void setBet(Double bet) {
        this.bet = bet;
    }

    public Double getValidbet() {
        return this.validbet;
    }

    public void setValidbet(Double validbet) {
        this.validbet = validbet;
    }

    public Double getPayout() {
        return this.payout;
    }

    public void setPayout(Double payout) {
        this.payout = payout;
    }

    public String getBettime() {
        return this.bettime;
    }

    public void setBettime(String bettime) {
        this.bettime = bettime;
    }

    public String getPayouttime() {
        return this.payouttime;
    }

    public void setPayouttime(String payouttime) {
        this.payouttime = payouttime;
    }

    public Integer getFlag() {
        return this.flag;
    }

    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBetip() {
        return this.betip;
    }

    public void setBetip(String betip) {
        this.betip = betip;
    }

    public Integer getPlaytype() {
        return this.playtype;
    }

    public void setPlaytype(Integer playtype) {
        this.playtype = playtype;
    }

    public Integer getDevicetype() {
        return this.devicetype;
    }

    public void setDevicetype(Integer devicetype) {
        this.devicetype = devicetype;
    }

    public String getTablecode() {
        return this.tablecode;
    }

    public void setTablecode(String tablecode) {
        this.tablecode = tablecode;
    }

    public Double getBeforecredit() {
        return this.beforecredit;
    }

    public void setBeforecredit(Double beforecredit) {
        this.beforecredit = beforecredit;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRound() {
        return this.round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getGameresult() {
        return this.gameresult;
    }

    public void setGameresult(String gameresult) {
        this.gameresult = gameresult;
    }

    public String getSport_odds() {
        return this.sport_odds;
    }

    public void setSport_odds(String sport_odds) {
        this.sport_odds = sport_odds;
    }

    public String getSport_competition() {
        return this.sport_competition;
    }

    public void setSport_competition(String sport_competition) {
        this.sport_competition = sport_competition;
    }

    public String getSport_market() {
        return this.sport_market;
    }

    public void setSport_market(String sport_market) {
        this.sport_market = sport_market;
    }

    public String getSport_selection() {
        return this.sport_selection;
    }

    public void setSport_selection(String sport_selection) {
        this.sport_selection = sport_selection;
    }

    public String getSport_simplifiedresult() {
        return this.sport_simplifiedresult;
    }

    public void setSport_simplifiedresult(String sport_simplifiedresult) {
        this.sport_simplifiedresult = sport_simplifiedresult;
    }

    public String getSport_types() {
        return this.sport_types;
    }

    public void setSport_types(String sport_types) {
        this.sport_types = sport_types;
    }

    public String getSport_category() {
        return this.sport_category;
    }

    public void setSport_category(String sport_category) {
        this.sport_category = sport_category;
    }

    public String getSport_extbillno() {
        return this.sport_extbillno;
    }

    public void setSport_extbillno(String sport_extbillno) {
        this.sport_extbillno = sport_extbillno;
    }

    public String getSport_thirdbillno() {
        return this.sport_thirdbillno;
    }

    public void setSport_thirdbillno(String sport_thirdbillno) {
        this.sport_thirdbillno = sport_thirdbillno;
    }

    public Integer getSport_bettype() {
        return this.sport_bettype;
    }

    public void setSport_bettype(Integer sport_bettype) {
        this.sport_bettype = sport_bettype;
    }

    public String getSport_system() {
        return this.sport_system;
    }

    public void setSport_system(String sport_system) {
        this.sport_system = sport_system;
    }

    public Integer getSport_live() {
        return this.sport_live;
    }

    public void setSport_live(Integer sport_live) {
        this.sport_live = sport_live;
    }

    public String getSport_currentscore() {
        return this.sport_currentscore;
    }

    public void setSport_currentscore(String sport_currentscore) {
        this.sport_currentscore = sport_currentscore;
    }
}

