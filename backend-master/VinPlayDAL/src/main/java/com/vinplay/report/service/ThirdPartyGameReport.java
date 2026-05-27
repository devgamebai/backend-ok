/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.report.service;

import com.vinplay.common.game3rd.AGGameRecordItem;
import com.vinplay.common.game3rd.CmdGameRecords;
import com.vinplay.common.game3rd.IbcGameRecordItem;
import com.vinplay.common.game3rd.SboGameRecord;
import com.vinplay.common.game3rd.ThirdPartyResponse;
import com.vinplay.common.game3rd.WMGameRecordItem;
import java.util.Date;
import java.util.List;

public interface ThirdPartyGameReport {
    public ThirdPartyResponse<List<AGGameRecordItem>> filterAG(String var1, String var2, String var3, String var4, int var5, String var6, String var7, Double var8, Double var9, String var10, int var11, int var12, String var13);

    public ThirdPartyResponse<List<WMGameRecordItem>> filterWM(String var1, String var2, String var3, String var4, Long var5, int var6, String var7, String var8, int var9, int var10);

    public ThirdPartyResponse<List<IbcGameRecordItem>> filterIBC(Long var1, String var2, String var3, Integer var4, Integer var5, String var6, Integer var7, String var8, Integer var9, String var10, String var11, Integer var12, Long var13, String var14, Double var15, String var16, Double var17, Double var18, Double var19, Integer var20, Integer var21, String var22, String var23, String var24, String var25, Long var26, String var27, Long var28, Date var29, Integer var30, String var31, String var32, Integer var33, Long var34, Long var35, String var36, int var37, String var38, String var39, int var40, int var41);

    public ThirdPartyResponse<List<CmdGameRecords>> filterCMD(String var1, String var2, Long var3, String var4, Long var5, String var6, String var7, Double var8, Double var9, Double var10, Double var11, String var12, Double var13, Double var14, String var15, String var16, String var17, Double var18, String var19, Integer var20, Integer var21, Integer var22, Integer var23, String var24, String var25, String var26, Integer var27, Long var28, String var29, Long var30, Integer var31, Integer var32, Integer var33, String var34, Integer var35, Long var36, Double var37, String var38, Double var39, Double var40, Double var41, Integer var42, String var43, Double var44, Long var45, String var46, String var47, String var48, String var49, String var50, String var51, Double var52, Double var53, Double var54, int var55, String var56, String var57, int var58, int var59);

    public List<AGGameRecordItem> showDetailAG(String var1);

    public List<WMGameRecordItem> showDetailWM(Long var1);

    public List<IbcGameRecordItem> showDetailIBC(Long var1);

    public List<CmdGameRecords> showDetailCMD(Long var1);

    public ThirdPartyResponse<List<SboGameRecord>> filterSBO(String var1, String var2, String var3, String var4, int var5, String var6, String var7, Double var8, Double var9, int var10, int var11, String var12);
}

