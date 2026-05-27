package com.vinplay.vbee.rmq.loguserinday;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.entities.report.LogCountUserPlay;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class LogReportDailyUserProcessor
        implements BaseProcessor<byte[], Boolean> {
    private static final org.apache.log4j.Logger logger = Logger.getLogger("vbee");


    public Boolean execute(Param<byte[]> param) {
        LogMoneyUserMessage message = (LogMoneyUserMessage) BaseMessage.fromBytes(param.get());
        if (!message.isBot()) {
            try {
                this.getDataUserCache(message);
            } catch (Exception e) {
                logger.info(e);
            }
        }
        return true;
    }
    
    
    public void getDataUserCache(LogMoneyUserMessage message) {
        String time = VinPlayUtils.getCurrentDateMarketing();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            return;
        }
        IMap<String, LogReportModel> dataMap = client.getMap("cacheUserSumLog");
        IMap<String, LogCountUserPlay> mapLogCountUserPlay = client.getMap("cacheUserCountLog");
        
        String key = message.getNickname() + "|_|" + time;
		boolean writeNewData = false;
		boolean writeNewDataCount = false;
		
        if (dataMap.containsKey(key)) {
        	//exist -> update
        	writeNewData = false;
        }else {
			//not exits -> insert
        	writeNewData =true;
		}
        if (mapLogCountUserPlay.containsKey(key)) {
        	//exist -> update
        	writeNewDataCount = false;
        }else {
			//not exits -> insert
        	writeNewDataCount =true;
		}
        try {
        	LogReportModel logReportModel = null;
        	LogCountUserPlay logCountUserPlay = null;
            dataMap.lock(key);
			if (writeNewData) {
				logReportModel = new LogReportModel();
			} else {
				logReportModel = dataMap.get(key);
			}
			
			mapLogCountUserPlay.lock(key);
			if (writeNewDataCount) {
				logCountUserPlay = new LogCountUserPlay();
			} else {
				logCountUserPlay = mapLogCountUserPlay.get(key);
			}
            
            String actionName = message.getActionName();
            long amount =  message.getMoneyExchange();
            
            if (Consts.REAL_DEPOSIT_VIN.contains(actionName)) {
                if (amount > 0L) {
                    logReportModel.deposit += Math.abs(amount);
                    logCountUserPlay.deposit++;
                }
            } else if (Consts.REAL_WITHDRAW_VIN.contains(actionName)) {
            	if (Consts.REQUEST_CASHOUT.equalsIgnoreCase(actionName)) {
					logReportModel.withdraw += Math.abs(amount);
					logCountUserPlay.withdraw++;
            	}else {
					logReportModel.withdraw -= Math.abs(amount);
					logCountUserPlay.withdraw--;
				}
                
            } else {
                int serviceID = Integer.parseInt(message.getServiceName());
                if (serviceID == Games.TAI_XIU.getId()) {
                    if (amount < 0) {
                        logReportModel.taixiu += Math.abs(amount);
                        logCountUserPlay.taixiu++;
                    } else {
                        if (actionName != null && actionName.equalsIgnoreCase("TaiXiuHoanTien")) {
                            logReportModel.taixiu -= Math.abs(amount);
                        } else {
                            logReportModel.taixiu_win += Math.abs(amount);
                        }
                    }
                } else if (serviceID == Games.BAU_CUA.getId()) {
                    if (amount < 0) {
                        logReportModel.baucua += Math.abs(amount);
                        logCountUserPlay.baucua++;
                    } else {
                        logReportModel.baucua_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.TLMN.getId()) {
                    if (amount < 0) {
                        logReportModel.tlmn += Math.abs(amount);
                        logCountUserPlay.tlmn++;
                    } else {
                        logReportModel.tlmn_win += Math.abs(amount);
                        logCountUserPlay.tlmn++;
                    }
                } else if (serviceID == Games.BA_CAY.getId()) {
                    if (amount < 0) {
                        logReportModel.bacay += Math.abs(amount);
                        logCountUserPlay.bacay++;
                    } else {
                        logReportModel.bacay_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.XOC_DIA.getId()) {
                    if (amount < 0) {
                        logReportModel.xocdia += Math.abs(amount);
                        logCountUserPlay.xocdia++;
                    } else {
                        logReportModel.xocdia_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.MINI_POKER.getId()) {
                    if (amount < 0) {
                        logReportModel.minipoker += Math.abs(amount);
                        logCountUserPlay.minipoker++;
                    } else {
                        logReportModel.minipoker_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.POKE_GO.getId()) { // pokemon
                    if (amount < 0) {
                        logReportModel.slot_pokemon += Math.abs(amount);
                        logCountUserPlay.slot_pokemon++;
                    } else {
                        logReportModel.slot_pokemon_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.GALAXY.getId()) { // galaxy
                    if (amount < 0) {
                        logReportModel.slot_galaxy += Math.abs(amount);
                        logCountUserPlay.slot_galaxy++;
                    } else {
                        logReportModel.slot_galaxy_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.CAO_THAP.getId()) {
                    if (amount < 0) {
                        logReportModel.caothap += Math.abs(amount);
                        logCountUserPlay.caothap++;
                    } else {
                        logReportModel.caothap_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.BENLEY.getId()) { // bitcoin
                    if (amount < 0) {
                        logReportModel.slot_bitcoin += Math.abs(amount);
                        logCountUserPlay.slot_bitcoin++;
                    } else {
                        logReportModel.slot_bitcoin_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.ROLL_ROYE.getId()) { // thanbai
                    if (amount < 0) {
                        logReportModel.slot_thanbai += Math.abs(amount);
                        logCountUserPlay.slot_thanbai++;
                    } else {
                        logReportModel.slot_thanbai_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.BIKINI.getId()) { // bikini
                    if (amount < 0) {
                        logReportModel.slot_bikini += Math.abs(amount);
                        logCountUserPlay.slot_bikini++;
                    } else {
                        logReportModel.slot_bikini_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.AUDITION.getId()) { // taydu
                    if (amount < 0) {
                        logReportModel.slot_taydu += Math.abs(amount);
                        logCountUserPlay.slot_taydu++;
                    } else {
                        logReportModel.slot_taydu_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.TAMHUNG.getId()) { // angrybird
                    if (amount < 0) {
                        logReportModel.slot_angrybird += Math.abs(amount);
                        logCountUserPlay.slot_angrybird++;
                    } else {
                        logReportModel.slot_angrybird_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.SPARTAN.getId()) { // than tai
                    if (amount < 0) {
                        logReportModel.slot_thantai += Math.abs(amount);
                        logCountUserPlay.slot_thantai++;
                    } else {
                        logReportModel.slot_thantai_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.MAYBACH.getId()) { // the thao
                    if (amount < 0) {
                        logReportModel.slot_thethao += Math.abs(amount);
                        logCountUserPlay.slot_thethao++;
                    } else {
                        logReportModel.slot_thethao_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.AG_GAMES.getId()) {
                    // game ag
                    if (amount < 0) {
                        logReportModel.ag += Math.abs(amount);
                        logCountUserPlay.ag++;
                    } else {
                        logReportModel.ag_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.WM_GAMES.getId()) {
                    // game wm
                    if (amount < 0) {
                        logReportModel.wm += Math.abs(amount);
                        logCountUserPlay.wm++;
                    } else {
                        logReportModel.wm_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.IBC2_GAMES.getId()) {
                    // game ibc
                    if (amount < 0) {
                        logReportModel.ibc += Math.abs(amount);
                        logCountUserPlay.ibc++;
                    } else {
                        logReportModel.ibc_win += Math.abs(amount);
                    }
                } else if (serviceID == Games.CMD_GAMES.getId()) {
                    // game cmd

                    if (amount < 0) {
                        logReportModel.cmd += Math.abs(amount);
                        logCountUserPlay.cmd++;
                    } else {
                        logReportModel.cmd_win += Math.abs(amount);
                    }
                }
                else if (serviceID == Games.GIFT_CODE.getId()) {
                    // cong tien khuyen mai
                	if (amount > 0) {
                        logReportModel.totalBonus += amount;
                    }
                	
                } else if (serviceID == Games.HOAN_TRA.getId()) {
                    // cong tien hoàn tiền chiết khấu
                	 if (amount > 0) {
                         logReportModel.totalRefund += amount;
                     }
                } else if (serviceID == Games.VERIFY_PHONE.getId()) {
                    // cong tien khuyen mai
                } else if (serviceID == Games.CHIEM_TINH.getId()) { // chiem tinh
                    if (amount < 0) {
                        logReportModel.slot_chiemtinh += Math.abs(amount);
                        logCountUserPlay.slot_chiemtinh++;
                    } else {
                        logReportModel.slot_chiemtinh_win += Math.abs(amount);
                    }
                }
            }

			dataMap.put(key, logReportModel, 86400, TimeUnit.SECONDS);
			if (writeNewData) { // tao row moi
				LogReportUtil.insertNewLogSQL(logReportModel);
			} else {
				boolean update = LogReportUtil.updateLogSQL(logReportModel);
				if (!update) {
					LogReportUtil.insertNewLogSQL(logReportModel);
				}
			}
			
			mapLogCountUserPlay.put(key, logCountUserPlay, 86400, TimeUnit.SECONDS);
			if (writeNewDataCount) { // tao row moi
				LogCountReportUtil.insertNewLogSQL(logCountUserPlay);
			} else {
				boolean update1 = LogCountReportUtil.updateLogSQL(logCountUserPlay);
				if (!update1) {
					LogCountReportUtil.insertNewLogSQL(logCountUserPlay);
				}
			}
			
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            dataMap.unlock(key);
            mapLogCountUserPlay.unlock(key);
        }

    }

}
