package game.modules.slot.room;

import game.modules.slot.utils.SlotHouseEdge;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.slot.VQVModule;
import game.modules.slot.cmd.send.vqv.BigWinVQVMsg;
import game.modules.slot.cmd.send.vqv.ForceStopAutoPlayVQVMsg;
import game.modules.slot.cmd.send.vqv.MinimizeResultVQVMsg;
import game.modules.slot.cmd.send.vqv.ResultSlotMsg;
import game.modules.slot.cmd.send.vqv.VQVFreeDailyMsg;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;
import game.modules.slot.entities.slot.vqv.AwardsOnLine;
import game.modules.slot.entities.slot.vqv.Line;
import game.modules.slot.entities.slot.vqv.VQVAward;
import game.modules.slot.entities.slot.vqv.VQVItem;
import game.modules.slot.entities.slot.vqv.VQVLines;
import game.modules.slot.utils.SlotUtils;
import game.modules.slot.utils.VQVUtils;
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

public class VQVRoom extends SlotRoom {
     private final Runnable gameLoopTask = new SlotRoom.GameLoopTask();
     private VQVLines lines = new VQVLines();
     private long lastTimeUpdatePotToRoom = 0L;
     private long lastTimeUpdateFundToRoom = 0L;
     private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
     private List boxValues = new ArrayList();

     public VQVRoom(VQVModule module, byte id, String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
          super(id, name, betValue, moneyType, pot, fund, initPotValue);
          this.gameName = Games.VUONG_QUOC_VIN.getName();
          this.cacheFreeName = this.gameName + betValue;
          this.module = module;
          if (this.moneyType == 1) {
               this.moneyTypeStr = "vin";
          } else {
               this.moneyTypeStr = "xu";
          }

          CacheService cacheService = new CacheServiceImpl();
          cacheService.setValue(name, (int)pot);
          this.betValue = betValue;
          this.initPotValue = initPotValue;
          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
          this.boxValues.add(10);
          this.boxValues.add(10);
          this.boxValues.add(10);
          this.boxValues.add(15);
          this.boxValues.add(20);
     }

     public void forceStopAutoPlay(User user) {
          super.forceStopAutoPlay(user);
          synchronized(this.usersAuto) {
               this.usersAuto.remove(user.getName());
               ForceStopAutoPlayVQVMsg msg = new ForceStopAutoPlayVQVMsg();
               SlotUtils.sendMessageToUser(msg, (User)user);
          }
     }

     private void broadcastBigWin(String username, byte result, long totalPrizes) {
          BigWinVQVMsg bigWinMsg = new BigWinVQVMsg();
          bigWinMsg.username = username;
          bigWinMsg.type = result;
          bigWinMsg.betValue = (short)this.betValue;
          bigWinMsg.totalPrizes = totalPrizes;
          bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
          this.module.sendMsgToAllUsers(bigWinMsg);
     }

     public synchronized ResultSlotMsg play(String username, String linesStr) {
          long referenceId = this.module.getNewReferenceId();
          SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.cacheFreeName, username);
          int luotQuayFree = freeSpin.getNum();
          int ratioFree = freeSpin.getRatio();
          if (luotQuayFree > 0) {
               linesStr = freeSpin.getLines();
               return this.playFree(username, linesStr, ratioFree, referenceId);
          } else {
               return this.playNormal(username, linesStr, referenceId);
          }
     }

     public synchronized ResultSlotMsg playNormal(String username, String linesStr, long referenceId) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          UserCacheModel u = this.userService.getUser(username);
          long totalBetValue = (long)(lineArr.length * this.betValue);
          ResultSlotMsg msg = new ResultSlotMsg();
          long fee;
          if (lineArr.length > 0 && !linesStr.isEmpty()) {
               if (totalBetValue > 0L) {
                    if (totalBetValue <= currentMoney) {
                         fee = totalBetValue * 2L / 100L;
                         MoneyResponse moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, "Quay Vương Quốc " + "vin".toUpperCase(), "Đặt cược Vương Quốc " + "vin".toUpperCase(), fee, referenceId, TransType.START_TRANS);
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
                              int countFreeSpin = 0;
                              int ratio = 0;
                              boolean var33 = false;

                              label215:
                              while(true) {
                                   String linesWin;
                                   String prizesOnLine;
                                   String haiSao;
                                   String matrixStr;
                                   //int countFreeSpin;
                                   //byte ratio;
                                   VQVItem[][] matrix;
                                   StringBuilder builderLinesWin;
                                   StringBuilder builderPrizesOnLine;
                                   label191:
                                   while(true) {
                                        while(true) {
                                             label163:
                                             while(true) {
                                                  if (enoughPair) {
                                                       break label215;
                                                  }

                                                  result = 0;
                                                  awardsOnLines.clear();
                                                  totalPrizes = 0L;
                                                  soTienNoHuKhongTruQuy = 0L;
                                                  tienThuongX2 = 0L;
                                                  linesWin = "";
                                                  prizesOnLine = "";
                                                  haiSao = "";
                                                  countFreeSpin = 0;
                                                  ratio = 0;
                                                  int ratioBonus = 0;
                                                  boolean forceNoHu = false;
                                                  int length;
                                                  if (lineArr.length >= 5) {
                                                       int soLanNoHu = ConfigGame.getIntValue(this.gameName + "_so_lan_no_hu");
                                                       if (soLanNoHu > 0 && this.fund > this.initPotValue * 2L) {
                                                            Random rd = new Random();
                                                            length = rd.nextInt(soLanNoHu);
                                                            if (length == 0) {
                                                                 forceNoHu = true;
                                                            }
                                                       }
                                                  }

                                                  if (forceNoHu) {
                                                       matrix = VQVUtils.generateMatrixNoHu(lineArr);
                                                  } else {
                                                       matrix = VQVUtils.generateMatrix();
                                                  }

                                                  String[] array = lineArr;
                                                  length = lineArr.length;

                                                  for(int i = 0; i < length; ++i) {
                                                       matrixStr = array[i];
                                                       List awardList = new ArrayList();
                                                       Line line = VQVUtils.getLine(this.lines, matrix, Integer.parseInt(matrixStr));
                                                       VQVUtils.calculateLine(line, awardList);
                                                       Iterator var45 = awardList.iterator();

                                                       while(var45.hasNext()) {
                                                            VQVAward award = (VQVAward)var45.next();
                                                            long moneyOnLine = 0L;
                                                            if (award.getRatio() > 0.0F) {
                                                                 moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                                            } else if (award == VQVAward.PENTA_JACKPOT) {
                                                                 if (result != 3) {
                                                                      if (this.huX2) {
                                                                           moneyOnLine = this.pot * 2L;
                                                                           tienThuongX2 = this.pot;
                                                                           soTienNoHuKhongTruQuy += this.pot;
                                                                      } else {
                                                                           moneyOnLine = this.pot;
                                                                      }

                                                                      result = 3;
                                                                      soTienNoHuKhongTruQuy += this.pot - this.initPotValue;
                                                                 } else {
                                                                      moneyOnLine = this.initPotValue;
                                                                 }
                                                            } else if (award.getRatio() == -2.0F) {
                                                                 if (ratioBonus > 0) {
                                                                      continue label163;
                                                                 }

                                                                 ratioBonus = 1;
                                                                 if (award == VQVAward.QUADRA_BONUS) {
                                                                      ratioBonus = 5;
                                                                 }

                                                                 MiniGameSlotResponse response = this.generatePickStars(ratioBonus);
                                                                 moneyOnLine = response.getTotalPrize();
                                                                 haiSao = response.getPrizes();
                                                                 if (result != 3) {
                                                                      result = 5;
                                                                 }
                                                            } else if (award.getRatio() == -3.0F) {
                                                                 if (countFreeSpin > 0) {
                                                                      continue label163;
                                                                 }

                                                                 ++countFreeSpin;
                                                                 ratio = 1;
                                                                 if (award.getDuplicate() == VQVAward.PENTA_SCATTER.getDuplicate()) {
                                                                      ratio = 15;
                                                                 } else if (award.getDuplicate() == VQVAward.QUADRA_SCATTER.getDuplicate()) {
                                                                      ratio = 5;
                                                                 }
                                                            }

                                                            AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                                            awardsOnLines.add(aol);
                                                       }
                                                  }

                                                  builderLinesWin = new StringBuilder();
                                                  builderPrizesOnLine = new StringBuilder();
                                                  Iterator var61 = awardsOnLines.iterator();

                                                  while(var61.hasNext()) {
                                                       AwardsOnLine entry2 = (AwardsOnLine)var61.next();
                                                       if (!u.isBot() && (entry2.getAward() == VQVAward.PENTA_JACKPOT || entry2.getAward() == VQVAward.QUADRA_JACKPOT || entry2.getAward() == VQVAward.TRIPLE_JACKPOT)) {
                                                            continue label163;
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
                                                       break label191;
                                                  } else if (this.fund - totalPrizes >= this.initPotValue * 2L || totalPrizes - totalBetValue < 0L) {
                                                       break label191;
                                                  }
                                             }
                                        }
                                   }

                                   // [Phase3b] RTP Engine: re-roll if force-lose fires (jackpot result==3,4 exempt)
                                   if (result != 3 && result != 4 && totalPrizes > 0L
                                           && SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_vqv")) {
                                       
                                       totalPrizes = 0;
                                       awardsOnLines.clear();
                                       builderLinesWin = new StringBuilder();
                                       builderPrizesOnLine = new StringBuilder();
                                       result = 0;
                                   }
                                   enoughPair = true;
                                   matrixStr = VQVUtils.matrixToString(matrix);
                                   // Phase 3b — pct-aware force-lose (protects jackpots result==3)
                                   if (result != 3 && totalPrizes > 0L
                                           && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_vqv")) {
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
                                                  GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game Vuong Quoc " + "vin".toUpperCase() + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "vin".toUpperCase());
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

                                   long moneyExchange = totalPrizes - tienThuongX2;
                                   String des = "Quay Vương Quốc " + "vin".toUpperCase();
                                   if (tienThuongX2 > 0L) {
                                        this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName, des, "Thưởng hũ X2", 0L, (Long)null, TransType.NO_VIPPOINT);
                                   }

                                   moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName, des, this.buildDescription(totalBetValue, totalPrizes, result), 0L, referenceId, TransType.END_TRANS);
                                   if (moneyRes != null && moneyRes.isSuccess()) {
                                        currentMoney = moneyRes.getCurrentMoney();
                                        if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                             this.broadcastMsgService.putMessage(Games.VUONG_QUOC_VIN.getId(), username, moneyExchange - totalBetValue);
                                        }
                                   }

                                   linesWin = builderLinesWin.toString();
                                   prizesOnLine = builderPrizesOnLine.toString();
                                   msg.referenceId = referenceId;
                                   msg.matrix = matrixStr;
                                   msg.linesWin = linesWin;
                                   msg.prize = totalPrizes;
                                   msg.haiSao = haiSao;
                                   if (countFreeSpin > 0) {
                                        msg.isFreeSpin = 1;
                                        msg.ratio = (byte)ratio;
                                        this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, ratio);
                                   } else {
                                        msg.isFreeSpin = 0;
                                   }

                                   try {
                                        this.slotService.logVQV(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                                        if (result == 3 || result == 4) {
                                             this.slotService.addTop(this.gameName, username, this.betValue, totalPrizes, currentTimeStr, result);
                                        }

                                        if (result == 3 || result == 2 || result == 4) {
                                             this.broadcastBigWin(username, (byte)result, totalPrizes);
                                        }
                                   } catch (InterruptedException var50) {
                                   } catch (TimeoutException var51) {
                                   } catch (IOException var52) {
                                   }

                                   this.saveFund();
                                   this.savePot();
                              }
                         } else {
                              result = 102;
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
          SlotUtils.logVQV(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
          return msg;
     }

     public synchronized ResultSlotMsg playFree(String username, String linesStr, int ratio, long referenceId) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          long totalBetValue = (long)(lineArr.length * this.betValue);
          ResultSlotMsg msg = new ResultSlotMsg();
          long totalPrizes;
          String linesWin;
          if (lineArr.length > 0 && !linesStr.isEmpty()) {
               if (totalBetValue > 0L) {
                    boolean enoughPair = false;
                    List awardsOnLines = new ArrayList();
                    totalPrizes = 0L;

                    label149:
                    while(true) {
                         String prizesOnLine;
                         VQVItem[][] matrix;
                         StringBuilder builderLinesWin;
                         StringBuilder builderPrizesOnLine;
                         label129:
                         do {
                              while(true) {
                                   label108:
                                   while(true) {
                                        if (enoughPair) {
                                             break label149;
                                        }

                                        result = 0;
                                        awardsOnLines.clear();
                                        totalPrizes = 0L;
                                        linesWin = "";
                                        prizesOnLine = "";
                                        String haiSao = "";
                                        boolean forceNoHu = false;
                                        int length;
                                        if (lineArr.length >= 5) {
                                             int soLanNoHu = ConfigGame.getIntValue(this.gameName + "_so_lan_no_hu");
                                             if (soLanNoHu > 0 && this.fund > this.initPotValue * 2L) {
                                                  Random rd = new Random();
                                                  length = rd.nextInt(soLanNoHu);
                                                  if (length == 0) {
                                                       forceNoHu = true;
                                                  }
                                             }
                                        }

                                        if (forceNoHu) {
                                             matrix = VQVUtils.generateMatrixNoHu(lineArr);
                                        } else {
                                             matrix = VQVUtils.generateMatrix();
                                        }

                                        String[] array = lineArr;
                                        length = lineArr.length;

                                        for(int i = 0; i < length; ++i) {
                                             String entry = array[i];
                                             List awardList = new ArrayList();
                                             Line line = VQVUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                                             VQVUtils.calculateLine(line, awardList);
                                             Iterator var31 = awardList.iterator();

                                             while(var31.hasNext()) {
                                                  VQVAward award = (VQVAward)var31.next();
                                                  long moneyOnLine = 0L;
                                                  if (award.getRatio() <= 0.0F) {
                                                       continue label108;
                                                  }

                                                  moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                                  AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                                  awardsOnLines.add(aol);
                                             }
                                        }

                                        builderLinesWin = new StringBuilder();
                                        builderPrizesOnLine = new StringBuilder();
                                        Iterator var44 = awardsOnLines.iterator();

                                        while(var44.hasNext()) {
                                             AwardsOnLine entry2 = (AwardsOnLine)var44.next();
                                             if (entry2.getAward() == VQVAward.PENTA_JACKPOT && !this.checkDieuKienNo(username)) {
                                                  continue label108;
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

                                        totalPrizes *= (long)ratio;
                                        continue label129;
                                   }
                              }
                         } while(this.fund - totalPrizes < this.initPotValue * 2L && totalPrizes - totalBetValue >= 0L);

                         enoughPair = true;
                         if (totalPrizes > 0L) {
                              if (result == 3) {
                                   if (this.huX2) {
                                        result = 4;
                                   }

                                   this.noHuX2();
                                   this.pot = this.initPotValue;
                                   this.fund -= totalPrizes;
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

                         if (totalPrizes > 0L) {
                              String des = "Vương Quốc " + "vin".toUpperCase() + " - Free";
                              MoneyResponse moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName, des, this.buildDescription(totalBetValue, totalPrizes, result), 0L, (Long)null, TransType.VIPPOINT);
                              if (moneyRes != null && moneyRes.isSuccess()) {
                                   currentMoney = moneyRes.getCurrentMoney();
                                   if (this.moneyType == 1 && totalPrizes >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                        this.broadcastMsgService.putMessage(Games.VUONG_QUOC_VIN.getId(), username, totalPrizes - totalBetValue);
                                   }
                              }
                         }

                         this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username);
                         linesWin = builderLinesWin.toString();
                         prizesOnLine = builderPrizesOnLine.toString();
                         msg.referenceId = referenceId;
                         msg.matrix = VQVUtils.matrixToString(matrix);
                         msg.linesWin = linesWin;
                         msg.prize = totalPrizes / (long)ratio;
                         msg.haiSao = "";
                         msg.isFreeSpin = 0;
                         msg.ratio = (byte)ratio;

                         try {
                              this.slotService.logVQV(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                              if (result == 3 || result == 4) {
                                   this.slotService.addTop(this.gameName, username, this.betValue, totalPrizes, currentTimeStr, result);
                              }

                              if (result == 3 || result == 2 || result == 4) {
                                   this.broadcastBigWin(username, (byte)result, totalPrizes);
                              }
                         } catch (InterruptedException var36) {
                         } catch (TimeoutException var37) {
                         } catch (IOException var38) {
                         }

                         this.saveFund();
                         this.savePot();
                    }
               } else {
                    result = 101;
               }
          } else {
               result = 101;
          }

          msg.result = (byte)result;
          msg.currentMoney = currentMoney;
          long endTime = System.currentTimeMillis();
          totalPrizes = endTime - startTime;
          linesWin = CommonUtils.getRatioTime(totalPrizes);
          SlotUtils.logVQV(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, totalPrizes, linesWin, currentTimeStr);
          return msg;
     }

     public synchronized ResultSlotMsg playFreeDaily(String username) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          long refernceId = this.module.getNewReferenceId();
          short result = 0;
          String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
          String[] lineArr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20".split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          ResultSlotMsg msg = new ResultSlotMsg();
          boolean enoughPair = false;
          List awardsOnLines = new ArrayList();
          long totalPrizes = 0L;

          while(true) {
               label71:
               while(!enoughPair) {
                    result = 0;
                    awardsOnLines.clear();
                    totalPrizes = 0L;
                    String linesWin = "";
                    String prizesOnLine = "";
                    String haiSao = "";
                    VQVItem[][] matrix = VQVUtils.generateMatrix();
                    String[] array = lineArr;
                    int length = lineArr.length;

                    for(int i = 0; i < length; ++i) {
                         String entry = array[i];
                         List awardList = new ArrayList();
                         Line line = VQVUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                         VQVUtils.calculateLine(line, awardList);
                         Iterator var27 = awardList.iterator();

                         while(var27.hasNext()) {
                              VQVAward award = (VQVAward)var27.next();
                              long money = 0L;
                              if (award.getRatio() <= 0.0F) {
                                   continue label71;
                              }

                              money = (long)(award.getRatio() * (float)this.betValue);
                              AwardsOnLine aol = new AwardsOnLine(award, money, line.getName());
                              awardsOnLines.add(aol);
                         }
                    }

                    StringBuilder builderLinesWin = new StringBuilder();
                    StringBuilder builderPrizesOnLine = new StringBuilder();
                    Iterator var40 = awardsOnLines.iterator();

                    while(var40.hasNext()) {
                         AwardsOnLine entry2 = (AwardsOnLine)var40.next();
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

                    if (this.fund - totalPrizes >= 0L && totalPrizes <= (long)ConfigGame.getIntValue("max_prize_free_daily", 2000)) {
                         enoughPair = true;
                         boolean updated = this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
                         if (!updated) {
                              result = 103;
                         } else if (totalPrizes > 0L) {
                              String des = "Vương Quốc " + "vin".toUpperCase() + " Free";
                              MoneyResponse moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, "VuongQuocVinVqFree", des, "Cược: 0, Thắng: " + totalPrizes, 0L, (Long)null, TransType.NO_VIPPOINT);
                              if (moneyRes != null && moneyRes.isSuccess()) {
                                   currentMoney = moneyRes.getCurrentMoney();
                              }
                         }

                         linesWin = builderLinesWin.toString();
                         prizesOnLine = builderPrizesOnLine.toString();
                         msg.referenceId = refernceId;
                         msg.matrix = VQVUtils.matrixToString(matrix);
                         msg.linesWin = linesWin;
                         msg.prize = totalPrizes;
                         msg.haiSao = "";

                         try {
                              this.slotService.logVQV(refernceId, username, (long)this.betValue, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20", linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                         } catch (InterruptedException var32) {
                         } catch (TimeoutException var33) {
                         } catch (IOException var34) {
                         }
                    }
               }

               msg.result = (byte)result;
               msg.currentMoney = currentMoney;
               long endTime = System.currentTimeMillis();
               long handleTime = endTime - startTime;
               String ratioTime = CommonUtils.getRatioTime(handleTime);
               SlotUtils.logVQV(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
               return msg;
          }
     }

     private MiniGameSlotResponse generatePickStars(int ratio) {
          MiniGameSlotResponse response = new MiniGameSlotResponse();
          int totalMoney = 0;
          List gifts = new ArrayList();
          PickStarGifts pickStarGifts = new PickStarGifts();
          String responsePickStars = "";
          int totalKeys = 1;

          for(int numPicks = 10; numPicks > 0; --numPicks) {
               PickStarGiftItem gift = pickStarGifts.pickRandomAndRandomGift();
               switch(gift) {
               case GOLD:
                    totalMoney += 4 * this.betValue * 1;
                    gifts.add(new PickStarGift(PickStarGiftItem.GOLD, 0));
                    break;
               case KEY:
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    break;
               case BOX:
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betValue * 1;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
               }
          }

          totalMoney *= ratio;
          responsePickStars = responsePickStars + ratio;
          Iterator var11 = gifts.iterator();

          while(var11.hasNext()) {
               PickStarGift pickStarGift = (PickStarGift)var11.next();
               if (responsePickStars.length() == 0) {
                    responsePickStars = responsePickStars + pickStarGift.getName();
               } else {
                    responsePickStars = responsePickStars + "," + pickStarGift.getName();
               }
          }

          response.setTotalPrize((long)totalMoney);
          response.setPrizes(responsePickStars);
          return response;
     }

     private int randomBoxValue() {
          Random rd = new Random();
          int n = rd.nextInt(this.boxValues.size());
          return (Integer)this.boxValues.get(n);
     }

     public short play(User user, String linesStr) {
          String username = user.getName();
          int numFree = 0;
          if (user.getProperty("numFreeDaily") != null) {
               numFree = (Integer)user.getProperty("numFreeDaily");
          }

          ResultSlotMsg msg = null;
          VQVFreeDailyMsg freeDailyMsg = new VQVFreeDailyMsg();
          if (numFree > 0) {
               msg = this.playFreeDaily(username);
               --numFree;
               freeDailyMsg.remain = (byte)numFree;
               if (numFree > 0) {
                    user.setProperty("numFreeDaily", numFree);
               } else {
                    user.removeProperty("numFreeDaily");
               }
          } else {
               msg = this.play(username, linesStr);
          }

          if (this.isUserMinimize(user)) {
               MinimizeResultVQVMsg miniMsg = new MinimizeResultVQVMsg();
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
                    Debug.trace(new Object[]{this.gameName + ": update pot poker error ", var6.getMessage()});
               }

               byte x2 = (byte)(this.huX2 ? 1 : 0);
               ((VQVModule)this.module).updatePot(this.id, this.pot, x2);
          }

     }

     private boolean checkDieuKienNo(String username) {
          try {
               UserModel u = this.userService.getUserByUserName(username);
               return u.isBot();
          } catch (Exception var3) {
               return false;
          }
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
                    SlotRoom.PlayListAutoUserTask task = new SlotRoom.PlayListAutoUserTask(tmp);
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
                              user.setMaxCount(user.isSpeedUp() ? 2 : 4);
                         } else if (result == 5) {
                              user.setMaxCount(15);
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
          if (model != null && model.getRotateFree() > 0) {
               user.setProperty("numFreeDaily", model.getRotateFree());
               VQVFreeDailyMsg freeDailyMsg = new VQVFreeDailyMsg();
               freeDailyMsg.remain = (byte)model.getRotateFree();
               SlotUtils.sendMessageToUser(freeDailyMsg, (User)user);
          } else {
               user.removeProperty("numFreeDaily");
          }

          if (result) {
               user.setProperty("MGROOM_" + this.gameName + "_INFO", this);
          }

          return result;
     }
}
