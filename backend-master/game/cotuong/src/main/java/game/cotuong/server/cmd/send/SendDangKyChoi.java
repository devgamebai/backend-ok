/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.cotuong.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.cotuong.server.GamePlayer;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendDangKyChoi
extends BaseMsg {
    public static final byte DANG_KY = 1;
    public static final byte HUY = 2;
    public static final byte DANG_KY_HUY = 3;
    public byte action = 1;
    public GamePlayer gp = null;

    public SendDangKyChoi() {
        super((short)3108);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        if (this.gp != null) {
            bf.put(this.action);
            bf.put((byte)this.gp.gameChair);
            this.putStr(bf, this.gp.pInfo.nickName);
            this.putLong(bf, this.gp.gameMoneyInfo.getCurrentMoneyFromCache());
        } else {
            bf.put((byte)0);
            bf.put((byte)0);
            this.putStr(bf, "");
            this.putLong(bf, 0L);
        }
        return this.packBuffer(bf);
    }
}

