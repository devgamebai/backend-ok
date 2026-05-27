package bitzero.engine.websocket.netty;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vinplay.vbee.common.config.VBeePath;

public final class WebSocketSslServerSslContext {
     private static final Logger logger = LoggerFactory.getLogger(WebSocketSslServerSslContext.class);
     private static final String PROTOCOL = "TLS";
     private final SSLContext _serverContext;
     private static String basePath = VBeePath.basePath;

     public static WebSocketSslServerSslContext getInstance() {
          return SingletonHolder.INSTANCE;
     }

     private WebSocketSslServerSslContext() {
          SSLContext serverContext = null;

          try {
               String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
               if (algorithm == null) {
                    algorithm = "SunX509";
               }

               try {
                    String keyStoreFilePath = System.getProperty("keystore.file.path");
                    String keyStoreFilePassword = System.getProperty("keystore.file.password");
                    KeyStore ks = KeyStore.getInstance("JKS");
                    FileInputStream fin = new FileInputStream(basePath.concat(keyStoreFilePath));
                    ks.load(fin, keyStoreFilePassword.toCharArray());
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                    kmf.init(ks, keyStoreFilePassword.toCharArray());
                    serverContext = SSLContext.getInstance("TLS");
                    serverContext.init(kmf.getKeyManagers(), (TrustManager[])null, (SecureRandom)null);
               } catch (Exception var8) {
                    throw new Error("Failed to initialize the server-side SSLContext", var8);
               }
          } catch (Exception var9) {
               if (logger.isErrorEnabled()) {
                    logger.error("Error initializing SslContextManager. " + var9.getMessage(), var9);
               }

               this._serverContext = serverContext;
               return;
          }

          this._serverContext = serverContext;
     }

     public SSLContext getServerContext() {
          return this._serverContext;
     }

     // $FF: synthetic method
     WebSocketSslServerSslContext(Object x0) {
          this();
     }

     private interface SingletonHolder {
          WebSocketSslServerSslContext INSTANCE = new WebSocketSslServerSslContext();
     }
}
