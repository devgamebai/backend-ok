package bitzero.util.socialcontroller.business.zm;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.exceptions.SocialControllerException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ZM_API_Common {
     private String subApi = "";
     protected String api = "";
     protected String secret = "";
     private final String VERSION = "2.0";
     private ZM_Config config;
     private final Logger logger = LoggerFactory.getLogger(this.getClass());

     public ZM_API_Common(ZM_Config config, String subApi) {
          this.config = config;
          this.subApi = subApi;
          this.secret = config.secret;
          this.setAPI(config.apiURL);
     }

     public String getAPI() {
          return this.api;
     }

     public void setAPI(String api) {
          this.api = api + this.subApi;
     }

     public String callMethod(String method, Map args) throws SocialControllerException {
          this.updateArgs(method, args);
          String respone = this.sendRequest(args);
          String result = this.parseRespone(respone);
          return result;
     }

     protected static String encryptMD5(String input) {
          byte[] defaultBytes = input.getBytes();

          try {
               MessageDigest algorithm = MessageDigest.getInstance("MD5");
               algorithm.reset();
               algorithm.update(defaultBytes);
               byte[] messageDigest = algorithm.digest();
               StringBuilder hexString = new StringBuilder();

               for(int i = 0; i < messageDigest.length; ++i) {
                    String hex = Integer.toHexString(255 & messageDigest[i]);
                    if (hex.length() == 1) {
                         hexString.append('0');
                    }

                    hexString.append(hex);
               }

               return hexString + "";
          } catch (NoSuchAlgorithmException var7) {
               CommonHandle.writeErrLog((Throwable)var7);
               return null;
          }
     }

     public void updateArgs(String method, Map args) {
          args.put("api_key", this.config.apiKey);
          args.put("v", "2.0");
          args.put("method", method);
          long now = System.nanoTime();
          long ms = now / 1000L;
          long nano = now % 1000L;
          args.put("call_id", ms + "." + nano);
          this.signRequest(args);
     }

     public void signRequest(Map args) {
          if (args.containsKey("sig")) {
               args.remove("sig");
          }

          String sig = "";
          Object[] arrKey = args.keySet().toArray();
          Arrays.sort(arrKey);

          for(int i = 0; i < arrKey.length; ++i) {
               String k = (String)arrKey[i];
               sig = sig + k + "=" + (String)args.get(k);
          }

          sig = sig + this.secret;
          args.put("sig", encryptMD5(sig));
     }

     public String sendRequest(Map args) throws SocialControllerException {
          String urlcoded = null;
          String result = null;

          try {
               urlcoded = encode(args);
               this.logger.info("url link : " + this.api + "?" + urlcoded);
               HttpClient httpclient = new DefaultHttpClient();
               HttpPost httppost = new HttpPost(this.api + "?" + urlcoded);
               HttpResponse response = httpclient.execute(httppost);
               HttpEntity entity = response.getEntity();
               result = EntityUtils.toString(entity, "UTF-8");
               this.logger.info(" response from Zing Me : " + result);
               return result;
          } catch (Exception var8) {
               CommonHandle.writeErrLog((Throwable)var8);
               throw new SocialControllerException(-1, " loi ");
          }
     }

     protected static String encode(Map args) {
          String result = "";
          Object[] arrKey = args.keySet().toArray();
          Arrays.sort(arrKey);

          for(int i = 0; i < arrKey.length; ++i) {
               String k = (String)arrKey[i];

               try {
                    result = result + k + "=" + URLEncoder.encode((String)args.get(k), "UTF-8") + "&";
               } catch (UnsupportedEncodingException var6) {
                    CommonHandle.writeErrLog((Throwable)var6);
               }
          }

          result = removeCharAt(result, result.length() - 1);
          return result;
     }

     private static String removeCharAt(String s, int pos) {
          StringBuilder buf = new StringBuilder(s.length() - 1);
          buf.append(s.substring(0, pos)).append(s.substring(pos + 1));
          return buf.toString();
     }

     private String parseRespone(String respone) throws SocialControllerException {
          try {
               JSONObject data = new JSONObject(respone);
               return data.getString("data");
          } catch (JSONException var3) {
               throw new SocialControllerException(-2, " loi ");
          }
     }
}
