package com.vinplay.dao.impl.ag;

import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dao.ag.AgDao;
import com.vinplay.dto.ag.AGGamesReportsDetailData;
import com.vinplay.item.AGUserItem;
import com.vinplay.logic.CommonMethod;
import com.vinplay.service.GameDaoService;
import com.vinplay.service.ag.impl.AgServiceImpl;
import com.vinplay.service.impl.GameDaoServiceImpl;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;

public class AgDaoImpl implements AgDao {

    private static final String COLLECTION_AGUSER = "aguser";
    private static final String COLLECTION_GAMERECORD = "aggamerecord";

    public static final Logger LOGGER = Logger.getLogger(AgServiceImpl.class);
    private GameDaoService gameDaoService = new GameDaoServiceImpl();

    @Override
    public boolean createUserNoMapping(String agId, String agPassword, int agCountId) {
        //INSERT INTO AGUSER(agid,loginname,password,agcountid) VALUES(p_ag_id,null,p_ag_password,p_ag_count_id);
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection<Document> col = db.getCollection(COLLECTION_AGUSER);
            Document doc = new Document();
            doc.append("agid", agId);
            doc.append("nick_name", null);
            doc.append("password", agPassword);
            doc.append("agcountid", agCountId);
            col.insertOne(doc);
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
        LOGGER.info("Insert_AG id : " + agId + " to aguser tbl ok!");
        return true;
    }

    @Override
    public boolean saveRecord(AGGamesReportsDetailData detailData) {
        String billNo = detailData.getBillNo() == null ? "" : detailData.getBillNo();
        String playerName = detailData.getPlayName() == null ? "" : detailData.getPlayName();
        String gameCode = detailData.getGameCode() == null ? "" : detailData.getGameCode();
        String payout = detailData.getNetAmount() == null ? "0" : detailData.getNetAmount();
        String bet = detailData.getBetAmount() == null ? "0" : detailData.getBetAmount();
        String validBet = detailData.getValidBetAmount() == null ? "0" : detailData.getValidBetAmount();
        String betTime = detailData.getBetTime() == null ? "" : detailData.getBetTime();
        String payoutTime = detailData.getRecalcuTime() == null ? "" : detailData.getRecalcuTime();
        String gameType = detailData.getGameType() == null ? "" : detailData.getGameType();
        String flag = detailData.getFlag() == null ? "" : detailData.getFlag();
        String playType = detailData.getPlayType() == null ? "" : detailData.getPlayType();
        String currency = detailData.getCurrency() == null ? "" : detailData.getCurrency();
        String tableCode = detailData.getTableCode() == null ? "" : detailData.getTableCode();
        String beforeCredit = detailData.getBeforeCredit() == null ? "" : detailData.getBeforeCredit();
        String betIP = detailData.getBetIP() == null ? "" : detailData.getBetIP();
        String platformType = detailData.getPlatformType() == null ? "" : detailData.getPlatformType();
        String remark = detailData.getRemark() == null ? "" : detailData.getRemark();
        String round = detailData.getRound() == null ? "" : detailData.getRound();
        String result = detailData.getResult() == null ? "" : detailData.getResult();
        String deviceType = detailData.getDeviceType() == null ? "" : detailData.getDeviceType();

        String saveBetTime = "";
        String savePayoutTime = "";
        if (!"".equals(betTime)) {
            saveBetTime = CommonMethod.convertToCurrentSysTime(CommonMethod.convertFromThirdPartiesTimeZoneToUTC(
                    betTime, GameThirdPartyInit.AG_TIMEZONE, "yyyy-MM-dd HH:mm:ss"), "yyyy-MM-dd HH:mm:ss");
        }
        if (!"".equals(payoutTime)) {
            savePayoutTime = CommonMethod.convertToCurrentSysTime(CommonMethod.convertFromThirdPartiesTimeZoneToUTC(
                    payoutTime, GameThirdPartyInit.AG_TIMEZONE, "yyyy-MM-dd HH:mm:ss"), "yyyy-MM-dd HH:mm:ss");
        }
        String nickName = gameDaoService.findNickNameByGameUserId("agid", playerName, COLLECTION_AGUSER);
        if (nickName == null || "".equals(nickName)) {
            LOGGER.info("[TaskCollectAGGamesLog] No user with AG game's name \"" + detailData.getPlayName() + "\"");
            if (payoutTime != "") {
                //updateLastTime(savePayoutTime);
                gameDaoService.updateLastTime("ag", savePayoutTime);
            }
            return false;
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection<Document> col = db.getCollection(COLLECTION_GAMERECORD);
            //TODO if exist -> update , not exist -> insert
            //SELECT COUNT(LOGINNAME) into v_count FROM AGGAMERECORD WHERE BILLNO = p_bill_no AND username = p_user_name AND gamecode =p_game_code AND round = p_round;
            Document conditions = new Document();
            conditions.put("billno", billNo);
            conditions.put("username", playerName);
            conditions.put("gamecode", gameCode);
            conditions.put("round", round);
            long count = col.count(conditions);

            BasicDBObject doc = new BasicDBObject();
            doc.append("billno", billNo);
            doc.append("username", playerName);
            doc.append("nickName", nickName);
            doc.append("gamecode", gameCode);
            doc.append("gametype", gameType);
            doc.append("platformtype", platformType);
            doc.append("bet", Double.parseDouble(bet) * GameThirdPartyInit.AG_RATE);
            doc.append("validbet", Double.parseDouble(validBet) * GameThirdPartyInit.AG_RATE);
            doc.append("payout", Double.parseDouble(payout) * GameThirdPartyInit.AG_RATE);
            doc.append("bettime", saveBetTime);
            doc.append("payouttime", savePayoutTime);
            doc.append("flag", flag);
            doc.append("currency", currency);
            doc.append("betip", betIP);
            doc.append("playtype", playType);
            doc.append("devicetype", deviceType);
            doc.append("tablecode", tableCode);
            doc.append("beforecredit", beforeCredit);
            doc.append("remark", remark);
            doc.append("round", round);
            doc.append("gameresult", result);
            doc.append("sport_odds", "");
            doc.append("sport_competition", "");
            doc.append("sport_market", "");
            doc.append("sport_selection", "");
            doc.append("sport_simplifiedresult", "");
            doc.append("sport_types", "");
            doc.append("sport_category", "");
            doc.append("sport_extbillno", "");
            doc.append("sport_thirdbillno", "");
            doc.append("sport_bettype", "");
            doc.append("sport_system", "");
            doc.append("sport_live", "");
            doc.append("sport_currentscore", "");
            if (count > 0) {
                //update
                col.updateOne(conditions, new Document("$set", doc));
            } else {
                //insert
                col.insertOne(new Document(doc));
                UserService userService = new UserServiceImpl();
    			long svalidBet = Math.round(doc.getDouble("validbet"));
    			long spayout = Math.round(doc.getDouble("payout"));

                if (svalidBet != 0) {

                    UserModel userModel = userService.getUserByNickName(nickName);
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), nickName, "AG",
                            Games.AG_GAMES.getId() + "", 0,
    						-Math.abs(svalidBet), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }

                if (spayout > 0) {
                    UserModel userModel = userService.getUserByNickName(nickName);
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), nickName, "AG",
                            Games.AG_GAMES.getId() + "", 0,
    						Math.abs(spayout), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }

            }
            gameDaoService.updateLastTime("ag", savePayoutTime);

        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
        return true;
    }

    private String findMinimumByColumn(String field) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_AGUSER);
        Document conditions = new Document();
        conditions.put("nick_name", null);

        FindIterable<?> cursor = collection.find(conditions).sort(new BasicDBObject(field, 1)).limit(1);
        Document doc = cursor != null ? (Document)cursor.first() : null;
        String agid = "";
        if (doc != null) {
            agid = doc.getString("agid");
        } else {
            return null;
        }
        return agid;
    }

    //	public boolean insertIbc2User(String ibcId, Double max_transfer, Double min_transfer, Integer oddtype,
//			Integer IBCCOUNTID,String nickName) {
//		MongoDatabase db = MongoDBConnectionFactory.getDB();
//        MongoCollection<Document> col = db.getCollection(COLLECTION_AGUSER);
//        Document doc = new Document();
//        doc.append("agcountid", ibcId);
//        doc.append("min_transfer", min_transfer);
//        doc.append("max_transfer", max_transfer);
//        doc.append("oddtype", oddtype);
//        doc.append("ibccountid", IBCCOUNTID);
//        doc.append("nick_name", nickName);
//        col.insertOne(doc);
//        LOGGER.info("[TaskAutoCreateUserIbc2] Insert ibc id : " + ibcId + " to ibcuser tbl ok!");
//        return true;
//	}
    @Override
    public AGUserItem mappingUser(String nickName) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_AGUSER);

        Document conditions = new Document();
        conditions.put("nick_name", nickName);
        FindIterable<?> iterable = collection.find(conditions);
        
        Document docs = iterable != null ? (Document)iterable.first() : null;
        AGUserItem agUser = null;
        if(docs!=null) {
        	agUser = new AGUserItem();
            agUser.setAgcountid(docs.getInteger("agcountid", 0));
            agUser.setAgid(docs.getString("agid"));
            agUser.setLoginname(nickName);
            agUser.setPassword(docs.getString("password"));
        }
        if (agUser != null && agUser.getAgid() != null) {
            // exist -> get data
            return agUser;
        } else {
            //insert
            int count = gameDaoService.countUserRemain(null, COLLECTION_AGUSER);
            if (count == 0) {
                LOGGER.error("There are no spaces left to store user in aguser table");
            } else {
                //UPDATE AGUSER SET loginname=p_loginname WHERE agcountid=v_min_row;
                String minAgIdRemain = findMinimumByColumn("agcountid");
                Document cond = new Document();
                cond.put("agid", minAgIdRemain);
                BasicDBObject doc = new BasicDBObject();
                doc.append("nick_name", nickName);
                collection.updateOne(cond, new Document("$set", doc));
                agUser = new AGUserItem();
                agUser.setAgid(minAgIdRemain);
                agUser.setLoginname(nickName);
                return agUser;
            }
        }
        return agUser;
    }

}
