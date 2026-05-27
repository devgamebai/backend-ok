/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.xocdia.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class XocDiaForceResult {
    public int forceType;
    public List<Byte> listWin;

    public XocDiaForceResult(int forceType, List<Byte> listWin) {
        this.forceType = forceType;
        this.listWin = listWin;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

