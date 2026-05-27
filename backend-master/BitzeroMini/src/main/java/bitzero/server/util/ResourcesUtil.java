package bitzero.server.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ResourcesUtil {
     public static String readTextData(String resourceName) throws IOException {
          return new String(readBinaryData(resourceName));
     }

     public static byte[] readBinaryData(String resourceName) throws IOException {
          byte[] byteData = (byte[])null;
          InputStream is = ResourcesUtil.class.getClassLoader().getResourceAsStream(resourceName);
          if (is == null) {
               throw new FileNotFoundException("Resource '" + resourceName + "' was not found");
          } else {
               BufferedInputStream bis = null;
               ByteArrayOutputStream baos = null;
               baos = new ByteArrayOutputStream();
               bis = new BufferedInputStream(is);

               int read;
               while((read = bis.read()) != -1) {
                    baos.write(read);
               }

               try {
                    if (bis != null) {
                         bis.close();
                    }

                    if (baos != null) {
                         baos.close();
                    }
               } catch (Exception var8) {
               }

               try {
                    if (bis != null) {
                         bis.close();
                    }

                    if (baos != null) {
                         baos.close();
                    }
               } catch (Exception var7) {
               }

               return byteData;
          }
     }
}
