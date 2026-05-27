package bitzero.engine.util;

import bitzero.engine.exceptions.BitZeroEngineException;
import java.io.File;

public class FileServices {
     private static final String WINDOWS_SEPARATOR = "\\";
     private static final String UNIX_SEPARATOR = "/";

     public static void recursiveMakeDir(String basePath, String dirStructure) throws BitZeroEngineException {
          dirStructure.replace("\\", "/");
          String finalPath = basePath + (basePath.endsWith("/") ? "" : "/") + dirStructure;
          File folder = new File(finalPath);
          System.out.println("Creating: " + finalPath);
          if (!folder.exists()) {
               boolean ok = folder.mkdirs();
               if (!ok) {
                    throw new BitZeroEngineException("Was not able to create the following folder(s): " + dirStructure);
               }
          }
     }
}
