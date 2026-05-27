/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.client.methods.CloseableHttpResponse
 */
package com.vinplay.otp.sender.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.client.methods.CloseableHttpResponse;

public class Utils {
    public static String getResponse(CloseableHttpResponse response) throws Exception {
        String respStr = "";
        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            String output;
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((output = br.readLine()) != null) {
                respStr = respStr + output;
            }
        }
        if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 201) {
            throw new Exception(respStr);
        }
        return respStr;
    }
}

