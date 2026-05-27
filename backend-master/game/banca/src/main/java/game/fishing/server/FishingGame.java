

package game.fishing.server;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.entities.PlayerInfo;
import game.fishing.server.Object.*;
import game.fishing.server.cmd.*;
import game.fishing.server.config.GameConfig;
import game.fishing.server.services.FishingGameServiceImpl;
import game.modules.gameRoom.cmd.send.SendNoHu;
import game.modules.gameRoom.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FishingGame extends GameServer {
    private volatile int serverState = 0;
    public volatile boolean isRegisterLoop = false;
    public volatile int playerCount;
    public volatile int timer;
    public ThongTinThangLon thongTinNoHu = null;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private boolean isTrialGame;
    private ConcurrentHashMap<Long, Fish> listFish = new ConcurrentHashMap<>();
    private Set<FishingPlayer> players = new HashSet<>();
    private int[] nMultipleValue;
    private int[] nMultipleIndex = {0, 0, 0, 0, 0, 0};
    private int[][] nFishMultiple = {
            {1, 3},                  // ca gai vang nho
            {1, 3},                // Ca tram nho
            {1, 3},                    // Ca duoi vang nhiet doi
            {2, 5},                   // Ca vang mat to
            {2, 5},             //  Ca tim nhiet doi
            {2, 5},             //  Ca he
            {3, 6},               //  Ca boc
            {3, 6},               //Ca dau su tu
            {3, 6},                  // Ca den long
            {4, 7},             //  Rua
            {5, 8},                     // Ca thien than
            {6, 9},                  // Ca buom
            {7, 10},              //  Muc ong
            {8, 11},                 //  Ca kiem
            {10, 15},                   //  Ca quy
            {17, 24},                    //  Ca map trang lon
            {33, 50},                  //  Ca map vang lon
            {50, 80},                   // CHim canh cut doi
            {66, 90},           // Ca map khong lo
            {83, 100},                   //  Rong vang
            {100, 130},                    //  Ly quy
            {133, 150},                //  tien ca
            {167, 200},               //  na tra
            {210, 240},              // phi tieu ho
            {5, 5},               //  kho bau
            {3000, 3000}};             // kim nguyen bao

    private int cbBackIndex = 0;
    private int gameDifficult = FishingGameServiceImpl.getInstance().GAME_FISHING_FEE_TO_KILL_FISH;
    private int nBulletCoolingTime = FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_BULLET_COOLDOWN;
    private int nBulletVelocity = FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_BULLET_VELOCTITY;
    private ScheduledFuture<?> loopTask;
    private ScheduledFuture<?> bossLoopTask;
    private ScheduledFuture<?> fishLineLoopTask;
    private volatile int timerBoss = 0;
    private volatile static long currFishNum = 0;
    private int countMultiLineFish = 0;
    private volatile long[] gameBank = new long[6];
    private int maxFishInMap;
    private int id;
    private int maxBigFishInMap;
    private int maxEffectFishInMap;
    private int timeChangeScene;
    private int timeGenBoss;
    private long totalGameFee = 0;
    private long totalMoneyPlayersFire = 0;
    private long totalMoneyPlayersWon = 0;
    private volatile boolean isBossing = false;
    public Map<Integer, List<FishPath>> paths = new HashMap<>();
    private int typeOfGame;


    public JSONObject toJONObject() {
        try {
            JSONObject json = new JSONObject();
            JSONArray arr = new JSONArray();

            for (int i = 0; i < 6; ++i) {
                FishingPlayer gp = this.getPlayerByChair(i);
                arr.put(gp.toJSONObject());
            }

            json.put("players", arr);
            return json;
        } catch (Exception var5) {
            return null;
        }
    }

    public int getId() {
        return this.id;
    }

    public synchronized void onGameMessage(User user, DataCmd cmd) {

        System.out.println("onGameMessage " + cmd.getId());
        try {

            switch (cmd.getId()) {
                case CmdDefine.FISHING_LEAVE_GAME:
                    leaveGame(user, cmd);
                    break;
                case CmdDefine.FISHING_UPDATE_LIST_USERS:
                    updateListUser(user, cmd);
                    break;
                case CmdDefine.FISHING_CHANGE_GUN:
                    changeGun(user, cmd);
                    break;
                case CmdDefine.FISHING_PLAYER_FIRE:
                    playerFire(user, cmd);
                    break;
                case CmdDefine.FISH_CATCH:
                    fishCatch(user, cmd);
                    break;
                case CmdDefine.FISHING_SHOOT_SPECIAL:
                    playerShootSpecial(user, cmd);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            logger.error(user.getId() + "_" + cmd.getId(), e);
        }

    }


    public void broadcastGame(BaseMsg msg) {
        for (GamePlayer gp : this.getPlayers()) {
            User u = gp.getUser();
            if (u != null && u.isConnected()) {
                ExtensionUtility.getExtension().send(msg, u);
            }
        }
    }

    public void sendForUser(User user, BaseMsg msg) {
        if (user != null && user.isConnected()) {
            ExtensionUtility.getExtension().send(msg, user);
        }
    }

    public FishingPlayer getPlayerByUser(User user) {
        Integer chair = (Integer) user.getProperty("user_chair");
        if (chair != null) {
            FishingPlayer gp = this.getPlayerByChair(chair);
            return gp != null && gp.pInfo != null && gp.pInfo.nickName.equalsIgnoreCase(user.getName()) ? gp : null;
        } else {
            return null;
        }
    }

    private void leaveGame(User user, DataCmd cmd) {
        FishingPlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            GameRoomManager.instance().leaveRoom(user, this.room);
        }
    }


    private void updateListUser(User user, DataCmd cmd) {
        ResponseFishingUpdateListUsers res = new ResponseFishingUpdateListUsers();
        res.listPlayers = this.getListUserPlaying();
        sendForUser(user, res);
    }

    private Set<FishingPlayer> getListUserPlaying() {
        Set<FishingPlayer> ls = new HashSet<>();
        for (FishingPlayer fp : this.players) {
            if (fp.getPlayerStatus() != 0)
                ls.add(fp);
        }
        return ls;
    }

    private void changeGun(User user, DataCmd cmd) {
        RequestChangeGun req = new RequestChangeGun(cmd);
        req.unpackData();
        int gun = req.gunID;

        if (gun < 0 || gun > 5)
            gun = 0;

        int pos = this.userChangeGun(user, gun);

        ResponseUserChangeGun response = new ResponseUserChangeGun();
        response.wChairID = pos;
        response.nMultipleIndex = gun;

        broadcastGame(response);
    }

    private void playerFire(User user, DataCmd cmd) {
        RequestPlayerFire req = new RequestPlayerFire(cmd);
        req.unpackData();
        int m_ChairID = req.m_ChairID;
        int nMultipleIndex = req.nMultipleIndex;
        long m_fishIndex = req.m_fishIndex;
        int m_index = req.m_index;
        int posX = req.posX;
        int posY = req.posY;
        if (nMultipleIndex < 0 || nMultipleIndex > 5)
            nMultipleIndex = 0;
        //Debug.trace("Player " + user.getId() + " fire  bullet id: " + m_index);

        long curMoney = this.userFire(user.getId(), m_index, nMultipleIndex);
        if (curMoney == -1) {
            Debug.warn("Player " + user.getId() + " fire fail bullet id: " + m_index);
            return;
        }


        ResponsePlayerFire response = new ResponsePlayerFire();
        response.wChairID = m_ChairID;
        response.currMoney = curMoney;
        response.posX = posX;
        response.posY = posY;
        response.nTrackFishIndex = m_fishIndex;
        response.nBulletScore = this.getnMultipleValue()[nMultipleIndex];

        broadcastGame(response);
    }

    private void playerShootSpecial(User user, DataCmd cmd) {
        RequestPlayerFireSpecial req = new RequestPlayerFireSpecial(cmd);
        req.unpackData();
        int specialID = req.specialID;
        int wChairID = req.m_ChairID;
        int angle = req.angle;
        int endPosX = req.endPosX;
        int endPosY = req.endPosY;

        ResponsePlayerShootSpecial response = new ResponsePlayerShootSpecial();
        response.specialID = specialID;
        response.wChairID = wChairID;
        response.angle = angle;
        response.endPosX = endPosX;
        response.endPosY = endPosY;
        broadcastGame(response);
    }

    private void fishCatch(User user, DataCmd cmd) {
        RequestFishCatch req = new RequestFishCatch(cmd);
        req.unpackData();
        int bulletId = req.m_index;
        int m_nMultipleIndex = req.m_nMultipleIndex;
        long[] fishList = req.listFishCatch;

        FishingPlayer fUser = null;

        for (FishingPlayer usr : this.getPlayers()) {
            if (usr.getUser().getId() == user.getId()) {
                fUser = usr;
                break;
            }
        }
        if (fUser == null) return;
        boolean hasSupply = false;
        boolean hasStayFish = false;
        boolean hasBigBang = false;
        List<FishCatchObject> checkedFishList = new ArrayList<>();
        if (bulletId > BulletEffect.Other.getType()) { // normal fire
            Bullet firedBullet = fUser.getBulletByID(bulletId);
            if (firedBullet == null || firedBullet.getMutipleIndex() != m_nMultipleIndex) return;
            checkedFishList = this.checkFishCollision(fishList, fUser, bulletId, m_nMultipleIndex);
        } else { // special fire bulletId = special bullet type
            if (bulletId != BulletEffect.Frozen.getType() && bulletId < BulletEffect.Boom.getType()) return;
            if (bulletId == BulletEffect.Other.getType() || bulletId == BulletEffect.Frozen.getType() || fUser.isHasSpecialGun()) {
                checkedFishList = this.checkFishSpecialFire(fishList, fUser, m_nMultipleIndex, bulletId);
                if (bulletId != BulletEffect.Other.getType())
                    fUser.setHasSpecialGun(false);
            }
        }


        if (checkedFishList.size() == 0)
            return;


        for (FishCatchObject fish : checkedFishList) {
            if (fish.nFishType == FishType.KhoBau.getType())
                hasSupply = true;
            if (fish.nFishType == FishType.TienCa.getType() && !this.isBossing())
                hasStayFish = true;
            if (fish.nFishType == FishType.CaBoom.getType() || fish.nFishType == FishType.NaTra.getType())
                hasBigBang = true;
        }


        ResponseFishCatch response = new ResponseFishCatch();
        response.fishArr = new ArrayList<>(checkedFishList);
        response.player = fUser;
        response.m_nMultipleValue = this.getnMultipleValue()[m_nMultipleIndex];
        response.m_nMultipleIndex = m_nMultipleIndex;
        broadcastGame(response);

        if (hasSupply) {
            int supplyType = MyUtils.getInstances().randomWithRange(SupplyType.EST_Gold.getType(), SupplyType.EST_NULL.getType());
            if (supplyType == SupplyType.EST_Electric.getType()) {
                if (this.cannotGenSpecialGun(m_nMultipleIndex))
                    supplyType = SupplyType.EST_Speed.getType();
            } else if (supplyType == SupplyType.EST_Laser.getType()) {
                if (this.cannotGenSpecialGun(m_nMultipleIndex))
                    supplyType = SupplyType.EST_Bignet.getType();
            } else if (supplyType == SupplyType.EST_Bomb.getType()) {
                if (this.cannotGenSpecialGun(m_nMultipleIndex))
                    supplyType = SupplyType.EST_Speed.getType();
            } else if (supplyType == SupplyType.EST_YuanBao.getType()) {
                if (!FishingGameServiceImpl.getInstance().canGenJackpot(this))
                    supplyType = SupplyType.EST_Gold.getType();
            } else if (supplyType == SupplyType.EST_Gold.getType()) {
                if (!this.canPlayerGetGoldReward(fUser, m_nMultipleIndex))
                    supplyType = SupplyType.EST_NULL.getType();
            } else if (supplyType == SupplyType.EST_Cold.getType()) {
                if (this.isBossing())
                    supplyType = SupplyType.EST_NULL.getType();
            }

            fUser.setNumOfShootToReciveSupply(0);
            fUser.setHasSpecialGun(supplyType == SupplyType.EST_Laser.getType() ||
                    supplyType == SupplyType.EST_Electric.getType() ||
                    supplyType == SupplyType.EST_Bomb.getType());

            ResponseFishingSuplyGame response2 = new ResponseFishingSuplyGame();
            response2.wChairID = fUser.getwChairID();
            response2.lSupplyCount = FishingGameServiceImpl.getInstance().GAME_FISHING_SUPPLY_TIME;
            response2.nSupplyType = supplyType;
            broadcastGame(response2);
            Debug.trace("Gen Supply " + supplyType + " for " + fUser.getId());
            if (supplyType == SupplyType.EST_Cold.getType())
                stayAllFish();
            else if (supplyType == SupplyType.EST_YuanBao.getType()) {
                List<FishPath> path = this.paths.get(9);
                TagBezierPoint[] arr = new TagBezierPoint[1];
                arr[0] = path.get(0).getPoint();
                this.genSpecialFishWithTypeAndPos(FishType.JackPot.getType(), arr);
                if (this.getTypeOfGame() == 1)
                    Debug.warn("Gen JACKPOT Normal in: " + this.getId() + " current: " + FishingGameServiceImpl.getInstance().getNormalJackpotHP());
                else
                    Debug.warn("Gen JACKPOT Vip in: " + this.getId() + " current: " + FishingGameServiceImpl.getInstance().getVipJackpotHP());
            }
        }
        if (hasStayFish) {
            stayAllFish();
            Debug.trace("Stay Fish " + "by " + fUser.getId());
        }

        if (hasBigBang) {
            Debug.trace("BigBang " + "by " + fUser.getId());
            List<FishCatchObject> checkedFishList2 = this.checkListFishOnBigBang(fUser, m_nMultipleIndex);
            ResponseFishCatch response2 = new ResponseFishCatch();
            response2.fishArr = new ArrayList<>(checkedFishList2);
            response2.player = fUser;
            response2.m_nMultipleValue = this.getnMultipleValue()[m_nMultipleIndex];
            response2.m_nMultipleIndex = m_nMultipleIndex;
            broadcastGame(response2);
        }

    }

    private void stayAllFish() {
        ArrayList<Long> listStayFish = new ArrayList<>();
        for (Fish f : this.getListFish()) {
            if (f != null && !f.isDead()) {
                f.stayFish(FishingGameServiceImpl.getInstance().GAME_FISHING_STAY_FISH_TIME);
                listStayFish.add(f.getIndex());
            }
        }
        ResponseStayFish response = new ResponseStayFish();
        response.listStayFish = listStayFish;
        response.nStayTime = FishingGameServiceImpl.getInstance().GAME_FISHING_STAY_FISH_TIME;
        broadcastGame(response);
    }

    public void sendToAllUser(BaseMsg msg) {
        for (Object r : GameRoomManager.instance().allGameRooms.values()) {
            GameRoom room = (GameRoom) r;
            for (Object u : room.userManager.values()) {
                User usr = (User) u;
                sendForUser(usr, msg);
            }
        }

    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.typeOfGame = ro.setting.rule;
        this.isTrialGame = this.typeOfGame == 0;
        this.id = GameRoomIdGenerator.instance().getId();
        this.initGame();
        for (int i = 0; i < 6; ++i) {
            FishingPlayer gp = new FishingPlayer();
            gp.chair = i;
            this.players.add(gp);
        }

    }

    public FishingPlayer getPlayerByChair(int i) {
        if (i >= 0 && i < 6) {
            for (FishingPlayer fp : this.players) {
                if (fp.chair == i)
                    return fp;
            }

        }
        return null;
    }

    public synchronized void onGameUserExit(User user) {
        System.out.println("onGameUserExit");
        Integer chair = (Integer) user.getProperty("user_chair");
        if (chair != null) {
            FishingPlayer gp = this.getPlayerByChair(chair);
            if (gp != null) {
                boolean disconnect = user.isConnected();
                this.removePlayerAtChair(chair, !disconnect);
                if (this.playerCount == 0) {
                    this.destroy();
                }
            }
        }
    }

    private synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        if (this.checkPlayerChair(chair)) {
            FishingPlayer gp = this.getPlayerByChair(chair);
            if (gp == null) return;
            this.notifyUserExit(gp, disconnect);
            if (gp.user != null) {
                gp.user.removeProperty("user_chair");
                gp.user.removeProperty("GAME_ROOM");
                gp.user.removeProperty("GAME_MONEY_INFO");
            }
            if (gp.pInfo != null) {
                gp.pInfo.setIsHold(false);

            }
            gp.user = null;
            gp.pInfo = null;
            if (gp.gameMoneyInfo != null) {
                ListGameMoneyInfo.instance().removeGameMoneyInfo(gp.gameMoneyInfo, this.room.getId());
            }
            gp.gameMoneyInfo = null;
            gp.setPlayerStatus(0);
            --this.playerCount;
        }
    }

    private void notifyUserExit(FishingPlayer player, boolean disconnect) {
        if (player.pInfo != null) {
            player.pInfo.setIsHold(false);
            ResponseFishingPlayerLeave res = new ResponseFishingPlayerLeave();
            res.player = player;
            broadcastGame(res);
        }

    }

    public boolean checkPlayerChair(int chair) {
        return chair >= 0 && chair < 6;
    }

    public synchronized void onGameUserDis(User user) {
        System.out.println("onGameUserDis");

        Integer chair = (Integer) user.getProperty("user_chair");
        if (chair != null) {
            FishingPlayer gp = this.getPlayerByChair(chair);
            if (gp != null) {
                boolean disconnect = user.isConnected();
                this.removePlayerAtChair(chair, !disconnect);
                GameRoomManager.instance().leaveRoom(user, this.room);
                if (this.playerCount == 0) {
                    this.destroy();
                }
            }
        }
    }

    public synchronized void onGameUserReturn(User user) {
        System.out.println("onGameUserReturn");

    }

    public synchronized void onGameUserEnter(User user) {
        System.out.println("onGameUserEnter");

        if (user != null) {
            PlayerInfo pInfo = PlayerInfo.getInfo(user);
            if (pInfo != null) {
                GameMoneyInfo moneyInfo = (GameMoneyInfo) user.getProperty("GAME_MONEY_INFO");
                if (moneyInfo != null) {
                    List<FishingPlayer> list = new ArrayList<>(this.players);
                    for (int i = 0; i < 6; ++i) {
                        FishingPlayer gp = list.get(i);
                        if (gp.getPlayerStatus() == 0) {
                            if (this.serverState == 0) {
                                gp.setPlayerStatus(2);
                            } else {
                                gp.setPlayerStatus(1);
                            }
                            gp.takeChair(user, pInfo, moneyInfo, this.isTrialGame);
                            gp.setGame(this);
                            ++this.playerCount;
                            if (this.playerCount == 1) {
                                this.startLoopFish();
                            }
                            this.nMultipleIndex[i] = 0;
                            if (this.getListUserPlaying().size() < 3)
                                gameDifficult = FishingGameServiceImpl.getInstance().GAME_FISHING_FEE_TO_KILL_FISH;
                            else if (this.getListUserPlaying().size() < 5)
                                gameDifficult = FishingGameServiceImpl.getInstance().GAME_FISHING_FEE_TO_KILL_FISH + 1;
                            else
                                gameDifficult = FishingGameServiceImpl.getInstance().GAME_FISHING_FEE_TO_KILL_FISH + 2;
                            this.notifyUserEnter(gp);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void notifyUserEnter(FishingPlayer fishingPlayer) {
        User user = fishingPlayer.getUser();
        if (user != null) {
            fishingPlayer.timeJoinRoom = System.currentTimeMillis();
            ResponsePlayerJoinFishingGame res = new ResponsePlayerJoinFishingGame();
            res.player = fishingPlayer;
            this.sendMsgExceptMe(res, user);
            this.notifyJoinRoomSuccess(fishingPlayer);
        }
    }

    public void notifyJoinRoomSuccess(FishingPlayer gamePlayer) {
        ResponseFishingGameInfo responseGameInfo = new ResponseFishingGameInfo();
        responseGameInfo.game = this;
        responseGameInfo.player = gamePlayer;
        this.send(responseGameInfo, gamePlayer.getUser());
    }

    public synchronized void onNoHu(ThongTinThangLon info) {
        this.thongTinNoHu = info;
    }

    public void notifyNoHu() {
        try {
            if (this.thongTinNoHu != null) {
                for (int i = 0; i < 4; ++i) {
                    FishingPlayer gp = this.getPlayerByChair(i);
                    if (gp.gameMoneyInfo.sessionId.equalsIgnoreCase(this.thongTinNoHu.moneySessionId) && gp.gameMoneyInfo.nickName.equalsIgnoreCase(this.thongTinNoHu.nickName)) {
                        gp.gameMoneyInfo.currentMoney = this.thongTinNoHu.currentMoney;
                        break;
                    }
                }

                SendNoHu msg = new SendNoHu();
                msg.info = this.thongTinNoHu;
                Iterator var11 = this.room.userManager.entrySet().iterator();

                while (var11.hasNext()) {
                    Entry<String, User> entry = (Entry) var11.next();
                    User u = (User) entry.getValue();
                    if (u != null) {
                        this.send(msg, u);
                    }
                }
            }
        } catch (Exception var8) {
            CommonHandle.writeErrLog(var8);
        } finally {
            this.thongTinNoHu = null;
        }

    }

    public void choNoHu(String nickName) {


    }


    public void init() {
        if (!this.isRegisterLoop) {
            this.startLoopFish();
            this.isRegisterLoop = true;
        }
    }

    public void destroy() {
        loopTask.cancel(false);
        if (bossLoopTask != null)
            bossLoopTask.cancel(false);
        this.isRegisterLoop = false;
        timer = 0;
        timerBoss = 0;
        FishingGameServiceImpl.getInstance().saveFishingJackpot();
    }

    public GameRoom getRoom() {
        return this.room;
    }

    public void setRoom(GameRoom room) {
        this.room = room;
    }

    public FishingGame() {
    }

    public int getTypeOfGame() {
        return typeOfGame;
    }

    public boolean isBossing() {
        return isBossing;
    }

    public long getTotalGameFee() {
        return totalGameFee;
    }

    public long[] getGameBank() {
        return gameBank;
    }

    public long getTotalGameBank() {
        long totalGameBank = 0;
        for (int i = 0; i < gameBank.length; i++)
            totalGameBank += gameBank[i];

        return totalGameBank;
    }

    public long getTotalMoneyPlayersFire() {
        return totalMoneyPlayersFire;
    }

    public long getTotalMoneyPlayersWon() {
        return totalMoneyPlayersWon;
    }

    private void initBossPath() {
        List<FishPath> list = new LinkedList<>();
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, -150), new CPoint(360, -150), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(360, -150), new CPoint(20, -150), new CPoint(360, -200), new CPoint(20, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, -150), new CPoint(360, -150), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(360, -150), new CPoint(20, -150), new CPoint(360, -200), new CPoint(20, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, -150), new CPoint(360, -150), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        paths.put(0, new LinkedList<>(list)); // u nguoc
        list.clear();


        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, -150), new CPoint(740, -150), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(740, -150), new CPoint(380, -150), new CPoint(740, -200), new CPoint(380, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, -150), new CPoint(740, -150), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(740, -150), new CPoint(380, -150), new CPoint(740, -200), new CPoint(380, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, -150), new CPoint(740, -150), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        paths.put(1, new LinkedList<>(list));
        list.clear();

        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, -150), new CPoint(1120, -150), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(1120, -150), new CPoint(760, -150), new CPoint(1120, -200), new CPoint(760, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, -150), new CPoint(1120, -150), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(1120, -150), new CPoint(760, -150), new CPoint(1120, -200), new CPoint(760, -200), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, -150), new CPoint(1120, -150), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        paths.put(2, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, 790), new CPoint(360, 790), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(360, 790), new CPoint(20, 790), new CPoint(360, 840), new CPoint(20, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, 790), new CPoint(360, 790), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(360, 790), new CPoint(20, 790), new CPoint(360, 840), new CPoint(20, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(20, 790), new CPoint(360, 790), new CPoint(20, 360), new CPoint(360, 360), 20000)));
        paths.put(3, new LinkedList<>(list));
        list.clear();

        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, 790), new CPoint(740, 790), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(740, 790), new CPoint(380, 790), new CPoint(740, 840), new CPoint(380, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, 790), new CPoint(740, 790), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(740, 790), new CPoint(380, 790), new CPoint(740, 840), new CPoint(380, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(380, 790), new CPoint(740, 790), new CPoint(380, 360), new CPoint(740, 360), 20000)));
        paths.put(4, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, 790), new CPoint(1120, 790), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(1120, 790), new CPoint(760, 790), new CPoint(1120, 840), new CPoint(760, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, 790), new CPoint(1120, 790), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(1120, 790), new CPoint(760, 790), new CPoint(1120, 840), new CPoint(760, 840), 1000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(760, 790), new CPoint(1120, 790), new CPoint(760, 360), new CPoint(1120, 360), 20000)));
        paths.put(5, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(6, new TagBezierPoint(new CPoint(-168, -244), new CPoint(208, -717), new CPoint(73, 100), new CPoint(643, -260), 10000)));
        list.add(new FishPath(6, new TagBezierPoint(new CPoint(208, -717), new CPoint(-617, 104), new CPoint(-314, -1142), new CPoint(-1021, -643), 10000)));
        list.add(new FishPath(6, new TagBezierPoint(new CPoint(-617, 104), new CPoint(795, -282), new CPoint(-226, 634), new CPoint(608, 460), 15000)));
        list.add(new FishPath(6, new TagBezierPoint(new CPoint(795, -282), new CPoint(100, -1278), new CPoint(892, -721), new CPoint(621, -1171), 10000)));
        list.add(new FishPath(6, new TagBezierPoint(new CPoint(100, -1278), new CPoint(-1192, -142), new CPoint(-628, -1407), new CPoint(-1271, -885), 10000)));
        list.add(new FishPath(6, new TagBezierPoint(new CPoint(-1192, -142), new CPoint(1328, -321), new CPoint(-900, 1378), new CPoint(1114, 1321), 10000)));
        paths.put(6, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(5, new TagBezierPoint(new CPoint(-305, 400), new CPoint(242, 400), new CPoint(-106, 400), new CPoint(81, 400), 7000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(242, 400), new CPoint(-62, 400), new CPoint(153, 400), new CPoint(65, 400), 5000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(-62, 400), new CPoint(387, 400), new CPoint(93, 400), new CPoint(246, 400), 5000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(387, 400), new CPoint(59, 400), new CPoint(296, 400), new CPoint(168, 400), 5000)));
        list.add(new FishPath(5, new TagBezierPoint(new CPoint(59, 400), new CPoint(1593, 400), new CPoint(459, 400), new CPoint(971, 400), 15000)));
        paths.put(7, new LinkedList<>(list));
        list.clear();

        list.add(new FishPath(1, new TagBezierPoint(new CPoint(-305, 400), new CPoint(1585, 400), new CPoint(340, 400), new CPoint(942, 400), 25000)));
        paths.put(8, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(1, new TagBezierPoint(new CPoint(1236, 360), new CPoint(-100, 360), new CPoint(942, 360), new CPoint(340, 360), 40000)));
        paths.put(9, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(2, new TagBezierPoint(new CPoint(-50, 400), new CPoint(638, 400), new CPoint(235, 1200), new CPoint(235, -400), 15000)));
        list.add(new FishPath(2, new TagBezierPoint(new CPoint(638, 400), new CPoint(1330, 378), new CPoint(1045, 1200), new CPoint(1045, -400), 15000)));
        paths.put(10, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(2, new TagBezierPoint(new CPoint(-50, 400), new CPoint(638, 400), new CPoint(235, -400), new CPoint(235, 1200), 15000)));
        list.add(new FishPath(2, new TagBezierPoint(new CPoint(638, 400), new CPoint(1330, 378), new CPoint(1045, -400), new CPoint(1045, 1200), 15000)));
        paths.put(11, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(4, new TagBezierPoint(new CPoint(235, -50), new CPoint(235, 200), new CPoint(-250, 150), new CPoint(720, 0), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(235, 200), new CPoint(235, 450), new CPoint(-250, 400), new CPoint(720, 250), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(235, 450), new CPoint(235, 700), new CPoint(-250, 650), new CPoint(720, 500), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(235, 700), new CPoint(235, 950), new CPoint(-250, 900), new CPoint(720, 750), 7000)));
        paths.put(12, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(4, new TagBezierPoint(new CPoint(640, -50), new CPoint(640, 200), new CPoint(155, 150), new CPoint(1125, 0), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(640, 200), new CPoint(640, 450), new CPoint(155, 400), new CPoint(1125, 250), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(640, 450), new CPoint(640, 700), new CPoint(155, 650), new CPoint(1125, 500), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(640, 700), new CPoint(640, 950), new CPoint(155, 900), new CPoint(1125, 750), 7000)));
        paths.put(13, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(4, new TagBezierPoint(new CPoint(1045, -50), new CPoint(1045, 200), new CPoint(560, 150), new CPoint(1530, 0), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(1045, 200), new CPoint(1045, 450), new CPoint(560, 400), new CPoint(1530, 250), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(1045, 450), new CPoint(1045, 700), new CPoint(560, 650), new CPoint(1530, 500), 7000)));
        list.add(new FishPath(4, new TagBezierPoint(new CPoint(1045, 700), new CPoint(1045, 950), new CPoint(560, 900), new CPoint(1530, 750), 7000)));
        paths.put(14, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(1, new TagBezierPoint(new CPoint(0, 0), new CPoint(0, 10), new CPoint(0, 2), new CPoint(0, 6), 45000)));
        paths.put(15, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, 750), new CPoint(0, 500), new CPoint(0, 500), new CPoint(0, 500), 3000)));
        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, 500), new CPoint(0, 500), new CPoint(0, 500), new CPoint(0, 500), 50000)));
        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, 500), new CPoint(0, -110), new CPoint(0, 100), new CPoint(0, 100), 10000)));
        paths.put(16, new LinkedList<>(list));
        list.clear();


        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, -110), new CPoint(0, 140), new CPoint(0, 140), new CPoint(0, 140), 3000)));
        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, 140), new CPoint(0, 140), new CPoint(0, 140), new CPoint(0, 140), 50000)));
        list.add(new FishPath(3, new TagBezierPoint(new CPoint(0, 140), new CPoint(0, 750), new CPoint(0, 540), new CPoint(0, 540), 10000)));
        paths.put(17, new LinkedList<>(list));
        list.clear();

        list.add(new FishPath(1, new TagBezierPoint(new CPoint(1300, 220), new CPoint(-150, 220), new CPoint(768, 220), new CPoint(368, 220), 30000)));
        paths.put(18, new LinkedList<>(list));
        list.clear();

        //cho boss 1
        list.add(new FishPath(1, new TagBezierPoint(new CPoint(1286, 360), new CPoint(836, 360), new CPoint(1036, 360), new CPoint(936, 360), 15000)));
        list.add(new FishPath(1, new TagBezierPoint(new CPoint(836, 360), new CPoint(800, 360), new CPoint(824, 360), new CPoint(812, 360), 20000)));
        list.add(new FishPath(1, new TagBezierPoint(new CPoint(800, 360), new CPoint(-150, 360), new CPoint(300, 360), new CPoint(0, 360), 15000)));
        paths.put(19, new LinkedList<>(list));
        list.clear();

        list.add(new FishPath(1, new TagBezierPoint(new CPoint(-150, 360), new CPoint(300, 360), new CPoint(100, 360), new CPoint(200, 360), 15000)));
        list.add(new FishPath(1, new TagBezierPoint(new CPoint(300, 360), new CPoint(336, 360), new CPoint(312, 360), new CPoint(324, 360), 20000)));
        list.add(new FishPath(1, new TagBezierPoint(new CPoint(336, 360), new CPoint(1286, 360), new CPoint(836, 360), new CPoint(1280, 360), 15000)));
        paths.put(20, new LinkedList<>(list));
        list.clear();
    }

    public boolean isTrialGame() {
        return isTrialGame;
    }

    private void initGame() {
        initBossPath();
        if (isTrialGame) {
            maxFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_FISH_EACH_PLAYER_TRIAL;
            maxBigFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_BIG_FISH_IN_MAP + 1;
            maxEffectFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_EFFECT_FISH_IN_MAP + 1;
            timeChangeScene = FishingGameServiceImpl.getInstance().GAME_FISHING_TIME_CHANGE_SCENE_TRIAL;
            timeGenBoss = timeChangeScene + 8;
            Arrays.fill(gameBank, FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_GAMEBANK_TRIAL);
            nBulletCoolingTime = FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_BULLET_COOLDOWN - 50;
            nMultipleValue = GameConfig.FISHING_GAME_BULLETS_NORMAL;
        } else {
            maxFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_FISH_EACH_PLAYER;
            maxBigFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_BIG_FISH_IN_MAP;
            maxEffectFishInMap = FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_EFFECT_FISH_IN_MAP;
            timeChangeScene = FishingGameServiceImpl.getInstance().GAME_FISHING_TIME_CHANGE_SCENE;
            timeGenBoss = timeChangeScene + 8;
            Arrays.fill(gameBank, FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_GAMEBANK);
            nBulletCoolingTime = FishingGameServiceImpl.getInstance().GAME_FISHING_DEFAULT_BULLET_COOLDOWN;

            if (typeOfGame == 1) {
                nMultipleValue = GameConfig.FISHING_GAME_BULLETS_NORMAL;
            } else {
                nMultipleValue = GameConfig.FISHING_GAME_BULLETS_VIP;
            }

        }

    }

    public int getGameDifficult() {
        return isTrialGame ? 0 : gameDifficult;
    }

    private void updateGameFee(double value) {
        this.totalGameFee += value;
    }

    private void updateTotalMoneyFireInGame(double value) {
        this.totalMoneyPlayersFire += value;
    }

    private void updateTotalMoneyPlayerWon(double value) {
        this.totalMoneyPlayersWon += value;
    }

    public void updateGameBank(int index, double value) {
        this.gameBank[index] += value;
    }

    private void loop() {

        System.out.println("Fish -- " + timer);
        if (timer % 5 == 0) {
            ResponseUpdateFishingGame res = new ResponseUpdateFishingGame();
            for (Fish f : listFish.values()) {
                if (f != null && !f.isDead())
                    res.listFish.add(f.getIndex());
            }

            res.nMultipleIndex = nMultipleIndex;
            this.broadcastGame(res);

        }
        if (timer < timeChangeScene) {
            if (canGenFish()) {
                if (timer > 0 && timer % 19 == 0)
                    genFlockFish();
                else if (timer > 0 && timer % 23 == 0)
                    genMultiFishWithLine();
                else if (timer > 0 && timer % 30 == 0) {
                    if (canGenBigFish())
                        genSingleBigFish();
                    else {
                        genSingleFishNormal();//x2 cá bé
                        genSingleFishNormal();
                    }
                } else if (timer > 0 && timer % 31 == 0) {
                    if (canGenEffectFish())
                        this.genEffectFish();
                    else {
                        genSingleFishNormal();//x2 cá bé
                        genSingleFishNormal();
                    }

                } else {
                    genSingleFishNormal(); //x2 cá bé
                    genSingleFishNormal();
                }


                long totalGameBank = 0;

                for (int i = 0; i < gameBank.length; i++)
                    totalGameBank += gameBank[i];

                if (this.getListUserPlaying().size() > 2 && totalGameBank > this.getListUserPlaying().size() * FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_GAMEBANK_TO_GEN_BIG_FISH_PER_PLAYER && timer % 10 == 0) {
                    if (canGenBigFish())
                        genSingleBigFish();
                    else if (canGenEffectFish())
                        this.genEffectFish();
                    else {
                        genSingleFishNormal();//x2 cá bé
                        genSingleFishNormal();
                    }
                }
            }
            if (timer > 0 && timer % 110 == 0 && FishingGameServiceImpl.getInstance().canGenJackpot(this)) {
                List<FishPath> path = paths.get(9);
                TagBezierPoint[] arr = new TagBezierPoint[1];
                arr[0] = path.get(0).getPoint();
                genSpecialFishWithTypeAndPos(FishType.JackPot.getType(), arr);
                if (typeOfGame == 1)
                    Debug.trace("Gen JACKPOT Normal in: " + id + " current: " + FishingGameServiceImpl.getInstance().getNormalJackpotHP());
                else
                    Debug.trace("Gen JACKPOT Vip in: " + id + " current: " + FishingGameServiceImpl.getInstance().getVipJackpotHP());
            }
        } else if (timer == timeChangeScene) {
            changeScene();
        } else if (timer == timeGenBoss) {
            if (this.getListUserPlaying().size() < 2) {
                timer = 0;
                Debug.trace("Cannot gen boss player < 2 ");
                return;
            }
            isBossing = true;
            int rand = MyUtils.getInstances().randomWithRange(1, 11);
            Debug.trace("Gen Boss_" + rand);
            switch (rand) {
                case 1:
                    genBoss_1();
                    break;
                case 2:
                    genBoss_2();
                    break;
                case 3:
                    genBoss_3();
                    break;
                case 4:
                    genBoss_4();
                    break;
                case 5:
                    genBoss_5();
                    break;
                case 6:
                    genBoss_6();
                    break;
                default:
                    genSpecialBoss(rand);
                    break;
            }
        }
        timer++;

    }

    public void startLoopFish() {
        timer = 0;
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    loop();
                } catch (Exception e) {
                    Debug.warn("Fishing", e);
                    destroy();
                }
            }
        };
        loopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 5, 1, TimeUnit.SECONDS);
    }

    private void bossLoop_1() {

        if (timerBoss == 70) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss == 10) {

            long currTime = Calendar.getInstance().getTimeInMillis();
            List<FishData> data = new LinkedList<>();
            for (int k = 0; k < 2; k++) {
                int bezierCount = paths.get(19 + k).size();
                List<FishPath> path = paths.get(19 + k);
                TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
                for (int j = 0; j < bezierCount; j++) {
                    arr[j] = path.get(j).getPoint();
                }
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(FishType.RongVang.getType());
                fishData.setnFishState(0);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(0, 0));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }
        if (timerBoss < 22) {

            int type = MyUtils.getInstances().randomWithRange(FishType.CaDuoiVangNho.getType(), FishType.CaDuoiVangNho.getType());
            long currTime = Calendar.getInstance().getTimeInMillis();
            List<FishData> data = new LinkedList<>();
            for (int k = 0; k < 6; k++) {
                int bezierCount = paths.get(k).size();
                List<FishPath> path = paths.get(k);
                TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
                for (int j = 0; j < bezierCount; j++) {
                    arr[j] = path.get(j).getPoint();
                }
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(type);
                fishData.setnFishState(0);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(0, 0));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }
        timerBoss++;
    }

    private void bossLoop_2() {

        if (timerBoss == 10) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss < 8) {

            int bezierCount = 1;
            int type = MyUtils.getInstances().randomWithRange(FishType.CaKiem.getType(), FishType.CaMapTrang.getType());
            int fishNums = type < FishType.CaVoi.getType() ? 2 : 1;
            List<FishPath> path = paths.get(18);
            TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }
            int state = FishState.Normal.getState();
            List<FishData> data = new LinkedList<>();
            for (int i = 0; i < fishNums; i++) {
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(type);
                fishData.setnFishState(state);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(0, fishNums > 1 ? i * 200 : 100));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(false);
                long currTime = Calendar.getInstance().getTimeInMillis();
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }

            sendFishCreate(data);

        }
        timerBoss++;
    }

    private void bossLoop_3() {

        if (timerBoss == 50) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss < 40) {
            long currTime = Calendar.getInstance().getTimeInMillis();
            List<FishData> data = new LinkedList<>();
            for (int k = 12; k < 15; k++) {
                int bezierCount = paths.get(k).size();
                List<FishPath> path = paths.get(k);
                TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
                for (int j = 0; j < bezierCount; j++) {
                    arr[j] = path.get(j).getPoint();
                }
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(FishType.CaHe.getType());
                fishData.setnFishState(0);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(-80, 100));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }
        timerBoss++;

    }

    private void bossLoop_4() {
        if (timerBoss == 65) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss == 10) {
            int bezierCount = paths.get(15).size();
            int type = MyUtils.getInstances().randomWithRange(FishType.ChimCanhCut.getType(), FishType.ChimCanhCut.getType());
            List<FishPath> path = paths.get(15);
            TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }
            int state = FishState.Normal.getState();
            List<FishData> data = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(type);
                fishData.setnFishState(state);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(378 * (i + 1), 360));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(false);
                long currTime = Calendar.getInstance().getTimeInMillis();
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);

        }
        if (timerBoss < 45) {

            int bezierCount = paths.get(10).size();
            int type = MyUtils.getInstances().randomWithRange(FishType.CaTim.getType(), FishType.CaTim.getType());
            List<FishPath> path = paths.get(10);
            TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }
            int state = FishState.Normal.getState();
            List<FishData> data = new LinkedList<>();

            FishData fishData = new FishData();
            fishData.setnFishKey(currFishNum);
            fishData.setnFishType(type);
            fishData.setnFishState(state);
            fishData.setbFlockKill(false);
            fishData.setfRotateAngle(0);
            fishData.setPointOffSet(new CPoint(-64, -60));
            fishData.setfInitalAngle(0);
            fishData.setbRepeatCreate(false);
            long currTime = Calendar.getInstance().getTimeInMillis();
            fishData.setlCreateTime(currTime);
            fishData.setnBezierCount(bezierCount);
            fishData.setTBzierPoint(arr);
            data.add(fishData);
            sendFishCreate(data);
            currFishNum++;

            bezierCount = paths.get(11).size();

            path = paths.get(11);
            arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }

            data = new LinkedList<>();

            fishData = new FishData();
            fishData.setnFishKey(currFishNum);
            fishData.setnFishType(type);
            fishData.setnFishState(state);
            fishData.setbFlockKill(false);
            fishData.setfRotateAngle(0);
            fishData.setPointOffSet(new CPoint(-64, -60));
            fishData.setfInitalAngle(0);
            fishData.setbRepeatCreate(false);
            fishData.setlCreateTime(currTime);
            fishData.setnBezierCount(bezierCount);
            fishData.setTBzierPoint(arr);
            data.add(fishData);
            sendFishCreate(data);
            currFishNum++;
        }
        timerBoss++;
    }

    private void bossLoop_5() {
        if (timerBoss == 50) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss < 40 && timerBoss % 2 == 0) {
            int _disBegin = 100;
            int _dis1 = 200;
            int _dis2 = 300;
            int _disEnd = 700;
            float xOld = 640;
            float yOld = 360;
            long currTime = Calendar.getInstance().getTimeInMillis();
            int bezierCount = 1;
            List<FishData> data = new LinkedList<>();
            int type = MyUtils.getInstances().randomWithRange(FishType.CaDuoiVangNho.getType(), FishType.CaDenLong.getType());
            for (int i = 0; i < 360; i += 20) {

                double xNew = xOld + _disBegin * Math.cos(Math.toRadians(i));
                double yNew = yOld + _disBegin * Math.sin(Math.toRadians(i));

                double xDes = xOld + _disEnd * Math.cos(Math.toRadians(i));
                double yDes = yOld + _disEnd * Math.sin(Math.toRadians(i));

                double x1 = xOld + _dis1 * Math.cos(Math.toRadians(i));
                double y1 = yOld + _dis1 * Math.sin(Math.toRadians(i));

                double x2 = xOld + _dis2 * Math.cos(Math.toRadians(i));
                double y2 = yOld + _dis2 * Math.sin(Math.toRadians(i));

                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(type);
                fishData.setnFishState(0);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(0, 0));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
                TagBezierPoint tPoint = new TagBezierPoint();
                tPoint.BeginPoint = new CPoint(xNew, yNew);
                tPoint.KeyOne = new CPoint(x1, y1);
                tPoint.KeyTwo = new CPoint(x2, y2);
                tPoint.EndPoint = new CPoint(xDes, yDes);
                tPoint.Time = 5000;
                arr[0] = tPoint;
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }
        timerBoss++;
    }

    private void bossLoop_6() {
        if (timerBoss == 35) {
            bossLoopTask.cancel(true);
            isBossing = false;
            timer = 0;
            timerBoss = 0;
            return;
        }
        if (timerBoss < 30) {
            int fishNum = 3;
            int fishType = (timerBoss % FishType.CaDuoiTo.getType()) + 1;
            long currTime = Calendar.getInstance().getTimeInMillis();
            List<FishPath> path = paths.get(18);
            int state = 0;
            if (fishType == FishType.CaDuoiVangNho.getType())
                state = FishState.King.getState();
            else if (fishType == FishType.CaVangMatTo.getType())
                state = FishState.Killer.getState();
            if (fishType == FishType.CaTramCo.getType())
                state = FishState.Aquatic.getState();
            int bezierCount = paths.get(18).size();
            TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }
            List<FishData> data = new LinkedList<>();
            for (int i = 0; i < fishNum; i++) {
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(fishType);
                fishData.setnFishState(state);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(0, i * 100));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }
        timerBoss++;
    }


    private CPoint randomPoint() {
        CPoint point = new CPoint(0, 0);
        point.x = MyUtils.getInstances().randomWithRange(100, 1036);
        point.y = MyUtils.getInstances().randomWithRange(100, 540);
        return point;
    }

    private CPoint randomPointLeft() {
        CPoint point = new CPoint(0, 0);
        point.x = -200;
        point.y = MyUtils.getInstances().randomWithRange(-100, 740);
        return point;
    }

    private CPoint randomPointRight() {
        CPoint point = new CPoint(0, 0);
        point.x = 1336;
        point.y = MyUtils.getInstances().randomWithRange(-100, 740);
        return point;
    }

    private void genSingleFishNormal() {
        FishData fishData = new FishData();
        fishData.setnFishKey(currFishNum);
        int type = MyUtils.getInstances().randomWithRange(0, FishType.CaDuoiTo.getType());
        int state = FishState.Normal.getState();
        if (type < FishType.CaThienThan.getType()) {
            int ran = MyUtils.getInstances().randomWithRange(0, 50);
            if (ran % 15 == 0 && type < FishType.CaVangMatTo.getType())
                state = FishState.Aquatic.getState();
            else if (ran % 10 == 0)
                state = FishState.King.getState();
        }
        fishData.setnFishType(type);
        fishData.setnFishState(state);
        fishData.setbFlockKill(false);
        fishData.setfRotateAngle(0);
        fishData.setPointOffSet(new CPoint(0, 0));
        fishData.setfInitalAngle(0);
        int bezierCount = MyUtils.getInstances().randomWithRange(1, 8);
        fishData.setbRepeatCreate(bezierCount != 1);
        long currTime = Calendar.getInstance().getTimeInMillis();
        fishData.setlCreateTime(currTime);
        fishData.setnBezierCount(bezierCount);
        TagBezierPoint[] arr = randomBezierArr(bezierCount);
        fishData.setTBzierPoint(arr);
        List<FishData> data = new LinkedList<>();
        data.add(fishData);
        sendFishCreate(data);
        currFishNum++;
    }

    private void genSingleFishNormalWithType(int fishType) {

        FishData fishData = new FishData();
        fishData.setnFishKey(currFishNum);

        int state = FishState.Normal.getState();
        if (fishType < 10) {
            int ran = MyUtils.getInstances().randomWithRange(0, 50);
            if (ran % 15 == 0 && fishType < 3)
                state = FishState.Aquatic.getState();
            else if (ran % 10 == 0)
                state = FishState.King.getState();
        }
        fishData.setnFishType(fishType);
        fishData.setnFishState(state);
        fishData.setbFlockKill(false);
        fishData.setfRotateAngle(0);
        fishData.setPointOffSet(new CPoint(0, 0));
        fishData.setfInitalAngle(0);
        int bezierCount = MyUtils.getInstances().randomWithRange(1, 8);
        fishData.setbRepeatCreate(bezierCount != 1);
        long currTime = Calendar.getInstance().getTimeInMillis();
        fishData.setlCreateTime(currTime);
        fishData.setnBezierCount(bezierCount);
        TagBezierPoint[] arr = randomBezierArr(bezierCount);
        fishData.setTBzierPoint(arr);
        List<FishData> data = new LinkedList<>();
        data.add(fishData);
        sendFishCreate(data);
        currFishNum++;
    }

    private void genEffectFish() {
        int rand = MyUtils.getInstances().randomWithRange(1, 10);
        if (rand == 2) {
            this.genSingleFishNormalWithType(FishType.TienCa.getType());
        } else if (rand == 5) {
            this.genSingleFishNormalWithType(FishType.NaTra.getType());
        } else if (rand == 9) {
            this.genSingleFishNormalWithType(FishType.CaBoom.getType());
        } else
            this.genSingleFishNormal();
    }

    private boolean canGenBigFish() {
        int count = 0;

        for (Fish f : listFish.values()) {
            if (f != null && !f.isDead() && f.getM_data().nFishType > FishType.CaDuoiTo.getType())
                count++;
        }
        int numfish;
        if (this.getListUserPlaying().size() < 3)
            numfish = maxBigFishInMap;
        else if (this.getListUserPlaying().size() < 5)
            numfish = maxBigFishInMap + 1;
        else
            numfish = maxBigFishInMap + 2;
        return count < numfish;
    }

    private boolean canGenEffectFish() {
        int count = 0;
        for (Fish f : listFish.values()) {
            if (f != null && !f.isDead() && f.getM_data().nFishType > FishType.CuopBien.getType())
                count++;
        }
        int numfish;
        if (this.getListUserPlaying().size() < 3)
            numfish = maxEffectFishInMap;
        else if (this.getListUserPlaying().size() < 5)
            numfish = maxEffectFishInMap + 1;
        else
            numfish = maxEffectFishInMap + 2;
        return count < numfish;
    }

    private void genSingleBigFish() {

        FishData fishData = new FishData();
        fishData.setnFishKey(currFishNum);
        int type = MyUtils.getInstances().randomWithRange(FishType.CaMapTrang.getType(), FishType.CuopBien.getType());
        int state = FishState.Normal.getState();
        fishData.setnFishType(type);
        fishData.setnFishState(state);
        fishData.setbFlockKill(false);
        fishData.setfRotateAngle(0);
        fishData.setPointOffSet(new CPoint(0, 0));
        fishData.setfInitalAngle(0);
        int bezierCount;
        if (type > FishType.CaVoi.getType())
            bezierCount = MyUtils.getInstances().randomWithRange(2, 3);
        else
            bezierCount = MyUtils.getInstances().randomWithRange(3, 4);
        fishData.setbRepeatCreate(bezierCount != 1);
        long currTime = Calendar.getInstance().getTimeInMillis();
        fishData.setlCreateTime(currTime);
        fishData.setnBezierCount(bezierCount);
        TagBezierPoint[] arr = randomBezierArr(bezierCount);
        fishData.setTBzierPoint(arr);
        List<FishData> data = new LinkedList<>();
        data.add(fishData);
        sendFishCreate(data);
        currFishNum++;
    }

    private void genBoss_1() {
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_1();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
    }

    private void genBoss_2() {

        int fishNum = 20;
        int fishType = FishType.CaTramCo.getType();
        long currTime = Calendar.getInstance().getTimeInMillis();
        for (int k = 16; k < 18; k++) {
            List<FishPath> path = paths.get(k);
            int bezierCount = paths.get(k).size();
            TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
            for (int i = 0; i < bezierCount; i++) {
                arr[i] = path.get(i).getPoint();
            }
            List<FishData> data = new LinkedList<>();
            for (int i = 0; i < fishNum; i++) {
                FishData fishData = new FishData();
                fishData.setnFishKey(currFishNum);
                fishData.setnFishType(fishType);
                fishData.setnFishState(0);
                fishData.setbFlockKill(false);
                fishData.setfRotateAngle(0);
                fishData.setPointOffSet(new CPoint(i * 60, 0));
                fishData.setfInitalAngle(0);
                fishData.setbRepeatCreate(bezierCount != 1);
                fishData.setlCreateTime(currTime);
                fishData.setnBezierCount(bezierCount);
                fishData.setTBzierPoint(arr);
                data.add(fishData);
                currFishNum++;
            }
            sendFishCreate(data);
        }

        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_2();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 5, 5, TimeUnit.SECONDS);

    }

    private void genBoss_3() {

        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_3();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);

    }

    private void genBoss_4() {
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_4();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
    }

    private void genBoss_5() {
        int bezierCount = paths.get(15).size();
        //int type = MyUtils.getInstances().randomWithRange(FishType.ChimCanhCut.getType(), FishType.ChimCanhCut.getType());
        List<FishPath> path = paths.get(15);
        TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
        for (int i = 0; i < bezierCount; i++) {
            arr[i] = path.get(i).getPoint();
        }
        int state = FishState.Normal.getState();
        List<FishData> data = new LinkedList<>();
        FishData fishData = new FishData();
        fishData.setnFishKey(currFishNum);
        fishData.setnFishType(FishType.CuopBien.getType());
        fishData.setnFishState(state);
        fishData.setbFlockKill(false);
        fishData.setfRotateAngle(0);
        fishData.setPointOffSet(new CPoint(640, 360));
        fishData.setfInitalAngle(0);
        fishData.setbRepeatCreate(false);
        long currTime = Calendar.getInstance().getTimeInMillis();
        fishData.setlCreateTime(currTime);
        fishData.setnBezierCount(bezierCount);
        fishData.setTBzierPoint(arr);
        data.add(fishData);
        currFishNum++;
        sendFishCreate(data);
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_5();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
    }

    private void genBoss_6() {
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    bossLoop_6();
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 3, TimeUnit.SECONDS);
    }

    private void genMultiFishWithLine() {
        final int type = MyUtils.getInstances().randomWithRange(0, FishType.Rua.getType());
        final int bezierCount = MyUtils.getInstances().randomWithRange(1, 8);
        final TagBezierPoint[] arr = randomBezierArr(bezierCount);
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    if (countMultiLineFish >= 6 || isBossing) {
                        countMultiLineFish = 0;
                        fishLineLoopTask.cancel(true);
                        return;
                    }
                    FishData fishData = new FishData();
                    fishData.setnFishKey(currFishNum);
                    fishData.setnFishType(type);
                    fishData.setnFishState(0);
                    fishData.setbFlockKill(false);
                    fishData.setfRotateAngle(0);
                    fishData.setPointOffSet(new CPoint(0, 0));
                    fishData.setfInitalAngle(0);
                    fishData.setbRepeatCreate(bezierCount != 1);
                    long currTime = Calendar.getInstance().getTimeInMillis();
                    fishData.setlCreateTime(currTime);
                    fishData.setnBezierCount(bezierCount);
                    fishData.setTBzierPoint(arr);
                    currFishNum++;
                    List<FishData> data = new LinkedList<>();
                    data.add(fishData);
                    sendFishCreate(data);
                    countMultiLineFish++;
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        fishLineLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 2, TimeUnit.SECONDS);

    }

    private void genFlockFish() {
        int type = MyUtils.getInstances().randomWithRange(FishType.CaGaiVangNho.getType(), FishType.CaVangMatTo.getType());
        int state = FishState.King.getState();
        int bezierCount = MyUtils.getInstances().randomWithRange(1, 8);
        TagBezierPoint[] arr = randomBezierArr(bezierCount);

        long currTime = Calendar.getInstance().getTimeInMillis();
        double posAdd = 40;
        List<FishData> data = new LinkedList<>();
        for (int i = 0; i < 4; i++) {
            FishData fishData = new FishData();
            fishData.setnFishKey(currFishNum);
            fishData.setnFishType(type);
            fishData.setnFishState(state);
            fishData.setbFlockKill(false);
            fishData.setfRotateAngle(0);
            fishData.setPointOffSet(new CPoint(0, 0));
            fishData.setfInitalAngle(0);
            fishData.setbRepeatCreate(bezierCount != 1);
            fishData.setlCreateTime(currTime);
            fishData.setnBezierCount(bezierCount);
            TagBezierPoint[] tempPoint = new TagBezierPoint[bezierCount];
            for (int j = 0; j < arr.length; j++) {
                TagBezierPoint tPoint = new TagBezierPoint();
                tPoint.setBeginPoint(new CPoint(arr[j].BeginPoint));
                tPoint.setKeyOne(new CPoint(arr[j].KeyOne));
                tPoint.setKeyTwo(new CPoint(arr[j].KeyTwo));
                tPoint.setEndPoint(new CPoint(arr[j].EndPoint));
                tPoint.setTime(arr[j].Time);
                tempPoint[j] = tPoint;
            }

            if (i == 0) {
                for (int j = 0; j < tempPoint.length; j++) {
                    tempPoint[j].BeginPoint.addPoint(posAdd);
                    tempPoint[j].KeyOne.addPoint(posAdd);
                    tempPoint[j].KeyTwo.addPoint(posAdd);
                    tempPoint[j].EndPoint.addPoint(posAdd);
                }
            } else if (i == 1) {
                for (int j = 0; j < tempPoint.length; j++) {
                    tempPoint[j].BeginPoint.addX(posAdd);
                    tempPoint[j].BeginPoint.addY(-posAdd);

                    tempPoint[j].KeyOne.addX(posAdd);
                    tempPoint[j].KeyOne.addY(-posAdd);

                    tempPoint[j].KeyTwo.addX(posAdd);
                    tempPoint[j].KeyTwo.addY(-posAdd);

                    tempPoint[j].EndPoint.addX(posAdd);
                    tempPoint[j].EndPoint.addY(-posAdd);
                }
            } else if (i == 2) {
                for (int j = 0; j < tempPoint.length; j++) {
                    tempPoint[j].BeginPoint.addPoint(-posAdd);
                    tempPoint[j].KeyOne.addPoint(-posAdd);
                    tempPoint[j].KeyTwo.addPoint(-posAdd);
                    tempPoint[j].EndPoint.addPoint(-posAdd);
                }
            } else {
                for (int j = 0; j < tempPoint.length; j++) {
                    tempPoint[j].BeginPoint.addX(-posAdd);
                    tempPoint[j].BeginPoint.addY(posAdd);

                    tempPoint[j].KeyOne.addX(-posAdd);
                    tempPoint[j].KeyOne.addY(posAdd);

                    tempPoint[j].KeyTwo.addX(-posAdd);
                    tempPoint[j].KeyTwo.addY(posAdd);

                    tempPoint[j].EndPoint.addX(-posAdd);
                    tempPoint[j].EndPoint.addY(posAdd);
                }
            }

            fishData.setTBzierPoint(tempPoint);
            data.add(fishData);
            currFishNum++;
        }
        sendFishCreate(data);


    }

    public void genSpecialFishWithTypeAndPos(int fishType, TagBezierPoint[] arrTPoint) {
        FishData fishData = new FishData();
        fishData.setnFishKey(currFishNum);
        int state = FishState.Normal.getState();
        fishData.setnFishType(fishType);
        fishData.setnFishState(state);
        fishData.setbFlockKill(false);
        fishData.setfRotateAngle(0);
        fishData.setPointOffSet(new CPoint(0, 0));
        fishData.setfInitalAngle(0);
        int bezierCount = 1;
        fishData.setbRepeatCreate(bezierCount != 1);
        long currTime = Calendar.getInstance().getTimeInMillis();
        fishData.setlCreateTime(currTime);
        fishData.setnBezierCount(bezierCount);
        if (arrTPoint.length == 0)
            arrTPoint = randomBezierArr(bezierCount);
        fishData.setTBzierPoint(arrTPoint);
        List<FishData> data = new LinkedList<>();
        data.add(fishData);
        sendFishCreate(data);
        currFishNum++;
    }

    public boolean hasJackpot() {
        for (Fish f : listFish.values()) {
            if (f != null && !f.isDead() && f.getM_data().nFishType == FishType.JackPot.getType())
                return true;
        }
        return false;
    }


    public boolean isStart() {
        return this.getListUserPlaying().size() > 0;
    }

    public Collection<Fish> getListFish() {
        return listFish.values();
    }

    public Set<FishingPlayer> getPlayers() {
        return players;
    }

    public int[][] getnFishMultiple() {
        return nFishMultiple;
    }

    public int[] getnMultipleValue() {
        return nMultipleValue;
    }

    public int[] getnMultipleIndex() {
        return nMultipleIndex;
    }

    public int getCbBackIndex() {
        return cbBackIndex;
    }

    public int getnBulletCoolingTime() {
        return nBulletCoolingTime;
    }

    public int getnBulletVelocity() {
        return nBulletVelocity;
    }

    public void removeUser(FishingPlayer user) {
        int pos = user.getwChairID();
        this.nMultipleIndex[pos] = 0;
        this.players.remove(user);
    }

    public int userChangeGun(User user, int gunID) {
        for (FishingPlayer usr : this.players) {
            if (user.getId() == usr.getId()) {
                usr.setGunID(gunID);
                int pos = usr.getwChairID();
                this.nMultipleIndex[pos] = gunID;
                return pos;
            }
        }
        return -1;
    }

    public List<FishCatchObject> checkFishCollision(long[] fishes, FishingPlayer userShooted, int bulletID, int nMultipleIndex) {
        synchronized (this) {
            List<FishCatchObject> checked = new LinkedList<>();
            if (!checkUserShooted(userShooted, bulletID)) {
                return checked;
            }

            for (long i : fishes) {
                if (i < 0)
                    continue;
                Fish f = listFish.get(i);
                if (f != null && !f.isDead()) {
                    if (f.isDeadAfterBeShooted(userShooted, nMultipleIndex, 0)) { // neu ca bi ban chet
                        long score = (long) (f.getScore() * nMultipleValue[nMultipleIndex]); //so tien user se nhan dc
                        boolean isJackpot = f.getM_data().nFishType == FishType.JackPot.getType(); // neu trong dam ca do co jackpot
                        if (isJackpot) {
                            if (!jackpotBroken(typeOfGame, userShooted, f, nMultipleIndex))
                                continue;
                            FishCatchObject obj = new FishCatchObject();
                            obj.nFishKey = f.getM_data().nFishKey;
                            obj.nFishType = f.getM_data().nFishType;
                            obj.nFishScore = userShooted.getJackpotMoney();
                            checked.add(obj);
                            listFish.remove(i);
                        } else if (score <= gameBank[nMultipleIndex]) { // neu gamebank du tra cho user
                            this.updateGameBank(nMultipleIndex, -score);
                            FishCatchObject obj = new FishCatchObject();
                            obj.nFishKey = f.getM_data().nFishKey;
                            obj.nFishType = f.getM_data().nFishType;
                            obj.nFishScore = score;
                            checked.add(obj);
                            f.onDestroy();
                            listFish.remove(i);
                            if(!this.isTrialGame){
                                MoneyResponse mnres = userShooted.gameMoneyInfo.updateMoney(score, this.room.getId(), this.id, 0L, false);
                            }
                            userShooted.setChips(userShooted.getChips() + score);
                            userShooted.updateFishCatch(f.getM_data().nFishType, score);
                            this.updateTotalMoneyPlayerWon(score);
                            userShooted.addTotalWon(score);
                        } else {
                            f.setFishHP(1);
                            //Debug.trace(userShooted.getId() + " Kill Fish " + f + " gold: " + score + " But not enough game bank[" + nMultipleIndex + "] " + gameBank[nMultipleIndex]);
                        }
                    }
                }
            }
            return checked;
        }
    }

    public List<FishCatchObject> checkFishSpecialFire(long[] fishes, FishingPlayer userShooted, int nMultipleIndex, int fireType) {
        synchronized (this) {
            List<FishCatchObject> checked = new LinkedList<>();
            for (long i : fishes) {
                if (i < 0)
                    continue;
                Fish f = listFish.get(i);
                if (f != null && !f.isDead()) {
                    if (f.getM_data().nFishType < FishType.RongVang.getType()) {
                        long score = (long) (f.getScore() * nMultipleValue[nMultipleIndex]); //so tien user se nhan dc
                        if (fireType == BulletEffect.Frozen.getType() || score <= gameBank[nMultipleIndex]) {
                            if (fireType != BulletEffect.Frozen.getType())
                                this.updateGameBank(nMultipleIndex, -score);

                            FishCatchObject obj = new FishCatchObject();
                            obj.nFishKey = f.getM_data().nFishKey;
                            obj.nFishType = f.getM_data().nFishType;
                            obj.nFishScore = score;
                            checked.add(obj);
                            f.onDestroy();
                            listFish.remove(i);
                            if(!this.isTrialGame) {
                                MoneyResponse mnres = userShooted.gameMoneyInfo.updateMoney(score, this.room.getId(), this.id, 0L, false);
                            }
                            userShooted.setChips(userShooted.getChips() + score);
                            userShooted.updateFishCatch(f.getM_data().nFishType, score);
                            this.updateTotalMoneyPlayerWon(score);
                            userShooted.addTotalWon(score);
                        }
                    } else if (f.getM_data().nFishType < FishType.KhoBau.getType() && f.isDeadAfterBeShooted(userShooted, nMultipleIndex, fireType)) {
                        long score = (long) (f.getScore() * nMultipleValue[nMultipleIndex]); //so tien user se nhan dc
                        if (score <= gameBank[nMultipleIndex]) { // neu gamebank du tra cho user
                            this.updateGameBank(nMultipleIndex, -score);
                            FishCatchObject obj = new FishCatchObject();
                            obj.nFishKey = f.getM_data().nFishKey;
                            obj.nFishType = f.getM_data().nFishType;
                            obj.nFishScore = score;
                            checked.add(obj);
                            f.onDestroy();
                            listFish.remove(i);
                            if(!this.isTrialGame) {
                                MoneyResponse mnres = userShooted.gameMoneyInfo.updateMoney(score, this.room.getId(), this.id, 0L, false);
                            }
                            userShooted.setChips(userShooted.getChips() + score);
                            userShooted.updateFishCatch(f.getM_data().nFishType, score);
                            this.updateTotalMoneyPlayerWon(score);
                            userShooted.addTotalWon(score);
                        } else {
                            f.setFishHP(1);
                        }
                    }
                }
            }
            return checked;
        }
    }

    public List<FishCatchObject> checkListFishOnBigBang(FishingPlayer userShooted, int nMultipleIndex) {
        synchronized (this) {
            List<FishCatchObject> checked = new LinkedList<>();
            for (Fish f : listFish.values()) {
                if (f != null && !f.isDead() && f.getM_data().getnFishType() < FishType.RongVang.getType()) {
                    long score = (long) (f.getScore() * nMultipleValue[nMultipleIndex]); //so tien user se nhan dc
                    if (score <= gameBank[nMultipleIndex]) { // ktra neu game bank du tra cho user va ca chet thi tra ve client
                        this.updateGameBank(nMultipleIndex, -score);
                        FishCatchObject obj = new FishCatchObject();
                        obj.nFishKey = f.getM_data().nFishKey;
                        obj.nFishType = f.getM_data().nFishType;
                        obj.nFishScore = score;
                        checked.add(obj);
                        f.onDestroy();
                        listFish.remove(f.getM_data().nFishKey);
                        if(!this.isTrialGame) {
                            MoneyResponse mnres = userShooted.gameMoneyInfo.updateMoney(score, this.room.getId(), this.id, 0L, false);
                        }
                        userShooted.setChips(userShooted.getChips() + score);
                        userShooted.updateFishCatch(f.getM_data().nFishType, score);
                        this.updateTotalMoneyPlayerWon(score);
                        userShooted.addTotalWon(score);
                    }
                }
            }
            return checked;
        }
    }

    private boolean checkUserShooted(FishingPlayer fUser, int bulletID) {
        return fUser != null && fUser.removeBulletByID(bulletID);
    }

    public void removeFish(long fishID) {
        listFish.remove(fishID);
    }

    public long userFire(int uId, int bulletIndex, int bulletIndexValue) {
        synchronized (this) {
            long value = nMultipleValue[bulletIndexValue];
            long currMoney = -1;
            FishingPlayer fPlayer = getFUserByID(uId);
            if (fPlayer != null) {
                if(!this.isTrialGame) {
                    MoneyResponse mnres = fPlayer.gameMoneyInfo.updateMoney(-value, this.room.getId(), this.id, 0L, false);
                    if (!mnres.isSuccess()) return currMoney;
                    currMoney = mnres.getCurrentMoney();
                }else if(fPlayer.getChips() < value) {
                    return currMoney;
                }
                Bullet bullet = new Bullet(bulletIndex, uId, bulletIndexValue);
                fPlayer.fire(bullet);
                fPlayer.setChips(fPlayer.getChips() - value);
                if(this.isTrialGame)
                    currMoney = fPlayer.getChips();
                // 2% jackpot bank, 2% phế còn lại vào gamebank
                double feeRate = value * (FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_FEE / 100.0);
                double jackpotRate = value * (FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_MONEY_TO_JACKPOT / 100.0);
                double jackpotBankRate = value * (FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_MONEY_TO_JACKPOT_BANK / 100.0);
                double moneyToGameBank = value - feeRate - jackpotBankRate;
                if (typeOfGame == 1) {
                    FishingGameServiceImpl.getInstance().updateNormalFishJackpot((int) jackpotRate);
                    FishingGameServiceImpl.getInstance().updateNormalFishJackpotBank((int) jackpotBankRate);
                } else if (typeOfGame == 2) {
                    FishingGameServiceImpl.getInstance().updateVipFishJackpot((int) jackpotRate);
                    FishingGameServiceImpl.getInstance().updateVipFishJackpotBank((int) jackpotBankRate);
                }
                this.updateGameBank(bullet.getMutipleIndex(), moneyToGameBank);
                if (!isTrialGame) {
                    fPlayer.addPlayerGameFee((int) feeRate);
                    this.updateGameFee(feeRate);
                    this.updateTotalMoneyFireInGame(value);
                }
                return currMoney;
            }
            return currMoney;
        }
    }

    private FishingPlayer getFUserByID(int id) {
        for (FishingPlayer usr : players) {
            if (usr.getId() == id)
                return usr;
        }
        return null;
    }

    private void sendFishCreate(List<FishData> fishData) {

        for (FishData data : fishData) {
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(data);
            this.listFish.put(data.nFishKey, fish);
        }
        for (FishingPlayer usr : this.getPlayers()) {
            for (FishData data : fishData) {
                data.setUnCreateTime((int) (data.getlCreateTime() - usr.getTimeJoinGame()));
            }
            ResponseFishCreate res = new ResponseFishCreate();
            res.listFish = new ArrayList<>(fishData);
            this.sendForUser(usr.getUser(), res);
        }
        //System.out.println("\nGen Fish" + fishData);

    }

    private TagBezierPoint[] randomBezierArr(int bezierCount) {
        TagBezierPoint[] arr = new TagBezierPoint[bezierCount];
        CPoint lastPoint = null;
        int rand = MyUtils.getInstances().randomWithRange(0, 1);
        for (int i = 0; i < bezierCount; i++) {
            TagBezierPoint tPoint = new TagBezierPoint();
            if (rand == 0) {
                if (i % 2 == 0) {
                    if (i > 0) {
                        tPoint.setBeginPoint(lastPoint);
                    } else {
                        lastPoint = randomPointLeft();
                        tPoint.setBeginPoint(lastPoint);
                    }
                    lastPoint = randomPointRight();
                    tPoint.setEndPoint(lastPoint);
                } else {
                    tPoint.setBeginPoint(lastPoint);
                    lastPoint = randomPointLeft();
                    tPoint.setEndPoint(lastPoint);
                }
            } else {
                if (i % 2 == 0) {
                    if (i > 0) {
                        tPoint.setBeginPoint(lastPoint);
                    } else {
                        lastPoint = randomPointRight();
                        tPoint.setBeginPoint(lastPoint);
                    }
                    lastPoint = randomPointLeft();
                    tPoint.setEndPoint(lastPoint);
                } else {
                    tPoint.setBeginPoint(lastPoint);
                    lastPoint = randomPointRight();
                    tPoint.setEndPoint(lastPoint);
                }
            }


            tPoint.setKeyOne(randomPoint());
            tPoint.setKeyTwo(randomPoint());
            if (bezierCount > 5)
                tPoint.setTime(MyUtils.getInstances().randomWithRange(10, 15) * 1000);
            else
                tPoint.setTime(MyUtils.getInstances().randomWithRange(20, 30) * 1000);
            arr[i] = tPoint;
        }
        return arr;
    }

    private void changeScene() {
        cbBackIndex++;
        cbBackIndex = cbBackIndex % 6;
        long currTime = Calendar.getInstance().getTimeInMillis();
        for (FishingPlayer usr : players)
            usr.setTimeJoinGame(currTime);
        ResponseChangeScene response = new ResponseChangeScene();
        response.cbBackIndex = cbBackIndex;
        this.broadcastGame(response);
        this.destroyAllFishWhenChangeScene();
        FishingGameServiceImpl.getInstance().saveFishingJackpot();
        Debug.trace("Change Scene " + currTime);
    }

    private boolean canGenFish() {
        int maxFishNum = this.getListUserPlaying().size() * maxFishInMap;
        int count = 0;
        for (Fish fish : listFish.values()) {
            if (fish != null && !fish.isDead())
                count++;
        }
        return maxFishNum > count;
    }

    public boolean cannotGenSpecialGun(int nMultipleIndex) {
        return someoneHasSpecialGun() || nMultipleIndex < 0 || nMultipleIndex > 5 || gameBank[nMultipleIndex] < FishingGameServiceImpl.getInstance().GAME_FISHING_RATE_GAMEBANK_TO_GEN_SPECIAL_GUN * nMultipleValue[nMultipleIndex];
    }

    private boolean someoneHasSpecialGun() {
        for (FishingPlayer player : players) {
            if (player.isHasSpecialGun())
                return true;
        }
        return false;
    }


    public boolean removePlayer(FishingPlayer player) {
        int pos = player.getwChairID();
        this.nMultipleIndex[pos] = 0;
        return players.remove(player);

    }

    private void destroyAllFishWhenChangeScene() {

        Set<FishingPlayer> tempSet = new HashSet<>();
        tempSet.addAll(players);

        for (Fish fish : listFish.values()) {
            if (fish != null) {
                fish.onDestroy();
            }
        }
        listFish.clear();
    }

    public boolean canPlayerGetGoldReward(FishingPlayer player, int nMultipleIndex) {
        if (player == null) return false;

        int randValue = MyUtils.getInstances().randomWithRange(10, FishingGameServiceImpl.getInstance().GAME_FISHING_MAX_GOLD_PLAYER_CAN_GET_FROM_REWARD);
        long score = randValue * nMultipleValue[nMultipleIndex]; //so tien user se nhan dc
        if (score <= gameBank[nMultipleIndex]) { // neu gamebank du tra cho user
            this.updateGameBank(nMultipleIndex, -score);
            player.setChips(player.getChips() + score);
            if(!this.isTrialGame) {
                MoneyResponse mnres = player.gameMoneyInfo.updateMoney(score, this.room.getId(), this.id, 0L, false);
            }
            this.updateTotalMoneyPlayerWon(score);
            Debug.trace(player.getId() + " get award " + score + " gold, game bank " + gameBank[nMultipleIndex]);
            if (!isTrialGame) {
                player.addTotalWon(score);
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FishingGame{" +
                "id=" + id +
                ", GameType=" + (typeOfGame == 0 ? " Trial" : typeOfGame == 1 ? " Normal" : " VIP") +
                ", GameBank=" + Arrays.toString(gameBank) +
                ", TotalGameFee =" + totalGameFee +
                '}';
    }

    private List<FishBossObject> genBoss_7() {

        List<FishBossObject> listFishBoss = new ArrayList<>();
        for (int i = 0; i < 66; i++) {
            FishBossObject boss = new FishBossObject();
            if (i < 30) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaBuom.getType());
            } else if (i < 40) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaThienThan.getType());
            } else if (i < 60) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaMuc.getType());
            } else if (i < 65) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.Rua.getType());
            } else {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.ChimCanhCut.getType());
            }
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }
        return listFishBoss;
    }

    private List<FishBossObject> genBoss_8() {

        List<FishBossObject> listFishBoss = new ArrayList<>();
        for (int i = 0; i < 114; i++) {
            FishBossObject boss = new FishBossObject();
            boss.setnFishKey(currFishNum);
            if (i < 100) {
                boss.setnFishType(FishType.CaGaiVangNho.getType());
            } else {
                boss.setnFishType(MyUtils.getInstances().randomWithRange(FishType.CaKiem.getType(), FishType.CaMapTrang.getType()));
            }
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }
        return listFishBoss;
    }

    private List<FishBossObject> genBoss_9() {

        List<FishBossObject> listFishBoss = new ArrayList<>();
        for (int i = 0; i < 46; i++) {
            FishBossObject boss = new FishBossObject();
            if (i < 20) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaDenLong.getType());
            } else if (i < 35) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.Rua.getType());
            } else if (i < 45) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaBuom.getType());
            } else {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaMapVang.getType());
            }
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }
        for (int i = 0; i < 46; i++) {
            FishBossObject boss = new FishBossObject();
            if (i < 20) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaDenLong.getType());
            } else if (i < 35) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.Rua.getType());
            } else if (i < 45) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaBuom.getType());
            } else {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaMapVang.getType());
            }
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }

        return listFishBoss;
    }

    private List<FishBossObject> genBoss_10() {

        List<FishBossObject> listFishBoss = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            FishBossObject boss = new FishBossObject();
            boss.setnFishKey(currFishNum);
            boss.setnFishType(MyUtils.getInstances().randomWithRange(FishType.CaKiem.getType(), FishType.CaMapTrang.getType()));
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }
        return listFishBoss;
    }

    private List<FishBossObject> genBoss_11() {

        List<FishBossObject> listFishBoss = new ArrayList<>();
        for (int i = 0; i < 94; i++) {
            FishBossObject boss = new FishBossObject();
            if (i < 40) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaDenLong.getType());
            } else if (i < 68) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.Rua.getType());
            } else if (i < 84) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaBuom.getType());
            } else if (i < 92) {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaMuc.getType());
            } else {
                boss.setnFishKey(currFishNum);
                boss.setnFishType(FishType.CaDuoiTo.getType());
            }
            listFishBoss.add(boss);
            FishData fData = new FishData();
            fData.nFishKey = boss.getnFishKey();
            fData.nFishType = boss.getnFishType();
            fData.nFishState = FishState.Normal.getState();
            fData.isFishGroup = true;
            Fish fish = new Fish();
            fish.setGame(this);
            fish.setM_data(fData);
            this.listFish.put(currFishNum, fish);
            currFishNum++;
        }

        return listFishBoss;
    }

    private void genSpecialBoss(int type) {

        List<FishBossObject> listFishBoss = null;
        timerBoss = 0;
        if (type == 7)
            listFishBoss = genBoss_7();
        else if (type == 8)
            listFishBoss = genBoss_8();
        else if (type == 9)
            listFishBoss = genBoss_9();
        else if (type == 10)
            listFishBoss = genBoss_10();
        else if (type == 11)
            listFishBoss = genBoss_11();
        ResponseGenFishBoss res = new ResponseGenFishBoss();
        res.listFishBoss = new ArrayList<>(listFishBoss);
        res.timeBoss = 55;
        res.groupID = (byte) (type % 7);
        this.broadcastGame(res);
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                try {
                    if (timerBoss >= 55) {
                        bossLoopTask.cancel(true);
                        isBossing = false;
                        timer = 0;
                        timerBoss = 0;
                        destroyAllFishWhenChangeScene();
                        return;
                    }
                    timerBoss++;
                } catch (Exception e) {
                    destroy();
                }
            }
        };
        bossLoopTask = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
    }

    private boolean jackpotBroken(int typeOfGame, FishingPlayer userShooted, Fish fish, int nMultipleIndex) {

        int[] firedBullet = userShooted.getTotalBullets();
        int numOfBullets = 0;
        for (int b : firedBullet)
            numOfBullets += b;
        if (numOfBullets < FishingGameServiceImpl.getInstance().GAME_FISHING_BULLET_TO_JACKPOT_BROKEN)
            return false; // ko bắn đủ đạn yêu cầu
//        if (DBCommon.isBlackListJackPot(userShooted.getId())) return false;
        if (nMultipleIndex < 5) return false; // ko phải mức đạn lớn nhất
        int jackpot = 0;
        if (typeOfGame == 1) {
            jackpot = FishingGameServiceImpl.getInstance().getMoneyNormalJackpot();
            if (userShooted.getId() == FishingGameServiceImpl.getInstance().getUserIDGame() ||
                    (MyUtils.getInstances().randomWithRange(0, FishingGameServiceImpl.getInstance().getPercent()) == 0)) {
                FishingGameServiceImpl.getInstance().resetNormalJackpot();
                Debug.trace("Reset Normal Jackpot game id: " + id + " at: " + Calendar.getInstance().getTime());
            } else
                return false;
        } else if (typeOfGame == 2) {
            jackpot = FishingGameServiceImpl.getInstance().getMoneyVipJackpot();
            if (userShooted.getId() == FishingGameServiceImpl.getInstance().getUserIDGame() ||
                    ( MyUtils.getInstances().randomWithRange(0, FishingGameServiceImpl.getInstance().getPercent()) == 0)) {
                FishingGameServiceImpl.getInstance().resetVipJackpot();
                Debug.trace("Reset Vip Jackpot game id: " + id + " at: " + Calendar.getInstance().getTime());
            } else
                return false;
        }

        ResponseFishingJackpotBroken res = new ResponseFishingJackpotBroken();
        res.jackpotValue = jackpot;
        res.playerName = userShooted.getName();
        this.sendToAllUser(res);

        fish.onDestroy();
        userShooted.setChips(userShooted.getChips() + jackpot);
        if(!this.isTrialGame) {
            MoneyResponse mnres = userShooted.gameMoneyInfo.updateMoney(jackpot, this.room.getId(), this.id, 0L, false);
        }
        userShooted.updateFishCatch(fish.getM_data().nFishType, jackpot);
        userShooted.setJackpotMoney(jackpot);
        this.updateTotalMoneyPlayerWon(jackpot);
        Debug.trace(userShooted.getId() + " Kill Jackpot : " + jackpot);
        userShooted.addTotalWon(jackpot);
        return true;
    }

}
