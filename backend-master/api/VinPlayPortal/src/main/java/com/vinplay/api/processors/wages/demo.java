/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.processors.wages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class demo {
    public static void main(String[] args) {
        System.out.println(demo.checkExist("ditmemay1", "123456"));
    }

    private static boolean checkExist(String username, String password) {
        String url = "https://bodergatez.dsrcgoms.net/user/register.aspx";
        String payload = "{\"fullname\":\"" + username + "\",\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"app_id\":\"bc114103\",\"avatar\":\"Avatar51\",\"os\":\"Windows\",\"device\":\"Computer\",\"browser\":\"chrome\",\"fg\":\"e4f4306109883b8ae06cee43e1ee669a\",\"aff_id\":\"hit\"}";
        try {
            String line;
            URL apiURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)apiURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("authority", "bodergatez.dsrcgoms.net");
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("accept-language", "en-US,en;q=0.9,vi;q=0.8");
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("dnt", "1");
            connection.setRequestProperty("origin", "https://play.hit32.club");
            connection.setRequestProperty("referer", "https://play.hit32.club/");
            connection.setRequestProperty("sec-ch-ua", "\"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"114\", \"Microsoft Edge\";v=\"114\"");
            connection.setRequestProperty("sec-ch-ua-mobile", "?0");
            connection.setRequestProperty("sec-ch-ua-platform", "Windows");
            connection.setRequestProperty("sec-fetch-dest", "empty");
            connection.setRequestProperty("sec-fetch-mode", "cors");
            connection.setRequestProperty("sec-fetch-site", "cross-site");
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.82");
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.getBytes());
            outputStream.flush();
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            String data = response.toString();
            System.out.println("Response Body: " + data);
            connection.disconnect();
            return data.contains("EXISTED");
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

