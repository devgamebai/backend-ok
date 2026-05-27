/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class CauXocDia {
    private int index = 0;
    private String data;

    public void setData(String data) {
        this.data = data;
        this.index = data.length() - 1;
    }

    public int getResult() {
        if (this.index < 0 || this.index >= this.data.length()) {
            return -1;
        }
        int result = Integer.parseInt("" + this.data.charAt(this.index));
        --this.index;
        return result;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getData() {
        return this.data;
    }
}

