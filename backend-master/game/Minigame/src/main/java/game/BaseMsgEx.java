/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game;

import bitzero.server.extensions.data.BaseMsg;

public class BaseMsgEx
extends BaseMsg {
    public BaseMsgEx(int type) {
        super((short)type);
    }

    protected BaseMsgEx(int type, int error) {
        super((short)type, error);
    }
}

