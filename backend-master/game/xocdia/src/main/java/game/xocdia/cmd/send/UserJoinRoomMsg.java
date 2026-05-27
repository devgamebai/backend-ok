/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import java.nio.ByteBuffer;

public class UserJoinRoomMsg
extends BaseMsg {
    public String nickname;
    public String avatar;
    public long money;

    public UserJoinRoomMsg(PlayerInfo pInfo) {
        super((short)3102);
        this.nickname = pInfo.nickName;
        this.avatar = pInfo.avatarUrl;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        this.putStr(bf, this.avatar);
        this.putLong(bf, this.money);
        return this.packBuffer(bf);
    }
}

