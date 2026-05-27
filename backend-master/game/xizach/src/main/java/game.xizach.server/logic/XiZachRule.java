/*
 * Decompiled with CFR 0.144.
 */
package game.xizach.server.logic;

import game.xizach.server.logic.GroupCard;

public class XiZachRule {
    public static int soSanhBai(GroupCard gc1, GroupCard gc2) {
        int rate = Math.max(gc1.getRate(), gc2.getRate());
        return gc1.soSanhBo(gc2) * rate;
    }
}

