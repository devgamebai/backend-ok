/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  com.google.gson.Gson
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuLiveServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.bson.Document
 */
package game.modules.minigame.room;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuLiveServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.description.TaiXiuDescription.TaiXiuDescriptionUtils;
import game.modules.minigame.cmd.rev.BetTaiXiuCmd;
import game.modules.minigame.cmd.send.txlive.BetInfoGameMsg;
import game.modules.minigame.cmd.send.txlive.BetTaiXiuLiveMsg;
import game.modules.minigame.cmd.send.txlive.FinishGameMsg;
import game.modules.minigame.cmd.send.txlive.GameInfoMsg;
import game.modules.minigame.cmd.send.txlive.RemainTimeGameMsg;
import game.modules.minigame.cmd.send.txlive.ResultGameMsg;
import game.modules.minigame.cmd.send.txlive.ResultTaiXiuLiveMsg;
import game.modules.minigame.cmd.send.txlive.StartGameMsg;
import game.modules.minigame.entities.MinigameConstant;
import game.modules.minigame.model.BetInfo;
import game.modules.minigame.room.MGRoom;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.bson.Document;

public class MGRoomTaiXiuLive
extends MGRoom {
    private long startTime = 0L;
    private short result = (short)-1;
    private MiniGameService mgService = new MiniGameServiceImpl();
    private UserService userService = new UserServiceImpl();
    private CacheService cacheService = new CacheServiceImpl();
    public List<TransactionTaiXiuDetail> listUserBet = new ArrayList<TransactionTaiXiuDetail>();
    public List<String> listResult = new ArrayList<String>();
    public int[] diceRs;
    public long betIndex = 0L;
    private Gson gson = new Gson();
    private String moneyTypeStr = "vin";
    private Long referenceId = 1L;
    private boolean enableBetting;
    private float tax = MinigameConstant.MINIGAME_TAX_TX;
    private TaiXiuLiveServiceImpl api = new TaiXiuLiveServiceImpl();

    public MGRoomTaiXiuLive(String name) {
        super(name);
    }

    public void startNewGame(long newReferenceId, long timeBet) {
        this.listUserBet.clear();
        this.referenceId = newReferenceId;
        this.enableBetting = true;
        StartGameMsg msg = new StartGameMsg();
        msg.referenceId = this.referenceId;
        msg.timeBet = timeBet;
        this.sendMessageToRoom(msg);
    }

    public void finish() {
        this.enableBetting = false;
        FinishGameMsg msg = new FinishGameMsg();
        this.sendMessageToRoom(msg);
    }

    public void updateRemainTime(long remainTime) {
        this.enableBetting = true;
        RemainTimeGameMsg msg = new RemainTimeGameMsg();
        msg.remainTime = remainTime;
        msg.referenceId = this.referenceId;
        this.sendMessageToRoom(msg);
    }

    public void result(short[] shorts) {
        this.enableBetting = false;
        ResultGameMsg msg = new ResultGameMsg();
        msg.currentMoney = 0L;
        msg.moneyWin = 0L;
        msg.dice1 = shorts[0];
        msg.dice2 = shorts[1];
        msg.dice3 = shorts[2];
        this.sendMessageToRoom(msg);
    }

    public void betInfo(BetInfo[] betInfos) {
        BetInfoGameMsg msg = new BetInfoGameMsg();
        msg.betInfos = betInfos;
        this.sendMessageToRoom(msg);
    }

    private boolean isUserBot(String nickName) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("usersSetWin");
        return userMap.containsKey(nickName) && (Boolean)userMap.get(nickName) != false;
    }

    public boolean isBot(String username) {
        // SUN-1xxx (2026-05-11): null-safe — see MGRoomTaiXiu.isBot for rationale.
        try {
            UserCacheModel model = this.userService.getUser(username);
            if (model == null) return false;
            return model.isBot();
        } catch (Throwable t) {
            return false;
        }
    }

    public void sendGameInfo(String linkLive, User user) {
        GameInfoMsg msg = new GameInfoMsg();
        msg.linkLive = linkLive;
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    public static short getMoneyType(int roomId) {
        return roomId == 0 ? (short)0 : 1;
    }

    public static String getKeyRoom(short moneyType) {
        return "" + moneyType + "_" + 2;
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        if (result) {
            user.setProperty("MGROOM_TAI_XIU_LIVE_INFO", this);
        }
        return result;
    }

    @Override
    public boolean quitRoom(User user) {
        boolean result = super.quitRoom(user);
        if (result) {
            user.removeProperty("MGROOM_TAI_XIU_LIVE_INFO");
        }
        return result;
    }

    public void saveResultTransaction(short[] shorts, int rs) throws Exception {
        this.api.saveResultTaiXiu(this.referenceId.longValue(), rs, (int)shorts[0], (int)shorts[1], (int)shorts[2], 0L, 0L, 0, 0, 0L, 0L, 0L, 0L, 1, 0L);
    }

    public void reward(int rs) throws IOException, InterruptedException, TimeoutException {
        for (TransactionTaiXiuDetail tx : this.listUserBet) {
            if (tx.betSide != rs) continue;
            tx.prize = tx.betValue * 2L;
            long fee = (long)(this.tax * (float)tx.prize / (200.0f - this.tax));
            MoneyResponse res2 = new MoneyResponse(false, "1001");
            res2 = this.userService.updateMoney(tx.username, tx.prize, "vin", "TaiXiuLive", Games.TAI_XIU_LIVE.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU_LIVE.getId() + "", this.referenceId, (byte)1), fee, Long.valueOf(this.referenceId), TransType.END_TRANS);
            if (!res2.isSuccess()) continue;
            this.api.updateTransactionTaiXiuDetail(tx);
            ResultTaiXiuLiveMsg msg = new ResultTaiXiuLiveMsg();
            msg.currentMoney = res2.getCurrentMoney();
            msg.totalMoney = tx.prize;
            this.sendMessageToUser((BaseMsg)msg, tx.username);
        }
    }

    public void bet(User user, DataCmd dataCmd) throws IOException, InterruptedException, TimeoutException {
        BetTaiXiuCmd cmd = new BetTaiXiuCmd(dataCmd);
        long currentMoney = this.userService.getMoneyUserCache(user.getName(), this.moneyTypeStr);
        MoneyResponse res = new MoneyResponse(false, "1001");
        int result = 2;
        if (!this.enableBetting) {
            result = 1;
        }
        if (cmd.betValue >= 100L) {
            TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId.longValue(), user.getId(), user.getName(), cmd.betValue, (int)cmd.betSide, (int)cmd.inputTime, 1);
            res = this.userService.updateMoney(user.getName(), -cmd.betValue, this.moneyTypeStr, "TaiXiuSicbo", Games.TAI_XIU_SICBO.getId() + "", TaiXiuDescriptionUtils.getTaiXiuBetDescription(Games.TAI_XIU_SICBO.getId() + "", this.referenceId, cmd.inputTime + "", cmd.betSide), 0L, Long.valueOf(this.referenceId), TransType.START_TRANS);
            if (res.isSuccess()) {
                boolean addNewTx = true;
                for (TransactionTaiXiuDetail tx : this.listUserBet) {
                    if (!tx.username.equals(user.getName())) continue;
                    tx.betValue += cmd.betValue;
                    this.api.updateTransactionTaiXiuDetail(tx);
                    addNewTx = false;
                }
                if (addNewTx) {
                    transTX.genTransactionCode();
                    this.api.saveTransactionTaiXiuDetail(transTX);
                    this.listUserBet.add(transTX);
                }
                currentMoney = res.getCurrentMoney();
                result = 0;
            } else {
                result = 1;
            }
        } else {
            result = 4;
        }
        BetTaiXiuLiveMsg msg = new BetTaiXiuLiveMsg();
        msg.Error = (byte)result;
        msg.currentMoney = currentMoney;
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    public void insertTokenLiveToDb(String token) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("token_live");
        Document doc = new Document();
        doc.append("token", token);
        col.insertOne(doc);
    }

    public void clearTokenLiveToDb() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("token_live");
        col.drop();
    }

    public String getTokenLiveFromDb() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("token_live");
        Document doc = (Document)col.find().first();
        if (doc == null) {
            return "";
        }
        return doc.getString("token");
    }
}

