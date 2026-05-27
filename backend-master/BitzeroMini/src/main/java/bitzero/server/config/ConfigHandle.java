package bitzero.server.config;

import bitzero.server.exceptions.ExceptionMessageComposer;
import bitzero.server.util.DebugConsole;
import com.vinplay.vbee.common.config.VBeePath;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigHandle {
     private static ConfigHandle _instance;
     private static final Object lock = new Object();
     private Random r = new Random(System.currentTimeMillis());
     private Properties props;
     private ConcurrentHashMap longPropsCaching;
     private ConcurrentHashMap listPropsCaching;
     private ConcurrentHashMap propsByGames;

     private ConfigHandle() {
          try {
               this.longPropsCaching = new ConcurrentHashMap();
               this.listPropsCaching = new ConcurrentHashMap();
               this.props = new Properties();
               String path = VBeePath.basePath;
               // Fallback: if basePath is empty, use working directory
               if (path == null || path.isEmpty()) {
                    path = System.getProperty("user.dir", "");
               }
               File configFile = new File(path + File.separator + "conf" + File.separator + "cluster.properties");
               System.out.println("[ConfigHandle] basePath='" + VBeePath.basePath + "' resolved='" + path + "' file=" + configFile.getAbsolutePath() + " exists=" + configFile.exists());
               this.props.load(new FileInputStream(configFile));
               System.out.println("[ConfigHandle] games=" + this.props.getProperty("games") + " dev_mod=" + this.props.getProperty("dev_mod"));
               this.propsByGames = new ConcurrentHashMap();
               String[] games = this.props.getProperty("games").split(";");

               for(int i = 0; i < games.length; ++i) {
                    Properties p = new Properties();
                    File f = new File(path + File.separator + "conf" + File.separator + games[i] + ".properties");
                    if (f.exists()) {
                         p.load(new FileInputStream(f));
                    }

                    p.setProperty("defaultScore", p.getProperty("defaultScore", "0"));
                    this.propsByGames.put(games[i], p);
               }
          } catch (Exception var5) {
               var5.printStackTrace();
               ExceptionMessageComposer msg = new ExceptionMessageComposer(var5);
               msg.setDescription("An error occurred during the Execution");
               DebugConsole.log.error(msg.toString());
          }

     }

     public static ConfigHandle instance() {
          if (_instance == null) {
               synchronized(lock) {
                    if (_instance == null) {
                         _instance = new ConfigHandle();
                    }
               }
          }

          return _instance;
     }

     public String get(String name) {
          return this.props != null ? this.props.getProperty(name) : null;
     }

     public Long getLong(String name) {
          Long result = (Long)this.longPropsCaching.get(name);
          if (result == null) {
               result = new Long(0L);

               try {
                    result = Long.parseLong(this.props.getProperty(name));
               } catch (Exception var5) {
                    var5.printStackTrace();
                    ExceptionMessageComposer msg = new ExceptionMessageComposer(var5);
                    msg.setDescription("An error occurred during the Execution");
                    DebugConsole.log.error(msg.toString());
               }

               this.longPropsCaching.put(name, result);
          }

          return result;
     }

     public Boolean getBoolean(String name) {
          return this.getLong(name) == 1L;
     }

     public Boolean getBoolean(String game, String name) {
          return this.getLong(game, name) == 1L;
     }

     public String getRandom(String name) {
          String[] data = (String[])this.listPropsCaching.get(name);
          if (data == null) {
               data = new String[0];

               try {
                    data = this.props.getProperty(name).split(";");
               } catch (Exception var5) {
                    ExceptionMessageComposer msg = new ExceptionMessageComposer(var5);
                    msg.setDescription("An error occurred during the Execution");
                    DebugConsole.log.error(msg.toString());
               }

               this.listPropsCaching.put(name, data);
          }

          return data.length > 0 ? data[this.r.nextInt(data.length)] : null;
     }

     public String get(String game, String name) {
          return this.propsByGames.get(game) != null ? ((Properties)this.propsByGames.get(game)).getProperty(name) : null;
     }

     public Long getLong(String game, String name) {
          Long result = null;

          try {
               result = Long.parseLong(((Properties)this.propsByGames.get(game)).getProperty(name));
          } catch (Exception var6) {
               var6.printStackTrace();
               ExceptionMessageComposer msg = new ExceptionMessageComposer(var6);
               msg.setDescription("An error occurred during the Execution");
               DebugConsole.log.error(msg.toString());
          }

          return result;
     }

     public int getInt(String game, String name) {
          int result = 0;

          try {
               result = Integer.parseInt(((Properties)this.propsByGames.get(game)).getProperty(name).trim());
          } catch (Exception var6) {
               var6.printStackTrace();
               ExceptionMessageComposer msg = new ExceptionMessageComposer(var6);
               msg.setDescription("An error occurred during the Execution");
               DebugConsole.log.error(msg.toString());
          }

          return result;
     }
}
