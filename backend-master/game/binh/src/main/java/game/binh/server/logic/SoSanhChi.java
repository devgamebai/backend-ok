/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic;

public class SoSanhChi {
    public int chiCount1 = 0;
    public int chiCount2 = 0;

    public boolean motSapHai() {
        return this.chiCount2 > this.chiCount1;
    }

    public boolean haiSapMot() {
        return this.chiCount1 > this.chiCount2;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("chiCount1").append(this.chiCount1).append("/");
        sb.append("chiCount2").append(this.chiCount2).append("/");
        return sb.toString();
    }
}

