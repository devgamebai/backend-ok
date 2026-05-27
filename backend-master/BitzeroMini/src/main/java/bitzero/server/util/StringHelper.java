package bitzero.server.util;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class StringHelper {
     private static final String ASCII_FOLDER = "config/ascii/";
     private static final String ASCII_EXT = ".txt";

     public static void fillRight(StringBuilder sb, char c, int max) {
          sb.append(fillString(c, max));
     }

     public static void fillLeft(StringBuilder sb, char c, int max) {
          sb.insert(0, fillString(c, max));
     }

     private static String fillString(char c, int len) {
          StringBuilder sb = new StringBuilder();

          for(int i = 0; i < len; ++i) {
               sb.append(c);
          }

          return sb.toString();
     }

     public static String getAsciiMessage(String messageName) {
          String filePath = "config/ascii/" + messageName + ".txt";
          String asciiMessage = null;

          try {
               asciiMessage = FileUtils.readFileToString(new File(filePath));
          } catch (IOException var4) {
          }

          return asciiMessage;
     }
}
