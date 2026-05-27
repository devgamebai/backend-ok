/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.http.HttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.impl.client.DefaultHttpClient
 */
package game.third.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class Request {
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static boolean isPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod());
    }

    public static boolean isGet(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod());
    }

    public static String getRequest(String url) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        StringBuffer result = new StringBuffer();
        String line = "";
        try {
            HttpResponse response = client.execute((HttpUriRequest)get);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        catch (Exception e) {
            return "";
        }
        return result.toString();
    }
}

