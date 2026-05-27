/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.report.service.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.common.game3rd.AGGameRecordItem;
import com.vinplay.common.game3rd.CmdGameRecords;
import com.vinplay.common.game3rd.IbcGameRecordItem;
import com.vinplay.common.game3rd.SboGameRecord;
import com.vinplay.common.game3rd.ThirdPartyResponse;
import com.vinplay.common.game3rd.WMGameRecordItem;
import com.vinplay.report.service.ThirdPartyGameReport;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ThirdPartyGameReportImpl
implements ThirdPartyGameReport {
    private static final Logger logger = Logger.getLogger((String)"backend");

    @Override
    public ThirdPartyResponse<List<IbcGameRecordItem>> filterIBC(Long transid, String playername, String transactiontime, Integer matchid, Integer leagueid, String leaguename, Integer awayid, String awayidname, Integer homeid, String homeidname, String matchdatetime, Integer bettype, Long parlayrefno, String betteam, Double hdp, String sporttype, Double awayhdp, Double homehdp, Double odds, Integer awayscore, Integer homescore, String islive, String islucky, String parlay_type, String combo_type, Long stake, String bettag, Long winloseamount, Date winlostdatetime, Integer versionkey, String lastballno, String ticketstatus, Integer oddstype, Long actual_stake, Long refund_amount, String nick_name, int flagTime, String fromTime, String endTime, int page, int maxItem) {
        try {
            String pattern;
            final HashSet set = new HashSet();
            final ArrayList records = new ArrayList();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            num.add(3, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("ibc2gamerecord");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (nick_name != null && !nick_name.isEmpty()) {
                pattern = ".*" + nick_name + ".*";
                conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (playername != null && !playername.isEmpty()) {
                pattern = ".*" + playername + ".*";
                conditions.put("playername", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (ticketstatus != null && !ticketstatus.isEmpty()) {
                conditions.put("ticketstatus", ticketstatus);
            }
            if (transid != null) {
                conditions.put("transid", transid);
            }
            if (matchid != null) {
                conditions.put("matchid", matchid);
            }
            if (homeid != null) {
                conditions.put("homeid", homeid);
            }
            if (stake != null) {
                conditions.put("stake", stake);
            }
            if (winloseamount != null) {
                conditions.put("winloseamount", winloseamount);
            }
            if (winlostdatetime != null) {
                conditions.put("winlostdatetime", winlostdatetime);
            }
            if (refund_amount != null) {
                conditions.put("refund_amount", refund_amount);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                BasicDBObject obj;
                if (flagTime == 1) {
                    if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        obj = new BasicDBObject();
                        obj.put("$gte", new Timestamp(dateFormat.parse(fromTime).getTime()));
                        obj.put("$lte", new Timestamp(dateFormat.parse(endTime).getTime()));
                        conditions.put("transactiontime", obj);
                    }
                } else if (flagTime == 2) {
                    if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        obj = new BasicDBObject();
                        obj.put("$gte", new Timestamp(dateFormat.parse(fromTime).getTime()));
                        obj.put("$lte", new Timestamp(dateFormat.parse(endTime).getTime()));
                        conditions.put("matchdatetime", obj);
                    }
                } else if (flagTime == 3 && fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    obj = new BasicDBObject();
                    obj.put("$gte", new Timestamp(dateFormat.parse(fromTime).getTime()));
                    obj.put("$lte", new Timestamp(dateFormat.parse(endTime).getTime()));
                    conditions.put("winlostdatetime", obj);
                }
            }
            catch (Exception obj) {
                // empty catch block
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    IbcGameRecordItem model = new IbcGameRecordItem(document.getLong("transid"), document.getString("playername"), document.getDate("transactiontime"), document.getInteger("matchid"), document.getInteger("leagueid"), document.getString("leaguename"), document.getInteger("awayid"), document.getString("awayidname"), document.getInteger("homeid"), document.getString("homeidname"), document.getDate("matchdatetime"), document.getInteger("bettype"), document.getLong("parlayrefno"), document.getString("betteam"), document.getDouble("hdp"), document.getString("sporttype"), document.getDouble("awayhdp"), document.getDouble("homehdp"), document.getDouble("odds"), document.getInteger("awayscore"), document.getInteger("homescore"), document.getString("islive"), document.getString("islucky"), document.getString("parlay_type"), document.getString("combo_type"), document.getLong("stake"), document.getString("bettag"), document.getLong("winloseamount"), document.getDate("winlostdatetime"), document.getInteger("versionkey"), document.getString("lastballno"), document.getString("ticketstatus"), document.getInteger("oddstype"), document.getLong("actual_stake"), document.getLong("refund_amount"), document.getString("nick_name"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            final List<String> lstTicketStatus = Arrays.asList("won", "lose", "draw", "half won", "half lose");
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Long bet = document.getLong("stake");
                    Long validBet = document.getLong("actual_stake");
                    Long payout = document.getLong("winloseamount") + validBet;
                    set.add(document.getString("nick_name"));
                    long count = (Long)num.get(0) + 1L;
                    String ticketStatus = document.getString("ticketstatus");
                    if (ticketStatus != null && lstTicketStatus.contains(ticketStatus.toLowerCase())) {
                        long totalBet = (Long)num.get(1) + bet;
                        long totalValidBet = (Long)num.get(2) + validBet;
                        long totalPayout = (Long)num.get(3) + payout;
                        num.set(1, totalBet);
                        num.set(2, totalValidBet);
                        num.set(3, totalPayout);
                    }
                    num.set(0, count);
                }
            });
            ThirdPartyResponse<List<IbcGameRecordItem>> res = new ThirdPartyResponse<List<IbcGameRecordItem>>((Long)num.get(0), (Long)num.get(1), (Long)num.get(2), (Long)num.get(3), records);
            res.setTotalPlayer(set.size());
            logger.info(("ibclog response =" + res.toJson()));
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public ThirdPartyResponse<List<AGGameRecordItem>> filterAG(String username, String nickName, String billNo, String gameCode, int flagTime, String fromTime, String endTime, Double betAmount, Double winAmount, String betIp, int page, int maxItem, String gameType) {
        try {
            BasicDBObject obj;
            String pattern;
            final ArrayList records = new ArrayList();
            final HashSet set = new HashSet();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            num.add(3, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("aggamerecord");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (username != null && !username.isEmpty()) {
                pattern = ".*" + username + ".*";
                conditions.put("username", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (nickName != null && !nickName.isEmpty()) {
                pattern = ".*" + nickName + ".*";
                conditions.put("nickName", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (billNo != null && !billNo.isEmpty()) {
                conditions.put("billno", billNo);
            }
            if (gameCode != null && !gameCode.isEmpty()) {
                conditions.put("gamecode", gameCode);
            }
            if (gameType != null && !gameType.isEmpty()) {
                conditions.put("gametype", gameType);
            }
            if (betIp != null && !betIp.isEmpty()) {
                conditions.put("betip", betIp);
            }
            if (flagTime == 1) {
                if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("bettime", obj);
                }
            } else if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("payouttime", obj);
            }
            if (betAmount != null) {
                obj = new BasicDBObject();
                obj.put("$gte", betAmount);
                conditions.put("bet", obj);
            }
            if (winAmount != null) {
                obj = new BasicDBObject();
                obj.put("$gte", winAmount);
                conditions.put("payout", obj);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    AGGameRecordItem model = new AGGameRecordItem(document.getString("billno"), document.getString("username"), document.getString("nickName"), document.getString("gamecode"), document.getString("gametype"), document.getString("platformtype"), document.getDouble("bet"), document.getDouble("validbet"), document.getDouble("payout"), document.getString("bettime"), document.getString("payouttime"), Integer.parseInt(document.getString("flag")), document.getString("currency"), document.getString("betip"), Integer.parseInt(document.getString("playtype")), Integer.parseInt(document.getString("devicetype")), document.getString("tablecode"), Double.parseDouble(document.getString("beforecredit")), document.getString("remark"), document.getString("round"), document.getString("gameresult"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Double bet = document.getDouble("bet");
                    Double validBet = document.getDouble("validbet");
                    Double payout = document.getDouble("payout");
                    set.add(document.getString("nickName"));
                    long count = (Long)num.get(0) + 1L;
                    Integer flag = Integer.parseInt(document.getString("flag"));
                    if (flag != null && (flag == 1 || flag == 4)) {
                        long totalBet = (Long)num.get(1) + bet.longValue();
                        long totalValidBet = (Long)num.get(2) + validBet.longValue();
                        long totalPayout = (Long)num.get(3) + payout.longValue();
                        num.set(1, totalBet);
                        num.set(2, totalValidBet);
                        num.set(3, totalPayout);
                    }
                    num.set(0, count);
                }
            });
            ThirdPartyResponse<List<AGGameRecordItem>> res = new ThirdPartyResponse<List<AGGameRecordItem>>((Long)num.get(0), (Long)num.get(1), (Long)num.get(2), (Long)num.get(3), records);
            res.setTotalPlayer(set.size());
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    private Double getDouble(String data) {
        try {
            return Double.parseDouble(data);
        }
        catch (NumberFormatException e) {
            logger.error("pase double ThirdParty error", (Throwable)e);
            return 0.0;
        }
    }

    private Long getLong(String data) {
        try {
            return Long.parseLong(data);
        }
        catch (NumberFormatException e) {
            logger.error("pase double ThirdParty error", (Throwable)e);
            return 0L;
        }
    }

    private Integer getInteger(String data) {
        try {
            return Integer.parseInt(data);
        }
        catch (NumberFormatException e) {
            logger.error("pase Integer ThirdParty error", (Throwable)e);
            return 0;
        }
    }

    @Override
    public ThirdPartyResponse<List<WMGameRecordItem>> filterWM(String nick_name, String user, String ip, String gameName, Long betid, int flagTime, String fromTime, String endTime, int page, int maxItem) {
        try {
            BasicDBObject obj;
            String pattern;
            final HashSet set = new HashSet();
            final ArrayList records = new ArrayList();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            num.add(3, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("wmgamerecord");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (nick_name != null && !nick_name.isEmpty()) {
                pattern = ".*" + nick_name + ".*";
                conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (user != null && !user.isEmpty()) {
                pattern = ".*" + user + ".*";
                conditions.put("user", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (ip != null && !ip.isEmpty()) {
                conditions.put("ip", ip);
            }
            if (betid != null) {
                conditions.put("betid", betid);
            }
            if (flagTime == 1) {
                if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("bettime", obj);
                }
            } else if (flagTime == 2) {
                if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("settime", obj);
                }
            } else if (flagTime == 3) {
                if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("rootbettime", obj);
                }
            } else if (flagTime == 4 && fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("rootsettime", obj);
            }
            if (gameName != null && !gameName.isEmpty()) {
                conditions.put("gamename", gameName);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    WMGameRecordItem model = new WMGameRecordItem(document.getLong("betid"), document.getString("nick_name"), document.getString("user"), document.getString("bettime"), document.getString("settime"), document.getString("rootbettime"), document.getString("rootsettime"), document.getDouble("bet"), document.getDouble("validbet"), document.getDouble("winloss"), document.getDouble("water"), document.getDouble("waterbet"), document.getDouble("result"), document.getString("betcode"), document.getString("betresult"), document.getInteger("gameid"), document.getString("ip"), document.getInteger("event"), document.getInteger("eventchild"), document.getInteger("tableid"), document.getString("gameresult"), document.getString("gamename"), document.getInteger("commission"), document.getString("reset"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Double bet = document.getDouble("bet");
                    Double validBet = document.getDouble("validbet");
                    Double payout = document.getDouble("winloss");
                    set.add(document.getString("nick_name"));
                    long count = (Long)num.get(0) + 1L;
                    long totalBet = (Long)num.get(1) + bet.longValue();
                    long totalValidBet = (Long)num.get(2) + validBet.longValue();
                    long totalPayout = (Long)num.get(3) + payout.longValue();
                    num.set(0, count);
                    num.set(1, totalBet);
                    num.set(2, totalValidBet);
                    num.set(3, totalPayout);
                }
            });
            ThirdPartyResponse<List<WMGameRecordItem>> res = new ThirdPartyResponse<List<WMGameRecordItem>>((Long)num.get(0), (Long)num.get(1), (Long)num.get(2), (Long)num.get(3), records);
            res.setTotalPlayer(set.size());
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public ThirdPartyResponse<List<CmdGameRecords>> filterCMD(String sourcename, String referenceno, Long soctransid, String isfirsthalf, Long transdate, String ishomegive, String isbethome, Double betamount, Double outstanding, Double hdp, Double odds, String currency, Double winamount, Double exchangerate, String winlosestatus, String transtype, String dangerstatus, Double memcommission, String betip, Integer homescore, Integer awayscore, Integer runhomescore, Integer runawayscore, String isrunning, String rejectreason, String sporttype, Integer choice, Long workingdate, String oddstype, Long matchdate, Integer hometeamid, Integer awayteamid, Integer leagueid, String specialid, Integer statuschange, Long stateupdatets, Double memcommissionset, String iscashout, Double cashouttotal, Double cashouttakeback, Double cashoutwinloseamount, Integer betsource, String aosexcluding, Double mmrpercent, Long matchid, String matchgroupid, String betremarks, String isspecial, String betdate, String settleddate, String loginname, Double stake, Double payout, Double realbet, int flagtime, String fromTime, String endTime, int page, int maxItem) {
        try {
            final HashSet set = new HashSet();
            final ArrayList records = new ArrayList();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            num.add(3, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("cmdgamerecord");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (sourcename != null && !sourcename.isEmpty()) {
                conditions.put("sourcename", sourcename);
            }
            if (referenceno != null && !referenceno.isEmpty()) {
                conditions.put("referenceno", referenceno);
            }
            if (dangerstatus != null && !dangerstatus.isEmpty()) {
                conditions.put("dangerstatus", dangerstatus);
            }
            if (betip != null && !betip.isEmpty()) {
                conditions.put("betip", betip);
            }
            if (loginname != null && !loginname.isEmpty()) {
                conditions.put("nick_name", loginname);
            }
            if (flagtime != 0 && fromTime != null && endTime != null && !fromTime.isEmpty() && !endTime.isEmpty()) {
                BasicDBObject obj;
                if (flagtime == 1) {
                    Date date1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(fromTime);
                    Date date2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                    BasicDBObject obj2 = new BasicDBObject();
                    obj2.put("$gte", (date1.getTime() * 1000000L));
                    obj2.put("$lte", (date2.getTime() * 1000000L));
                    conditions.put("transdate", obj2);
                }
                if (flagtime == 2) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("betdate", obj);
                }
                if (flagtime == 3) {
                    obj = new BasicDBObject();
                    obj.put("$gte", fromTime);
                    obj.put("$lte", endTime);
                    conditions.put("settleddate", obj);
                }
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    CmdGameRecords model = new CmdGameRecords();
                    model.setId(document.getInteger("id").intValue());
                    model.setSourcename(document.getString("sourcename"));
                    model.setReferenceno(document.getString("referenceno"));
                    model.setSoctransid(document.getInteger("soctransid").intValue());
                    model.setIsfirsthalf(document.getBoolean("isfirsthalf").toString());
                    model.setTransdate(document.getLong("transdate"));
                    model.setIshomegive(document.getBoolean("ishomegive").toString());
                    model.setIsbethome(document.getBoolean("isbethome").toString());
                    model.setBetamount(document.getDouble("betamount"));
                    model.setOutstanding(document.getDouble("outstanding"));
                    model.setHdp(document.getDouble("hdp"));
                    model.setOdds(document.getDouble("odds"));
                    model.setCurrency(document.getString("currency"));
                    model.setWinamount(document.getDouble("winamount"));
                    model.setExchangerate(document.getDouble("exchangerate"));
                    model.setWinlosestatus(document.getString("winlosestatus"));
                    model.setTranstype(document.getString("transtype"));
                    model.setDangerstatus(document.getString("dangerstatus"));
                    model.setMemcommission(document.getDouble("memcommission"));
                    model.setBetip(document.getString("betip"));
                    model.setHomescore(document.getInteger("homescore"));
                    model.setAwayscore(document.getInteger("awayscore"));
                    model.setRunhomescore(document.getInteger("runhomescore"));
                    model.setRunawayscore(document.getInteger("runawayscore"));
                    model.setIsrunning(document.getBoolean("isrunning").toString());
                    model.setRejectreason(document.getString("rejectreason"));
                    model.setSporttype(document.getString("sporttype"));
                    model.setChoice(document.getInteger("choice"));
                    model.setWorkingdate(document.getInteger("workingdate").intValue());
                    model.setOddstype(document.getString("oddstype"));
                    model.setMatchdate(document.getLong("matchdate"));
                    model.setHometeamid(document.getInteger("hometeamid"));
                    model.setAwayteamid(document.getInteger("awayteamid"));
                    model.setLeagueid(document.getInteger("leagueid"));
                    model.setSpecialid(document.getString("specialid"));
                    model.setStatuschange(document.getInteger("statuschange"));
                    model.setStateupdatets(document.getLong("stateupdatets"));
                    model.setMemcommissionset(document.getDouble("memcommissionset"));
                    model.setIscashout(document.getBoolean("iscashout").toString());
                    model.setCashouttotal(document.getDouble("cashouttotal"));
                    model.setCashouttakeback(document.getDouble("cashouttakeback"));
                    model.setCashoutwinloseamount(document.getDouble("cashoutwinloseamount"));
                    model.setBetsource(document.getInteger("betsource"));
                    model.setAosexcluding(document.getString("aosexcluding"));
                    model.setMmrpercent(document.getDouble("mmrpercent"));
                    model.setMatchid(document.getInteger("matchid").intValue());
                    model.setMatchgroupid(document.getString("matchgroupid"));
                    model.setBetremarks(document.getString("betremarks"));
                    model.setIsspecial(document.getBoolean("isspecial").toString());
                    model.setBetdate(document.getString("betdate"));
                    model.setSettleddate(document.getString("settleddate"));
                    model.setLoginname(document.getString("nick_name"));
                    model.setRealbet(document.getDouble("realBet"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Double bet = 0.0;
                    Double validBet = 0.0;
                    Double payout = 0.0;
                    set.add(document.getString("nick_name"));
                    long count = (Long)num.get(0) + 1L;
                    long totalBet = (Long)num.get(1) + bet.longValue();
                    long totalValidBet = (Long)num.get(2) + validBet.longValue();
                    long totalPayout = (Long)num.get(3) + payout.longValue();
                    num.set(0, count);
                    num.set(1, totalBet);
                    num.set(2, totalValidBet);
                    num.set(3, totalPayout);
                }
            });
            ThirdPartyResponse<List<CmdGameRecords>> res = new ThirdPartyResponse<List<CmdGameRecords>>((Long)num.get(0), (Long)num.get(1), (Long)num.get(2), (Long)num.get(3), records);
            res.setTotalPlayer(set.size());
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public List<AGGameRecordItem> showDetailAG(String billNo) {
        try {
            final ArrayList<AGGameRecordItem> records = new ArrayList<AGGameRecordItem>();
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("aggamerecord");
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (billNo != null && !billNo.isEmpty()) {
                conditions.put("billno", billNo);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    AGGameRecordItem model = new AGGameRecordItem(document.getString("billno"), document.getString("username"), document.getString("nickName"), document.getString("gamecode"), document.getString("gametype"), document.getString("platformtype"), document.getDouble("bet"), document.getDouble("validbet"), document.getDouble("payout"), document.getString("bettime"), document.getString("payouttime"), Integer.parseInt(document.getString("flag")), document.getString("currency"), document.getString("betip"), Integer.parseInt(document.getString("playtype")), Integer.parseInt(document.getString("devicetype")), document.getString("tablecode"), Double.parseDouble(document.getString("beforecredit")), document.getString("remark"), document.getString("round"), document.getString("gameresult"));
                    records.add(model);
                }
            });
            return records;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public List<WMGameRecordItem> showDetailWM(Long betid) {
        try {
            final ArrayList<WMGameRecordItem> records = new ArrayList<WMGameRecordItem>();
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("wmgamerecord");
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (betid != null) {
                conditions.put("betid", betid);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    WMGameRecordItem model = new WMGameRecordItem(document.getLong("betid"), document.getString("nick_name"), document.getString("user"), document.getString("bettime"), document.getString("settime"), document.getString("rootbettime"), document.getString("rootsettime"), document.getDouble("bet"), document.getDouble("validbet"), document.getDouble("winloss"), document.getDouble("water"), document.getDouble("waterbet"), document.getDouble("result"), document.getString("betcode"), document.getString("betresult"), document.getInteger("gameid"), document.getString("ip"), document.getInteger("event"), document.getInteger("eventchild"), document.getInteger("tableid"), document.getString("gameresult"), document.getString("gamename"), document.getInteger("commission"), document.getString("reset"));
                    records.add(model);
                }
            });
            return records;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public List<IbcGameRecordItem> showDetailIBC(Long transid) {
        try {
            final ArrayList<IbcGameRecordItem> records = new ArrayList<IbcGameRecordItem>();
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("ibc2gamerecord");
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (transid != null) {
                conditions.put("transid", transid);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    IbcGameRecordItem model = new IbcGameRecordItem(document.getLong("transid"), document.getString("playername"), document.getDate("transactiontime"), document.getInteger("matchid"), document.getInteger("leagueid"), document.getString("leaguename"), document.getInteger("awayid"), document.getString("awayidname"), document.getInteger("homeid"), document.getString("homeidname"), document.getDate("matchdatetime"), document.getInteger("bettype"), document.getLong("parlayrefno"), document.getString("betteam"), document.getDouble("hdp"), document.getString("sporttype"), document.getDouble("awayhdp"), document.getDouble("homehdp"), document.getDouble("odds"), document.getInteger("awayscore"), document.getInteger("homescore"), document.getString("islive"), document.getString("islucky"), document.getString("parlay_type"), document.getString("combo_type"), document.getLong("stake"), document.getString("bettag"), document.getLong("winloseamount"), document.getDate("winlostdatetime"), document.getInteger("versionkey"), document.getString("lastballno"), document.getString("ticketstatus"), document.getInteger("oddstype"), document.getLong("actual_stake"), document.getLong("refund_amount"), document.getString("nick_name"));
                    records.add(model);
                }
            });
            return records;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public List<CmdGameRecords> showDetailCMD(Long id) {
        try {
            final ArrayList<CmdGameRecords> records = new ArrayList<CmdGameRecords>();
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("cmdgamerecord");
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (id != null) {
                conditions.put("id", id);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    CmdGameRecords model = new CmdGameRecords();
                    model.setId(document.getInteger("id").intValue());
                    model.setSourcename(document.getString("sourcename"));
                    model.setReferenceno(document.getString("referenceno"));
                    model.setSoctransid(document.getInteger("soctransid").intValue());
                    model.setIsfirsthalf(document.getBoolean("isfirsthalf").toString());
                    model.setTransdate(document.getLong("transdate"));
                    model.setIshomegive(document.getBoolean("ishomegive").toString());
                    model.setIsbethome(document.getBoolean("isbethome").toString());
                    model.setBetamount(document.getDouble("betamount"));
                    model.setOutstanding(document.getDouble("outstanding"));
                    model.setHdp(document.getDouble("hdp"));
                    model.setOdds(document.getDouble("odds"));
                    model.setCurrency(document.getString("currency"));
                    model.setWinamount(document.getDouble("winamount"));
                    model.setExchangerate(document.getDouble("exchangerate"));
                    model.setWinlosestatus(document.getString("winlosestatus"));
                    model.setTranstype(document.getString("transtype"));
                    model.setDangerstatus(document.getString("dangerstatus"));
                    model.setMemcommission(document.getDouble("memcommission"));
                    model.setBetip(document.getString("betip"));
                    model.setHomescore(document.getInteger("homescore"));
                    model.setAwayscore(document.getInteger("awayscore"));
                    model.setRunhomescore(document.getInteger("runhomescore"));
                    model.setRunawayscore(document.getInteger("runawayscore"));
                    model.setIsrunning(document.getBoolean("isrunning").toString());
                    model.setRejectreason(document.getString("rejectreason"));
                    model.setSporttype(document.getString("sporttype"));
                    model.setChoice(document.getInteger("choice"));
                    model.setWorkingdate(document.getInteger("workingdate").intValue());
                    model.setOddstype(document.getString("oddstype"));
                    model.setMatchdate(document.getLong("matchdate"));
                    model.setHometeamid(document.getInteger("hometeamid"));
                    model.setAwayteamid(document.getInteger("awayteamid"));
                    model.setLeagueid(document.getInteger("leagueid"));
                    model.setSpecialid(document.getString("specialid"));
                    model.setStatuschange(document.getInteger("statuschange"));
                    model.setStateupdatets(document.getLong("stateupdatets"));
                    model.setMemcommissionset(document.getDouble("memcommissionset"));
                    model.setIscashout(document.getBoolean("iscashout").toString());
                    model.setCashouttotal(document.getDouble("cashouttotal"));
                    model.setCashouttakeback(document.getDouble("cashouttakeback"));
                    model.setCashoutwinloseamount(document.getDouble("cashoutwinloseamount"));
                    model.setBetsource(document.getInteger("betsource"));
                    model.setAosexcluding(document.getString("aosexcluding"));
                    model.setMmrpercent(document.getDouble("mmrpercent"));
                    model.setMatchid(document.getInteger("matchid").intValue());
                    model.setMatchgroupid(document.getString("matchgroupid"));
                    model.setBetremarks(document.getString("betremarks"));
                    model.setIsspecial(document.getBoolean("isspecial").toString());
                    model.setBetdate(document.getString("betdate"));
                    model.setSettleddate(document.getString("settleddate"));
                    model.setLoginname(document.getString("nick_name"));
                    model.setRealbet(document.getDouble("realBet"));
                    records.add(model);
                }
            });
            ArrayList<CmdGameRecords> res = records;
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public ThirdPartyResponse<List<SboGameRecord>> filterSBO(String playerName, String nickName, String refno, String status, int flagTime, String fromTime, String endTime, Double betAmount, Double winAmount, int page, int maxItem, String sporttype) {
        final HashSet set = new HashSet();
        final ArrayList<Long> num = new ArrayList<Long>();
        num.add(0, 0L);
        num.add(1, 0L);
        num.add(2, 0L);
        num.add(3, 0L);
        final ArrayList records = new ArrayList();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            BasicDBObject obj;
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("sbogamerecord");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (refno != null && !"".equals(refno)) {
                conditions.put("refno", refno);
            }
            if (playerName != null && !"".equals(playerName)) {
                conditions.put("player_name", playerName);
            }
            if (nickName != null && !"".equals(nickName)) {
                conditions.put("nick_name", nickName);
            }
            if (status != null && !"".equals(status)) {
                conditions.put("status", status);
            }
            if (fromTime != null && !"".equals(fromTime) && endTime != null && !"".equals(endTime)) {
                obj = new BasicDBObject();
                obj.put("$gte", new Timestamp(dateFormat.parse(fromTime).getTime()));
                obj.put("$lte", new Timestamp(dateFormat.parse(endTime).getTime()));
                if (flagTime == 0) {
                    conditions.put("ordertime", obj);
                } else if (flagTime == 1) {
                    conditions.put("winlostdate", obj);
                } else {
                    conditions.put("modifydate", obj);
                }
            }
            if (sporttype != null && !"".equals(sporttype)) {
                conditions.put("sporttype", sporttype);
            }
            if (betAmount != null) {
                obj = new BasicDBObject();
                obj.put("$gte", betAmount);
                conditions.put("betAmount", obj);
            }
            if (winAmount != null) {
                obj = new BasicDBObject();
                obj.put("$gte", winAmount);
                conditions.put("winAmount", winAmount);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    SboGameRecord model = new SboGameRecord(document.getString("refno"), document.getString("player_name"), document.getString("nick_name"), document.getString("sporttype"), document.getDate("ordertime"), document.getDate("winlostdate"), document.getDate("modifydate"), document.getDouble("odds"), document.getString("oddsstyle"), document.getLong("stake"), document.getLong("actualstake"), document.getString("currency"), document.getString("status"), document.getLong("winlose"), document.getDouble("turnover"), document.getString("ishalfwonlose"), document.getString("islive"), document.getLong("maxwinwithoutactualstakerecord"), document.getString("ip"), document.getString("betoption"), document.getString("markettype"), document.getDouble("hdp"), document.getString("league"), document.getString("match"), document.getString("livescore"), document.getString("htscore"), document.getString("ftscore"), document.getLong("payout"), document.getLong("effective_bet"), document.getLong("valid_stake"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            final List<String> lstTicketStatus = Arrays.asList("won", "lose", "draw", "half won", "half lose");
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Long bet = document.getLong("stake");
                    Long validBet = document.getLong("actualstake");
                    Long payout = document.getLong("payout");
                    set.add(document.getString("nick_name"));
                    long count = (Long)num.get(0) + 1L;
                    String ticketStatus = document.getString("status");
                    if (ticketStatus != null && lstTicketStatus.contains(ticketStatus.toLowerCase())) {
                        long totalBet = (Long)num.get(1) + bet;
                        long totalValidBet = (Long)num.get(2) + validBet;
                        long totalPayout = (Long)num.get(3) + payout;
                        num.set(1, totalBet);
                        num.set(2, totalValidBet);
                        num.set(3, totalPayout);
                    }
                    num.set(0, count);
                }
            });
            ThirdPartyResponse<List<SboGameRecord>> res = new ThirdPartyResponse<List<SboGameRecord>>((Long)num.get(0), (Long)num.get(1), (Long)num.get(2), (Long)num.get(3), records);
            res.setTotalPlayer(set.size());
            logger.info(("sbolog response =" + res.toJson()));
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }
}

