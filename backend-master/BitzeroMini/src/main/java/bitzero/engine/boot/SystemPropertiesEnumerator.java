package bitzero.engine.boot;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import org.slf4j.Logger;

public class SystemPropertiesEnumerator {
     private static final String NEW_LINE = System.getProperty("line.separator");
     private static final String TAB = "\t";
     private static final String[] props = new String[]{"os.name", "os.arch", "os.version", "java.version", "java.vendor", "java.vendor.url", "java.vm.specification.version", "java.vm.version", "java.vm.vendor", "java.vm.name", "java.io.tmpdir"};

     public void logProperties(Logger logger) {
          this.logSystemInfo(logger);
          this.logNetCardsInfo(logger);
     }

     private void logSystemInfo(Logger logger) {
          StringBuilder sb = (new StringBuilder("System Info:")).append(NEW_LINE);
          Runtime rt = Runtime.getRuntime();
          sb.append("\t").append("Processor(s): ").append(rt.availableProcessors()).append(NEW_LINE);
          sb.append("\t").append("VM Max. memory: ").append(rt.maxMemory() / 1000000L).append("MB").append(NEW_LINE);
          String[] as;
          int j = (as = props).length;

          for(int i = 0; i < j; ++i) {
               String prop = as[i];
               sb.append("\t").append(prop).append(": ").append(System.getProperty(prop)).append(NEW_LINE);
          }

          sb.append("\t").append("Default charset: " + Charset.defaultCharset()).append(NEW_LINE);
          logger.info(sb.toString());
     }

     private void logNetCardsInfo(Logger logger) {
          StringBuilder sb = (new StringBuilder("Network Info:")).append(NEW_LINE);

          try {
               Enumeration list = NetworkInterface.getNetworkInterfaces();

               while(list.hasMoreElements()) {
                    NetworkInterface iFace = (NetworkInterface)list.nextElement();
                    sb.append("\t").append("Card: ").append(iFace.getDisplayName()).append(NEW_LINE);
                    Enumeration addresses = iFace.getInetAddresses();

                    while(addresses.hasMoreElements()) {
                         InetAddress adr = (InetAddress)addresses.nextElement();
                         sb.append("\t").append("\t").append(" ->").append(adr.getHostAddress()).append(NEW_LINE);
                    }
               }

               logger.info(sb.toString());
          } catch (SocketException var7) {
               logger.warn("Exception while discovering network cards: " + var7.getMessage());
          }

     }
}
