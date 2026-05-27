package game.modules.slot.room;

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
import game.modules.slot.KhoBauModule;
import game.modules.slot.cmd.send.khobau.BigWinKhoBauMsg;
import game.modules.slot.cmd.send.khobau.ForceStopAutoPlayPokeGoMsg;
import game.modules.slot.cmd.send.khobau.KhoBauFreeDailyMsg;
import game.modules.slot.cmd.send.khobau.MinimizeResultKhoBauMsg;
import game.modules.slot.cmd.send.khobau.ResultSlotMsg;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;
import game.modules.slot.entities.slot.khobau.AwardsOnLine;
import game.modules.slot.entities.slot.khobau.KhoBauAward;
import game.modules.slot.entities.slot.khobau.KhoBauItem;
import game.modules.slot.entities.slot.khobau.KhoBauLines;
import game.modules.slot.entities.slot.khobau.Line;
import game.modules.slot.utils.KhoBauUtils;
import game.modules.slot.utils.SlotForceResultHelper;
import game.modules.slot.utils.SlotHouseEdge;
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

public class KhoBauRoom extends SlotRoom {
     private final Runnable gameLoopTask = new GameLoopTask();
     private KhoBauLines lines = new KhoBauLines();
     private long lastTimeUpdatePotToRoom = 0L;
     private long lastTimeUpdateFundToRoom = 0L;
     private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
     private List boxValues = new ArrayList();

     public KhoBauRoom(KhoBauModule module, byte id, String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
          super(id, name, betValue, moneyType, pot, fund, initPotValue);
          this.gameName = Games.KHO_BAU.getName();
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
               ForceStopAutoPlayPokeGoMsg msg = new ForceStopAutoPlayPokeGoMsg();
               SlotUtils.sendMessageToUser(msg, (User)user);
          }
     }

     public synchronized ResultSlotMsg play(String username, String linesStr) {
          long startTime = System.currentTimeMillis();
          String currentTimeStr = DateTimeUtils.getCurrentTime();
          long referenceId = this.module.getNewReferenceId();
          short result = 0;
          String[] lineArr = linesStr.split(",");
          long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
          UserCacheModel u = this.userService.getUser(username);
          long totalBetValue = (long)(lineArr.length * this.betValue);
          Debug.trace("KHO_BAU PLAY: user=" + username + " currentMoney=" + currentMoney + " totalBet=" + totalBetValue + " lines=" + lineArr.length + " betValue=" + this.betValue + " moneyType=" + this.moneyTypeStr + " userCache=" + (u != null ? "found" : "NULL"));
          ResultSlotMsg msg = new ResultSlotMsg();
          long fee;
          if (lineArr.length > 0 && !linesStr.isEmpty()) {
               if (totalBetValue > 0L) {
                    if (totalBetValue <= currentMoney) {
                         fee = totalBetValue * 2L / 100L;
                         MoneyResponse moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, Games.KHO_BAU.getName(), "Quay Kho Báu", "Đặt cược Kho Báu", fee, referenceId, TransType.START_TRANS);
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

                              label201:
                              while(true) {
                                   String linesWin;
                                   String prizesOnLine;
                                   String haiSao;
                                   String matrixStr;
                                   KhoBauItem[][] matrix;
                                   StringBuilder builderLinesWin;
                                   StringBuilder builderPrizesOnLine;
                                   label177:
                                   while(true) {
                                        while(true) {
                                             label149:
                                             while(true) {
                                                  if (enoughPair) {
                                                       break label201;
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
                                                       int soLanNoHu = ConfigGame.getIntValue("KhoBau_so_lan_no_hu");
                                                       if (soLanNoHu > 0 && this.fund > this.initPotValue * 2L) {
                                                            Random rd = new Random();
                                                            length = rd.nextInt(soLanNoHu);
                                                            if (length == 0) {
                                                                 forceNoHu = true;
                                                            }
                                                       }
                                                  }

                                                  // Check for admin-forced result (SUN-68/69)
                                                  int[][] forcedIds = SlotForceResultHelper.checkAndConsume("KHOBAU", username, 3, 5);
                                                  if (forcedIds != null) {
                                                       matrix = new KhoBauItem[3][5];
                                                       for (int fi = 0; fi < 3; fi++)
                                                            for (int fj = 0; fj < 5; fj++)
                                                                 matrix[fi][fj] = KhoBauItem.findItem((byte) forcedIds[fi][fj]);
                                                  } else if (forceNoHu) {
                                                       matrix = KhoBauUtils.generateMatrixNoHu(lineArr);
                                                  } else {
                                                       matrix = KhoBauUtils.generateMatrix();
                                                  }

                                                  String[] array = lineArr;
                                                  length = lineArr.length;

                                                  for(int i = 0; i < length; ++i) {
                                                       matrixStr = array[i];
                                                       List awardList = new ArrayList();
                                                       Line line = KhoBauUtils.getLine(this.lines, matrix, Integer.parseInt(matrixStr));
                                                       KhoBauUtils.calculateLine(line, awardList);
                                                       Iterator var42 = awardList.iterator();

                                                       while(var42.hasNext()) {
                                                            KhoBauAward award = (KhoBauAward)var42.next();
                                                            long moneyOnLine = 0L;
                                                            if (award.getRatio() > 0.0F) {
                                                                 moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
                                                            } else if (award != KhoBauAward.PENTA_POUCH) {
                                                                 MiniGameSlotResponse response = this.generatePickStars();
                                                                 moneyOnLine = response.getTotalPrize();
                                                                 haiSao = response.getPrizes();
                                                                 if (result != 3) {
                                                                      result = 5;
                                                                 }
                                                            } else {
                                                                 Iterator var46 = awardsOnLines.iterator();

                                                                 while(var46.hasNext()) {
                                                                      AwardsOnLine e = (AwardsOnLine)var46.next();
                                                                      if (e.getAward() == KhoBauAward.PENTA_POUCH) {
                                                                           continue label149;
                                                                      }
                                                                 }

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

                                                            AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
                                                            awardsOnLines.add(aol);
                                                       }
                                                  }

                                                  builderLinesWin = new StringBuilder();
                                                  builderPrizesOnLine = new StringBuilder();
                                                  Iterator var56 = awardsOnLines.iterator();

                                                  while(var56.hasNext()) {
                                                       AwardsOnLine entry2 = (AwardsOnLine)var56.next();
                                                       if ((entry2.getAward() == KhoBauAward.PENTA_POUCH || entry2.getAward() == KhoBauAward.QUADRA_POUCH || entry2.getAward() == KhoBauAward.TRIPLE_POUCH) && !u.isBot()) {
                                                            continue label149;
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
                                                       break label177;
                                                  } else if (this.fund - totalPrizes >= this.initPotValue * 2L || totalPrizes - totalBetValue < 0L) {
                                                       break label177;
                                                  }
                                             }
                                        }
                                   }

                                   // [Phase3b] RTP Engine: re-roll if force-lose fires (jackpot result==3,4 exempt)
                                   if (result != 3 && result != 4 && totalPrizes > 0L
                                           && SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_khobau")) {
                                       
                                       totalPrizes = 0;
                                       awardsOnLines.clear();
                                       builderLinesWin = new StringBuilder();
                                       builderPrizesOnLine = new StringBuilder();
                                       result = 0;
                                   }
                                   enoughPair = true;
                                   matrixStr = KhoBauUtils.matrixToString(matrix);
                                   // Phase 3b — pct-aware force-lose (runs AFTER fund-guard loop above).
                                   // Never touches jackpot wins (result==3) to avoid frustrating huge-prize deny.
                                   if (result != 3 && totalPrizes > 0L
                                           && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_khobau")) {
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
                                                  GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game Kho Bau phong " + this.betValue + ". So tien no hu: " + totalPrizes + " vin");
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
                                   String des = "Quay Kho Báu ";
                                   if (tienThuongX2 > 0L) {
                                        this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, Games.KHO_BAU.getName(), "Quay Kho Báu ", "Thưởng hũ X2", 0L, (Long)null, TransType.NO_VIPPOINT);
                                   }

                                   moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, Games.KHO_BAU.getName(), "Quay Kho Báu ", this.buildDescription(totalBetValue, totalPrizes, result), 0L, referenceId, TransType.END_TRANS);
                                   if (moneyRes != null && moneyRes.isSuccess()) {
                                        currentMoney = moneyRes.getCurrentMoney();
                                        if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                             this.broadcastMsgService.putMessage(Games.KHO_BAU.getId(), username, moneyExchange - totalBetValue);
                                        }
                                   }

                                   linesWin = builderLinesWin.toString();
                                   prizesOnLine = builderPrizesOnLine.toString();
                                   msg.referenceId = referenceId;
                                   msg.matrix = KhoBauUtils.matrixToString(matrix);
                                   msg.linesWin = linesWin;
                                   msg.prize = totalPrizes;
                                   msg.haiSao = haiSao;

                                   try {
                                        this.slotService.logKhoBau(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                                        if (result == 3 || result == 4) {
                                             this.slotService.addTop(Games.KHO_BAU.getName(), username, this.betValue, totalPrizes, currentTimeStr, result);
                                        }

                                        if (result == 3 || result == 2 || result == 4) {
                                             BigWinKhoBauMsg bigWinMsg = new BigWinKhoBauMsg();
                                             bigWinMsg.username = username;
                                             bigWinMsg.type = (byte)result;
                                             bigWinMsg.betValue = (short)this.betValue;
                                             bigWinMsg.totalPrizes = totalPrizes;
                                             bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
                                             this.module.sendMsgToAllUsers(bigWinMsg);
                                        }
                                   } catch (InterruptedException var48) {
                                   } catch (TimeoutException var49) {
                                   } catch (IOException var50) {
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
          SlotUtils.logKhoBau(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
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
                    KhoBauItem[][] matrix = KhoBauUtils.generateMatrix();
                    String[] array = lineArr;
                    int length = lineArr.length;

                    for(int i = 0; i < length; ++i) {
                         String entry = array[i];
                         List awardList = new ArrayList();
                         Line line = KhoBauUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                         KhoBauUtils.calculateLine(line, awardList);
                         Iterator var27 = awardList.iterator();

                         while(var27.hasNext()) {
                              KhoBauAward award = (KhoBauAward)var27.next();
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
                              String des = "Quay Kho Báu Free";
                              MoneyResponse moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, "KhoBauVqFree", "Quay Kho Báu Free", "Cược: 0, Thắng: " + totalPrizes, 0L, (Long)null, TransType.NO_VIPPOINT);
                              if (moneyRes != null && moneyRes.isSuccess()) {
                                   currentMoney = moneyRes.getCurrentMoney();
                              }
                         }

                         linesWin = builderLinesWin.toString();
                         prizesOnLine = builderPrizesOnLine.toString();
                         msg.referenceId = refernceId;
                         msg.matrix = KhoBauUtils.matrixToString(matrix);
                         msg.linesWin = linesWin;
                         msg.prize = totalPrizes;
                         msg.haiSao = "";

                         try {
                              this.slotService.logKhoBau(refernceId, username, (long)this.betValue, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20", linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
               SlotUtils.logKhoBau(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
               return msg;
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
                    // SUN-58: KEY ends the bonus game (lose tile)
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    numPicks = 0; // force exit loop
                    break;
               case BOX:
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betValue * totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
                    break;
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

          ResultSlotMsg msg = null;
          KhoBauFreeDailyMsg freeDailyMsg = new KhoBauFreeDailyMsg();
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
               MinimizeResultKhoBauMsg miniMsg = new MinimizeResultKhoBauMsg();
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
                    Debug.trace(new Object[]{"KHO BAU: update fund kho bau error ", var6.getMessage()});
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
               ((KhoBauModule)this.module).updatePot(this.id, this.pot, x2);
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
                    PlayListAutoUserTask task = new PlayListAutoUserTask(tmp);
                    this.executor.execute(task);
               }
          }

          usersPlay.clear();
     }

     protected void playListAuto(List users) {
          Iterator var2 = users.iterator();

          while(true) {
               while(var2.hasNext()) { AutoUser user = (AutoUser)var2.next();
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
               KhoBauFreeDailyMsg freeDailyMsg = new KhoBauFreeDailyMsg();
               freeDailyMsg.remain = (byte)model.getRotateFree();
               SlotUtils.sendMessageToUser(freeDailyMsg, (User)user);
          } else {
               user.removeProperty("numFreeDaily");
          }

          if (result) {
               user.setProperty("MGROOM_KHO_BAU_INFO", this);
          }

          return result;
     }
}
