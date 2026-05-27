package game.modules.XocDia.model;

import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import game.modules.XocDia.model.bet.BetItem;
import game.modules.XocDia.model.bet.XocDiaBetDoorItem;
import game.utils.GameUtil;
import bitzero.util.common.business.Debug;

public class XocDiaBetModel {
    public static XocDiaBetModel _instance;
    public MiniGameService mgService;
    public boolean flagRunPayMoney;
    public boolean flagRunDice;
    public long referenceIdXocDia;

    public static final byte BET_PHASE = 0;
    public static final byte DICE_PHASE = 1;
    public static final byte PAY_PHASE = 2;

    public long[] totalBet;
    public long startTime;
    public byte isStart;
    public byte status;
    public long[] listBotBet;

    // 6 doors: chan(0), le(1), 3-1(2), 1-3(3), 4-0(4), 0-4(5)
    public XocDiaBetDoorItem[] mDoor;

    public static XocDiaBetModel getInstance() {
        if (_instance == null) {
            _instance = new XocDiaBetModel();
        }
        return _instance;
    }

    private XocDiaBetModel() {
        this.mgService = new MiniGameServiceImpl();
        this.flagRunPayMoney = false;
        this.flagRunDice = false;
        this.referenceIdXocDia = 1;
        this.totalBet = new long[6];
        this.startTime = GameUtil.getTimeStampInSeconds();
        this.isStart = 0;
        this.status = BET_PHASE;
        this.listBotBet = new long[6];
        this.mDoor = new XocDiaBetDoorItem[6];
        for (int i = 0; i < 6; i++) {
            this.mDoor[i] = new XocDiaBetDoorItem();
        }
        try {
            this.referenceIdXocDia = this.mgService.getReferenceId(10); // 10 for XocDia Full
        } catch (Exception e) {
            Debug.trace(new Object[]{"Load XocDia referenceId error", e.getMessage()});
        }
    }

    public void reset() {
        for (int i = 0; i < 6; i++) {
            this.totalBet[i] = 0;
            this.listBotBet[i] = 0;
            this.mDoor[i].clear();
        }
        this.flagRunPayMoney = false;
        this.flagRunDice = false;
        this.referenceIdXocDia++;
        this.startTime = GameUtil.getTimeStampInSeconds();
        this.isStart = 1;
        this.status = BET_PHASE;
    }

    public void saveReferenceIdXocDia() {
        try {
            this.mgService.saveReferenceId(this.referenceIdXocDia, 10);
        } catch (Exception e) {
            Debug.trace(new Object[]{"Save XocDia referenceId error", e.getMessage()});
        }
    }

    public void updateStatus(byte newStatus) {
        this.status = newStatus;
        this.startTime = GameUtil.getTimeStampInSeconds();
    }

    public synchronized boolean bet(long userId, String userName, byte door, long money) {
        return bet(userId, userName, door, money, false);
    }

    public synchronized boolean bet(long userId, String userName, byte door, long money, boolean isBot) {
        if (door < 0 || door > 5) return false;
        if (money <= 0) return false;
        if (this.status != BET_PHASE) return false;

        this.totalBet[door] += money;
        if (isBot) {
            this.listBotBet[door] += money;
        }

        BetItem existingBet = this.mDoor[door].listBet.get(Long.valueOf(userId));
        if (existingBet != null) {
            existingBet.money += money;
        } else {
            this.mDoor[door].listBet.put(Long.valueOf(userId), new BetItem(userId, money, userName, isBot));
        }
        return true;
    }
}
