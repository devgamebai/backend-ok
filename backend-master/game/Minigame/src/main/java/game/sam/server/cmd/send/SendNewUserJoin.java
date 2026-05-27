/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import game.entities.PlayerInfo;
import java.nio.ByteBuffer;

public class SendNewUserJoin
extends BaseMsgEx {
    public long money;
    public String uName;
    public String avtUrl;
    public int uChair;
    public int uStatus;

    public SendNewUserJoin() {
        super(3121);
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

