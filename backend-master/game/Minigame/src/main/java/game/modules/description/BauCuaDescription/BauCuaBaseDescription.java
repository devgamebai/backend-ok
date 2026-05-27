/*
 * Decompiled with CFR 0.152.
 */
package game.modules.description.BauCuaDescription;

import game.modules.description.BaseDescription;

public class BauCuaBaseDescription
extends BaseDescription {
    public long referenceId;
    public byte action;

    public BauCuaBaseDescription(String gameID, long referenceId, byte action) {
        super((byte)5, gameID);
        this.referenceId = referenceId;
        this.action = action;
    }
}

