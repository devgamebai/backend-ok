/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetTokenStreamProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(GetTokenStreamProcessor.class);
    private static final String TARGET_URL = "https://hitclub.me/get-tokenstream.html";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String execute(Param<HttpServletRequest> param) {
        StringBuilder rs;
        block26: {
            rs = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(TARGET_URL);
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("x-requested-with", "XMLHttpRequest");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));){
                        String line;
                        while ((line = in.readLine()) != null) {
                            rs.append(line);
                        }
                        break block26;
                    }
                }
                logger.error(("GetTokenStreamProcessor - HTTP error code: " + responseCode));
            }
            catch (Exception e) {
                logger.error("GetTokenStreamProcessor - error when calling target URL", (Throwable)e);
            }
            finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    }
                    catch (Exception exception) {}
                }
            }
        }
        return rs.toString();
    }
}

