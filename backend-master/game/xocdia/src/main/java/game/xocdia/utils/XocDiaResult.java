/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 *  game.utils.NumberUtils
 *  game.xocdia.conf.XocDiaConfig
 *  game.xocdia.conf.XocDiaForceResult
 *  scala.util.Random
 */
package game.xocdia.utils;

import game.utils.GameUtils;
import game.utils.NumberUtils;
import game.xocdia.conf.XocDiaConfig;
import game.xocdia.conf.XocDiaForceResult;
import game.xocdia.entities.PotType;
import java.util.ArrayList;
import java.util.List;
import scala.util.Random;

public class XocDiaResult {
    private byte count;
    private List<Integer> dinces;

    public XocDiaResult(byte count) {
        this.count = count;
    }

    public byte getCount() {
        return this.count;
    }

    public void setCount(byte count) {
        this.count = count;
    }

    public List<Integer> getDinces() {
        return this.dinces;
    }

    public void setDinces(List<Integer> dinces) {
        this.dinces = dinces;
    }

    public XocDiaResult() {
        this.count = 0;
        this.dinces = new ArrayList<Integer>();
        this.dinces.clear();
    }

    public void generateResult(List<Integer> rsCheat, XocDiaForceResult xdForce) {
        if (GameUtils.isCheat && rsCheat != null && rsCheat.size() == 4) {
            this.dinces = rsCheat;
            this.count = 0;
            for (Integer i : this.dinces) {
                if (i % 2 != 0) continue;
                this.count = (byte)(this.count + 1);
            }
        } else {
            int forceType = xdForce.forceType;
            Random rd = new Random(System.nanoTime());
            do {
                this.dinces = new ArrayList<Integer>();
                this.dinces.clear();
                this.count = 0;
                for (int i = 0; i < 4; ++i) {
                    int rNum = rd.nextInt(100);
                    if (rNum % 2 == 0) {
                        this.dinces.add(0);
                        this.count = (byte)(this.count + 1);
                        continue;
                    }
                    this.dinces.add(1);
                }
            } while (forceType == 0 && this.count % 2 != 0 || forceType == 1 && this.count % 2 == 0 || forceType == 3 && !this.checkListWinSuccess(xdForce.listWin));
            if (!(forceType != -1 || this.count != 0 && this.count != 4 || NumberUtils.isDoWithRatio((double)XocDiaConfig.ratioResult4))) {
                this.count = (byte)2;
                int dc = this.dinces.get(0) == 0 ? 1 : 0;
                int k1 = rd.nextInt(4);
                this.dinces.set(k1, dc);
                int k2 = 0;
                while ((k2 = rd.nextInt(4)) == k1) {
                }
                this.dinces.set(k2, dc);
            }
        }
    }

    private boolean checkListWinSuccess(List<Byte> listPWin) {
        List<Byte> listPotCanWin = this.getPotsWin();
        for (Byte pWin : listPotCanWin) {
            if (listPWin.contains(pWin)) continue;
            return false;
        }
        return true;
    }

    public List<Byte> getPotsWin() {
        ArrayList<Byte> potsId = new ArrayList<Byte>();
        switch (this.count) {
            case 0: {
                potsId.add(PotType.EVEN.getId());
                potsId.add(PotType.FOUR_BLACK.getId());
                break;
            }
            case 1: {
                potsId.add(PotType.ODD.getId());
                potsId.add(PotType.ONE_WHITE.getId());
                break;
            }
            case 2: {
                potsId.add(PotType.EVEN.getId());
                break;
            }
            case 3: {
                potsId.add(PotType.ODD.getId());
                potsId.add(PotType.ONE_BLACK.getId());
                break;
            }
            case 4: {
                potsId.add(PotType.EVEN.getId());
                potsId.add(PotType.FOUR_WHITE.getId());
                break;
            }
        }
        return potsId;
    }
}

