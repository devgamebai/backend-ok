/*
 * Decompiled with CFR 0.152.
 */
package game.modules.description.CaoThapDescription;

import game.modules.description.BaseDescription;

public class CaoThapBaseDescription
extends BaseDescription {
    public long referenceId;
    public short step;
    public byte action;

    public CaoThapBaseDescription(String gameID, long referenceId, short step, byte action) {
        super((byte)4, gameID);
        this.referenceId = referenceId;
        this.step = step;
        this.action = action;
    }
}

