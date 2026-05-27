/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HttpClient {
    public static final int TIME_OUT = 60000;

    public static String get(String uri) throws Exception {
        int c;
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuffer response = new StringBuffer();
        while ((c = ((Reader)in).read()) >= 0) {
            response.append((char)c);
        }
        ((Reader)in).close();
        conn.disconnect();
        return response.toString();
    }

    public static String post(String uri, String postData) throws Exception {
        int c;
        URL url = new URL(uri);
        byte[] postDataBytes = postData.getBytes("UTF-8");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        OutputStream outputStream = conn.getOutputStream();
        outputStream.write(postDataBytes);
        outputStream.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuffer response = new StringBuffer();
        while ((c = ((Reader)in).read()) >= 0) {
            response.append((char)c);
        }
        ((Reader)in).close();
        outputStream.close();
        conn.disconnect();
        return response.toString();
    }

    /**
     * send email to game-email api
     * @param apiKey
     * @param uri
     * @param postData
     * @return
     * @throws Exception
     */
    public static String sendPostEmail(String apiKey, String uri, String postData) throws Exception {
        int c;
        URL url = new URL(uri);
        byte[] postDataBytes = postData.getBytes("UTF-8");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Api-Key", apiKey);
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        OutputStream outputStream = conn.getOutputStream();
        outputStream.write(postDataBytes);
        outputStream.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuffer response = new StringBuffer();
        while ((c = ((Reader)in).read()) >= 0) {
            response.append((char)c);
        }
        ((Reader)in).close();
        outputStream.close();
        conn.disconnect();
        return response.toString();
    }
}

