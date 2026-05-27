/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.mission.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RewardMissionCmd
extends BaseCmd {
    public String missionName;
    public byte moneyType;

    public RewardMissionCmd(DataCmd data) {
        super(data);
        this.unpackData();
    }

    public void unpackData() {
        ByteBuffer buffer = this.makeBuffer();
        this.missionName = this.readString(buffer);
        this.moneyType = this.readByte(buffer);
    }
}

