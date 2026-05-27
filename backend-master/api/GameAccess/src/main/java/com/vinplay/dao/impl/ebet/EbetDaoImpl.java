package com.vinplay.dao.impl.ebet;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dao.ebet.EbetDao;
import com.vinplay.dto.ebet.BetHistoriesDto;
import com.vinplay.dto.ebet.EbetUserItem;
import com.vinplay.interfaces.ebet.MemberEbetService;
import com.vinplay.logic.CommonMethod;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQPublishTask;

public class EbetDaoImpl implements EbetDao {

	private static final Logger LOGGER = Logger.getLogger(EbetDaoImpl.class);

	private static final String COLLECTION_GAMERECORD = "ebetgamerecord";

	@Override
	public List<Integer> maxEbetUser() throws Exception {
		List<Integer> result = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				CallableStatement call = conn.prepareCall("CALL p_AG_MaxEbetUser(?,?)");) {
			call.registerOutParameter(1, java.sql.Types.INTEGER);
			call.registerOutParameter(2, java.sql.Types.INTEGER);
			call.execute();
			result.add(call.getInt(1));
			result.add(call.getInt(2));
		} catch (SQLException e) {
			LOGGER.error(e);
		}
		return result;
	}

    @Override
    public boolean generateEbetUser(String ebetid, int ebetcountid,String password) throws Exception {
        try(Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		CallableStatement call = conn.prepareCall("CALL p_AG_GeneralEbetUser(?,?,?)");) {
            call.setString(1, ebetid);
            call.setInt(2, ebetcountid);
            call.setString(3, password);
            int rs = call.executeUpdate();
			if (rs > 0) {
				LOGGER.info("[TaskAutoCreateUserEbet] create" + ebetid + " to ebetuser tbl ok!");
                return true;
            }
			LOGGER.error("[TaskAutoCreateUserEbet] Insert ebetid " + ebetid + "to ebetuser tbl error!");
        } catch (SQLException e) {
            LOGGER.error(e);
            return false;
        } 
        return false;
    }

    private String convertRightDateTime(Integer timeStamp) {
		Date time = new Date((long)timeStamp*1000);
		String dateTime = CommonMethod.dateToStr(time,"yyyy-MM-dd HH:mm:ss");
		return dateTime;
	}
	@Override
	public boolean saveEbetBetLog(List<BetHistoriesDto> betHistory) throws Exception {
		for (BetHistoriesDto gri : betHistory) {
			EbetUserItem eBetUserItem = findEbetUserByEbetId(gri.getUsername());
			if (eBetUserItem == null || eBetUserItem.getLoginname() == null) {
				LOGGER.info("[TaskCollectEBetLog] P_AG_EBetLog there is no loginname mapping with eBETId: "
						+ gri.getUsername());
				continue;
			}
			MongoDatabase db = MongoDBConnectionFactory.getDB();
			long balance = gri.getBalance() != null ? gri.getBalance().longValue() *1000 : 0;
			long bet = gri.getBet() != null ? gri.getBet().longValue() * 1000 : 0;
			long validbet = gri.getValidBet() != null ? gri.getValidBet().longValue() * 1000 : 0;
			long profit = balance + validbet;
			
	        try {
	            MongoCollection<Document> col = db.getCollection(COLLECTION_GAMERECORD);
	            BasicDBObject doc = new BasicDBObject();
				doc.append("validbet", validbet);
				doc.append("nick_name", eBetUserItem.getLoginname());
				doc.append("userid", gri.getUserId() != null ? gri.getUserId() : 0);
				doc.append("ebetid", eBetUserItem.getEbetid());
				doc.append("tigercard", gri.getTigerCard() != null ? gri.getTigerCard() : 0);
				doc.append("roundno", gri.getRoundNo());
				doc.append("playercards", gri.getPlayerCards() != null ? CommonMethod.convertArrayToString(gri.getPlayerCards()) : null);
				doc.append("platform", gri.getPlatform() != null ? gri.getPlatform() : 0);
				doc.append("payouttime", gri.getPayoutTime() != null ? convertRightDateTime(gri.getPayoutTime()) : null);
				doc.append("payout", profit);
				doc.append("judgeresult", gri.getJudgeResult() != null ? CommonMethod.convertArrayToString(gri.getJudgeResult()) : null);
				doc.append("gametype", gri.getGameType() != null ? gri.getGameType() : 0); // profit is balance + bet
				doc.append("gamename", gri.getGameName());
				doc.append("ebetnumber", gri.getNumber() != null ? gri.getNumber() : 0);
				doc.append("dragoncard", gri.getDragonCard() != null ? gri.getDragonCard() : 0);
				doc.append("createtime", gri.getCreateTime() != null ? convertRightDateTime(gri.getCreateTime()) : null);
				doc.append("bethistoryid", gri.getBetHistoryId());
				doc.append("bet", bet);
				doc.append("bankercards", gri.getBankerCards() != null ? CommonMethod.convertArrayToString(gri.getBankerCards()) : null);
				doc.append("alldices", gri.getAllDices() != null ? CommonMethod.convertArrayToString(gri.getAllDices()) : null);
				
				Document conditions = new Document();
	            conditions.put("bethistoryid", gri.getBetHistoryId());
	            long count = col.count(conditions);
	            if (count > 0) {
					//update
					 col.updateOne(conditions, new Document("$set", doc));
					 LOGGER.info("[TaskCollectEBetLog] P_AG_EBetLog Storage success");
				}else {
					//insert
					col.insertOne(new Document(doc));
					UserService userService = new UserServiceImpl();
					UserModel userModel = userService.getUserByNickName(eBetUserItem.getLoginname());
					if (validbet != 0) {
                        LogMoneyUserMessage message = new LogMoneyUserMessage(0, eBetUserItem.getLoginname(), "EBET",
                                Games.EBET_GAMES.getId() + "", 0,
                                -Math.abs(validbet), "vin",
                                "", 0, false, false);
                        message.setReferralCode(userModel.getReferralCode());
                        RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                        taskReportUser.start();
                    }

                    if (profit != 0) {
                        LogMoneyUserMessage message = new LogMoneyUserMessage(0, eBetUserItem.getLoginname(), "EBET",
                                Games.EBET_GAMES.getId() + "", 0,
                                Math.abs(profit), "vin",
                                "", 0, false, false);
                        message.setReferralCode(userModel.getReferralCode());
                        RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                        taskReportUser.start();
                    }
				}
			} catch (Exception ex) {
				LOGGER.error("ex", ex);
			} 
		}
		return false;
	}

    @Override
    public EbetUserItem findEbetUserByNickName(String nickName) throws SQLException {
        EbetUserItem ebet = null;
        String sql = "SELECT * FROM ebetuser WHERE nick_name=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		PreparedStatement stm = conn.prepareStatement(sql);) {
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                ebet = new EbetUserItem(rs);
            }
            rs.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return ebet;
    }

    @Override
    public EbetUserItem findEbetUserByEbetId(String ebetId) throws SQLException {
        String sql = "SELECT * FROM ebetuser WHERE ebetid=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		PreparedStatement stm = conn.prepareStatement(sql);) {
            stm.setString(1, ebetId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new EbetUserItem(rs);
            }
            rs.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return null;
    }

    @Override
    public EbetUserItem mappingUserEbet(String nickName) throws SQLException {
        ResultSet rs = null;
        int param = 1;
        int timeStamp = (int) (System.currentTimeMillis() / 1000L);
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		 CallableStatement  call = conn.prepareCall("CALL p_AG_MappingEBETUser(?,?)");){
            call.setString(param++, nickName);
            call.setInt(param++, timeStamp);
            rs = call.executeQuery();
            if (rs.next()) {
                String ebetid = rs.getString("ebetid");
                if (ebetid != null && !"".equals(ebetid)) {
                	EbetUserItem userItem = new EbetUserItem(rs);
                	String token = CommonMethod.encoding(timeStamp, userItem.getEbetid(), userItem.getPassword()); //generate access token
    				userItem.setToken(token);
                    return userItem;
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return null;
    }

}
