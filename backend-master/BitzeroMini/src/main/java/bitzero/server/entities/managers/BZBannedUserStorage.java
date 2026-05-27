package bitzero.server.entities.managers;

import bitzero.server.exceptions.ExceptionMessageComposer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZBannedUserStorage implements IBannedUserStorage {
     private static final String DATA_FOLDER = "data/bannedusers/";
     private static final String DATA_FILE = "users.bin";
     private final Logger log = LoggerFactory.getLogger(this.getClass());
     private volatile boolean isProperlyInited = false;

     public void init() {
          try {
               this.checkFolderStructure();
               this.isProperlyInited = true;
               this.log.info("BanUserStorage initialized");
          } catch (IOException var5) {
               ExceptionMessageComposer emc = new ExceptionMessageComposer(var5);
               emc.setDescription("Unable to initialize the Banned User Manage storage.");
               File fullPath = new File("data/bannedusers/");
               String thePath = fullPath == null ? "Unable to obtain path. Your installation might be corrupted." : fullPath.getAbsolutePath();
               emc.setPossibleCauses("Write permissions are probably not available in: " + thePath);
               if (fullPath == null) {
                    emc.addInfo("An additional problem was found: BitZeroServer is not able to determine the absolute path of the storage folder. Should be {your-bitzero-folder}/data/bannedusers/");
               }

               this.log.warn(emc.toString());
          }

     }

     public void destroy() {
     }

     public BanUserData load() throws Exception {
          this.checkInited();
          String dataFile = "data/bannedusers/users.bin";
          FileInputStream fileInStream = new FileInputStream(dataFile);
          ObjectInputStream objInStream = new ObjectInputStream(fileInStream);
          BanUserData banUserData = (BanUserData)objInStream.readObject();
          objInStream.close();
          return banUserData;
     }

     public synchronized void save(BanUserData data) throws IOException {
          this.checkInited();
          String dataFile = "data/bannedusers/users.bin";
          FileOutputStream fileOStream = new FileOutputStream(dataFile);
          ObjectOutputStream oStream = new ObjectOutputStream(fileOStream);
          oStream.writeObject(data);
          oStream.flush();
          oStream.close();
     }

     private void checkFolderStructure() throws IOException {
          String folderName = "data/bannedusers/";
          File targetFolder = new File(folderName);
          if (!targetFolder.isDirectory()) {
               FileUtils.forceMkdir(targetFolder);
          }

     }

     private void checkInited() {
          if (!this.isProperlyInited) {
               throw new IllegalStateException("Banned User storage class cannot operate correctly because initialization failed. Please check your startup logs. ");
          }
     }
}
