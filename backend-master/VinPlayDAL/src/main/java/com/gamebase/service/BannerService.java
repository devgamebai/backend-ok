/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service;

import com.gamebase.entities.BannerModel;
import java.util.List;

public interface BannerService {
    public Boolean addNewBanner(BannerModel var1);

    public Boolean updateBannerById(BannerModel var1);

    public Boolean deleteBanner(Integer var1);

    public List<BannerModel> getListActive();
}

