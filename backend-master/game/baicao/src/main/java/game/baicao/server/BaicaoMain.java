package game.baicao.server;

import bitzero.server.BitZeroServer;
import com.vinplay.vbee.common.config.VBeePath;

public class BaicaoMain {

     public static void main(String[] args) {

          // init base path
          VBeePath.initBasePath(BaicaoMain.class);

          boolean clusterMode = false;
          boolean useConsole = false;
          if (args.length > 0) {
               clusterMode = args[0].equalsIgnoreCase("cluster");
               useConsole = args.length > 1 && args[1].equalsIgnoreCase("console");
          }

          BitZeroServer bzServer = BitZeroServer.getInstance();
          bzServer.setClustered(clusterMode);
          if (useConsole) {
               bzServer.startDebugConsole();
          }

          bzServer.start();
     }
}
