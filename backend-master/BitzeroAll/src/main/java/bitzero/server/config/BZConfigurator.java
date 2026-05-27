package bitzero.server.config;

import bitzero.server.exceptions.BZException;
import bitzero.server.exceptions.BZRuntimeException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vinplay.vbee.common.config.VBeePath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BZConfigurator implements IConfigurator {
     private final String BACKUP_FOLDER = "_backups";
     private volatile CoreSettings coreSettings;
     private volatile ServerSettings serverSettings;
     private volatile List zonesSettings;
     private final Logger log = LoggerFactory.getLogger(this.getClass());
     private static String basePath = VBeePath.basePath;

     public void loadConfiguration() throws FileNotFoundException {
          this.coreSettings = this.loadCoreSettings();
          this.serverSettings = this.loadServerSettings();
          if (this.serverSettings.webSocket == null) {
               this.serverSettings.webSocket = new ServerSettings.WebSocketEngineSettings();
          }

     }

     public CoreSettings getCoreSettings() {
          return this.coreSettings;
     }

     public synchronized ServerSettings getServerSettings() {
          return this.serverSettings;
     }

     public synchronized List getZoneSettings() {
          return this.zonesSettings;
     }

     public synchronized ZoneSettings getZoneSetting(String zoneName) {
          if (this.zonesSettings == null) {
               throw new IllegalStateException("No Zone configuration has been loaded yet!");
          } else {
               ZoneSettings settings = null;
               Iterator iterator = this.zonesSettings.iterator();

               while(iterator.hasNext()) {
                    ZoneSettings item = (ZoneSettings)iterator.next();
                    if (item.name.equals(zoneName)) {
                         settings = item;
                         break;
                    }
               }

               return settings;
          }
     }

     public synchronized ZoneSettings getZoneSetting(int id) {
          if (this.zonesSettings == null) {
               throw new IllegalStateException("No Zone configuration has been loaded yet!");
          } else {
               ZoneSettings settings = null;
               Iterator iterator = this.zonesSettings.iterator();

               while(iterator.hasNext()) {
                    ZoneSettings item = (ZoneSettings)iterator.next();
                    if (item.getId() == id) {
                         settings = item;
                         break;
                    }
               }

               return settings;
          }
     }

     public synchronized void removeZoneSetting(String name) throws IOException {
          ZoneSettings settings = this.getZoneSetting(name);
          if (settings != null) {
               String path = FilenameUtils.concat(basePath.concat("zones/"), settings.name + ".zone.xml");
               this.makeBackup(path);
               FileUtils.forceDelete(new File(path));
               this.zonesSettings.remove(settings);
          }

     }

     public synchronized List loadZonesConfiguration() throws BZException {
          this.zonesSettings = new ArrayList();
          List zoneDefinitionFiles = this.getZoneDefinitionFiles(basePath.concat("zones/"));
          Iterator iterator = zoneDefinitionFiles.iterator();

          while(iterator.hasNext()) {
               File file = (File)iterator.next();

               try {
                    FileInputStream inStream = new FileInputStream(file);
                    this.log.info("Loading: " + file.toString());
                    this.zonesSettings.add((ZoneSettings)this.getZonesXStreamDefinitions().fromXML(inStream));
               } catch (FileNotFoundException var5) {
                    throw new BZRuntimeException("Could not locate Zone definition file: " + file.getAbsolutePath());
               }
          }

          return this.zonesSettings;
     }

     public synchronized void saveServerSettings(boolean makeBackup) throws IOException {
          if (makeBackup) {
               this.makeBackup(basePath.concat("config/server.xml"));
          }

          OutputStream outStream = new FileOutputStream(basePath.concat("config/server.xml"));
          this.getServerXStreamDefinitions().toXML(this.serverSettings, outStream);
     }

     public synchronized void saveZoneSettings(ZoneSettings settings, boolean makeBackup) throws IOException {
          String filePath = FilenameUtils.concat(basePath.concat("zones/"), settings.name + ".zone.xml");
          if (makeBackup) {
               this.makeBackup(filePath);
          }

          OutputStream outStream = new FileOutputStream(filePath);
          this.getZonesXStreamDefinitions().toXML(settings, outStream);
     }

     public synchronized void saveNewZoneSettings(ZoneSettings settings) throws IOException {
          if (this.getZoneSetting(settings.name) != null) {
               throw new IllegalArgumentException("Save request failed. The new Zone name is already in use: " + settings.name);
          } else {
               this.saveZoneSettings(settings, false);
               this.zonesSettings.add(settings);
          }
     }

     public synchronized void saveZoneSettings(ZoneSettings zoneSettings, boolean makeBackup, String oldZoneName) throws IOException {
          String newFilePath = FilenameUtils.concat(basePath.concat("zones/"), zoneSettings.name + ".zone.xml");
          String oldFilePath = FilenameUtils.concat(basePath.concat("zones/"), oldZoneName + ".zone.xml");
          if (makeBackup) {
               this.makeBackup(oldFilePath);
          }

          OutputStream outStream = new FileOutputStream(newFilePath);
          this.getZonesXStreamDefinitions().toXML(zoneSettings, outStream);
          FileUtils.forceDelete(new File(oldFilePath));
     }

     private CoreSettings loadCoreSettings() throws FileNotFoundException {
          FileInputStream inStream = new FileInputStream(basePath.concat("config/core.xml"));
          XStream xstream = new XStream(new DomDriver());
          xstream.addPermission(com.thoughtworks.xstream.security.AnyTypePermission.ANY);
          xstream.alias("coreSettings", CoreSettings.class);
          return (CoreSettings)xstream.fromXML(inStream);
     }

     private ServerSettings loadServerSettings() throws FileNotFoundException {
          FileInputStream inStream = new FileInputStream(basePath.concat("config/server.xml"));
          return (ServerSettings)this.getServerXStreamDefinitions().fromXML(inStream);
     }

     private XStream getServerXStreamDefinitions() {
          XStream xstream = new XStream(new DomDriver());
          xstream.addPermission(com.thoughtworks.xstream.security.AnyTypePermission.ANY);
          xstream.alias("serverSettings", ServerSettings.class);
          xstream.alias("socket", ServerSettings.SocketAddress.class);
          xstream.useAttributeFor(ServerSettings.SocketAddress.class, "address");
          xstream.useAttributeFor(ServerSettings.SocketAddress.class, "port");
          xstream.useAttributeFor(ServerSettings.SocketAddress.class, "type");
          xstream.alias("ipFilter", ServerSettings.IpFilterSettings.class);
          xstream.alias("flashCrossdomainPolicy", ServerSettings.FlashCrossDomainPolicySettings.class);
          xstream.alias("remoteAdmin", ServerSettings.RemoteAdminSettings.class);
          xstream.alias("adminUser", ServerSettings.AdminUser.class);
          xstream.alias("mailer", ServerSettings.MailerSettings.class);
          xstream.alias("webServer", ServerSettings.WebServerSettings.class);
          xstream.alias("bannedUserManager", ServerSettings.BannedUserManagerSettings.class);
          return xstream;
     }

     private XStream getZonesXStreamDefinitions() {
          XStream xstream = new XStream(new DomDriver());
          xstream.addPermission(com.thoughtworks.xstream.security.AnyTypePermission.ANY);
          return xstream;
     }

     private List getZoneDefinitionFiles(String path) throws BZException {
          List files = new ArrayList();
          File currDir = new File(path);
          if (currDir.isDirectory()) {
               File[] afile;
               int j = (afile = currDir.listFiles()).length;

               for(int i = 0; i < j; ++i) {
                    File f = afile[i];
                    if (f.getName().endsWith(".zone.xml")) {
                         files.add(f);
                    }
               }

               return files;
          } else {
               throw new BZException("Invalid zones definition folder: " + currDir);
          }
     }

     private void makeBackup(String filePath) throws IOException {
          String basePath = FilenameUtils.getPath(filePath);
          String backupBasePath = FilenameUtils.concat(basePath, "_backups");
          DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm-ss");
          String backupId = (new DateTime()).toString(fmt);
          String backupFileName = FilenameUtils.concat(backupBasePath, backupId + "__" + FilenameUtils.getName(filePath));
          File sourceFile = new File(filePath);
          File backupFile = new File(backupFileName);
          File backupDir = new File(backupBasePath);
          if (!backupDir.isDirectory()) {
               FileUtils.forceMkdir(backupDir);
          }

          FileUtils.copyFile(sourceFile, backupFile);
     }
}
