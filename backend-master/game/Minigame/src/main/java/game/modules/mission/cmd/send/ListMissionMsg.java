/*
 * Decompiled with CFR 0.144.
 */
package game.modules.mission.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ListMissionMsg
extends BaseMsgEx {
    public String listMission = "";

    public ListMissionMsg() {
        super(21000);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.listMission);
        return this.packBuffer(bf);
    }
}

