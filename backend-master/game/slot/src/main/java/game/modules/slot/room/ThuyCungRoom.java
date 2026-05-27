package game.modules.slot.room;

import game.modules.slot.utils.SlotHouseEdge;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.slot.ThuyCungModule;
import game.modules.slot.cmd.send.thuycung.ThuyCungFreeDailyMsg;
import game.modules.slot.cmd.send.thuycung.ThuyCungTotalFreeSpin;
import game.modules.slot.cmd.send.thuycung.BigWinThuyCungMsg;
import game.modules.slot.cmd.send.thuycung.ForceStopAutoPlayThuyCungMsg;
import game.modules.slot.cmd.send.thuycung.MinimizeResultThuyCungMsg;
import game.modules.slot.cmd.send.thuycung.ResultThuyCungMsg;
import game.modules.slot.cmd.send.thuycung.UpdatePotThuyCungMsg;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.AwardsOnLine;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.avengers.AvengersAward;
import game.modules.slot.entities.slot.avengers.AvengersAwards;
import game.modules.slot.entities.slot.avengers.AvengersFreeSpinAward;
import game.modules.slot.entities.slot.avengers.AvengersItem;
import game.modules.slot.entities.slot.avengers.AvengersLines;
import game.modules.slot.utils.AvengersUtils;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;
import game.util.GameUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThuyCungRoom extends SlotRoom {
     private final Runnable gameLoopTask = new GameLoopTask();
     private AvengersLines lines = new AvengersLines();
     private long lastTimeUpdatePotToRoom = 0L;
     private long lastTimeUpdateFundToRoom = 0L;
     private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);

     public ThuyCungRoom(ThuyCungModule module, byte id, String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
          super(id, name, betValue, moneyType, pot, fund, initPotValue);
          this.module = module;
          this.moneyType = moneyType;
          this.gameName = Games.THETHAO.getName();
          this.cacheFreeName = this.gameName + betValue;
          CacheService cacheService = new CacheServiceImpl();
          cacheService.setValue(name, (int)pot);
          this.betValue = betValue;
          this.initPotValue = initPotValue;
          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
     }

     public void forceStopAutoPlay(User user) {
          super.forceStopAutoPlay(user);
          synchronized(this.usersAuto) {
               this.usersAuto.remove(user.getName());
               ForceStopAutoPlayThuyCungMsg msg = new ForceStopAutoPlayThuyCungMsg();
               SlotUtils.sendMessageToUser(msg, (User)user);
          }
     }

     public ResultThuyCungMsg play(String username, String linesStr, boolean isBot) {
          long referenceId = this.module.getNewReferenceId();
          SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.cacheFreeName, username);
          int luotQuayFree = freeSpin.getNum();
          int ratioFree = freeSpin.getRatio();
          if (luotQuayFree > 0 && !isBot) {
               linesStr = freeSpin.getLines();
               return this.playFree(username, linesStr, freeSpin.getItemsWild(), ratioFree, referenceId);
          } else {
               return this.playNormal(username, linesStr, referenceId);
          }
     }

     public ResultThuyCungMsg playNormal(String username, String linesStr, long referenceId) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          UserCacheModel u = this.userService.getUser(username);
          long totalBetValue = (long)(lineArr.length * this.betValue);
          ResultThuyCungMsg msg = new ResultThuyCungMsg();
          long fee;
          if (lineArr.length > 0 && !linesStr.isEmpty()) {
               if (totalBetValue > 0L) {
                    if (totalBetValue <= currentMoney) {
                         fee = totalBetValue * 2L / 100L;
                         MoneyResponse moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, "ThuyCung", "Dat cuoc ThuyCung", fee, referenceId, TransType.START_TRANS);
                         if (moneyRes != null && moneyRes.isSuccess()) {
                              long moneyToPot = totalBetValue * 1L / 100L;
                              long moneyToFund = totalBetValue - fee - moneyToPot;
                              this.fund += moneyToFund;
                              this.pot += moneyToPot;
                              boolean enoughPair = false;
                              List awardsOnLines = new ArrayList();
                              long totalPrizes = 0L;
                              long soTienNoHuKhongTruQuy = 0L;
                              long tienThuongX2 = 0L;
                              int countScatter = 0;
                              int countBonus = 0;
                              MiniGameSlotResponse miniGameSlot = null;

                              label219:
                              while(true) {
                                   String linesWin;
                                   String prizesOnLine;
                                   String matrixStr;
                                   AvengersItem[][] matrix;
                                   StringBuilder builderLinesWin;
                                   StringBuilder builderPrizesOnLine;
                                   label195:
                                   while(true) {
                                        label181:
                                        while(true) {
                                             int j;
                                             int length;
                                             do {
                                                  if (enoughPair) {
                                                       break label219;
                                                  }

                                                  result = 0;
                                                  awardsOnLines.clear();
                                                  totalPrizes = 0L;
                                                  soTienNoHuKhongTruQuy = 0L;
                                                  tienThuongX2 = 0L;
                                                  linesWin = "";
                                                  prizesOnLine = "";
                                                  miniGameSlot = null;
                                                  countScatter = 0;
                                                  countBonus = 0;
                                                  boolean forceNoHu = false;
                                                  Random rd2;
                                                  if (lineArr.length >= 5) {
                                                       int soLanNoHu = ConfigGame.getIntValue(this.gameName + "_so_lan_no_hu", 0);
                                                       if (soLanNoHu > 0 && this.fund > this.initPotValue * 2L) {
                                                            rd2 = new Random();
                                                            j = rd2.nextInt(soLanNoHu);
                                                            if (j == 0) {
                                                                 forceNoHu = true;
                                                            }
                                                       }
                                                  }

                                                  if (forceNoHu) {
                                                       matrix = AvengersUtils.generateMatrixNoHu(lineArr);
                                                  } else {
                                                       matrix = AvengersUtils.generateMatrix();
                                                  }

                                                  for(int i = 0; i < 3; ++i) {
                                                       for(j = 0; j < 5; ++j) {
                                                            if (matrix[i][j] == AvengersItem.SCATTER) {
                                                                 ++countScatter;
                                                            } else if (matrix[i][j] == AvengersItem.BONUS) {
                                                                 ++countBonus;
                                                            }
                                                       }
                                                  }

                                                  if (countBonus < 3 && countScatter < 3) {
                                                       break;
                                                  }

                                                  rd2 = new Random();
                                                  j = lineArr.length * 100 / 25;
                                                  length = rd2.nextInt(100);
                                             } while(length >= j);

                                             if (countBonus >= 3) {
                                                  miniGameSlot = AvengersUtils.addMiniGameSlot(this.betValue, countBonus);
                                                  AvengersAward award = AvengersAwards.getAward(AvengersItem.BONUS, countBonus);
                                                  AwardsOnLine aol = new AwardsOnLine(award, miniGameSlot.getTotalPrize(), "line0");
                                                  awardsOnLines.add(aol);
                                                  result = 5;
                                             }

                                             AvengersItem[][] matrixWild = AvengersUtils.revertMatrix(matrix);
                                             String[] array = lineArr;
                                             length = lineArr.length;

                                             for(int k = 0; k < length; ++k) {
                                                  matrixStr = array[k];
                                                  List awardList = new ArrayList();
                                                  Line line = AvengersUtils.getLine(this.lines, matrixWild, Integer.parseInt(matrixStr));
                                                  AvengersUtils.calculateLine(line, awardList);
                                                  Iterator var45 = awardList.iterator();

                                                  while(var45.hasNext()) {
                                                       AvengersAward award2 = (AvengersAward)var45.next();
                                                       long moneyOnLine = 0L;
                                                       if (award2.getRatio() > 0.0F) {
                                                            moneyOnLine = (long)(award2.getRatio() * (float)this.betValue);
                                                       } else if (award2 == AvengersAward.PENTA_JACK_POT) {
                                                            if (result == 3) {
                                                                 moneyOnLine = this.initPotValue;
                                                            } else {
                                                                 if (this.huX2) {
                                                                      moneyOnLine = this.pot * 2L;
                                                                      tienThuongX2 = this.pot;
                                                                      soTienNoHuKhongTruQuy += this.pot;
                                                                 } else {
                                                                      moneyOnLine = this.pot;
                                                                 }

                                                                 result = 3;
                                                                 soTienNoHuKhongTruQuy += this.pot - this.initPotValue;
                                                            }
                                                       }

                                                       AwardsOnLine aol2 = new AwardsOnLine(award2, moneyOnLine, line.getName());
                                                       awardsOnLines.add(aol2);
                                                  }
                                             }

                                             builderLinesWin = new StringBuilder();
                                             builderPrizesOnLine = new StringBuilder();
                                             Iterator var64 = awardsOnLines.iterator();

                                             while(var64.hasNext()) {
                                                  AwardsOnLine entry2 = (AwardsOnLine)var64.next();
                                                  if (entry2.getAward() == AvengersAward.PENTA_JACK_POT && !u.isBot()) {
                                                       continue label181;
                                                  }

                                                  totalPrizes += entry2.getMoney();
                                                  builderLinesWin.append(",");
                                                  builderLinesWin.append(entry2.getLineId());
                                                  builderPrizesOnLine.append(",");
                                                  builderPrizesOnLine.append(entry2.getMoney());
                                             }

                                             if (builderLinesWin.length() > 0) {
                                                  builderLinesWin.deleteCharAt(0);
                                             }

                                             if (builderPrizesOnLine.length() > 0) {
                                                  builderPrizesOnLine.deleteCharAt(0);
                                             }

                                             if (result == 3) {
                                                  if (this.fund - (totalPrizes - soTienNoHuKhongTruQuy) < 0L) {
                                                       continue;
                                                  }
                                                  break label195;
                                             } else if (this.fund - totalPrizes >= this.initPotValue * 2L || totalPrizes - totalBetValue < 0L) {
                                                  break label195;
                                             }
                                        }
                                   }

                                   // [Phase3b] RTP Engine: re-roll if force-lose fires (jackpot result==3,4 exempt)
                                   if (result != 3 && result != 4 && totalPrizes > 0L
                                           && SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_thuycung")) {
                                       
                                       totalPrizes = 0;
                                       awardsOnLines.clear();
                                       builderLinesWin = new StringBuilder();
                                       builderPrizesOnLine = new StringBuilder();
                                       result = 0;
                                   }
                                   enoughPair = true;
                                   matrixStr = AvengersUtils.matrixToString(matrix);
                                   // Phase 3b — pct-aware force-lose (protects jackpots result==3)
                                   if (result != 3 && totalPrizes > 0L
                                           && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_sah")) {
                                        totalPrizes = 0L;
                                        awardsOnLines.clear();
                                        builderLinesWin.setLength(0);
                                        builderPrizesOnLine.setLength(0);
                                        result = 0;
                                   }
                                   if (totalPrizes > 0L) {
                                        if (result == 3) {
                                             if (this.huX2) {
                                                  result = 4;
                                             }

                                             this.noHuX2();
                                             this.pot = this.initPotValue;
                                             this.fund -= totalPrizes - soTienNoHuKhongTruQuy;
                                             if (this.moneyType == 1) {
                                                  GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game ThuyCung phong " + this.betValue + ". So tien no hu: " + totalPrizes + " vin");
                                             }

                                             this.slotService.logNoHu(referenceId, this.gameName, username, this.betValue, linesStr, matrixStr, builderLinesWin.toString(), builderPrizesOnLine.toString(), totalPrizes, result, currentTimeStr);
                                        } else {
                                             this.fund -= totalPrizes;
                                             if (result == 0) {
                                                  if (totalPrizes >= (long)(this.betValue * 100)) {
                                                       result = 2;
                                                  } else {
                                                       result = 1;
                                                  }
                                             }
                                        }
                                   }

                                   msg.freeSpin = (byte)this.setFreeSpin(username, linesStr, countScatter);
                                   long moneyExchange = totalPrizes - tienThuongX2;
                                   String des = "Quay ThuyCung ";
                                   if (tienThuongX2 > 0L) {
                                        this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName, "Quay ThuyCung ", "Thuong hu X2", 0L, (Long)null, TransType.NO_VIPPOINT);
                                   }

                                   moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName, "Quay ThuyCung ", this.buildDescription(totalBetValue, totalPrizes, result), 0L, referenceId, TransType.END_TRANS);
                                   if (moneyRes != null && moneyRes.isSuccess()) {
                                        currentMoney = moneyRes.getCurrentMoney();
                                        if (this.moneyType == 1 && moneyExchange - (long)this.betValue >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                             this.broadcastMsgService.putMessage(Games.THETHAO.getId(), username, moneyExchange - (long)this.betValue);
                                        }
                                   }

                                   linesWin = builderLinesWin.toString();
                                   prizesOnLine = builderPrizesOnLine.toString();
                                   msg.referenceId = referenceId;
                                   msg.matrix = AvengersUtils.matrixToString(matrix);
                                   msg.linesWin = linesWin;
                                   msg.prize = totalPrizes;
                                   msg.isFreeSpin = false;
                                   if (miniGameSlot != null) {
                                        msg.haiSao = miniGameSlot.getPrizes();
                                   }

                                   try {
                                        this.slotService.logAvengers(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                                        if (result == 3 || result == 4) {
                                             this.slotService.addTop(this.gameName, username, this.betValue, totalPrizes, currentTimeStr, result);
                                        }

                                        if (result == 3 || result == 2 || result == 4) {
                                             BigWinThuyCungMsg bigWinMsg = new BigWinThuyCungMsg();
                                             bigWinMsg.username = username;
                                             bigWinMsg.type = (byte)result;
                                             bigWinMsg.betValue = (short)this.betValue;
                                             bigWinMsg.totalPrizes = totalPrizes;
                                             bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
                                             this.module.sendMsgToAllUsers(bigWinMsg);
                                        }
                                   } catch (Exception var50) {
                                        var50.printStackTrace();
                                   }

                                   this.saveFund();
                                   this.savePot();
                              }
                         }
                    } else {
                         result = 102;
                    }
               } else {
                    result = 101;
               }
          } else {
               result = 101;
          }

          msg.result = (byte)result;
          msg.currentMoney = currentMoney;
          fee = System.currentTimeMillis();
          long handleTime = fee - startTime;
          String ratioTime = CommonUtils.getRatioTime(handleTime);
          SlotUtils.logAvengers(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
          return msg;
     }

     public ResultThuyCungMsg playFreeDaily(String username, long referenceId) {
          String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25";
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          short result = 0;
          String[] lineArr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25".split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          UserCacheModel u = this.userService.getUser(username);
          ResultThuyCungMsg msg = new ResultThuyCungMsg();
          boolean enoughPair = false;
          List awardsOnLines = new ArrayList();
          long totalPrizes = 0L;
          long tienThuongX2 = 0L;
          int countScatter = 0;
          boolean var21 = false;

          while(true) {
               while(true) {
                    label110:
                    while(true) {
                         String linesWin;
                         String prizesOnLine;
                         AvengersItem[][] matrix;
                         int countBonus;
                         do {
                              do {
                                   if (enoughPair) {
                                        msg.result = (byte)result;
                                        msg.currentMoney = currentMoney;
                                        long endTime = System.currentTimeMillis();
                                        long handleTime = endTime - startTime;
                                        String ratioTime = CommonUtils.getRatioTime(handleTime);
                                        SlotUtils.logAvengers(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
                                        return msg;
                                   }

                                   result = 0;
                                   awardsOnLines.clear();
                                   totalPrizes = 0L;
                                   linesWin = "";
                                   prizesOnLine = "";
                                   countScatter = 0;
                                   countBonus = 0;
                                   matrix = AvengersUtils.generateMatrix();

                                   for(int i = 0; i < 3; ++i) {
                                        for(int j = 0; j < 5; ++j) {
                                             if (matrix[i][j] == AvengersItem.SCATTER) {
                                                  ++countScatter;
                                             } else if (matrix[i][j] == AvengersItem.BONUS) {
                                                  ++countBonus;
                                             }
                                        }
                                   }
                              } while(countBonus >= 3);
                         } while(countScatter >= 3);

                         AvengersItem[][] matrixWild = AvengersUtils.revertMatrix(matrix);
                         String[] array = lineArr;
                         int length = lineArr.length;

                         for(int k = 0; k < length; ++k) {
                              String entry = array[k];
                              List awardList = new ArrayList();
                              Line line = AvengersUtils.getLine(this.lines, matrixWild, Integer.parseInt(entry));
                              AvengersUtils.calculateLine(line, awardList);
                              Iterator var32 = awardList.iterator();

                              while(var32.hasNext()) {
                                   AvengersAward award = (AvengersAward)var32.next();
                                   long moneyOnLine = 0L;
                                   if (award.getRatio() > 0.0F) {
                                        moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                   } else if (award == AvengersAward.PENTA_JACK_POT) {
                                        continue label110;
                                   }

                                   AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                   awardsOnLines.add(aol);
                              }
                         }

                         StringBuilder builderLinesWin = new StringBuilder();
                         StringBuilder builderPrizesOnLine = new StringBuilder();
                         Iterator var49 = awardsOnLines.iterator();

                         while(var49.hasNext()) {
                              AwardsOnLine entry2 = (AwardsOnLine)var49.next();
                              if (entry2.getAward() == AvengersAward.PENTA_JACK_POT && !u.isBot()) {
                                   continue label110;
                              }

                              totalPrizes += entry2.getMoney();
                              builderLinesWin.append(",");
                              builderLinesWin.append(entry2.getLineId());
                              builderPrizesOnLine.append(",");
                              builderPrizesOnLine.append(entry2.getMoney());
                         }

                         if (builderLinesWin.length() > 0) {
                              builderLinesWin.deleteCharAt(0);
                         }

                         if (builderPrizesOnLine.length() > 0) {
                              builderPrizesOnLine.deleteCharAt(0);
                         }

                         if (totalPrizes <= (long)ConfigGame.getIntValue("max_prize_free_daily", 2000)) {
                              enoughPair = true;
                              boolean updated = this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
                              if (!updated) {
                                   Debug.trace(username + " luot quay free " + this.gameName + " khong hop le");
                                   result = 103;
                              } else {
                                   long moneyExchange = totalPrizes - 0L;
                                   if (moneyExchange > 0L) {
                                        String des = "Quay ThuyCung Free ";
                                        MoneyResponse moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, "ThuyCungFree", "Quay ThuyCung Free ", "Cuoc: 0, Thang: " + totalPrizes, 0L, (Long)null, TransType.NO_VIPPOINT);
                                        if (moneyRes != null && moneyRes.isSuccess()) {
                                             currentMoney = moneyRes.getCurrentMoney();
                                        }
                                   }

                                   linesWin = builderLinesWin.toString();
                                   prizesOnLine = builderPrizesOnLine.toString();
                                   msg.referenceId = referenceId;
                                   msg.matrix = AvengersUtils.matrixToString(matrix);
                                   msg.linesWin = linesWin;
                                   msg.prize = totalPrizes;
                                   msg.isFreeSpin = false;

                                   try {
                                        this.slotService.logAvengers(referenceId, username, (long)this.betValue, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25", linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                                   } catch (InterruptedException var37) {
                                   } catch (TimeoutException var38) {
                                   } catch (IOException var39) {
                                   }
                              }
                         }
                    }
               }
          }
     }

     public ResultThuyCungMsg playFree(String username, String linesStr, String itemsWild, int ratio, long referenceId) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          long totalBetValue = 0L;
          ResultThuyCungMsg msg = new ResultThuyCungMsg();
          long fee = 0L;
          long moneyToPot = 0L;
          long moneyToFund = 0L;
          this.fund += 0L;
          this.pot += 0L;
          boolean enoughPair = false;
          List awardsOnLines = new ArrayList();
          long totalPrizes = 0L;

          while(true) {
               String linesWin;
               String prizesOnLine;
               AvengersItem[][] matrix;
               StringBuilder builderLinesWin;
               StringBuilder builderPrizesOnLine;
               int tmpPrizes;
               while(true) {
                    if (enoughPair) {
                         msg.result = (byte)result;
                         msg.currentMoney = currentMoney;
                         long endTime = System.currentTimeMillis();
                         long handleTime = endTime - startTime;
                         String ratioTime = CommonUtils.getRatioTime(handleTime);
                         SlotUtils.logAvengers(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
                         return msg;
                    }

                    result = 0;
                    awardsOnLines.clear();
                    totalPrizes = 0L;
                    linesWin = "";
                    prizesOnLine = "";
                    String haiSao = "";
                    matrix = AvengersUtils.generateMatrixFreeSpin(itemsWild);
                    String[] array = lineArr;
                    int length = lineArr.length;

                    for(int k = 0; k < length; ++k) {
                         String entry = array[k];
                         List awardList = new ArrayList();
                         Line line = AvengersUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                         AvengersUtils.calculateFreeSpinLine(line, awardList);
                         Iterator var37 = awardList.iterator();

                         while(var37.hasNext()) {
                              AvengersFreeSpinAward award = (AvengersFreeSpinAward)var37.next();
                              long moneyOnLine = 0L;
                              if (award.getRatio() > 0.0F) {
                                   moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                   AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                   awardsOnLines.add(aol);
                              }
                         }
                    }

                    builderLinesWin = new StringBuilder();
                    builderPrizesOnLine = new StringBuilder();
                    Iterator var51 = awardsOnLines.iterator();

                    while(var51.hasNext()) {
                         AwardsOnLine entry2 = (AwardsOnLine)var51.next();
                         totalPrizes += entry2.getMoney();
                         builderLinesWin.append(",");
                         builderLinesWin.append(entry2.getLineId());
                         builderPrizesOnLine.append(",");
                         builderPrizesOnLine.append(entry2.getMoney());
                    }

                    if (builderLinesWin.length() > 0) {
                         builderLinesWin.deleteCharAt(0);
                    }

                    if (builderPrizesOnLine.length() > 0) {
                         builderPrizesOnLine.deleteCharAt(0);
                    }

                    tmpPrizes = (int)totalPrizes;
                    totalPrizes *= (long)ratio;
                    if (result == 3) {
                         if (this.fund - totalPrizes < 0L) {
                              continue;
                         }
                         break;
                    } else if (this.fund - totalPrizes >= this.initPotValue * 2L || totalPrizes - 0L < 0L) {
                         break;
                    }
               }

               enoughPair = true;
               if (totalPrizes > 0L) {
                    this.fund -= totalPrizes;
                    if (result == 0) {
                         result = 1;
                    }
               }

               long moneyExchange = totalPrizes - 0L;
               String des = "ThuyCung - Free";
               MoneyResponse moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName, "ThuyCung - Free", this.buildDescription(0L, totalPrizes, result), 0L, (Long)null, TransType.VIPPOINT);
               if (moneyRes != null && moneyRes.isSuccess()) {
                    currentMoney = moneyRes.getCurrentMoney();
                    if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                         this.broadcastMsgService.putMessage(Games.THETHAO.getId(), username, moneyExchange);
                    }

                    this.slotService.addPrizes(this.cacheFreeName, username, tmpPrizes);
               }

               SlotFreeSpin freeSpin = this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username);
               if (freeSpin.getNum() == 0) {
                    ThuyCungTotalFreeSpin totalFreeSpinMsg = new ThuyCungTotalFreeSpin();
                    totalFreeSpinMsg.prize = freeSpin.getPrizes();
                    totalFreeSpinMsg.ratio = (byte)ratio;
                    SlotUtils.sendMessageToUser(totalFreeSpinMsg, (String)username);
               }

               StringBuilder sb = new StringBuilder();

               for(int i = 0; i < 3; ++i) {
                    for(int j = 0; j < 5; ++j) {
                         if (matrix[i][j] == AvengersItem.WILD) {
                              sb.append(i + "," + j + ",");
                         }
                    }
               }

               if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
               }

               this.slotService.setItemsWild(this.cacheFreeName, username, sb.toString());
               linesWin = builderLinesWin.toString();
               prizesOnLine = builderPrizesOnLine.toString();
               msg.referenceId = referenceId;
               msg.matrix = AvengersUtils.matrixToString(matrix);
               msg.linesWin = linesWin;
               msg.prize = totalPrizes;
               msg.haiSao = "";
               msg.freeSpin = (byte)freeSpin.getNum();
               msg.isFreeSpin = true;
               msg.itemsWild = sb.toString();
               msg.ratioFree = (byte)ratio;

               try {
                    this.slotService.logAvengers(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
               } catch (InterruptedException var43) {
               } catch (TimeoutException var44) {
               } catch (IOException var45) {
               }

               this.saveFund();
               this.savePot();
          }
     }

     private int setFreeSpin(String nickName, String lines, int countFreeSpin) {
          int soLuot = 0;
          switch(countFreeSpin) {
          case 3:
               soLuot = 8;
               this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 1);
               break;
          case 4:
               soLuot = 8;
               this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 2);
               break;
          case 5:
               soLuot = 8;
               this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 3);
          }

          return soLuot;
     }

     public short play(User user, String linesStr) {
          String username = user.getName();
          int numFree = 0;
          if (user.getProperty("numFreeDaily") != null) {
               numFree = (Integer)user.getProperty("numFreeDaily");
          }

          ResultThuyCungMsg msg = null;
          ThuyCungFreeDailyMsg freeDailyMsg = new ThuyCungFreeDailyMsg();
          if (numFree > 0) {
               msg = this.playFreeDaily(username, this.module.getNewReferenceId());
               --numFree;
               freeDailyMsg.remain = (byte)numFree;
               if (numFree > 0) {
                    user.setProperty("numFreeDaily", numFree);
               } else {
                    user.removeProperty("numFreeDaily");
               }
          } else {
               msg = this.play(username, linesStr, false);
          }

          if (this.isUserMinimize(user)) {
               MinimizeResultThuyCungMsg miniMsg = new MinimizeResultThuyCungMsg();
               miniMsg.prize = msg.prize;
               miniMsg.curretMoney = msg.currentMoney;
               miniMsg.result = msg.result;
               SlotUtils.sendMessageToUser(miniMsg, (User)user);
          } else {
               SlotUtils.sendMessageToUser(msg, (User)user);
               SlotUtils.sendMessageToUser(freeDailyMsg, (User)user);
          }

          return (short)msg.result;
     }

     protected void checkResetPot() {
          // stub
     }

     private void saveFund() {
          long currentTime = System.currentTimeMillis();
          if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
               try {
                    this.mgService.saveFund(this.name, this.fund);
               } catch (TimeoutException | IOException | InterruptedException var6) {
                    Debug.trace(new Object[]{this.gameName + ": update fund error ", var6.getMessage()});
               }

               this.lastTimeUpdateFundToRoom = currentTime;
          }

     }

     private void savePot() {
          long currentTime = System.currentTimeMillis();
          if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
               this.lastTimeUpdatePotToRoom = currentTime;

               try {
                    this.mgService.savePot(this.name, this.pot, this.huX2);
               } catch (TimeoutException | IOException | InterruptedException var6) {
                    Debug.trace(new Object[]{this.gameName + ": update pot error ", var6.getMessage()});
               }

               UpdatePotThuyCungMsg msg = new UpdatePotThuyCungMsg();
               msg.value = this.pot;
               msg.x2 = (byte)(this.huX2 ? 1 : 0);
               this.sendMessageToRoom(msg);
          }

     }

     public void updatePot(User user) {
          UpdatePotThuyCungMsg msg = new UpdatePotThuyCungMsg();
          msg.value = this.pot;
          msg.x2 = (byte)(this.huX2 ? 1 : 0);
          SlotUtils.sendMessageToUser(msg, (User)user);
     }

     protected void gameLoop() {
          List usersPlay = new ArrayList();
          synchronized(this.usersAuto) {
               Iterator var3 = this.usersAuto.values().iterator();

               while(var3.hasNext()) {
                    AutoUser user = (AutoUser)var3.next();
                    boolean play = user.incCount();
                    if (play) {
                         usersPlay.add(user);
                    }
               }
          }

          int numThreads = usersPlay.size() / 100 + 1;

          for(int i = 1; i <= numThreads; ++i) {
               int fromIndex = (i - 1) * 100;
               int toIndex = i * 100;
               if (toIndex > usersPlay.size()) {
                    toIndex = usersPlay.size();
               }

               List tmp = new ArrayList(usersPlay.subList(fromIndex, toIndex));
               if (tmp.size() > 0) {
                    PlayListAutoUserTask task = new PlayListAutoUserTask(tmp);
                    this.executor.execute(task);
               }
          }

          usersPlay.clear();
     }

     protected void playListAuto(List users) {
          Iterator var2 = users.iterator();

          while(true) {
               while(var2.hasNext()) {
                    AutoUser user = (AutoUser)var2.next();
                    short result = this.play(user.getUser(), user.getLines());
                    if (result != 3 && result != 4 && result != 101 && result != 102 && result != 100) {
                         if (result == 0) {
                              user.setMaxCount(user.isSpeedUp() ? 2 : 5);
                         } else if (result == 5) {
                              user.setMaxCount(20);
                         } else {
                              user.setMaxCount(user.isSpeedUp() ? 4 : 8);
                         }
                    } else {
                         this.forceStopAutoPlay(user.getUser());
                    }
               }

               users.clear();
               return;
          }
     }

     public boolean joinRoom(User user) {
          boolean result = super.joinRoom(user);
          SlotFreeDaily model = this.slotService.getLuotQuayFreeDaily(this.gameName, user.getName(), this.betValue);
          ThuyCungFreeDailyMsg freeDailyMsg = new ThuyCungFreeDailyMsg();
          if (model != null && model.getRotateFree() > 0) {
               user.setProperty("numFreeDaily", model.getRotateFree());
               freeDailyMsg.remain = (byte)model.getRotateFree();
          } else {
               user.removeProperty("numFreeDaily");
          }

          SlotUtils.sendMessageToUser(freeDailyMsg, (User)user);
          if (result) {
               user.setProperty("MGROOM_" + this.gameName + "_INFO", this);
          }

          return result;
     }
}
