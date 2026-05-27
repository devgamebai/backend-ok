/*
 * Decompiled with CFR 0.152.
 */
package game.modules.quest.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GetListQuestMsg
extends BaseMsgEx {
    public String listMission = "";

    public GetListQuestMsg(String listMission) {
        super(21000);
        this.listMission = listMission;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.listMission);
        return this.packBuffer(bf);
    }
}

