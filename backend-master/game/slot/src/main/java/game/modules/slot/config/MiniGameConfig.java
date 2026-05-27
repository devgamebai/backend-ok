package game.modules.slot.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.json.JSONObject;

public class MiniGameConfig {
     public JSONObject config;
     private static MiniGameConfig miniGameConfig = null;

     private MiniGameConfig() {
          this.initconfig();
     }

     public static MiniGameConfig instance() {
          if (miniGameConfig == null) {
               miniGameConfig = new MiniGameConfig();
          }

          return miniGameConfig;
     }

     public void initconfig() {
          String path = System.getProperty("user.dir");
          File file = new File(path + "/conf/slot.json");
          StringBuffer contents = new StringBuffer();
          BufferedReader reader = null;

          try {
               Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
               reader = new BufferedReader(r);
               String text = null;

               while((text = reader.readLine()) != null) {
                    contents.append(text).append(System.getProperty("line.separator"));
               }

               this.config = new JSONObject(contents.toString());
          } catch (Exception var7) {
               var7.printStackTrace();
          }

     }
}
