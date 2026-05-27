/*
 * Decompiled with CFR 0.144.
 */
package game.bacay.server.logic;

import game.bacay.server.logic.GroupCard;

public class BacayRule {
    public static int soSanhBai(GroupCard gc1, GroupCard gc2) {
        if (gc1.kiemTraBo() == 3 || gc2.kiemTraBo() == 3) {
            return gc1.soSanhBo(gc2) * 4;
        }
        if (gc1.kiemTraBo() == 2 || gc2.kiemTraBo() == 2) {
            return gc1.soSanhBo(gc2) * 3;
        }
        if (gc1.kiemTraBo() == 1 || gc2.kiemTraBo() == 1) {
            return gc1.soSanhBo(gc2) * 2;
        }
        return gc1.soSanhBo(gc2);
    }
}

