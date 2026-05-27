/*
 * Decompiled with CFR 0.144.
 */
package game.bacay.server;

public class sResultInfo {
    public int chair;
    public int cuocChuong;
    public int cuocGa;
    public int[] cuocKeCua = new int[8];
    public int[] cuocDanhBien = new int[8];
    public int[] duocYeuCauDanhBien = new int[8];
    public int thangChuong;
    public int thangGa;
    public int[] thangKeCua = new int[8];
    public int[] thangBien = new int[8];
    public long tienThangChuong = 0L;
    public long tienThangGa = 0L;
    public long[] tienThangKeCua = new long[8];
    public long[] tienThangBien = new long[8];
    public long tongTienCuoiVan;

    public String toString() {
        int i;
        StringBuilder sb = new StringBuilder();
        sb.append("\n====================BEGIN KET QUA======================\nchair:").append(this.chair).append("/");
        sb.append("cuocChuong:").append(this.cuocChuong).append("/");
        sb.append("cuocGa:").append(this.cuocGa).append("/\n");
        for (i = 0; i < 8; ++i) {
            if (this.cuocKeCua[i] != 0) {
                sb.append("cuocKeCua:").append(i).append(":").append(this.cuocKeCua[i]).append("/");
            }
            if (this.cuocDanhBien[i] != 0) {
                sb.append("cuocDanhBien:").append(i).append(":").append(this.cuocDanhBien[i]).append("/");
            }
            if (this.duocYeuCauDanhBien[i] == 0) continue;
            sb.append("duocYeuCauDanhBien:").append(i).append(":").append(this.duocYeuCauDanhBien[i]).append("/\n");
        }
        sb.append("thangChuong").append(this.thangChuong).append("/");
        sb.append("thangGa").append(this.thangGa).append("/\n");
        for (i = 0; i < 8; ++i) {
            if (this.thangKeCua[i] != 0) {
                sb.append("thangKeCua:").append(i).append(":").append(this.thangKeCua[i]).append("/");
            }
            if (this.thangBien[i] == 0) continue;
            sb.append("thangBien:").append(i).append(":").append(this.thangBien[i]).append("/\n");
        }
        sb.append("tienThangChuong:").append(this.tienThangChuong).append("/");
        sb.append("tienThangGa:").append(this.tienThangGa).append("/\n");
        for (i = 0; i < 8; ++i) {
            if (this.tienThangKeCua[i] != 0L) {
                sb.append("tienThangKeCua:").append(i).append(":").append(this.tienThangKeCua[i]).append("/");
            }
            if (this.tienThangBien[i] == 0L) continue;
            sb.append("tienThangBien:").append(i).append(":").append(this.tienThangBien[i]).append("\n");
        }
        sb.append("\n====================END KET QUA======================\n");
        return sb.toString();
    }

    public boolean daCuocChuong() {
        return this.cuocChuong != 0;
    }

    public long calculateTongTiLeThang() {
        long sum = 0L;
        sum += (long)this.thangChuong;
        sum += (long)this.thangGa;
        for (int i = 0; i < 8; ++i) {
            sum += (long)this.thangKeCua[i];
            sum += (long)this.thangBien[i];
        }
        return sum;
    }

    public long calculateThangChuong(boolean isChuong) {
        long sum = 0L;
        sum += (long)this.thangChuong;
        if (!isChuong) {
            for (int i = 0; i < 8; ++i) {
                sum += (long)this.thangKeCua[i];
            }
        }
        return sum;
    }

    public void reset() {
        this.cuocChuong = 0;
        this.cuocGa = 0;
        this.thangChuong = 0;
        this.thangGa = 0;
        this.tienThangChuong = 0L;
        this.tienThangGa = 0L;
        this.tongTienCuoiVan = 0L;
        for (int i = 0; i < 8; ++i) {
            this.cuocKeCua[i] = 0;
            this.cuocDanhBien[i] = 0;
            this.duocYeuCauDanhBien[i] = 0;
            this.thangKeCua[i] = 0;
            this.thangBien[i] = 0;
            this.tienThangKeCua[i] = 0L;
            this.tienThangBien[i] = 0L;
        }
    }

    private void tinhTienThangChuong(long chuongRate, long moneyBet) {
        if (this.thangChuong > 0) {
            this.tienThangChuong = (long)this.thangChuong * chuongRate;
            for (int i = 0; i < 8; ++i) {
                this.tienThangKeCua[i] = (long)this.thangKeCua[i] * chuongRate;
            }
        } else {
            this.tienThangChuong = (long)this.thangChuong * moneyBet;
            for (int i = 0; i < 8; ++i) {
                this.tienThangKeCua[i] = (long)this.thangKeCua[i] * moneyBet;
            }
        }
    }

    public void tinhTienThangTongNguoiChoi(long chuongRate, long moneyBet) {
        this.tinhTienThangChuong(chuongRate, moneyBet);
        this.tienThangGa = (long)this.thangGa * moneyBet;
        this.tongTienCuoiVan = this.tienThangChuong + this.tienThangGa;
        for (int i = 0; i < 8; ++i) {
            if (i == this.chair) continue;
            this.tienThangBien[i] = (long)this.thangBien[i] * moneyBet;
            this.tongTienCuoiVan += this.tienThangBien[i];
            this.tongTienCuoiVan += this.tienThangKeCua[i];
        }
    }

    public long tongTienDatCuoc() {
        long sum = 0L;
        sum += (long)this.cuocChuong;
        sum += (long)this.cuocGa;
        for (int i = 0; i < 8; ++i) {
            sum += (long)this.cuocKeCua[i];
            sum += (long)this.cuocDanhBien[i];
        }
        return sum;
    }

    public long tongTienDatCuocTreo() {
        long sum = 0L;
        sum += (long)(this.cuocChuong * 4);
        sum += (long)this.cuocGa;
        for (int i = 0; i < 8; ++i) {
            sum += (long)(this.cuocKeCua[i] * 4);
            sum += (long)(this.cuocDanhBien[i] * 4);
        }
        return sum;
    }

    public long tongTienDanhBien() {
        long sum = 0L;
        for (int i = 0; i < 8; ++i) {
            sum += this.tienThangBien[i];
        }
        return sum;
    }

    public long tongTienKeCua() {
        long sum = 0L;
        for (int i = 0; i < 8; ++i) {
            sum += this.tienThangKeCua[i];
        }
        return sum;
    }

    public void tinhTienThangTongCuaChuong(long moneyBet) {
        this.tongTienCuoiVan = this.tienThangChuong = (long)this.thangChuong * moneyBet;
    }
}

