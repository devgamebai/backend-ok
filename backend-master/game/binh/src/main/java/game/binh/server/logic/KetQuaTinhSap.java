/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic;

public class KetQuaTinhSap {
    public int tinhSap1;
    public int tinhSap2;
    public int tongChiThang;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tinhSap1:").append(this.tinhSap1).append("/");
        sb.append("tinhSap2:").append(this.tinhSap2).append("/");
        sb.append("tongChiThang:").append(this.tongChiThang).append("/");
        return sb.toString();
    }
}

