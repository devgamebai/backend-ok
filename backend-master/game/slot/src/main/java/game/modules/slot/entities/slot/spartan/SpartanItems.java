/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.spartan;

public class SpartanItems {
    private static int[][] config = new int[][]{{8, 8, 0, 35, 10, 10, 12, 25, 75, 80, 85}, {8, 8, 50, 25, 10, 15, 20, 25, 50, 60, 65}, 
        {8, 8, 30, 35, 10, 15, 20, 25, 50, 60, 65}, {8, 8, 25, 30, 20, 20, 20, 25, 50, 55, 60}, {8, 8, 0, 35, 10, 15, 20, 30, 65, 75, 85}};
    private SpartanWheel[] wheels = new SpartanWheel[5];

    public SpartanItems() {
        for (int i = 0; i < 5; ++i) {
            this.wheels[i] = new SpartanWheel();
            for (int j = 0; j < 11; ++j) {
                for (int k = 0; k < config[i][j]; ++k) {
                    this.wheels[i].addItem(SpartanItem.findItem((byte)j));
                }
            }
        }
    }

    public SpartanItem random(int wheel) {
        return this.wheels[wheel].random();
    }

    public void refundItem(SpartanItem item, int wheel) {
        this.wheels[wheel].addItem(item);
    }
}

