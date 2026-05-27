/*
 * Decompiled with CFR 0.152.
 */
package game.modules.description.TaiXiuDescription;

import game.modules.description.BaseDescription;

public class TaiXiuBetDescription
extends BaseDescription {
    public long referenceId;
    public String time;
    public short betSide;

    public TaiXiuBetDescription(String gameID, long referenceId, String time, short betSide) {
        super((byte)6, gameID);
        this.referenceId = referenceId;
        this.time = time;
        this.betSide = betSide;
    }
}

