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
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.slot.NuDiepVienModule;
import game.modules.slot.cmd.send.ndv.BigWinNDVMsg;
import game.modules.slot.cmd.send.ndv.ForceStopAutoPlayNDVMsg;
import game.modules.slot.cmd.send.ndv.MinimizeResultNDVMsg;
import game.modules.slot.cmd.send.ndv.NDVFreeDailyMsg;
import game.modules.slot.cmd.send.ndv.ResultNDVMsg;
import game.modules.slot.cmd.send.ndv.UpdatePotNDVMsg;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.AwardsOnLine;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;
import game.modules.slot.entities.slot.ndv.NDVAward;
import game.modules.slot.entities.slot.ndv.NDVItem;
import game.modules.slot.entities.slot.ndv.NDVLines;
import game.modules.slot.utils.NuDiepVienUtils;
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

public class NuDiepVienRoom extends SlotRoom {
     private final Runnable gameLoopTask = new GameLoopTask();
     private NDVLines lines = new NDVLines();
     private long lastTimeUpdatePotToRoom = 0L;
     private long lastTimeUpdateFundToRoom = 0L;
     private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
     private List boxValues = new ArrayList();

     public NuDiepVienRoom(NuDiepVienModule module, byte id, String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
          super(id, name, betValue, moneyType, pot, fund, initPotValue);
          this.module = module;
          this.moneyType = moneyType;
          this.gameName = Games.NU_DIEP_VIEN.getName();
          this.pot = pot;
          CacheService cacheService = new CacheServiceImpl();
          cacheService.setValue(name, (int)pot);
          this.fund = fund;
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
               ForceStopAutoPlayNDVMsg msg = new ForceStopAutoPlayNDVMsg();
               SlotUtils.sendMessageToUser(msg, (User)user);
          }
     }

     public synchronized ResultNDVMsg play(String username, String linesStr) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          long referenceId = this.module.getNewReferenceId();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          UserCacheModel u = this.userService.getUser(username);
          long totalBetValue = (long)(lineArr.length * this.betValue);
          ResultNDVMsg msg = new ResultNDVMsg();
          long fee;
          if (lineArr.length > 0 && !linesStr.isEmpty()) {
               if (totalBetValue > 0L) {
                    if (totalBetValue <= currentMoney) {
                         fee = totalBetValue * 2L / 100L;
                         MoneyResponse moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, Games.NU_DIEP_VIEN.getName(), "Quay Nữ Điệp Viên", "Đặt cược Nữ Điệp Viên", fee, referenceId, TransType.START_TRANS);
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

                              label187:
                              while(true) {
                                   String linesWin;
                                   String prizesOnLine;
                                   String haiSao;
                                   String matrixStr;
                                   NDVItem[][] matrix;
                                   StringBuilder builderLinesWin;
                                   StringBuilder builderPrizesOnLine;
                                   label163:
                                   while(true) {
                                        label151:
                                        while(true) {
                                             if (enoughPair) {
                                                  break label187;
                                             }

                                             result = 0;
                                             awardsOnLines.clear();
                                             totalPrizes = 0L;
                                             soTienNoHuKhongTruQuy = 0L;
                                             tienThuongX2 = 0L;
                                             linesWin = "";
                                             prizesOnLine = "";
                                             haiSao = "";
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
                                                  matrix = NuDiepVienUtils.generateMatrixNoHu(lineArr);
                                             } else {
                                                  matrix = NuDiepVienUtils.generateMatrix();
                                             }

                                             String[] array = lineArr;
                                             length = lineArr.length;

                                             for(int i = 0; i < length; ++i) {
                                                  matrixStr = array[i];
                                                  List awardList = new ArrayList();
                                                  Line line = NuDiepVienUtils.getLine(this.lines, matrix, Integer.parseInt(matrixStr));
                                                  NuDiepVienUtils.calculateLine(line, awardList);
                                                  Iterator var42 = awardList.iterator();

                                                  while(var42.hasNext()) {
                                                       NDVAward award = (NDVAward)var42.next();
                                                       long moneyOnLine = 0L;
                                                       if (award.getRatio() > 0.0F) {
                                                            moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                                       } else if (award == NDVAward.PENTA_NU_DIEP_VIEN) {
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
                                                       } else if (award == NDVAward.QUADRA_KIEM_NHAT) {
                                                            MiniGameSlotResponse response = this.generatePickStars();
                                                            moneyOnLine = response.getTotalPrize();
                                                            haiSao = response.getPrizes();
                                                            if (result != 3) {
                                                                 result = 5;
                                                            }
                                                       }

                                                       AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                                       awardsOnLines.add(aol);
                                                  }
                                             }

                                             builderLinesWin = new StringBuilder();
                                             builderPrizesOnLine = new StringBuilder();
                                             Iterator var55 = awardsOnLines.iterator();

                                             while(var55.hasNext()) {
                                                  AwardsOnLine entry2 = (AwardsOnLine)var55.next();
                                                  if ((entry2.getAward() == NDVAward.PENTA_NU_DIEP_VIEN || entry2.getAward() == NDVAward.QUADRA_NU_DIEP_VIEN || entry2.getAward() == NDVAward.TRIPLE_NU_DIEP_VIEN) && !u.isBot()) {
                                                       continue label151;
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
                                             } else if (this.fund - totalPrizes < 0L) {
                                                  continue;
                                             }
                                             break label163;
                                        }
                                   }

                                   // [Phase3b] RTP Engine: re-roll if force-lose fires (jackpot result==3,4 exempt)
                                   if (result != 3 && result != 4 && totalPrizes > 0L
                                           && SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_nudiepvien")) {
                                       
                                       totalPrizes = 0;
                                       awardsOnLines.clear();
                                       builderLinesWin = new StringBuilder();
                                       builderPrizesOnLine = new StringBuilder();
                                       result = 0;
                                   }
                                   enoughPair = true;
                                   matrixStr = NuDiepVienUtils.matrixToString(matrix);
                                   // Phase 3b — pct-aware force-lose (protects jackpots result==3)
                                   if (result != 3 && totalPrizes > 0L
                                           && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_ndv")) {
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
                                                  GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game Nu diep vien phong " + this.betValue + ". So tien no hu: " + totalPrizes + " vin");
                                             }

                                             this.slotService.logNoHu(referenceId, this.gameName, username, this.betValue, linesStr, matrixStr, builderLinesWin.toString(), builderPrizesOnLine.toString(), totalPrizes, result, currentTimeStr);
                                        } else {
                                             this.fund -= totalPrizes;
                                             if (result == 0) {
                                                  if (totalPrizes >= (long)(this.betValue * 500)) {
                                                       result = 2;
                                                  } else {
                                                       result = 1;
                                                  }
                                             }
                                        }
                                   }

                                   long moneyExchange = totalPrizes - tienThuongX2;
                                   String des = "Quay Nữ điệp viên ";
                                   if (tienThuongX2 > 0L) {
                                        this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, Games.NU_DIEP_VIEN.getName(), "Quay Nữ điệp viên ", "Thưởng hũ X2", 0L, (Long)null, TransType.NO_VIPPOINT);
                                   }

                                   moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, Games.NU_DIEP_VIEN.getName(), "Quay Nữ điệp viên ", this.buildDescription(totalBetValue, totalPrizes, result), 0L, referenceId, TransType.END_TRANS);
                                   if (moneyRes != null && moneyRes.isSuccess()) {
                                        currentMoney = moneyRes.getCurrentMoney();
                                        if (this.moneyType == 1 && moneyExchange - totalBetValue >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                             this.broadcastMsgService.putMessage(Games.NU_DIEP_VIEN.getId(), username, moneyExchange - totalBetValue);
                                        }
                                   }

                                   linesWin = builderLinesWin.toString();
                                   prizesOnLine = builderPrizesOnLine.toString();
                                   msg.referenceId = referenceId;
                                   msg.matrix = NuDiepVienUtils.matrixToString(matrix);
                                   msg.linesWin = linesWin;
                                   msg.prize = totalPrizes;
                                   msg.haiSao = haiSao;

                                   try {
                                        this.slotService.logNuDiepVien(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                                        if (result == 3 || result == 4) {
                                             this.slotService.addTop(Games.NU_DIEP_VIEN.getName(), username, this.betValue, totalPrizes, currentTimeStr, result);
                                        }

                                        if (result == 3 || result == 2 || result == 4) {
                                             BigWinNDVMsg bigWinMsg = new BigWinNDVMsg();
                                             bigWinMsg.username = username;
                                             bigWinMsg.type = (byte)result;
                                             bigWinMsg.betValue = (short)this.betValue;
                                             bigWinMsg.totalPrizes = totalPrizes;
                                             bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
                                             this.module.sendMsgToAllUsers(bigWinMsg);
                                        }
                                   } catch (InterruptedException var47) {
                                   } catch (TimeoutException var48) {
                                   } catch (IOException var49) {
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
          SlotUtils.logNuDiepVien(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
          return msg;
     }

     public synchronized ResultNDVMsg playFreeDaily(String username) {
          String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          long refernceId = this.module.getNewReferenceId();
          short result = 0;
          String[] lineArr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20".split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          ResultNDVMsg msg = new ResultNDVMsg();
          boolean enoughPair = false;
          List awardsOnLines = new ArrayList();
          long totalPrizes = 0L;

          while(true) {
               while(true) {
                    label76:
                    while(!enoughPair) {
                         result = 0;
                         awardsOnLines.clear();
                         totalPrizes = 0L;
                         String linesWin = "";
                         String prizesOnLine = "";
                         String haiSao = "";
                         NDVItem[][] matrix = NuDiepVienUtils.generateMatrix();
                         String[] array = lineArr;
                         int length = lineArr.length;

                         for(int i = 0; i < length; ++i) {
                              String entry = array[i];
                              List awardList = new ArrayList();
                              Line line = NuDiepVienUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                              NuDiepVienUtils.calculateLine(line, awardList);
                              Iterator var27 = awardList.iterator();

                              while(var27.hasNext()) {
                                   NDVAward award = (NDVAward)var27.next();
                                   long moneyOnLine = 0L;
                                   if (award.getRatio() <= 0.0F) {
                                        continue label76;
                                   }

                                   moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                   AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                   awardsOnLines.add(aol);
                              }
                         }

                         StringBuilder builderLinesWin = new StringBuilder();
                         StringBuilder builderPrizesOnLine = new StringBuilder();
                         Iterator var40 = awardsOnLines.iterator();

                         while(var40.hasNext()) {
                              AwardsOnLine entry2 = (AwardsOnLine)var40.next();
                              if (entry2.getAward() == NDVAward.PENTA_NU_DIEP_VIEN) {
                                   continue label76;
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

                         if (result != 3 && this.fund - totalPrizes >= 0L && totalPrizes <= (long)ConfigGame.getIntValue("max_prize_free_daily", 2000)) {
                              enoughPair = true;
                              boolean updated = this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
                              if (!updated) {
                                   result = 103;
                              } else if (totalPrizes > 0L) {
                                   String des = "Quay Nữ Điệp Viên Free";
                                   MoneyResponse moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, "NuDiepVienVqFree", "Quay Nữ Điệp Viên Free", "Cược: 0, Thắng: " + totalPrizes, 0L, (Long)null, TransType.NO_VIPPOINT);
                                   if (moneyRes != null && moneyRes.isSuccess()) {
                                        currentMoney = moneyRes.getCurrentMoney();
                                   }
                              }

                              linesWin = builderLinesWin.toString();
                              prizesOnLine = builderPrizesOnLine.toString();
                              msg.referenceId = refernceId;
                              msg.matrix = NuDiepVienUtils.matrixToString(matrix);
                              msg.linesWin = linesWin;
                              msg.prize = totalPrizes;
                              msg.haiSao = "";

                              try {
                                   this.slotService.logNuDiepVien(refernceId, username, (long)this.betValue, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20", linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
                    SlotUtils.logNuDiepVien(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
                    return msg;
               }
          }
     }

     private MiniGameSlotResponse generatePickStars() {
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
                    totalMoney += 4 * this.betValue * totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.GOLD, 0));
                    break;
               case KEY:
                    ++numPicks;
                    ++totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    break;
               case BOX:
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betValue * totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
               }
          }

          Iterator var10 = gifts.iterator();

          while(var10.hasNext()) {
               PickStarGift pickStarGift = (PickStarGift)var10.next();
               if (responsePickStars.length() == 0) {
                    responsePickStars = pickStarGift.getName();
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

          ResultNDVMsg msg = null;
          NDVFreeDailyMsg freeDailyMsg = new NDVFreeDailyMsg();
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
               MinimizeResultNDVMsg miniMsg = new MinimizeResultNDVMsg();
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
                    Debug.trace(new Object[]{"NU DIEP VIEN: update fund error ", var6.getMessage()});
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
                    Debug.trace(new Object[]{"NU DIEP VIEN: update pot error ", var6.getMessage()});
               }

               UpdatePotNDVMsg msg = new UpdatePotNDVMsg();
               msg.value = this.pot;
               msg.x2 = (byte)(this.huX2 ? 1 : 0);
               this.sendMessageToRoom(msg);
          }

     }

     public void updatePot(User user) {
          UpdatePotNDVMsg msg = new UpdatePotNDVMsg();
          msg.value = this.pot;
          msg.x2 = (byte)(this.huX2 ? 1 : 0);
          SlotUtils.sendMessageToUser(msg, (User)user);
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
               NDVFreeDailyMsg freeDailyMsg = new NDVFreeDailyMsg();
               freeDailyMsg.remain = (byte)model.getRotateFree();
               SlotUtils.sendMessageToUser(freeDailyMsg, (User)user);
          } else {
               user.removeProperty("numFreeDaily");
          }

          if (result) {
               user.setProperty("MGROOM_NU_DIEP_VIEN_INFO", this);
          }

          return result;
     }
}
