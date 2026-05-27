/*
 * Decompiled with CFR 0.152.
 */
package game.Jetty;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionUtils {
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        String[] pairs;
        LinkedHashMap<String, String> query_pairs = new LinkedHashMap<String, String>();
        if (query == null) {
            return query_pairs;
        }
        for (String pair : pairs = query.split("&")) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}

