package bitzero.server.extensions;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.api.IBZApi;
import bitzero.server.api.response.ResponseApi;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.extensions.data.BaseMsg;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseBZExtension implements IBZExtension, IBZEventListener {
     private String name;
     private String fileName;
     private String configFileName;
     private ExtensionLevel level;
     private ExtensionType type;
     private volatile boolean active = true;
     protected final BitZeroServer bz = BitZeroServer.getInstance();
     private Properties configProperties;
     private ExtensionReloadMode reloadMode;
     private String currentPath;
     protected volatile int lagSimulationMillis = 0;
     private final Logger logger = LoggerFactory.getLogger("Extensions");
     protected final ResponseApi resApi;
     protected final IBZApi bzApi;

     public BaseBZExtension() {
          this.resApi = this.bz.getAPIManager().getResApi();
          this.bzApi = this.bz.getAPIManager().getBzApi();
     }

     public String getCurrentFolder() {
          return this.currentPath;
     }

     public void monitor() {
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          if (this.name != null) {
               throw new BZRuntimeException("Cannot redefine name of extension: " + this.toString());
          } else {
               this.name = name;
               this.currentPath = "extensions/" + name + "/";
          }
     }

     public IBZApi getApi() {
          return this.bzApi;
     }

     public String getExtensionFileName() {
          return this.fileName;
     }

     public Properties getConfigProperties() {
          return this.configProperties;
     }

     public String getPropertiesFileName() {
          return this.configFileName;
     }

     public void setPropertiesFileName(String fileName) throws IOException {
          if (this.configFileName != null) {
               throw new BZRuntimeException("Cannot redefine properties file name of an extension: " + this.toString());
          } else {
               boolean isDefault = false;
               if (fileName != null && fileName.length() != 0 && !fileName.equals("config.properties")) {
                    this.configFileName = fileName;
               } else {
                    isDefault = true;
                    this.configFileName = "config.properties";
               }

               String fileToLoad = "extensions/" + this.name + "/" + this.configFileName;
               if (isDefault) {
                    this.loadDefaultConfigFile(fileToLoad);
               } else {
                    this.loadCustomConfigFile(fileToLoad);
               }

          }
     }

     public void handleServerEvent(IBZEvent ibzevent) throws Exception {
     }

     public Object handleInternalMessage(String cmdName, Object params) {
          return null;
     }

     private void loadDefaultConfigFile(String fileName) {
          this.configProperties = new Properties();

          try {
               this.configProperties.load(new FileInputStream(fileName));
          } catch (IOException var3) {
          }

     }

     private void loadCustomConfigFile(String fileName) throws IOException {
          this.configProperties = new Properties();
          this.configProperties.load(new FileInputStream(fileName));
     }

     public void setExtensionFileName(String fileName) {
          if (this.fileName != null) {
               throw new BZRuntimeException("Cannot redefine file name of an extension: " + this.toString());
          } else {
               this.fileName = fileName;
          }
     }

     public void addEventListener(IBZEventType eventType, IBZEventListener listener) {
          this.bz.getEventManager().addEventListener(eventType, listener);
     }

     public void removeEventListener(IBZEventType eventType, IBZEventListener listener) {
          this.bz.getEventManager().removeEventListener(eventType, listener);
     }

     public boolean isActive() {
          return this.active;
     }

     public void setActive(boolean flag) {
          this.active = flag;
     }

     public ExtensionLevel getLevel() {
          return this.level;
     }

     public void setLevel(ExtensionLevel level) {
          if (this.level != null) {
               throw new BZRuntimeException("Cannot change level for extension: " + this.toString());
          } else {
               this.level = level;
          }
     }

     public void sendExceptMe(BaseMsg bmsg, List listUser, User u) {
          listUser.remove(u);
          ArrayList recipients = null;
          recipients = new ArrayList();
          Iterator iterator = listUser.iterator();

          while(iterator.hasNext()) {
               ISession session = ((User)iterator.next()).getSession();
               recipients.add(session);
          }

          this.resApi.sendExtResponse(bmsg, (Collection)recipients);
     }

     public void sendExceptMe(BaseMsg bmsg, List listSS, ISession ss) {
          listSS.remove(ss);
          this.resApi.sendExtResponse(bmsg, (Collection)listSS);
     }

     public void sendUsers(BaseMsg bmsg, List listUser) {
          Collection recipients = null;
          recipients = new ArrayList();
          Iterator iterator = listUser.iterator();

          while(iterator.hasNext()) {
               ISession session = ((User)iterator.next()).getSession();
               recipients.add(session);
          }

          this.resApi.sendExtResponse(bmsg, (Collection)recipients);
     }

     public void send(BaseMsg bmsg, Collection recipients) {
          this.resApi.sendExtResponse(bmsg, recipients);
     }

     public void send(BaseMsg bmsg, User recipient) {
          this.resApi.sendExtResponse(bmsg, recipient);
     }

     public void send(BaseMsg bmsg, List recipients) {
          Iterator var3 = recipients.iterator();

          while(var3.hasNext()) {
               User recipient = (User)var3.next();
               this.resApi.sendExtResponse(bmsg, recipient);
          }

     }

     public void send(BaseMsg bmsg, ISession recipient) {
          this.resApi.sendExtResponse(bmsg, recipient);
     }

     public String toString() {
          return String.format("{ Ext: %s, Type: %s, Lev: %s, %s, %s }", this.name, this.type, this.level, "{}");
     }

     public void trace(Object... args) {
          this.logger.info(this.getTraceMessage(args));
     }

     public void trace(ExtensionLogLevel level, Object... args) {
          if (level == ExtensionLogLevel.DEBUG) {
               this.logger.debug(this.getTraceMessage(args));
          } else if (level == ExtensionLogLevel.INFO) {
               this.logger.info(this.getTraceMessage(args));
          } else if (level == ExtensionLogLevel.WARN) {
               this.logger.warn(this.getTraceMessage(args));
          } else if (level == ExtensionLogLevel.ERROR) {
               this.logger.error(this.getTraceMessage(args));
          }

     }

     private String getTraceMessage(Object... args) {
          StringBuilder traceMsg = (new StringBuilder()).append("{").append(this.name).append("}: ");
          Object[] aobj = args;
          int j = args.length;

          for(int i = 0; i < j; ++i) {
               Object o = aobj[i];
               traceMsg.append(o.toString()).append(" ");
          }

          return traceMsg.toString();
     }

     protected void removeEventsForListener(IBZEventListener listener) {
     }

     private void checkLagSimulation() {
          if (this.lagSimulationMillis > 0) {
               try {
                    this.logger.debug("Lag simulation, sleeping for: " + this.lagSimulationMillis + "ms.");
                    Thread.sleep((long)this.lagSimulationMillis);
               } catch (InterruptedException var2) {
                    this.logger.warn("Interruption during lag simulation: " + var2);
               }
          }

     }
}
