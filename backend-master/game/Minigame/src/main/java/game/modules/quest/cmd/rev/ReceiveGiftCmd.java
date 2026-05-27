/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseCmd
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.Debug
 */
package game.modules.quest.cmd.rev;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import java.nio.ByteBuffer;

public class ReceiveGiftCmd
extends BaseCmd {
    public int indexQuest = 0;

    public ReceiveGiftCmd(DataCmd data) {
        super(data);
        this.unpackData();
    }

    public void unpackData() {
        try {
            ByteBuffer buffer = this.makeBuffer();
            this.indexQuest = this.readByte(buffer);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
    }
}

