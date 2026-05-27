/*
 * Decompiled with CFR 0.144.
 */
package game.lieng.server.logic;

import game.lieng.server.logic.GroupCard;

public class LiengRule {
    public static synchronized int soSanhBoBai(GroupCard gc1, GroupCard gc2) {
        return gc1.soSanhBo(gc2);
    }
}

