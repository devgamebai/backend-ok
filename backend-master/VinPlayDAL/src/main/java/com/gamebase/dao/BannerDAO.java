/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao;

import com.gamebase.entities.BannerModel;
import java.util.List;

public interface BannerDAO {
    public long countlistBanner(String var1, Integer var2, String var3, Integer var4, String var5);

    public List<BannerModel> listBanner(String var1, Integer var2, String var3, Integer var4, String var5, int var6, int var7);

    public BannerModel BannerDetail(Integer var1);

    public Boolean addNewBanner(BannerModel var1);

    public Boolean updateBannerById(BannerModel var1);

    public Boolean deleteBanner(Integer var1);

    public Boolean deleteBanner(String var1);

    public List<BannerModel> getListActive();
}

