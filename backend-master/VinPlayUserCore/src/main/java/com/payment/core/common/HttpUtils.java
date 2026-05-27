/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.httpclient.DefaultHttpMethodRetryHandler
 *  org.apache.commons.httpclient.HttpClient
 *  org.apache.commons.httpclient.HttpMethod
 *  org.apache.commons.httpclient.HttpState
 *  org.apache.commons.httpclient.HttpVersion
 *  org.apache.commons.httpclient.NameValuePair
 *  org.apache.commons.httpclient.methods.GetMethod
 *  org.apache.commons.httpclient.methods.PostMethod
 *  org.apache.commons.httpclient.methods.RequestEntity
 *  org.apache.commons.httpclient.methods.StringRequestEntity
 *  org.apache.commons.httpclient.params.HttpClientParams
 *  org.apache.log4j.Logger
 */
package com.payment.core.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

public class HttpUtils {
    private static final Logger logger = Logger.getLogger(HttpUtils.class);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String postFormData(String url, Map<String, String> params) {
        HttpClient httpClient = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setParameter("http.useragent", "Mozilla/4.0 (compatible; FIREFOX 9.0; IBM AIX 5)");
        clientParams.setHttpElementCharset("UTF-8");
        HttpState httpState = new HttpState();
        httpClient.setParams(clientParams);
        httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(20000);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        httpClient.setState(httpState);
        clientParams.setVersion(HttpVersion.HTTP_1_1);
        long t1 = System.currentTimeMillis();
        PostMethod postMethod = new PostMethod(url);
        String responSt = null;
        try {
            postMethod.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler());
            ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) {
                list.add(new NameValuePair(key, params.get(key)));
            }
            postMethod.setRequestBody(list.toArray(new NameValuePair[0]));
            int statusCode = httpClient.executeMethod((HttpMethod)postMethod);
            if (statusCode != 200) {
                logger.error(("Method failed: " + postMethod.getStatusLine()));
            }
            long t2 = System.currentTimeMillis();
            logger.info(("EBET: Total elapsed http request/response time in milliseconds: " + (t2 - t1)));
            responSt = postMethod.getResponseBodyAsString();
        }
        catch (Exception ex) {
            logger.error("getWebContentByURL:", (Throwable)ex);
        }
        finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return responSt;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String postData(String url, String json) {
        HttpClient httpClient = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setParameter("http.useragent", "Mozilla/4.0 (compatible; FIREFOX 9.0; IBM AIX 5)");
        clientParams.setHttpElementCharset("UTF-8");
        HttpState httpState = new HttpState();
        httpClient.setParams(clientParams);
        httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(20000);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        httpClient.setState(httpState);
        clientParams.setVersion(HttpVersion.HTTP_1_1);
        long t1 = System.currentTimeMillis();
        PostMethod postMethod = new PostMethod(url);
        String responSt = null;
        try {
            postMethod.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler());
            StringRequestEntity requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
            postMethod.setRequestEntity((RequestEntity)requestEntity);
            int statusCode = httpClient.executeMethod((HttpMethod)postMethod);
            if (statusCode != 200) {
                logger.error(("Method failed: " + postMethod.getStatusLine()));
            }
            long t2 = System.currentTimeMillis();
            logger.info(("EBET: Total elapsed http request/response time in milliseconds: " + (t2 - t1)));
            responSt = postMethod.getResponseBodyAsString();
        }
        catch (Exception ex) {
            logger.error("getWebContentByURL:", (Throwable)ex);
        }
        finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return responSt;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String getData(String url) {
        HttpClient httpClient = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setParameter("http.useragent", "Mozilla/4.0 (compatible; FIREFOX 9.0; IBM AIX 5)");
        clientParams.setHttpElementCharset("UTF-8");
        HttpState httpState = new HttpState();
        httpClient.setParams(clientParams);
        httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(10000);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        httpClient.setState(httpState);
        clientParams.setVersion(HttpVersion.HTTP_1_1);
        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler());
        String responSt = null;
        try {
            int statusCode = httpClient.executeMethod((HttpMethod)getMethod);
            if (statusCode != 200) {
                logger.error(("Method failed: " + getMethod.getStatusLine()));
            }
            responSt = getMethod.getResponseBodyAsString();
        }
        catch (Exception ex) {
            logger.error(("getWebContentByURL:," + url), (Throwable)ex);
        }
        finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
        return responSt;
    }

    public static String SSLGetMethod(String urls) throws Exception {
        URL url = new URL(urls);
        HostnameVerifier hv = new HostnameVerifier(){

            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                logger.info(("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost()));
                return true;
            }
        };
        try {
            HttpUtils.trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        }
        catch (Exception ex) {
            logger.error("SSLGetMethod", (Throwable)ex);
        }
        InputStream in = url.openStream();
        BufferedReader bin = new BufferedReader(new InputStreamReader(in, "GB2312"));
        String s = null;
        StringBuilder sb = new StringBuilder("");
        while ((s = bin.readLine()) != null) {
            sb.append(s);
        }
        bin.close();
        in.close();
        return sb.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String sendData(String urls, String xml) {
        URL url = null;
        OutputStreamWriter out = null;
        BufferedReader br = null;
        URLConnection connection = null;
        try {
            try {
                url = new URL(urls);
            }
            catch (MalformedURLException e) {
                logger.error("URL ERROR", (Throwable)e);
            }
            HostnameVerifier hv = new HostnameVerifier(){

                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    logger.info(("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost()));
                    return true;
                }
            };
            try {
                HttpUtils.trustAllHttpsCertificates();
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            }
            catch (Exception ex) {
                logger.error("sendData", (Throwable)ex);
            }
            connection = url.openConnection();
            connection.setDoOutput(true);
            out = new OutputStreamWriter(connection.getOutputStream(), "8859_1");
            out.write(xml);
            br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String readLine = "";
            StringBuilder resultStr = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                resultStr.append(readLine);
            }
            String string = resultStr.toString();
            return string;
        }
        catch (IOException e) {
            logger.error("sendData", (Throwable)e);
        }
        finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                }
                catch (IOException e) {
                    logger.error("ex", (Throwable)e);
                }
            }
            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    logger.error("ex", (Throwable)e);
                }
            }
            if (connection != null) {
                ((HttpURLConnection)connection).disconnect();
            }
        }
        return null;
    }

    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        miTM tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    static class miTM
    implements TrustManager,
    X509TrustManager {
        miTM() {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }
    }
}

