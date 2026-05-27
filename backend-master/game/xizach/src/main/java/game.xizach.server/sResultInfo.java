/*
 * Decompiled with CFR 0.144.
 */
package game.xizach.server;

public class sResultInfo {
    public int chair;
    public long thangChuong = 0L;
    public long tongTienCuoiVan = 0L;
    public long currentTienThang = 0L;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====================BEGIN KET QUA======================\nchair:").append(this.chair).append("/");
        sb.append("thangChuong").append(this.thangChuong).append("/");
        sb.append("tienThangChuong:").append(this.tongTienCuoiVan).append("/");
        sb.append("\n====================END KET QUA======================\n");
        return sb.toString();
    }

    public long calculateTongTiLeThang() {
        long sum = 0L;
        return sum += this.thangChuong;
    }

    public long calculateThangChuong(boolean isChuong) {
        long sum = 0L;
        return sum += this.thangChuong;
    }

    public void reset() {
        this.thangChuong = 0L;
        this.tongTienCuoiVan = 0L;
        this.currentTienThang = 0L;
    }

    public long tongTienDatCuoc() {
        long sum = 0L;
        return ++sum;
    }

    public long tongTienDatCuocTreo() {
        long sum = 0L;
        return sum += 5L;
    }
}

