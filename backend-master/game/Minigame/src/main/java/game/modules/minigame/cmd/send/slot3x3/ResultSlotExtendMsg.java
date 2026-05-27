/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.minigame.cmd.send.slot3x3;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.minigame.LineWin;
import java.nio.ByteBuffer;
import java.util.List;

public class ResultSlotExtendMsg
extends BaseMsg {
    public long prize;
    public int winType;
    public int result;
    public int mutil;
    public int mutil1;
    public int mutil2;
    public int spinId;
    public int[] showItem;
    public List<LineWin> listLineWin;
    public long currMoney;

    public ResultSlotExtendMsg() {
        super((short)8003);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.result);
        bf.putLong(this.prize);
        bf.putInt(this.winType);
        bf.putInt(this.mutil);
        bf.putInt(this.mutil1);
        bf.putInt(this.mutil2);
        bf.putInt(this.spinId);
        int size1 = this.showItem.length;
        bf.putInt(size1);
        for (int i = 0; i < size1; ++i) {
            bf.putInt(this.showItem[i]);
        }
        int size = this.listLineWin.size();
        bf.putInt(size);
        for (int i = 0; i < size; ++i) {
            bf.putInt(this.listLineWin.get(i).getLine());
            bf.putDouble(this.listLineWin.get(i).getPrizeAmount());
            if (this.listLineWin.get(i).isJackpot()) {
                bf.putInt(1);
                continue;
            }
            bf.putInt(0);
        }
        this.putStr(bf, "");
        this.putStr(bf, "");
        this.putStr(bf, "");
        bf.putLong(this.currMoney);
        return this.packBuffer(bf);
    }
}

