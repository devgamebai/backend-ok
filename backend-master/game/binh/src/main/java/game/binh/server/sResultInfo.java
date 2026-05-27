/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server;

import game.binh.server.logic.KetQuaSoBai;

public class sResultInfo {
    KetQuaSoBai[] resultWithPlayer = new KetQuaSoBai[4];

    public sResultInfo() {
        for (int i = 0; i < this.resultWithPlayer.length; ++i) {
            this.resultWithPlayer[i] = new KetQuaSoBai();
        }
    }

    public KetQuaSoBai getResultWithPlayer(int chair) {
        this.resultWithPlayer[chair].chair = chair;
        return this.resultWithPlayer[chair];
    }

    public void resetResult() {
        for (int i = 0; i < this.resultWithPlayer.length; ++i) {
            this.resultWithPlayer[i] = new KetQuaSoBai();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sResultInfo:------------------------------------------------------------------------------------\n");
        for (int i = 0; i < this.resultWithPlayer.length; ++i) {
            sb.append(this.resultWithPlayer[i]);
        }
        sb.append("------------------------------------------------------------------------------------------------\n");
        return sb.toString();
    }
}

