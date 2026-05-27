package com.vinplay.service.ibc2.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import com.vinplay.vbee.common.statics.MongoCollections;
import com.vinplay.vbee.common.utils.VinPlayUtils;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dto.ResultFormat;
import com.vinplay.dto.ibc2.BetDetail;
import com.vinplay.dto.ibc2.Ibc2CheckUserBalanceReqDto;
import com.vinplay.dto.ibc2.Ibc2CheckUserBalanceRespDto;
import com.vinplay.dto.ibc2.Ibc2FundTransferReqDto;
import com.vinplay.dto.ibc2.Ibc2FundTransferRespDto;
import com.vinplay.dto.ibc2.LogInRespDto;
import com.vinplay.dto.ibc2.common.CheckBalanceDataRespDto;
import com.vinplay.interfaces.ibc2.FundTransferIbc2Service;
import com.vinplay.interfaces.ibc2.MemberIbc2Service;
import com.vinplay.logic.CommonMethod;
import com.vinplay.logic.InitData;
import com.vinplay.payment.utils.PayCommon.PAYSTATUS;
import com.vinplay.service.GameDaoService;
import com.vinplay.service.ibc2.Ibc2Dao;
import com.vinplay.service.impl.GameDaoServiceImpl;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.utils.BaseResponse;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.response.TranferMoneyResponse;
public class Ibc2DaoImpl implements Ibc2Dao {

	private static final String COLLECTION_IBC2USER="ibc2user";
	private static final String COLLECTION_VERSIONKEY="sportversionkey";
	private static final String COLLECTION_GAMERECORD="ibc2gamerecord";

	private static final int ODDS_TYPE = 1;
	private static final double MAX_TRANSFER = 100000000;
	private static final double MIN_TRANSFER = 100000; // 100 for ibc platform, 100.000 for lottery platform
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public static final Logger LOGGER = Logger.getLogger(Ibc2DaoImpl.class);

	private GameDaoService gameDao = new GameDaoServiceImpl();

	@Override
	public Integer getMaxFieldValue(String fieldName) {
		if (fieldName == null)
			fieldName = "ibccountid";
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);
        FindIterable<?> cursor = collection.find().sort(new BasicDBObject(fieldName,-1)).limit(1);
		Document doc = cursor != null ? (Document)cursor.first() : null;
        int id = 0;
        if (doc != null) {
        	id = doc.getInteger(fieldName);
        }else {
			return 0;
		}
        return id;
	}

	@Override
	public Integer countUserRemain(String nickName) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);
        Document conditions = new Document();
        conditions.put("nick_name", nickName);
        Long count = collection.count(conditions);
        return count.intValue();
	}

	@Override
	public boolean insertIbc2User(String ibcId, Double max_transfer, Double min_transfer, Integer oddtype,
			Integer IBCCOUNTID,String nickName) {
		MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection(COLLECTION_IBC2USER);
        Document doc = new Document();
        doc.append("ibcid", ibcId);
        doc.append("min_transfer", min_transfer);
        doc.append("max_transfer", max_transfer);
        doc.append("oddtype", oddtype);
        doc.append("ibccountid", IBCCOUNTID);
        doc.append("nick_name", nickName);
        col.insertOne(doc);
        LOGGER.info("[TaskAutoCreateUserIbc2] Insert ibc id : " + ibcId + " to ibcuser tbl ok!");
        return true;
	}



	private static final List<String> lstTicketStatus = Arrays
			.asList(new String[] { "won", "lose", "draw", "half won", "half lose" });
	
	public static void main(String[] args) {
		BigDecimal x  =new BigDecimal(1000.99999);
		Double y =x.doubleValue();
		System.out.println(y.longValue());
	}
	@Override
	public boolean saveLogs(BetDetail reg) {
		Long trans_id = reg.getTrans_id() != null ? reg.getTrans_id() : 0;
		String player_name = reg.getVendor_member_id() != null ? reg.getVendor_member_id() : "";
		Timestamp transaction_time = reg.getTransaction_time() != null ? new java.sql.Timestamp(reg.getTransaction_time().getTime()) : null;
		int match_id = reg.getMatch_id() != null ? reg.getMatch_id() : 0;
		int league_id = reg.getLeague_id() != null ? reg.getLeague_id() : 0;
		String league_name = reg.getLeague_id() != null ? reg.getLeague_id().toString() : "";
		int away_id = reg.getAway_id() != null ? reg.getAway_id() : 0;
		String away_id_name = reg.getAway_id() != null ? reg.getAway_id().toString() : "";
		int home_id = reg.getHome_id() != null ? reg.getHome_id() : 0;
		String home_id_name = reg.getHome_id() != null ? reg.getHome_id().toString() : "";
		Timestamp match_date_time = reg.getMatch_datetime() != null ? new java.sql.Timestamp(reg.getMatch_datetime().getTime()) : null;
		int bet_type = reg.getBet_type() != null ? reg.getBet_type() : 0;
		long parlay_ref_no = reg.getParlay_ref_no() != null ? reg.getParlay_ref_no().longValue() : 0;
		String bet_team = reg.getBet_team() != null ? reg.getBet_team() : "";
		Double hdp = 0D;
		String sport_type = reg.getSport_type() != null ? reg.getSport_type().toString() : "";
		Double away_hdp = reg.getAway_hdp() != null ? reg.getAway_hdp().doubleValue() : 0;
		Double home_hdp = reg.getHome_hdp() != null ? reg.getHome_hdp().doubleValue() : 0;
		Double odds = reg.getOdds() != null ? reg.getOdds().doubleValue() : 0;
		int away_score = reg.getAway_score() != null ? reg.getAway_score() : 0;
		int home_score = reg.getHome_score() != null ? reg.getHome_score() : 0;
		String is_live = reg.getIslive() != null ? reg.getIslive() : "";
		String is_lucky = reg.getIsLucky() != null ? reg.getIsLucky() : "";
		String parlay_type = reg.getParlay_ref_no() != null ? reg.getParlay_ref_no().toString() : "";
		String combo_type = "";
		Double stake = reg.getStake() != null ? reg.getStake().doubleValue() : 0d;
		String bet_tag = "";
		Double win_lose_amount = reg.getWinlost_amount() != null ? reg.getWinlost_amount().doubleValue() : 0;

		Timestamp win_lost_datetime = reg.getWinlost_datetime() != null ? new java.sql.Timestamp(reg.getWinlost_datetime().getTime()) : null;
		int version_key = reg.getVersion_key() != null ? reg.getVersion_key() : 0;
		String last_ball_no = "";
		String ticket_status = reg.getTicket_status() != null ? reg.getTicket_status() : "";
		int odds_type = reg.getOdds_type() != null ? reg.getOdds_type() : 0;

		Double actual_stake, refund_amount = 0d;
		if (odds < 0) {
			if (odds_type == 5) {
				actual_stake = stake * odds * (-1) / 100;
			} else {
				actual_stake = stake * odds * (-1);
			}
			
		} else {
			actual_stake = stake;
		}
		refund_amount = stake - actual_stake;
		String nickName = gameDao.findNickNameByGameUserId("ibcid", player_name, COLLECTION_IBC2USER);
		if (nickName == null || "".equals(nickName)) {
			LOGGER.error("[TASK_IBC_LOG] User is not exist , player_name=" + player_name);
			return false;
		}
		UserService userService = new UserServiceImpl();
		UserModel userModel = userService.getUserByNickName(nickName);
		MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> colRecord = db.getCollection(COLLECTION_GAMERECORD);
        //NOTE: Không đổi kiểu dữ lieeu jinssert vào mongo
        BasicDBObject doc = new BasicDBObject();
		doc.append("transid", trans_id);
		doc.append("playername", player_name);
		doc.append("transactiontime", transaction_time);
		doc.append("matchid", match_id);
		doc.append("leagueid", league_id);
		doc.append("leaguename",league_name );
		doc.append("awayid", away_id);
		doc.append("awayidname", away_id_name);
		doc.append("homeid", home_id);
		doc.append("homeidname", home_id_name);
		doc.append("matchdatetime", match_date_time);
		doc.append("bettype", bet_type);
		doc.append("parlayrefno", parlay_ref_no);
		doc.append("betteam", bet_team);
		doc.append("hdp", hdp);
		doc.append("sporttype",sport_type );
		doc.append("awayhdp", away_hdp);
		doc.append("homehdp", home_hdp);
		doc.append("odds", odds);
		doc.append("awayscore", away_score);
		doc.append("homescore", home_score);
		doc.append("islive", is_live);
		doc.append("islucky", is_lucky);
		doc.append("parlay_type", parlay_type);
		doc.append("combo_type", combo_type);
		doc.append("stake", stake.longValue() * GameThirdPartyInit.IBC2_RATE);
		doc.append("bettag", bet_tag);
		doc.append("winloseamount",win_lose_amount.longValue() * GameThirdPartyInit.IBC2_RATE);
		doc.append("winlostdatetime", win_lost_datetime);
		doc.append("versionkey", version_key);
		doc.append("lastballno", last_ball_no);
		doc.append("ticketstatus", ticket_status);
		doc.append("oddstype", odds_type);
		doc.append("actual_stake",  actual_stake.longValue() * GameThirdPartyInit.IBC2_RATE);
		doc.append("refund_amount", refund_amount.longValue() * GameThirdPartyInit.IBC2_RATE);
		doc.append("nick_name", nickName);

		try {
			MongoCollection<Document> colUser = db.getCollection(COLLECTION_GAMERECORD);
			Document conditions = new Document();
			conditions.put("transid", trans_id);
			FindIterable<?> iterable = colUser.find(conditions).limit(1);
			Document dataOld = iterable != null ? (Document)iterable.first() : null;
			if (dataOld !=null) {
				//update
				colRecord.updateOne(conditions, new Document("$set", doc));
				//push queue
				if(lstTicketStatus.contains(ticket_status)) {
					// check ticketstatus old
					String tStatus = dataOld.getString("ticketstatus");
					if(!lstTicketStatus.contains(tStatus)) {//truoc do la runnign,..
						if (actual_stake != 0) {
							
							LogMoneyUserMessage message = new LogMoneyUserMessage(0, nickName, "IBC2",
	                            Games.IBC2_GAMES.getId() + "", 0,
	                            -Math.abs(actual_stake.longValue() * GameThirdPartyInit.IBC2_RATE), "vin",
	                            "", 0, false, false);
							message.setReferralCode(userModel.getReferralCode());
	                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
	                    taskReportUser.start();
						}
						if (win_lose_amount >= 0) {
							
							LogMoneyUserMessage message = new LogMoneyUserMessage(0, nickName, "IBC2",
									Games.IBC2_GAMES.getId() + "", 0,
									Math.abs(win_lose_amount.longValue() * GameThirdPartyInit.IBC2_RATE), "vin",
									"", 0, false, false);
							message.setReferralCode(userModel.getReferralCode());
							RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
							taskReportUser.start();
						}
						
					}
//					else {
//						//get old value
//						//truoc do la win lose r
//						long old_actual_stake = dataOld.getLong("actual_stake");
//						long old_win_lose_amount = dataOld.getLong("win_lose_amount");
//						if(actual_stake > old_actual_stake) {
//							//update thêm số thừa
//							
//						}
//						if(win_lose_amount > old_win_lose_amount) {
//							//update thêm số thừa
//							
//						}
//					}
				}
				
				
			} else {
				// insert
				colRecord.insertOne(new Document(doc));
				//push queue
				if (lstTicketStatus.contains(ticket_status)) {
					if (actual_stake != 0) {
						LogMoneyUserMessage message = new LogMoneyUserMessage(0, nickName, "IBC2",
								Games.IBC2_GAMES.getId() + "", 0,
								-Math.abs(actual_stake.longValue() * GameThirdPartyInit.IBC2_RATE), "vin", "", 0, false,
								false);
						message.setReferralCode(userModel.getReferralCode());
						RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance",
								602);
						taskReportUser.start();
					}

					if (win_lose_amount > 0) {
						LogMoneyUserMessage message = new LogMoneyUserMessage(0, nickName, "IBC2",
								Games.IBC2_GAMES.getId() + "", 0,
								Math.abs(win_lose_amount.longValue() * GameThirdPartyInit.IBC2_RATE), "vin", "", 0,
								false, false);
						message.setReferralCode(userModel.getReferralCode());
						RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance",
								602);
						taskReportUser.start();
					}
				}

			}

			return true;
		} catch (Exception e) {
			LOGGER.error(e);
			return false;
		}

	}
	
	private BaseResponse<LogInRespDto> genNewUser(String nickName){
		Integer countRemain = this.countUserRemain(null);//nick_name is null
		if (countRemain != null && countRemain > 0) {
			//get min
			String minIbcid = findMinimumByColumn("ibccountid");//nick_name is ibccountid
			//update
			if(minIbcid !=null) {
				boolean isUpdate = this.updateUser(minIbcid, nickName);
				if (isUpdate) {
					//call API
					MemberIbc2Service memberIbcService =new MemberIbc2Service();
					LogInRespDto loginDto = memberIbcService.login(minIbcid);
					if(loginDto == null) {
						LOGGER.error("[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
						return new BaseResponse<LogInRespDto>(1, "[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
					}
					return new BaseResponse<LogInRespDto>(loginDto);
				}
			}
		}
		return null;
	}

	@Override
	public BaseResponse<LogInRespDto> loginIbc(String nickname) {
		Integer count = this.countUserRemain(nickname);
		if (count == null || count > 1) {
			return null;
		} else if (count == 0) {
			// not exist user -> gen new user
			return genNewUser(nickname);
		} else if (count == 1) {
			// exist 1 user -> get ibcid to call api
			//get ibcid
			String ibcid = findIbcIdUserByNickName(nickname);
			//call api
			if (ibcid != null && !"".equals(ibcid)) {
				MemberIbc2Service memberIbcService =new MemberIbc2Service();
				LogInRespDto loginDto = memberIbcService.login(ibcid);
				if(loginDto == null) {
					LOGGER.error("[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
					return new BaseResponse<LogInRespDto>(1, "[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
				}
				return new BaseResponse<LogInRespDto>(loginDto);
			}
		}
		return null;
	}

	public String findIbcIdUserByNickName(String nickName) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);

        Document conditions = new Document();
        conditions.put("nick_name", nickName);
        FindIterable<?> iterable = collection.find(conditions);
        Document doc = iterable != null ? (Document)iterable.first() : null;
		String ibcid = "";
		if (doc != null) {
			ibcid = doc.getString("ibcid");
		}
		return ibcid;
	}



	public String findMinimumByColumn(String field) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);
        Document conditions = new Document();
        conditions.put("nick_name", null);
        FindIterable<?> cursor = collection.find(conditions).sort(new BasicDBObject(field,1)).limit(1);
        Document doc = cursor != null ? (Document)cursor.first() : null;
		String ibcid = "";
		if (doc != null) {
			ibcid = doc.getString("ibcid");
		} else {
			return null;
		}
		return ibcid;
	}

	@Override
	public boolean updateUser(String ibcid, String nickName) {
		try {
			MongoDatabase db = MongoDBConnectionFactory.getDB();
			MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);

			Document conditions = new Document();
			conditions.put("ibcid", ibcid);
			BasicDBObject doc = new BasicDBObject();
			doc.append("nick_name", nickName);
			collection.updateOne(conditions, new Document("$set", doc));
			return true;
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return false;
	}

	@Override
	public BaseResponse<CheckBalanceDataRespDto> getBalance(String nickName) {
		if (nickName == null || "".equals(nickName)) {
			return new BaseResponse<CheckBalanceDataRespDto> (-1, "Nickname is null or empty");
		}
		String ibcid = findIbcIdUserByNickName(nickName);
		if (ibcid != null && !"".equals(ibcid)) {
			return CheckUserBalance(ibcid);
		}else {
			genNewUser(nickName);
			CheckBalanceDataRespDto balanceResponse = new CheckBalanceDataRespDto();
			balanceResponse.setBalance(0d);
			return new BaseResponse<CheckBalanceDataRespDto>(balanceResponse);
		}

	}

	private BaseResponse<CheckBalanceDataRespDto> CheckUserBalance(String playerName) {
		BaseResponse<CheckBalanceDataRespDto> rf = new BaseResponse<CheckBalanceDataRespDto>();
		try {
			Ibc2CheckUserBalanceReqDto reqDto = new Ibc2CheckUserBalanceReqDto();
			reqDto.setVendor_member_ids(playerName);
			reqDto.setWallet_id(1);
			// CALL API
			MemberIbc2Service service = new MemberIbc2Service();
			Ibc2CheckUserBalanceRespDto rsDto = service.CheckUserBalance(reqDto);
			if (rsDto != null) {
				if (rsDto.getError_code() != 0) {
					rf.setCode(1);
					if (rsDto.getError_code() != null
							&& (rsDto.getError_code() == 11000 || rsDto.getError_code() == 12999)) {
						rf.setMessage("Bảo trì hệ thống");
					} else {
						rf.setMessage("Thất bại: (Liên hệ CSKH)");
					}
					return rf;
				} else {
					CheckBalanceDataRespDto balanceResponse = rsDto.getData().get(0);
					if(balanceResponse.getBalance()==null) {
						balanceResponse.setBalance(0d);
					}
					rf.setCode(0);
					rf.setData(balanceResponse);
					return rf;
				}

			} else {
				rf.setCode(1);
				rf.setMessage("Thất bại: (Liên hệ CSKH)");
				return rf;
			}
		} catch (Exception e) {
			rf.setMessage(e.getMessage());
			rf.setCode(-1);
			return rf;
		}
	}

	public String checkExist(String nickname) {
		Integer count = this.countUserRemain(nickname);
		if (count == null || count > 1) {
			return null;
		} else if (count == 0) {
			// not exist user -> gen new user
			Integer countRemain = this.countUserRemain(null);//nick_name is null
			if (countRemain != null && countRemain > 0) {
				//get min
				String minIbcid = findMinimumByColumn("ibccountid");//nick_name is ibccountid
				//update
				if(minIbcid !=null) {
					boolean isUpdate = this.updateUser(minIbcid, nickname);
					if (isUpdate) {
						//call API
						MemberIbc2Service memberIbcService =new MemberIbc2Service();
						LogInRespDto loginDto = memberIbcService.login(minIbcid);
						if(loginDto == null) {
							LOGGER.error("[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
							return null;
						}
						return minIbcid;
					}
				}
			}
		} else if (count == 1) {
			// exist 1 user -> get ibcid to call api
			//get ibcid
			String ibcid = findIbcIdUserByNickName(nickname);
			//call api
			if (ibcid != null && !"".equals(ibcid)) {
				MemberIbc2Service memberIbcService =new MemberIbc2Service();
				LogInRespDto loginDto = memberIbcService.login(ibcid);
				if(loginDto == null) {
					LOGGER.error("[UILoginIbc]: Không connect được tới IBC2: loginDto = " + loginDto);
					return null;
				}
				return ibcid;
			}
		}
		return null;
	}

	@Override
	public BaseResponse<Double> transfer(String nickName, Integer direction, Double amount, String ip) {
		String playerName = "";
		BaseResponse<Double> rf = new BaseResponse<Double>();

		if (!CommonMethod.ValidateRequest(nickName)) {
			LOGGER.info("IbcSportSerice playerName : " + nickName + "Trong 10s chỉ thực hiện request 1 lần ");
			return new BaseResponse<Double>(1,"Xin chờ 10 giây để thực hiện giao dich tiếp theo ");
		}
		// validation amount
		if (amount == null || amount <= 0) {
			LOGGER.error("IbcSportSerice playerName : " + playerName + "Giao dịch thất bại , amount = " + amount);
			return new BaseResponse<Double>(2,"Giao dịch thất bại ");
		}
		if (amount < MIN_TRANSFER) {
			return new BaseResponse<Double>(2, "Số tiền chuyển khoản tối thiểu là 100.000 VNĐ");
		}
		if (amount > MAX_TRANSFER) {
			return new BaseResponse<Double>(2, "Số tiền chuyển khoản tối đa là 100.000.000 VNĐ");
		}
		LOGGER.info("IbcSportSerice playerName : " + nickName + " : " + playerName + " start  FundTransfer ");
		//get Wid
        String wid = CommonMethod.GetCurDate("yyMMddHHmmss") + InitData.getids();
		try {
			// get playerName
			BaseResponse<CheckBalanceDataRespDto> ibcUserBalance = getBalance(nickName);
			if (ibcUserBalance != null && ibcUserBalance.getCode() == 0) {
				CheckBalanceDataRespDto ibcUser= ibcUserBalance.getData();
				if(ibcUser== null) {
					return new BaseResponse<Double>(2, "Hệ thống đang bảo trì");
				}
				playerName = ibcUser.getVendor_member_id();
				Double ibcBalance = ibcUser.getBalance();
				if (direction == 1) {
					//check format money
					if (amount % 1000 != 0) {
						return new BaseResponse<Double>(2, "Quý khách vui lòng nhập số tiền chẵn (là hệ số của 1000 vnđ) ");
					}
					// deposit
					return deposit(playerName, nickName, wid, amount, direction, ip);
				} else if (direction == 0) {
					//check balance amount
					if(ibcBalance * 1000 < amount) {
						return new BaseResponse<Double>(6, "Số dư của quý khách không đủ");
					}
					// withdraw
					return withdraw(playerName, nickName, wid, amount, direction, ip);
				} else {
					rf.setCode(1);
					rf.setMessage("Transaction type is incorrect!");
					LOGGER.error("IbcSportSerice playerName : " + playerName + " msg : Transaction type is incorrect!");
					return rf;
				}

			} else {
				LOGGER.error("IbcSportSerice playerName : " + playerName + " Can not get Sport ID : is null");
				return new BaseResponse<>(3, "Can not connect Sport ID!");
			}
		} catch (Exception ex) {
			rf.setCode(1);
			rf.setMessage(ex.getMessage());
			LOGGER.error("IbcSportSerice playerName :" + playerName, ex);
			return rf;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BaseResponse<Double> withdrawAll() {
		BaseResponse rf =new BaseResponse(0,"SUCCESS",null);
		try {
			MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
	        MongoCollection<Document> collection = db.getCollection(COLLECTION_IBC2USER);
	        
	        Document conditions = new Document();
	        BasicDBObject obj = new BasicDBObject();
            obj.put("$ne", null);
            conditions.put("nick_name", obj);
	        
	        FindIterable<?> iterable = collection.find(conditions);
	        iterable.forEach((Block)new Block<Document>(){

	            public void apply(Document doc) {
					// get Wid
					String wid = CommonMethod.GetCurDate("yyMMddHHmmss") + InitData.getids();
					String ibcid = doc.getString("ibcid");
					String nickName = doc.getString("nick_name");
					BaseResponse<CheckBalanceDataRespDto> uBalance = CheckUserBalance(ibcid);
					if (uBalance != null && uBalance.getCode() == 0) {
						Double balance = uBalance.getData().getBalance();
						if (balance != null && balance.longValue() > 0) {
							try {
								BaseResponse<Double> re = withdraw(ibcid, nickName, wid, balance * 1000, 0, "");
								if (re != null && re.getCode() == 0) {
									// success
									LOGGER.info("success_transfer nickName=" + nickName + " , ibcid=" + ibcid + ",uBalance="
											+ uBalance.toJson());
								} else {
									LOGGER.error("loi_transfer nickName=" + nickName + " , ibcid=" + ibcid + ",uBalance="
											+ uBalance.toJson());
								}
							} catch (Exception e) {
								LOGGER.error("loi_transfer nickName=" + nickName + " , ibcid=" + ibcid + ",uBalance="
										+ uBalance.toJson());
							}
						}else {
							LOGGER.error("loi_transfer nickName=" + nickName + " , ibcid=" + ibcid + ",uBalance="
									+ uBalance.toJson());
						}
						
					} else {
						LOGGER.error("loi_transfer nickName=" + nickName + " , ibcid=" + ibcid + ",uBalance="
								+ uBalance.toJson());
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
			
		} catch (Exception ex) {
			rf.setCode(1);
			rf.setMessage(ex.getMessage());
			LOGGER.error(ex+"");
			return rf;
		}
		return rf;
	}



	 private static BaseResponse<Double> deposit(String playerName ,String nickName ,String wid, Double amount ,Integer direction ,String ip) throws Exception{
		 BaseResponse<Double> rf = new BaseResponse<Double>();
			//update balance database
			//get current balance
			MoneyInGameService moneyService = new MoneyInGameServiceImpl();
			//check current balance
			UserService userService = new UserServiceImpl();
			UserModel u = userService.getUserByNickName(nickName);
			if (u.getVin() < amount.longValue()) {
				rf.setData(null);
                rf.setCode(-1);
                rf.setMessage("Không đủ số dư");
				return rf;
			}
			MoneyResponse moneyResponse = moneyService.updateMoneyGame3rdUser(nickName, amount.longValue() * (-1),
					"vin", "ibc2", "IBC2_DEPOSIT", "NẠP TIỀN IBC2", 0, false);
			if(moneyResponse.getErrorCode().equals("0")){
	        	//call API
	        	Ibc2FundTransferRespDto resDto = transferMoney(playerName, wid, amount/1000, direction, 1);
	            if (resDto.getData().getStatus() == 0) {
	                if (resDto.getError_code() == 0) {
	                	//success
	                    LOGGER.info("IbcSportSerice playerName : "+nickName+" : "+playerName+" finish  FundTransfer ");
	                    rf.setData(resDto.getData().getAfter_amount());
	                    rf.setCode(0);
	                    return rf;
	                } else {
	                    LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	                    return new BaseResponse<Double>(5,"Transaction failed, Please try again later!");
	                }
	            } else if (resDto.getData().getStatus() == 2) {
	            	//pedding
	                 LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	                 return new BaseResponse<Double>(6,"Transaction pending, Please contact customer service!");
	            }else if (resDto.getData().getStatus() == 4) {
	            	//pedding
	                 LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	                 return new BaseResponse<Double>(4,"Số tiền chuyển khoản vượt quá giới hạn. (CK tối thiểu = 100.0000, CK tối đa = 1000000.0000)");
	            }else{
	            	//error
	            	 LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	                 return new BaseResponse<Double>(7,"Transaction failed, Please try again later!");
	            }
	        }else{
	        	return new BaseResponse<Double>(8,"Update account unsuccessful !");
	        }
		}

		private static BaseResponse<Double> withdraw(String playerName ,String nickName ,String wid, Double amount ,Integer direction ,String ip) throws Exception{
			BaseResponse<Double> rf = new BaseResponse<Double>();
			//call GA
			Ibc2FundTransferRespDto resDto = transferMoney(playerName, wid, amount/1000, direction, 1);
			if (resDto != null && resDto.getData() != null && resDto.getData().getStatus() == 0) {
	            if (resDto.getError_code() == 0) {
	            	MoneyInGameService moneyService = new MoneyInGameServiceImpl();
	            	//update database
	    			MoneyResponse moneyResponse = moneyService.updateMoneyGame3rdUser(nickName, amount.longValue(),
	    					"vin", "ibc2", "IBC2_WITHDRAW", "RÚT TIỀN IBC2", 0, false);
					if ("0".equals(moneyResponse.getErrorCode())) {
	                	//success
	                    LOGGER.info("IbcSportSerice playerName : "+nickName+" : "+playerName+" finish  FundTransfer ");
	                    rf.setData(resDto.getData().getAfter_amount());
	                    rf.setCode(0);
	                    return rf;
	                }else {
	                    LOGGER.info("IbcSportSerice playerName : "+nickName+" : "+playerName+" finish  FundTransfer but can not update ekhelo balance");
	                    rf.setData(resDto.getData().getAfter_amount());
	                    rf.setMessage("Finish  FundTransfer but can not save your ekhelo balance, Please contact customer service!");
	                    rf.setCode(1);
	                    return rf;
	                }
	            } else {
	                LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	                return new BaseResponse<Double>(9,"Transaction failed, Please try again later!");
	            }
	        } else if (resDto.getData().getStatus() == 2) { // res : pedding
	            LOGGER.info("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	            return new BaseResponse<Double>(10,"Transaction pending, Please contact customer service!");
	        }else{
	            LOGGER.error("IbcSportSerice playerName : "+playerName+" Error_code : "+resDto.getError_code()+" msg :"+resDto.getMessage());
	            return new BaseResponse<Double>(11,"Transaction pending, Please contact customer service!");
	        }
		}
	    private static Ibc2FundTransferRespDto transferMoney(String vender_member_id, String transId, Double amount,Integer direction, Integer walletId ) {
	    	FundTransferIbc2Service service =new FundTransferIbc2Service();
	        Ibc2FundTransferReqDto reqDto = new Ibc2FundTransferReqDto();
	        reqDto.setVendor_member_id(vender_member_id);
	        reqDto.setDirection(direction);
	        reqDto.setAmount(amount);
			reqDto.setVendor_trans_id(GameThirdPartyInit.IBC2_PLAYER_PREFIX + transId);
	        reqDto.setWallet_id(walletId);
	        reqDto.setCurrency(51);
	        Ibc2FundTransferRespDto resDto = service.FundTransfer(reqDto);
	        return resDto;
	    }

}
