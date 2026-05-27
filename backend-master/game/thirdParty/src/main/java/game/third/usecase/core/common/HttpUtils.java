/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.io.IOUtils
 *  org.apache.http.HttpEntity
 *  org.apache.http.HttpResponse
 *  org.apache.http.client.ClientProtocolException
 *  org.apache.http.client.HttpClient
 *  org.apache.http.client.methods.HttpEntityEnclosingRequestBase
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpRequestBase
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.conn.ClientConnectionManager
 *  org.apache.http.conn.scheme.Scheme
 *  org.apache.http.conn.scheme.SchemeRegistry
 *  org.apache.http.conn.scheme.SocketFactory
 *  org.apache.http.conn.ssl.SSLSocketFactory
 *  org.apache.http.entity.StringEntity
 *  org.apache.http.impl.client.DefaultHttpClient
 *  org.apache.http.params.BasicHttpParams
 *  org.apache.http.params.HttpConnectionParams
 *  org.apache.http.params.HttpParams
 */
package game.third.usecase.core.common;

import game.third.usecase.core.exception.ConnectionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpUtils {
    private static final String DEFAULT_ENCODING = "UTF-8";
    private transient StringEntity entity;
    private final transient HttpRequestBase request;

    private HttpUtils(HttpRequestBase request, StringEntity entity) {
        this.request = request;
        this.entity = entity;
    }

    private HttpUtils(HttpRequestBase request) {
        this.request = request;
    }

    public static HttpUtils newPost(String url, String entity) throws UnsupportedEncodingException {
        HttpPost base = new HttpPost(url);
        base.addHeader("Content-Type", "application/x-www-form-urlencoded");
        StringEntity stringEntity = new StringEntity(entity, DEFAULT_ENCODING);
        return new HttpUtils((HttpRequestBase)base, stringEntity);
    }

    public static HttpUtils newGet(String url) {
        HttpGet base = new HttpGet(url);
        HttpUtils template = new HttpUtils((HttpRequestBase)base);
        return template;
    }

    public String execute() {
        HttpResponse response;
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout((HttpParams)httpParams, (int)30000);
        HttpConnectionParams.setSoTimeout((HttpParams)httpParams, (int)15000);
        DefaultHttpClient client = new DefaultHttpClient((HttpParams)httpParams);
        HttpClient httpclient = UntrustedHTTPsWrapper.wrap((HttpClient)client);
        if (this.entity != null && this.request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)this.request).setEntity((HttpEntity)this.entity);
        }
        try {
            response = httpclient.execute((HttpUriRequest)this.request);
        }
        catch (ClientProtocolException ex) {
            throw new ConnectionException("IO error!", ex);
        }
        catch (IOException ex) {
            throw new ConnectionException("client protocol error!", ex);
        }
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            throw new RuntimeException("Http fail, status=" + status + ", reason=" + response.getStatusLine().getReasonPhrase(), null);
        }
        try {
            HttpEntity result = response.getEntity();
            InputStream iscont = result.getContent();
            byte[] bscont = IOUtils.toByteArray((InputStream)iscont);
            String str = new String(bscont, DEFAULT_ENCODING);
            if (str == null || str.isEmpty()) {
                throw new RuntimeException("Server returns HTTP 200 with empty string.", null);
            }
            return str;
        }
        catch (IOException ex) {
            throw new ConnectionException("get content error!", ex);
        }
        catch (IllegalStateException ex) {
            throw new RuntimeException("http invalid state!", ex);
        }
    }

    private static class UntrustedHTTPsWrapper {
        private UntrustedHTTPsWrapper() {
        }

        public static HttpClient wrap(HttpClient base) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                X509TrustManager tm = new X509TrustManager(){

                    @Override
                    public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                ctx.init(null, new TrustManager[]{tm}, null);
                SSLSocketFactory ssf = new SSLSocketFactory(ctx);
                ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                ClientConnectionManager ccm = base.getConnectionManager();
                SchemeRegistry srg = ccm.getSchemeRegistry();
                srg.register(new Scheme("https", (SocketFactory)ssf, 443));
                return new DefaultHttpClient(ccm, base.getParams());
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}

