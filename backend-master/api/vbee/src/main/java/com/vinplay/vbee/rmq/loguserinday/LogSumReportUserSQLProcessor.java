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
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class LogSumReportUserSQLProcessor implements BaseProcessor<byte[], Boolean> {
    private static final org.apache.log4j.Logger logger = Logger.getLogger("vbee");

    /** Bật log chi tiết mỗi message (rất nhiều dòng). JVM: -Dlog.report.user.sql.debug=true */
    private static final boolean DEBUG_EACH_MESSAGE = true;

    private static final String P = "[LogSumReportUserSQL] ";

    public static final Object lock = new Object();

    public Boolean execute(Param<byte[]> param) {
        LogMoneyUserMessage message = (LogMoneyUserMessage) BaseMessage.fromBytes(param.get());
        if (message.isBot()) {
            if (DEBUG_EACH_MESSAGE) {
                logger.debug(P + "SKIP bot nick=" + message.getNickname());
            }
            return true;
        }

        if (Consts.ADMIN.equals(message.getServiceName())) {
            if (DEBUG_EACH_MESSAGE) {
                logger.debug(P + "SKIP ADMIN serviceName nick=" + message.getNickname());
            }
            return true;
        }
        try {
            this.getDataUserCache(message);
        } catch (Exception e) {
            logger.error(P + "getDataUserCache exception nick=" + message.getNickname()
                    + " serviceName=" + message.getServiceName() + " action=" + message.getActionName(), e);
        }
        return true;
    }

    public void getDataUserCache(LogMoneyUserMessage message) {
        String time = VinPlayUtils.getCurrentDateMarketing();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            logger.warn(P + "Hazelcast client NULL -> không ghi log_report_user. nick=" + message.getNickname()
                    + " time=" + time);
            return;
        }
        IMap<String, LogReportModel> dataMap = client.getMap("cacheUserSumLog");
        IMap<String, LogCountUserPlay> mapLogCountUserPlay = client.getMap("cacheUserCountLog");
        
        String nickName = message.getNickname();
        final long amount =  message.getMoneyExchange();
        String key = nickName + "|_|" + time;
        //  logger.info("logsum report user -1");
        boolean writeNewData = false;
        boolean writeNewDataCount = false;
        if (!dataMap.containsKey(key)) {
            // lay data ra, neu data = null tao data moi roi cho vao cache
            synchronized (lock) {
                if (!dataMap.containsKey(key) || dataMap.get(key) == null) {
                    LogReportModel logReportModel = LogReportUtil.getLogReportModelSQL(nickName, time);
                    if (logReportModel.id == -1) {// chua co trong database
                        logReportModel.nick_name = nickName;
                        logReportModel.time = time;
                        writeNewData = true;
                        logger.info(P + "Cache miss + chưa có DB row -> sẽ INSERT khi flush. key=" + key);
                    } else if (DEBUG_EACH_MESSAGE) {
                        logger.debug(P + "Cache miss nhưng đã có DB id=" + logReportModel.id + " key=" + key);
                    }
                    dataMap.put(key, logReportModel, 86400, TimeUnit.SECONDS);
                }
            }
        }

        if (!mapLogCountUserPlay.containsKey(key)) {
            synchronized (lock) {
                if (!mapLogCountUserPlay.containsKey(key)) {
                    LogCountUserPlay logCountUserPlay = LogCountReportUtil.getLogReportModelSQL(nickName, time);
                    if (logCountUserPlay.id == -1) {
                        logCountUserPlay.nick_name = nickName;
                        logCountUserPlay.time = time;
                        writeNewDataCount = true;
                        logger.info(P + "log_count_user_play: chưa có DB -> sẽ INSERT. key=" + key);
                    }
                    mapLogCountUserPlay.put(key, logCountUserPlay);
                }
            }
        }
        
        try {
            dataMap.lock(key);
            LogReportModel logReportModel = dataMap.get(key);
            LogCountUserPlay logCountUserPlay = mapLogCountUserPlay.get(key);

            if (logReportModel == null) {
                logger.error(P + "logReportModel NULL sau lock, bỏ qua. key=" + key);
                return;
            }
            if (logCountUserPlay == null) {
                logger.error(P + "logCountUserPlay NULL (cacheUserCountLog thiếu key), bỏ qua DB ghi. key=" + key
                        + " nick=" + nickName);
                return;
            }

            if (Consts.REAL_DEPOSIT_VIN.contains(message.getActionName())) {
                if (amount > 0L) {
                    logReportModel.deposit += Math.abs(amount);
                    logCountUserPlay.deposit++;
                }
            } else if (Consts.REAL_WITHDRAW_VIN.contains(message.getActionName())) {
            	if (Consts.REQUEST_CASHOUT.equalsIgnoreCase(message.getActionName())) {
					logReportModel.withdraw += Math.abs(amount);
					logCountUserPlay.withdraw++;
            	}else {
					logReportModel.withdraw -= Math.abs(amount);
					logCountUserPlay.withdraw--;
				}
                
            } else {
				int serviceID = -1;
				try {
					serviceID = Integer.parseInt(message.getServiceName());
				} catch (NumberFormatException e) {
					logger.warn(P + "serviceName không parse được int -> return, KHÔNG cộng game. nick=" + nickName
							+ " serviceName=\"" + message.getServiceName() + "\" action=" + message.getActionName()
							+ " amount=" + amount);
					return;
				}
                if (serviceID == Games.TAI_XIU.getId()) {
                    if (amount < 0) {
                        logReportModel.taixiu += Math.abs(amount);
                        logCountUserPlay.taixiu++;
                    } else {
                        // Hoàn tiền cân cửa: actionName = TaiXiuHoanTien (serviceName vẫn là game id)
                        if (message.getActionName() != null
                                && message.getActionName().equalsIgnoreCase("TaiXiuHoanTien")) {
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
                } else if (serviceID == Games.GIFT_CODE.getId()) {
                    // cong tien khuyen mai
                	if (amount > 0) {
                        logReportModel.totalBonus += amount;
                    }
                	
                } else if (serviceID == Games.MOON_NIGHT.getId()) {
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
                }else if (serviceID == Games.CHIEM_TINH.getId()) { // chiem tinh
                    if (amount < 0) {
                        logReportModel.slot_chiemtinh += Math.abs(amount);
                        logCountUserPlay.slot_chiemtinh++;
                    } else {
                        logReportModel.slot_chiemtinh_win += Math.abs(amount);
                    }
                }else if (serviceID == Games.TAI_XIU_ST.getId()) { // chiem tinh
                    if (amount < 0) {
                        logReportModel.taixiu_st += Math.abs(amount);
                        logCountUserPlay.taixiu_st++;
                    } else {
                        logReportModel.taixiu_st_win += Math.abs(amount);
                    }
                }else if (serviceID == Games.SHOT_FISH.getId()) { // ban ca
                    if (amount < 0) {
                        logReportModel.fish += Math.abs(amount);
                        logCountUserPlay.fish++;
                    } else {
                        logReportModel.fish_win += Math.abs(amount);
					}
				} else if (serviceID == Games.ROLL_ROYE.getId()) { // than bai
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
                } else if (serviceID == Games.EBET_GAMES.getId()) { // than bai
					if (amount < 0) {
						logReportModel.ebet += Math.abs(amount);
						logCountUserPlay.ebet++;
					} else {
						logReportModel.ebet_win += Math.abs(amount);
					}
				} else if (serviceID == Games.SBO_GAMES.getId()) { // than bai
					if (amount < 0) {
						logReportModel.sbo += Math.abs(amount);
						logCountUserPlay.sbo++;
					} else {
						logReportModel.sbo_win += Math.abs(amount);
					}
				} else if (serviceID == Games.DIEM_DANH.getId()) {
					logReportModel.attendance = Math.abs(amount);
					logCountUserPlay.attendance++;
				} else if (serviceID == Games.SAM.getId()) {
					if (amount < 0) {
						logReportModel.sam += Math.abs(amount);
						logCountUserPlay.sam++;
					} else {
						logReportModel.sam_win += Math.abs(amount);
					}
				} else if (serviceID == Games.BINH.getId()) {
					if (amount < 0) {
						logReportModel.binh += Math.abs(amount);
						logCountUserPlay.binh++;
					} else {
						logReportModel.binh_win += Math.abs(amount);
					}
				} else if (serviceID == Games.TA_LA.getId()) {
					if (amount < 0) {
						logReportModel.tala += Math.abs(amount);
						logCountUserPlay.tala++;
					} else {
						logReportModel.tala_win += Math.abs(amount);
					}
				} else if (serviceID == Games.LIENG.getId()) {
					if (amount < 0) {
						logReportModel.lieng += Math.abs(amount);
						logCountUserPlay.lieng++;
					} else {
						logReportModel.lieng_win += Math.abs(amount);
					}
				} else if (serviceID == Games.XI_TO.getId()) {
					if (amount < 0) {
						logReportModel.xito += Math.abs(amount);
						logCountUserPlay.xito++;
					} else {
						logReportModel.xito_win += Math.abs(amount);
					}
				} else if (serviceID == Games.BAI_CAO.getId()) {
					if (amount < 0) {
						logReportModel.baicao += Math.abs(amount);
						logCountUserPlay.baicao++;
					} else {
						logReportModel.baicao_win += Math.abs(amount);
					}
				} else if (serviceID == Games.POKER.getId()) {
					if (amount < 0) {
						logReportModel.poker += Math.abs(amount);
						logCountUserPlay.poker++;
					} else {
						logReportModel.poker_win += Math.abs(amount);
					}
				} else if (serviceID == Games.XI_DZACH.getId()) {
					if (amount < 0) {
						logReportModel.xidzach += Math.abs(amount);
						logCountUserPlay.xidzach++;
					} else {
						logReportModel.xidzach_win += Math.abs(amount);
					}
				} else if (serviceID == Games.HAM_CA_MAP.getId()) {
					if (amount < 0) {
						logReportModel.hamcamap += Math.abs(amount);
						logCountUserPlay.hamcamap++;
					} else {
						logReportModel.hamcamap_win += Math.abs(amount);
					}
                } else if (serviceID == Games.TAI_XIU_SICBO.getId()) {
					if (amount < 0) {
						logReportModel.taixiu_sicbo += Math.abs(amount);
						logCountUserPlay.taixiu_sicbo++;
					} else {
						if (message.getActionName() != null
								&& message.getActionName().equalsIgnoreCase("TaiXiuHoanTien")) {
							logReportModel.taixiu_sicbo -= Math.abs(amount);
						} else {
							logReportModel.taixiu_sicbo_win += Math.abs(amount);
						}
					}
				} else if (serviceID == Games.OVER_UNDER.getId()) {
					if (amount < 0) {
						logReportModel.over_under += Math.abs(amount);
						logCountUserPlay.over_under++;
					} else {
						logReportModel.over_under_win += Math.abs(amount);
					}
				} else if (serviceID == Games.SAMTRUYEN.getId()) {
					if (amount < 0) {
						logReportModel.samtruyen += Math.abs(amount);
						logCountUserPlay.samtruyen++;
					} else {
						logReportModel.samtruyen_win += Math.abs(amount);
					}
				} else if (serviceID == Games.RANGE_ROVER.getId()) {
					if (amount < 0) {
						logReportModel.range_rover += Math.abs(amount);
						logCountUserPlay.range_rover++;
					} else {
						logReportModel.range_rover_win += Math.abs(amount);
					}
				} else if (serviceID == Games.SEXYGIRL.getId()) {
					if (amount < 0) {
						logReportModel.sexygirl += Math.abs(amount);
						logCountUserPlay.sexygirl++;
					} else {
						logReportModel.sexygirl_win += Math.abs(amount);
					}
				} else if (serviceID == Games.LODE.getId()) {
					if (amount < 0) {
						logReportModel.lode += Math.abs(amount);
						logCountUserPlay.lode++;
					} else {
						logReportModel.lode_win += Math.abs(amount);
					}
				} else {
					logger.warn(P + "serviceID chưa map cột log_report_user: " + serviceID
							+ " nick=" + nickName + " action=" + message.getActionName());
				}
            }

            dataMap.put(key, logReportModel, 86400, TimeUnit.SECONDS);
            mapLogCountUserPlay.put(key, logCountUserPlay, 86400, TimeUnit.SECONDS);
            
			if (logReportModel.code == null || "".equals(logReportModel.code)) {
				// set refferalCode
				IMap<String, UserModel> userCache = client.getMap("users");
				String referralCode = "";
				if (userCache != null && userCache.containsKey(nickName)) {
					UserModel user = userCache.get(nickName);
					referralCode = user.getReferralCode();
				}
				logReportModel.code = referralCode;
			}
            
            if (writeNewData) {
                boolean inserted = LogReportUtil.insertNewLogSQL(logReportModel);
                if (inserted) {
                    logger.info(P + "INSERT log_report_user OK nick=" + nickName + " time=" + time + " key=" + key);
                } else {
                    logger.error(P + "INSERT log_report_user FAILED (insertNewLogSQL=false). nick=" + nickName
                            + " time=" + time + " -> kiểm tra SQL/cột DB, xem log vbee từ LogReportUtil");
                }
            } else {
                boolean updated = LogReportUtil.updateLogSQL(logReportModel);
                if (updated) {
                    if (DEBUG_EACH_MESSAGE) {
                        logger.debug(P + "UPDATE log_report_user OK nick=" + nickName + " time=" + time);
                    }
                } else {
                    boolean inserted = LogReportUtil.insertNewLogSQL(logReportModel);
                    if (inserted) {
                        logger.warn(P + "UPDATE miss -> INSERT log_report_user OK nick=" + nickName + " time=" + time);
                    } else {
                        logger.error(P + "UPDATE miss và INSERT cũng FAIL nick=" + nickName + " time=" + time
                                + " id=" + logReportModel.id);
                    }
                }
            }

            if (writeNewDataCount) {
                boolean insertedC = LogCountReportUtil.insertNewLogSQL(logCountUserPlay);
                if (!insertedC) {
                    logger.error(P + "INSERT log_count_user_play FAILED nick=" + nickName + " time=" + time);
                }
            } else {
                boolean updatedC = LogCountReportUtil.updateLogSQL(logCountUserPlay);
                if (!updatedC) {
                    boolean insertedC = LogCountReportUtil.insertNewLogSQL(logCountUserPlay);
                    if (!insertedC) {
                        logger.error(P + "UPDATE+INSERT log_count_user_play đều FAIL nick=" + nickName + " time=" + time);
                    }
                }
            }

            if (DEBUG_EACH_MESSAGE) {
                logger.debug(P + "done nick=" + nickName + " svc=" + message.getServiceName()
                        + " action=" + message.getActionName() + " amt=" + amount
                        + " writeNew=" + writeNewData + "/" + writeNewDataCount);
            }
        } catch (Exception e) {
            logger.error(P + "exception khi flush DB nick=" + nickName + " key=" + key, e);
        } finally {
            dataMap.unlock(key);
        }

    }

}
