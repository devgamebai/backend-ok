/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.xocdia.entities.GamePot;
import game.xocdia.entities.RefundModel;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class RefundMsg
extends BaseMsg {
    public Vector<GamePot> potList;
    public Map<String, RefundModel> refundMap;

    public RefundMsg() {
        super((short)3118);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.refundMap.size());
        for (GamePot pot : this.potList) {
            bf.put(pot.id);
            bf.putLong(pot.moneyRefund);
            bf.putLong(pot.totalMoney);
        }
        for (Map.Entry entry : this.refundMap.entrySet()) {
            this.putStr(bf, (String)entry.getKey());
            bf.putLong(((RefundModel)entry.getValue()).moneyRefund);
            bf.putLong(((RefundModel)entry.getValue()).currentMoney);
            this.putStr(bf, ((RefundModel)entry.getValue()).pots);
            this.putStr(bf, ((RefundModel)entry.getValue()).moneyRfPots);
        }
        return this.packBuffer(bf);
    }
}

