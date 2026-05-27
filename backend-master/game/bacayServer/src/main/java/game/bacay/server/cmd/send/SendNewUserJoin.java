/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import java.nio.ByteBuffer;

public class SendNewUserJoin
extends BaseMsg {
    public long money;
    public String uName;
    public String avtUrl;
    public int uChair;
    public int uStatus;

    public SendNewUserJoin() {
        super((short)3121);
    }

    public void setBaseInfo(PlayerInfo pInfo) {
        this.uName = pInfo.nickName;
        this.avtUrl = pInfo.avatarUrl;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.uName);
        this.putStr(bf, this.avtUrl);
        this.putLong(bf, this.money);
        bf.put((byte)this.uChair);
        bf.put((byte)this.uStatus);
        return this.packBuffer(bf);
    }
}

