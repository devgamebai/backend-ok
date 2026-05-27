/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.CommonHandle
 *  com.vinplay.dal.service.PotService
 *  com.vinplay.dal.service.impl.PotServiceImpl
 *  com.vinplay.vbee.common.models.PotModel
 *  com.vinplay.vbee.common.response.PotResponse
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import bitzero.util.common.business.CommonHandle;
import com.vinplay.dal.service.PotService;
import com.vinplay.dal.service.impl.PotServiceImpl;
import com.vinplay.vbee.common.models.PotModel;
import com.vinplay.vbee.common.response.PotResponse;
import game.modules.gameRoom.config.HuVangConfig;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.utils.GameUtils;
import java.util.Calendar;
import org.json.JSONObject;

public class ThongTinHuVang {
    private PotService potService = new PotServiceImpl();
    public boolean firstTime = true;
    private static ThongTinHuVang ins = null;

    private ThongTinHuVang() {
    }

    public static ThongTinHuVang instance() {
        if (ins == null) {
            ins = new ThongTinHuVang();
        }
        return ins;
    }

    public void addMoneyInLoop() {
        try {
            if (!GameUtils.isHuVang) {
                return;
            }
            JSONObject json = HuVangConfig.instance().getConfigCurrentGame();
            if (json == null) {
                return;
            }
            int remain = HuVangConfig.instance().kiemTraThoiGianHuVangGameHienTai();
            Calendar now = Calendar.getInstance();
            int hour = now.get(11);
            int minute = now.get(12);
            if (remain != 0 && hour == 0 && minute == 0 || this.firstTime) {
                this.firstTime = false;
                String gameName = json.getString("gameName");
                long initial = json.getLong("initial");
                this.potService.addMoneyPot(gameName, initial, true);
                PotModel potModel = this.potService.getPot(gameName);
            } else if (remain < 0) {
                String gameName = json.getString("gameName");
                long add = json.getLong("add");
                this.potService.addMoneyPot(gameName, add, false);
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public ThongTinThangLon addMoneyForUser(User user, ThongTinThangLon info) {
        if (!GameUtils.enable_payment) {
            info.MoneyAdd = 10000000;
            info.currentMoney = 10000000;
            return info;
        }
        PotResponse res = this.potService.noHu(info.moneySessionId, info.nickName, info.gameName, String.valueOf(info.roomId), 0, String.valueOf(info.gameId), info.gameName, info.rate, info.moneyBet, info.desc);
        if (!res.isSuccess()) {
            CommonHandle.writeErrLogDebug((Object[])new Object[]{"LoiNoHu", info.moneySessionId, info.nickName, info.gameName, info.roomId, 0, info.gameId, info.gameName, info.rate});
            return null;
        }
        info.MoneyAdd = res.getMoneyExchange();
        info.currentMoney = res.getCurrentMoneyUser();
        return info;
    }

    public boolean dangChayHu() {
        int remain = HuVangConfig.instance().kiemTraThoiGianHuVangGameHienTai();
        return remain < 0;
    }

    public long getGoldAmount(String game) {
        PotModel pot = this.potService.getPot(game);
        if (pot != null) {
            return pot.getValue();
        }
        return 0;
    }
}

