/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

public class HttpURLClient {
    private static final String USER_AGENT = "Mozilla/5.0";

    public static String sendGET(String url) throws IOException {
        String inputLine;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection)obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    public static String sendPOST(String url, Map<String, String> param) throws IOException {
        String inputLine;
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection)obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        String urlParameters = HttpURLClient.buildParams(param);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    public static String buildParams(Map<String, String> fields) throws UnsupportedEncodingException {
        StringBuffer buf = new StringBuffer("");
        ArrayList<String> fieldNames = new ArrayList<String>(fields.keySet());
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String)itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                buf.append(URLEncoder.encode(fieldName, "UTF-8"));
                buf.append('=');
                buf.append(URLEncoder.encode(fieldValue, "UTF-8"));
            }
            if (!itr.hasNext()) continue;
            buf.append('&');
        }
        return buf.toString();
    }
}

