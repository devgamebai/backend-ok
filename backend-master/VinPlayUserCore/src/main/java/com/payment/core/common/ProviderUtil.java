/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.json.JSONObject
 */
package com.payment.core.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONObject;

public class ProviderUtil {
    public static JSONObject getResponse(CloseableHttpResponse response) throws Exception {
        String respStr = "";
        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            String output;
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((output = br.readLine()) != null) {
                respStr = respStr + output;
            }
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception(respStr);
        }
        JSONObject obj = new JSONObject(respStr);
        return obj;
    }

    public static JSONObject getResponseAll(CloseableHttpResponse response) throws Exception {
        String respStr = "";
        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            String output;
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((output = br.readLine()) != null) {
                respStr = respStr + output;
            }
        }
        JSONObject obj = new JSONObject(respStr);
        return obj;
    }
}

