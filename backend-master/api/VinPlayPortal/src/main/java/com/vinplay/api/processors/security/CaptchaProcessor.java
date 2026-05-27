/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.github.cage.Cage
 *  com.github.cage.IGenerator
 *  com.github.cage.image.EffectConfig
 *  com.github.cage.image.Painter
 *  com.github.cage.image.Painter$Quality
 *  com.github.cage.image.ScaleConfig
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.utils.StringUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.security;

import com.github.cage.Cage;
import com.github.cage.IGenerator;
import com.github.cage.image.EffectConfig;
import com.github.cage.image.Painter;
import com.github.cage.image.ScaleConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.response.CaptchaResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.utils.StringUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import org.apache.log4j.Logger;

public class CaptchaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    // 2026-05-14: portal-side captcha (player register, c=124) is NOT gated
    // by CAPTCHA_REQUIRED any more — the env flag now only disables captcha
    // for admin + agency login flows (backend-api CaptchaProcessor). Player
    // signup keeps the captcha unconditionally as an anti-bot baseline.

    public String execute(Param<HttpServletRequest> param) {
        try {
            IGenerator<Font> fonts = new IGenerator<Font>(){

                public Font next() {
                    return new Font("Monospaced", 0, 8);
                }
            };
            Painter painter = new Painter(109, 38, (Color)null, Painter.Quality.MAX, new EffectConfig(false, false, false, false, new ScaleConfig(1.0f, 1.0f)), (Random)null);
            Cage cage = new Cage(painter, (IGenerator)fonts, (IGenerator)null, (String)null, (Float)null, (IGenerator)null, (Random)null);
            com.vinplay.vbee.common.cache.DistCache<String, String> captchaCache =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheCaptcha", String.class);
            String captcha = StringUtils.randomString((int)3);
            String idCaptcha = VinPlayUtils.getMD5Hash((String)captcha).substring(0, 3) + System.currentTimeMillis();
            captchaCache.put(idCaptcha, captcha, 3L, TimeUnit.MINUTES);
            String img = DatatypeConverter.printBase64Binary(cage.draw(captcha));
            CaptchaResponse res = new CaptchaResponse(idCaptcha, img);
            return res.toJson();
        }
        catch (Exception e) {
            logger.debug(e);
            return "";
        }
    }

}

