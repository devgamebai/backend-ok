/*
 * Decompiled with CFR 0.152.
 */
package game.modules.description.XocDiaDescription;

import game.modules.description.BaseDescription;

public class XocDiaBaseDescription
extends BaseDescription {
    public long referenceId;
    public byte action;

    public XocDiaBaseDescription(String gameID, long referenceId, byte action) {
        super((byte)13, gameID);
        this.referenceId = referenceId;
        this.action = action;
    }
}

