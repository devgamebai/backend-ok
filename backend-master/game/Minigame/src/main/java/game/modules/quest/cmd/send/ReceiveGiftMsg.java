/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 */
package game.modules.quest.cmd.send;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ReceiveGiftMsg
extends BaseMsgEx {
    public boolean isSuccess = false;
    public long currentMoney;

    public ReceiveGiftMsg(String userName, boolean isSuccess) {
        super(21001);
        UserServiceImpl userService = new UserServiceImpl();
        this.isSuccess = isSuccess;
        // SUN-748 regression fix: use vin balance, not vin_total (cumulative P&L).
        this.currentMoney = userService.getMoneyUserCache(userName, "vin");
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, this.isSuccess);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

