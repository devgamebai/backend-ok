package com.vinplay.dao.sbo.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dao.sbo.SboDao;
import com.vinplay.dto.sbo.SboRecordDetail;
import com.vinplay.dto.sbo.SboSubRecord;
import com.vinplay.dto.sbo.SboUserDto;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQPublishTask;

public class SboDaoImpl implements SboDao {

	private static final Logger LOGGER = Logger.getLogger(SboDaoImpl.class);

	private static final String COLLECTION_GAMERECORD = "sbogamerecord";

	@Override
	public List<Integer> maxSboUser() throws Exception {
		List<Integer> result = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				CallableStatement call = conn.prepareCall("CALL p_AG_MaxSboUser(?,?)");) {
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
    public boolean generateSboUser(String sboid, int sbocountid) throws Exception {
        try(Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		CallableStatement call = conn.prepareCall("CALL p_AG_GeneralSboUser(?,?)");) {
            call.setString(1, sboid);
            call.setInt(2, sbocountid);
            int rs = call.executeUpdate();
			if (rs > 0) {
				LOGGER.info("[TaskAutoCreateUserSbo] create" + sboid + " to sbouser tbl ok!");
                return true;
            }
			LOGGER.error("[TaskAutoCreateUserSbo] Insert sboid " + sboid + "to sbouser tbl error!");
        } catch (SQLException e) {
            LOGGER.error(e);
            return false;
        }
        return false;
    }

	private Timestamp convertTimetoZone7(String time) {
		if (time != null && !"".equals(time)) {
			try {
				LocalDateTime times = LocalDateTime.parse(time).plusHours(11);
				return Timestamp.valueOf(times);
			} catch (DateTimeParseException e) {
			}
		}
		return Timestamp.valueOf(LocalDateTime.now());
	}
	
	public static void main(String[] args) {
		LocalDateTime x = LocalDateTime.now();
		Timestamp t = Timestamp.valueOf(x);
		System.out.println(x.toString());
		System.out.println(t.toString());
	}
	
	private static final List<String> STATUS = Arrays
			.asList(new String[] { "won", "lose", "half won", "half lose", "draw" });
	private static final List<String> STATUS_ODDS = Arrays
			.asList(new String[] { "won", "lose", "half won", "half lose" });
    @Override
    public boolean saveSboBetLog(SboRecordDetail record) throws Exception {
    	String playerName = record.getUsername();
    	Timestamp orderTime = convertTimetoZone7(record.getOrderTime());
    	Timestamp winloseDate = convertTimetoZone7(record.getWinLostDate());
    	Timestamp modifyDate = convertTimetoZone7(record.getModifyDate());
    	Timestamp settleTime = convertTimetoZone7(record.getSettleTime());
		
		long stake = record.getStake() != null ? record.getStake().longValue() * GameThirdPartyInit.SBO_RATE : 0;
		long acutualStake = record.getActualStake() != null
				? record.getActualStake().longValue() * GameThirdPartyInit.SBO_RATE
				: 0;
		long winloseAmount = record.getWinLost() != null ? record.getWinLost().longValue() * GameThirdPartyInit.SBO_RATE
				: 0;
		//status
		String status = record.getStatus();
		
		if("true".equals(record.getIsHalfWonLose())) {
			if("won".equals(status)) {
				status = "half won";
			}else if ("lose".equals(status)) {
				status = "half lose";
			}else {
				status = status.toLowerCase();
			}
		} else {
			status = status.toLowerCase();
		}
//		 --1.Tính v_payout = p_winlose;
			long payout =0;
			if(winloseAmount<0) {
				payout = 0;
			}else {
				payout = winloseAmount;
			}
			long winamount = winloseAmount - acutualStake;
			long effective = winamount < 0 ? winamount * (-1) : winamount;
		//
		List<SboSubRecord> lstSboSub = record.getSubBet();
		SboSubRecord sbosub =null;
		if (lstSboSub != null && !lstSboSub.isEmpty()) {
			sbosub = lstSboSub.get(0);
		}else {
			sbosub = new SboSubRecord();
		}
		//check exist
        SboUserDto sboUser = findSboUserBySboId(playerName);
        if (sboUser == null) {
            return false;
        }
        UserService userService = new UserServiceImpl();
        UserModel userModel = userService.getUserByNickName(sboUser.getLoginname());
        //oddstype
		String oddstype = record.getOddsStyle();
		Double odds = record.getOdds();
		long validStake =0;
		if("M".equals(oddstype)) {
			if(0.5 <= odds || odds < 0.01){
				if(STATUS_ODDS.contains(status)) {
					validStake = effective;
				}
			}
		}else if ("H".equals(oddstype)) {
			if(odds>0.5) {
				if(STATUS_ODDS.contains(status)) {
					validStake = effective;
				}
			}
		}else if ("E".equals(oddstype)) {
			if(odds>=1.5) {
				if(STATUS_ODDS.contains(status)) {
					validStake = effective;
				}
			}
		}else if ("I".equals(oddstype)) {
			if(odds >= -2.0) {
				if(STATUS_ODDS.contains(status)) {
					validStake = effective;
				}
			}
		}
		String nickName = sboUser.getLoginname();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection<Document> col = db.getCollection(COLLECTION_GAMERECORD);
            BasicDBObject doc = new BasicDBObject();
            doc.append("refno", record.getRefNo());
			doc.append("player_name", record.getUsername());
			doc.append("nick_name", nickName);
			doc.append("sporttype", record.getSportsType());
			doc.append("ordertime", orderTime);
			doc.append("winlostdate", winloseDate);
			doc.append("modifydate", modifyDate);
			doc.append("settletime",settleTime);
			doc.append("odds", record.getOdds()!= null ? record.getOdds() : 0);
			doc.append("oddsstyle", oddstype);
			doc.append("stake", stake);
			doc.append("actualstake", acutualStake);
			doc.append("currency", record.getCurrency());
			doc.append("status", status);
			doc.append("winlose", winloseAmount);
			doc.append("turnover", record.getTurnover()!= null ? record.getTurnover() * GameThirdPartyInit.SBO_RATE: 0);
			doc.append("ishalfwonlose", record.getIsHalfWonLose());// boolean,
			doc.append("islive", record.getIsLive());// boolean,
			doc.append("maxwinwithoutactualstakerecord",
					record.getMaxWinWithoutActualStake() != null
							? record.getMaxWinWithoutActualStake().longValue() * GameThirdPartyInit.SBO_RATE
							: 0);
			doc.append("ip", record.getIp());
			doc.append("betoption",sbosub.getBetOption());
			doc.append("markettype",sbosub.getMarketType());
			doc.append("hdp",sbosub.getHdp());
			doc.append("league",sbosub.getLeague());
			doc.append("match",sbosub.getMatch());
			doc.append("livescore",sbosub.getLiveScore());
			doc.append("htscore",sbosub.getHtScore());
			doc.append("ftscore",sbosub.getFtScore());
			doc.append("payout",payout);
			doc.append("effective_bet",effective);
			doc.append("valid_stake",validStake);
			
			Document conditions = new Document();
            conditions.put("refno", record.getRefNo());
            long count = col.count(conditions);
            if (count > 0) {
                // update
                col.updateOne(conditions, new Document("$set", doc));
                LOGGER.info("[TaskCollectSBOLog] Process update log success ! ,getRefNo = " + record.getRefNo());
                if(STATUS.contains(status)) {
                	if (payout != 0) {
                        LogMoneyUserMessage message = new LogMoneyUserMessage(0, sboUser.getLoginname(), "SBO",
                                Games.SBO_GAMES.getId() + "", 0,
                                Math.abs(payout), "vin",
                                "", 0, false, false);
                        message.setReferralCode(userModel.getReferralCode());
                        RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                        taskReportUser.start();
                    }
                }
            } else {
                // insert
                col.insertOne(new Document(doc));
                LOGGER.info("[TaskCollectSBOLog]  Process insert log success! ,getRefNo= " + record.getRefNo());
               
                if(STATUS.contains(status)) {
                	 if (validStake != 0) {
                         LogMoneyUserMessage message = new LogMoneyUserMessage(0, sboUser.getLoginname(), "SBO",
                                 Games.SBO_GAMES.getId() + "", 0,
                                 -Math.abs(validStake), "vin",
                                 "", 0, false, false);
                         message.setReferralCode(userModel.getReferralCode());
                         RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                         taskReportUser.start();
                     }

                     if (payout != 0) {
                         LogMoneyUserMessage message = new LogMoneyUserMessage(0, sboUser.getLoginname(), "SBO",
                                 Games.SBO_GAMES.getId() + "", 0,
                                 Math.abs(payout), "vin",
                                 "", 0, false, false);
                         message.setReferralCode(userModel.getReferralCode());
                         RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                         taskReportUser.start();
                     }
                }
               
            }

            return true;
        } catch (Exception ex) {
            LOGGER.error("[TaskCollectSBOLog] Process saving log had error! ", ex);
            return false;
        }
    }

    @Override
    public SboUserDto findSboUserByNickName(String nickName) throws SQLException {
        SboUserDto sbo = null;
        String sql = "SELECT * FROM sbouser WHERE nick_name=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		PreparedStatement stm = conn.prepareStatement(sql);) {
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                sbo = new SboUserDto(rs.getString("sboid"), nickName,
                        (long)rs.getInt("sbocountid"));
            }else {
            	//add user 
            	return mappingUserSbo(nickName);//update nickName
			}
            rs.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return sbo;
    }

    @Override
    public SboUserDto findSboUserBySboId(String sboId) throws SQLException {
        String sql = "SELECT * FROM sbouser WHERE sboid=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		PreparedStatement stm = conn.prepareStatement(sql);) {
            stm.setString(1, sboId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new SboUserDto(sboId, rs.getString("nick_name"),
                       (long) rs.getInt("sbocountid"));
            }
            rs.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return null;
    }

    @Override
    public SboUserDto mappingUserSbo(String nickName) throws SQLException {
        ResultSet rs = null;
        int param = 1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		 CallableStatement  call = conn.prepareCall("CALL p_AG_MappingSBOUser(?)");	){
            call.setString(param++, nickName);
            rs = call.executeQuery();
            if (rs.next()) {
                String sboid = rs.getString("sboid");
                if (sboid != null && !"".equals(sboid)) {
                    return new SboUserDto(rs);
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
