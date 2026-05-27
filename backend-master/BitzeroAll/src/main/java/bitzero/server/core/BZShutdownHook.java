package bitzero.server.core;

import bitzero.server.BitZeroServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZShutdownHook extends Thread {
     private final Logger log = LoggerFactory.getLogger(this.getClass());

     public BZShutdownHook() {
          super("BZ-Alpha ShutdownHook");
     }

     public void run() {
          this.log.warn("BZ-Alpha is shutting down. The process may take a few seconds...");
          BitZeroServer.getInstance().getExtensionManager().destroy();
          System.out.println("BitZeroServer is stoped");
     }
}
