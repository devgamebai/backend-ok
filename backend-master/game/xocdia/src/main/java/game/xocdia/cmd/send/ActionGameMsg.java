/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ActionGameMsg
extends BaseMsg {
    public static final byte START_NEW_GAME = 1;
    public static final byte START_BETTING = 2;
    public static final byte START_PURCHASE = 3;
    public static final byte START_REJECT = 4;
    public static final byte START_BALANCE = 5;
    public static final byte START_REWARD = 6;
    public byte action;
    public byte time;

    public ActionGameMsg() {
        super((short)3105);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.action);
        bf.put(this.time);
        return this.packBuffer(bf);
    }
}

