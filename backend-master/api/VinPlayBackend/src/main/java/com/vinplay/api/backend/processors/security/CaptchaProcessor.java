package com.vinplay.api.backend.processors.security;

import com.github.cage.Cage;
import com.github.cage.IGenerator;
import com.github.cage.image.EffectConfig;
import com.github.cage.image.Painter;
import com.github.cage.image.ScaleConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.json.JSONObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.utils.StringUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CaptchaProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static boolean captchaRequired() {
        String v = System.getenv("CAPTCHA_REQUIRED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        // 2026-05-13: if CAPTCHA_REQUIRED=0, return a "disabled" sentinel so
        // the admin/agency FE can skip rendering the captcha image. Previously
        // the generator always returned an image even when LoginAdminProcessor
        // would skip validation — so operators saw a captcha that wasn't
        // actually enforced.
        if (!captchaRequired()) {
            JSONObject res = new JSONObject();
            res.put("disabled", true);
            return res.toString();
        }
        try {
            IGenerator<Font> fonts = new IGenerator<Font>() {
                private final Random rng = new Random();
                private final String[] families = {"SansSerif", "Serif", "Monospaced"};
                @Override
                public Font next() {
                    int style = rng.nextBoolean() ? Font.BOLD : Font.ITALIC;
                    return new Font(families[rng.nextInt(families.length)], style, 40 + rng.nextInt(8));
                }
            };
            // ripple=true, blur=FALSE (removes blurriness), outline=true, rotate=true
            Painter painter = new Painter(300, 100, Color.WHITE, Painter.Quality.MAX, 
                    new EffectConfig(true, false, true, true, new ScaleConfig(0.95f, 1.05f)), null);
            Cage cage = new Cage(painter, fonts, null, "png", null, null, null);
            com.vinplay.vbee.common.cache.DistCache<String, String> captchaCache =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheCaptcha", String.class);

            String captcha = StringUtils.randomString(5);
            String idCaptcha = java.util.UUID.randomUUID().toString();
            
            captchaCache.put(idCaptcha, captcha, 3L, TimeUnit.MINUTES);
            String img = DatatypeConverter.printBase64Binary(cage.draw(captcha));
            
            JSONObject res = new JSONObject();
            res.put("id", idCaptcha);
            res.put("img", img);
            return res.toString();
        } catch (Exception e) {
            logger.error("Error generating captcha", e);
            return "{\"success\":false,\"errorCode\":\"500\",\"message\":\"captcha gen failed\"}";
        }
    }
}
