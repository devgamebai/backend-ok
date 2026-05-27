/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class HistoryTaiXiuLiveProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public String execute(Param<HttpServletRequest> param) {
        StringBuilder rs = new StringBuilder();
        try {
            URL url = new URL("https://api-tx-g8.quayso1.com/api/v1/round/running/v2/?token=1-22d536ea268d0e813a62f0e6323148d5");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("authority", "api-tx-g8.quayso1.com");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("accept-language", "en-US,en;q=0.9,vi;q=0.8");
            conn.setRequestProperty("dnt", "1");
            conn.setRequestProperty("origin", "https://i.go88.us/");
            conn.setRequestProperty("referer", "https://i.go88.us/");
            conn.setRequestProperty("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Microsoft Edge\";v=\"121\", \"Chromium\";v=\"121\"");
            conn.setRequestProperty("sec-ch-ua-mobile", "?0");
            conn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
            conn.setRequestProperty("sec-fetch-dest", "empty");
            conn.setRequestProperty("sec-fetch-mode", "cors");
            conn.setRequestProperty("sec-fetch-site", "cross-site");
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String inputLine;
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    rs.append(inputLine);
                }
                in.close();
                System.out.println("Response:");
                System.out.println(rs.toString());
            } else {
                System.out.println("GET request failed. Response code: " + responseCode);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return rs.toString();
    }
}

