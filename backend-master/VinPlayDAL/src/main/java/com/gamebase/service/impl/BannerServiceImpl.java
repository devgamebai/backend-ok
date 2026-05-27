package com.gamebase.service.impl;

import com.gamebase.dao.BannerDAO;
import com.gamebase.dao.impl.BannerDAOImpl;
import com.gamebase.entities.BannerModel;
import com.gamebase.service.BannerService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BannerServiceImpl implements BannerService {

    private final BannerDAO bannerDao = new BannerDAOImpl();

    private DistCache<Integer, BannerModel> cache() {
        return CacheFactory.get("bannerCache", BannerModel.class);
    }

    @Override
    public Boolean addNewBanner(BannerModel bannerModel) {
        boolean success = bannerDao.addNewBanner(bannerModel);
        if (success) cache().clear();
        return success;
    }

    @Override
    public Boolean updateBannerById(BannerModel bannerModel) {
        boolean success = bannerDao.updateBannerById(bannerModel);
        if (success) cache().clear();
        return success;
    }

    @Override
    public Boolean deleteBanner(Integer id) {
        boolean success = bannerDao.deleteBanner(id);
        if (success) cache().clear();
        return success;
    }

    @Override
    public List<BannerModel> getListActive() {
        DistCache<Integer, BannerModel> bannerCache = cache();
        List<BannerModel> banners = new ArrayList<>();
        if (bannerCache.isEmpty()) {
            BannerDAOImpl dao = new BannerDAOImpl();
            banners = dao.getListActive();
            for (BannerModel b : banners) {
                bannerCache.put(b.getId(), b, 30L, TimeUnit.MINUTES);
            }
        } else {
            for (Map.Entry<Integer, BannerModel> e : bannerCache.entrySet()) {
                banners.add(e.getValue());
            }
        }
        return banners;
    }
}
