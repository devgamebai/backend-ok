/*
 * Decompiled with CFR 0.152.
 */
package game.modules.description.TaiXiuDescription;

import game.modules.description.BaseDescription;

public class TaiXiuBaseDescription
extends BaseDescription {
    public long referenceId;
    public byte action;

    public TaiXiuBaseDescription(String gameID, long referenceId, byte action) {
        super((byte)7, gameID);
        this.referenceId = referenceId;
        this.action = action;
    }
}

