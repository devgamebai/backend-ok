/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.models.SocialModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.api.utils;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.models.SocialModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class SocialUtils {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static final String API_FACEBOOK = "https://graph.facebook.com/v2.7/me?access_token=";
    private static final String API_GOOGLE = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final String[] GG_IDS = new String[]{"632901182605-s59smrvsrd3resph64nmbiovaa6v08oj.apps.googleusercontent.com", "632901182605-6cf5gj22ef531btoja5j6ebuccmson1f.apps.googleusercontent.com", "632901182605-35bmu82riimsndsr9g65a9fn4osunjv1.apps.googleusercontent.com", "632901182605-t0p14rbls6h017jspnrd8fantpqb4ooi.apps.googleusercontent.com", "632901182605-oalvs5e7gk6nu9kqea90bcf5e9bhkeli.apps.googleusercontent.com", "632901182605-idkb0lrdompljkdmfmoq2p7bc56ore40.apps.googleusercontent.com", "632901182605-ah8sn546fs259v87gaiinhf6ugopbr99.apps.googleusercontent.com", "632901182605-imk18vb4q850ddqn6m43c05vm8867jcu.apps.googleusercontent.com", "632901182605-h5qhgpad213jmo4mch2vf4t90fj7tb95.apps.googleusercontent.com", "632901182605-vl30qrp1svkg4mqa1ggrqa1qg17l7som.apps.googleusercontent.com", "632901182605-86fgr3vj27tf353nm2q8n347hh0s5sg9.apps.googleusercontent.com", "632901182605-mqkpean5vceq844fipah1amd36osmbu0.apps.googleusercontent.com", "632901182605-evru2518innmn7uf9atqe01m9mt5ktt8.apps.googleusercontent.com", "632901182605-u90388ie3s2v5mqb4iiql3gkgeej46d9.apps.googleusercontent.com", "632901182605-tjlq8d2433p2d90ibko12hmhkqn2c5ng.apps.googleusercontent.com", "632901182605-9aaavf95hm6tu2lpv0htrski7jrnff5f.apps.googleusercontent.com", "632901182605-7jact08ff5tq6ms0mrk6jo2klkr5u2it.apps.googleusercontent.com"};

    public static void socialSuccess(IMap<String, SocialModel> socialMap, String socialId, String accessToken) {
        if (socialMap.containsKey(socialId)) {
            try {
                socialMap.lock(socialId);
                SocialModel model = (SocialModel)socialMap.get(socialId);
                model.setAccessToken(accessToken);
                model.setLoginTime(new Date());
                socialMap.put(socialId, model);
            }
            catch (Exception e) {
                logger.debug(e);
                return;
            }
            try {
                socialMap.unlock(socialId);
            }
            catch (Exception e) {}
        } else {
            socialMap.put(socialId, new SocialModel(accessToken, socialId, new Date()));
        }
    }

    public static String getSocialId(IMap<String, SocialModel> socialMap, String accessToken, String social) {
        String id = null;
        if (socialMap != null) {
            for (Map.Entry entry : socialMap.entrySet()) {
                if (!((SocialModel)entry.getValue()).getAccessToken().equals(accessToken) || VinPlayUtils.socialTimeout((Date)((SocialModel)entry.getValue()).getLoginTime())) continue;
                id = ((SocialModel)entry.getValue()).getSocialId();
                break;
            }
        }
        if (id == null) {
            if (social.equals("fb")) {
                id = SocialUtils.getIdFacebook(accessToken);
            } else if (social.equals("gg")) {
                id = SocialUtils.getIdGoogle(accessToken);
            }
        }
        return id;
    }

    private static String getIdFacebook(String accessToken) {
        try {
            String res = SocialUtils.getData(API_FACEBOOK + accessToken);
            if (!res.isEmpty()) {
                JSONObject obj = new JSONObject(res);
                String id = obj.getString("id");
                return id;
            }
            return "";
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    private static String getIdGoogle(String accessToken) {
        try {
            String res = SocialUtils.getData(API_GOOGLE + accessToken);
            if (!res.isEmpty()) {
                JSONObject obj = new JSONObject(res);
                String id = obj.getString("sub");
                String clientId = obj.getString("azp");
                for (String str : GG_IDS) {
                    if (!str.equalsIgnoreCase(clientId)) continue;
                    return id;
                }
                return "";
            }
            return "";
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    private static String getData(String sUrl) throws IOException {
        String output;
        String res = "";
        URL url = new URL(sUrl);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (conn.getResponseCode() != 200) {
            return res;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((output = br.readLine()) != null) {
            res = String.valueOf(res) + output;
        }
        conn.disconnect();
        return res;
    }
}

